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

package org.mustbe.consulo.nuget;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.editor.notifications.EditorNotificationProvider;
import org.mustbe.consulo.nuget.dom.NuGetPackagesFile;
import org.mustbe.consulo.nuget.module.extension.NuGetModuleExtension;
import org.mustbe.consulo.nuget.module.extension.NuGetMutableModuleExtension;
import org.mustbe.consulo.nuget.module.extension.NuGetRepositoryWorker;
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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetFileHeader implements EditorNotificationProvider<EditorNotificationPanel>
{
	private static final Key<EditorNotificationPanel> KEY = Key.create("nuget-file-header");
	private final Project myProject;

	public NuGetFileHeader(Project project)
	{
		myProject = project;
	}

	@NotNull
	@Override
	public Key<EditorNotificationPanel> getKey()
	{
		return KEY;
	}

	@RequiredReadAction
	@Nullable
	@Override
	public EditorNotificationPanel createNotificationPanel(@NotNull final VirtualFile file, @NotNull FileEditor fileEditor)
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

		NuGetModuleExtension extension = ModuleUtil.getExtension(moduleForPsiElement, NuGetModuleExtension.class);
		if(extension == null)
		{
			return null;
		}

		if(!Comparing.equal(file, extension.getConfigFile()))
		{
			return null;
		}

		NuGetPackagesFile packagesFile = extension.getPackagesFile();
		if(packagesFile == null)
		{
			return null;
		}

		EditorNotificationPanel editorNotificationPanel = new EditorNotificationPanel();
		editorNotificationPanel.setText("NuGet");

		final NuGetRepositoryWorker worker = extension.getWorker();

		if(!worker.isUpdateInProgress())
		{
			editorNotificationPanel.createActionLabel("Update dependencies", new Runnable()
			{
				@Override
				public void run()
				{
					worker.forceUpdate();
				}
			});
			editorNotificationPanel.createActionLabel("Remove NuGet support", new Runnable()
			{
				@Override
				@RequiredDispatchThread
				public void run()
				{
					final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(moduleForPsiElement).getModifiableModel();

					worker.cancelTasks();

					NuGetMutableModuleExtension mutableModuleExtension = modifiableModel.getExtension(NuGetMutableModuleExtension.class);
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
