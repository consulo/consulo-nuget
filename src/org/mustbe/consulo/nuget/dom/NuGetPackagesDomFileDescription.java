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

package org.mustbe.consulo.nuget.dom;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.nuget.NuGetFileTypeFactory;
import org.mustbe.consulo.nuget.module.extension.NuGetModuleExtension;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;
import icons.NuGetIcons;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetPackagesDomFileDescription extends DomFileDescription<NuGetPackagesFile>
{
	public NuGetPackagesDomFileDescription()
	{
		super(NuGetPackagesFile.class, "packages");
	}

	@Override
	public boolean isMyFile(@NotNull XmlFile file, @Nullable Module module)
	{
		String name = file.getVirtualFile().getName();
		if(!Comparing.equal(NuGetFileTypeFactory.PACKAGES_CONFIG, name))
		{
			return false;
		}

		Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(file);
		if(moduleForPsiElement == null)
		{
			return false;
		}
		return ModuleUtil.getExtension(moduleForPsiElement, NuGetModuleExtension.class) != null;
	}

	@Nullable
	@Override
	public Icon getFileIcon(@Iconable.IconFlags int flags)
	{
		return NuGetIcons.NuGet;
	}
}
