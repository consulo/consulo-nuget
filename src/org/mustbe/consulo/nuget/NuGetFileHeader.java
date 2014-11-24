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

import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.nuget.dom.NuGetPackagesFile;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetFileHeader extends EditorNotifications.Provider<EditorNotificationPanel>
{
	private static final Key<EditorNotificationPanel> KEY = Key.create("nuget-file-header");
	private final Project myProject;

	public NuGetFileHeader(Project project)
	{
		myProject = project;
	}

	@Override
	public Key<EditorNotificationPanel> getKey()
	{
		return KEY;
	}

	@Nullable
	@Override
	public EditorNotificationPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor)
	{
		if(file.getFileType() != XmlFileType.INSTANCE)
		{
			return null;
		}

		PsiFile maybeXmlFile = PsiManager.getInstance(myProject).findFile(file);
		if(!(maybeXmlFile instanceof XmlFile))
		{
			return null;
		}
		DomFileElement<NuGetPackagesFile> fileElement = DomManager.getDomManager(myProject).getFileElement((XmlFile) maybeXmlFile,
				NuGetPackagesFile.class);
		if(fileElement != null)
		{
			EditorNotificationPanel editorNotificationPanel = new EditorNotificationPanel();
			editorNotificationPanel.createActionLabel("Update dependencies", new Runnable()
			{
				@Override
				public void run()
				{
				}
			});
			return editorNotificationPanel;
		}
		return null;
	}
}
