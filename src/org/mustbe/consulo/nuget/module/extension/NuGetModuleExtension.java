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

package org.mustbe.consulo.nuget.module.extension;

import org.consulo.lombok.annotations.LazyInstance;
import org.consulo.module.extension.impl.ModuleExtensionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.nuget.NuGetFileTypeFactory;
import org.mustbe.consulo.nuget.dom.NuGetPackagesFile;
import com.intellij.openapi.roots.ModuleRootLayer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetModuleExtension extends ModuleExtensionImpl<NuGetModuleExtension>
{
	public NuGetModuleExtension(@NotNull String id, @NotNull ModuleRootLayer moduleRootLayer)
	{
		super(id, moduleRootLayer);
	}

	@NotNull
	@LazyInstance
	public NuGetRepositoryWorker getWorker()
	{
		return new NuGetRepositoryWorker(NuGetModuleExtension.this);
	}

	@Nullable
	public NuGetPackagesFile getPackagesFile()
	{
		VirtualFile moduleDir = getModule().getModuleDir();
		if(moduleDir == null)
		{
			return null;
		}
		VirtualFile fileByRelativePath = moduleDir.findFileByRelativePath(NuGetFileTypeFactory.PACKAGES_CONFIG);
		if(fileByRelativePath == null)
		{
			return null;
		}
		PsiFile maybeXmlFile = PsiManager.getInstance(getProject()).findFile(fileByRelativePath);
		if(!(maybeXmlFile instanceof XmlFile))
		{
			return null;
		}
		DomFileElement<NuGetPackagesFile> fileElement = DomManager.getDomManager(getProject()).getFileElement((XmlFile) maybeXmlFile,
				NuGetPackagesFile.class);
		return fileElement == null ? null : fileElement.getRootElement();
	}
}
