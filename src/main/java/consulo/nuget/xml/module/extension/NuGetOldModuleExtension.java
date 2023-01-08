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

package consulo.nuget.xml.module.extension;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.NotNullLazyValue;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionBase;
import consulo.nuget.module.extension.NuGetModuleExtension;
import consulo.nuget.module.extension.NuGetPackageInfo;
import consulo.nuget.xml.dom.NuGetXmlPackage;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomManager;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetOldModuleExtension extends ModuleExtensionBase<NuGetOldModuleExtension> implements NuGetModuleExtension<NuGetOldModuleExtension>
{
	public static final String PACKAGES_CONFIG = "packages.config";

	private final NotNullLazyValue<NuGetRepositoryWorker> myWorkerValue = NotNullLazyValue.createValue(() -> new NuGetRepositoryWorker(this));
	protected String myConfigFileUrl;

	public NuGetOldModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer)
	{
		super(id, moduleRootLayer);
	}

	@Override
	public void update()
	{
		getWorker().forceUpdate();
	}

	@Override
	public void installPackage(NuGetPackageInfo packageInfo)
	{
		WriteCommandAction.runWriteCommandAction(getProject(), () -> {
			NuGetXmlPackagesFile packagesFile = getPackagesFile();
			if(packagesFile == null)
			{
				return;
			}

			NuGetXmlPackage nuGetXmlPackage = packagesFile.addPackage();

			nuGetXmlPackage.getId().setValue(packageInfo.id());
			nuGetXmlPackage.getVersion().setValue(packageInfo.version());
			nuGetXmlPackage.getTargetFramework().setValue(packageInfo.framework());
		});
	}

	@Nonnull
	@Override
	public List<NuGetPackageInfo> getInstalledPackages()
	{
		NuGetXmlPackagesFile packagesFile = getPackagesFile();
		if(packagesFile == null)
		{
			return List.of();
		}
		
		List<NuGetPackageInfo> packageInfos = new ArrayList<>();
		for(NuGetXmlPackage xmlPackage : packagesFile.getPackages())
		{
			packageInfos.add(new NuGetPackageInfo(xmlPackage.getId().getValue(), xmlPackage.getTargetFramework().getValue(), xmlPackage.getTargetFramework().getValue()));
		}
		return packageInfos;
	}

	@RequiredReadAction
	@Override
	public void commit(@Nonnull NuGetOldModuleExtension mutableModuleExtension)
	{
		super.commit(mutableModuleExtension);
		myConfigFileUrl = StringUtil.nullize(mutableModuleExtension.myConfigFileUrl, true);
	}

	@Override
	protected void getStateImpl(@Nonnull Element element)
	{
		if(myConfigFileUrl != null)
		{
			element.setAttribute("config-file-url", myConfigFileUrl);
		}
	}

	@RequiredReadAction
	@Override
	protected void loadStateImpl(@Nonnull Element element)
	{
		myConfigFileUrl = element.getAttributeValue("config-file-url");
	}

	@Nonnull
	public NuGetRepositoryWorker getWorker()
	{
		return myWorkerValue.getValue();
	}

	public String getConfigFileUrl()
	{
		return myConfigFileUrl;
	}

	@Nullable
	public VirtualFile getConfigFile()
	{
		if(StringUtil.isEmpty(myConfigFileUrl))
		{
			VirtualFile moduleDir = getModule().getModuleDir();
			if(moduleDir == null)
			{
				return null;
			}
			return moduleDir.findFileByRelativePath(PACKAGES_CONFIG);
		}
		return VirtualFileManager.getInstance().findFileByUrl(myConfigFileUrl);
	}

	@Nullable
	public NuGetXmlPackagesFile getPackagesFile()
	{
		VirtualFile fileByRelativePath = getConfigFile();
		if(fileByRelativePath == null)
		{
			return null;
		}
		PsiFile maybeXmlFile = PsiManager.getInstance(getProject()).findFile(fileByRelativePath);
		if(!(maybeXmlFile instanceof XmlFile))
		{
			return null;
		}
		DomFileElement<NuGetXmlPackagesFile> fileElement = DomManager.getDomManager(getProject()).getFileElement((XmlFile) maybeXmlFile, NuGetXmlPackagesFile.class);
		return fileElement == null ? null : fileElement.getRootElement();
	}
}
