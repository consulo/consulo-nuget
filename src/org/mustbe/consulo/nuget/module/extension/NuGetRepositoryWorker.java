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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.nuget.dom.NuGetPackage;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.val;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetRepositoryWorker extends NuGetBasedRepositoryWorker
{
	private final NuGetModuleExtension myExtension;

	public NuGetRepositoryWorker(NuGetModuleExtension extension)
	{
		super(extension.getModule());
		myExtension = extension;
	}

	@Nullable
	@Override
	protected VirtualFile getPackagesDir()
	{
		VirtualFile moduleDir = myModule.getModuleDir();
		if(moduleDir == null)
		{
			return null;
		}
		return moduleDir.findFileByRelativePath(PACKAGES_DIR);
	}

	@NotNull
	@Override
	protected Map<String, PackageInfo> getPackagesInfo()
	{
		val packagesFile = myExtension.getPackagesFile();
		if(packagesFile == null)
		{
			return Collections.emptyMap();
		}

		Map<String, PackageInfo> map = new TreeMap<String, PackageInfo>();
		for(NuGetPackage nuGetPackage : packagesFile.getPackages())
		{
			String idValue = nuGetPackage.getId().getValue();
			String versionValue = nuGetPackage.getVersion().getValue();
			String targetFrameworkValue = nuGetPackage.getTargetFramework().getValue();
			if(idValue == null || versionValue == null || targetFrameworkValue == null)
			{
				continue;
			}
			map.put(idValue + "." + versionValue, new PackageInfo(idValue, versionValue, targetFrameworkValue));
		}
		return map;
	}
}
