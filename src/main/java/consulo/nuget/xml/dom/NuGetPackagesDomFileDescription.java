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

package consulo.nuget.xml.dom;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.Iconable;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.nuget.icon.NuGetIconGroup;
import consulo.nuget.xml.module.extension.NuGetOldModuleExtension;
import consulo.nuget.xml.module.extension.NuGetXmlPackagesFile;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomFileDescription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 24.11.14
 */
@ExtensionImpl
public class NuGetPackagesDomFileDescription extends DomFileDescription<NuGetXmlPackagesFile>
{
	public NuGetPackagesDomFileDescription()
	{
		super(NuGetXmlPackagesFile.class, "packages");
	}

	@Override
	public boolean isMyFile(@Nonnull XmlFile file)
	{
		VirtualFile virtualFile = file.getVirtualFile();
		if(virtualFile == null)
		{
			return false;
		}

		Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(file);
		if(moduleForPsiElement == null)
		{
			return false;
		}
		NuGetOldModuleExtension nuGetModuleExtension = ModuleUtilCore.getExtension(moduleForPsiElement, NuGetOldModuleExtension.class);
		return nuGetModuleExtension != null && Comparing.equal(nuGetModuleExtension.getConfigFile(), virtualFile);
	}

	@Nullable
	@Override
	public Image getFileIcon(@Iconable.IconFlags int flags)
	{
		return NuGetIconGroup.nuget();
	}
}
