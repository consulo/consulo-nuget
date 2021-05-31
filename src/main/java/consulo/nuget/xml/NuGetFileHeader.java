/*
 * Copyright 2013-2014 must-be.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.nuget.xml;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import consulo.annotation.access.RequiredReadAction;
import consulo.editor.notifications.EditorNotificationProvider;
import consulo.nuget.xml.module.extension.NuGetXmlPackagesFile;
import consulo.nuget.xml.module.extension.NuGetOldModuleExtension;
import consulo.nuget.xml.module.extension.NuGetOldMutableModuleExtension;
import consulo.nuget.xml.module.extension.NuGetRepositoryWorker;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetFileHeader implements EditorNotificationProvider<EditorNotificationPanel>
{
	private final Project myProject;

	public NuGetFileHeader(Project project)
	{
		myProject = project;
	}

	@RequiredReadAction
	@Nullable
	@Override
	public EditorNotificationPanel createNotificationPanel(@Nonnull final VirtualFile file, @Nonnull FileEditor fileEditor)
	{
		if(file.getFileType() != XmlFileType.INSTANCE)
		{
			return null;
		}

		final Module moduleForPsiElement = ModuleUtilCore.findModuleForFile(file, myProject);
		if(moduleForPsiElement == null)
		{
			return null;
		}

		NuGetOldModuleExtension extension = ModuleUtil.getExtension(moduleForPsiElement, NuGetOldModuleExtension.class);
		if(extension == null)
		{
			return null;
		}

		if(!Comparing.equal(file, extension.getConfigFile()))
		{
			return null;
		}

		NuGetXmlPackagesFile packagesFile = extension.getPackagesFile();
		if(packagesFile == null)
		{
			return null;
		}

		EditorNotificationPanel editorNotificationPanel = new EditorNotificationPanel();
		editorNotificationPanel.setText("NuGet");

		final NuGetRepositoryWorker worker = extension.getWorker();

		if(!worker.isUpdateInProgress())
		{
			editorNotificationPanel.createActionLabel("Update dependencies", () -> worker.forceUpdate());
			editorNotificationPanel.createActionLabel("Remove NuGet support", new Runnable()
			{
				@Override
				@RequiredUIAccess
				public void run()
				{
					final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(moduleForPsiElement).getModifiableModel();

					worker.cancelTasks();

					NuGetOldMutableModuleExtension mutableModuleExtension = modifiableModel.getExtension(NuGetOldMutableModuleExtension.class);
					assert mutableModuleExtension != null;
					mutableModuleExtension.setEnabled(false);

					ApplicationManager.getApplication().runWriteAction(new Runnable()
					{
						@Override
						public void run()
						{
							modifiableModel.commit();
							EditorNotifications.updateAll();
						}
					});
				}
			});
			return editorNotificationPanel;
		}
		return null;
	}
}
