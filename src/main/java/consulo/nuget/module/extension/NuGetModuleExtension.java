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

package consulo.nuget.module.extension;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import consulo.annotation.access.RequiredReadAction;
import consulo.module.extension.impl.ModuleExtensionImpl;
import consulo.nuget.dom.NuGetPackagesFile;
import consulo.roots.ModuleRootLayer;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetModuleExtension extends ModuleExtensionImpl<NuGetModuleExtension>
{
	public static final String PACKAGES_CONFIG = "packages.config";

	private final NotNullLazyValue<NuGetRepositoryWorker> myWorkerValue = NotNullLazyValue.createValue(() -> new NuGetRepositoryWorker(this));
	protected String myConfigFileUrl;

	public NuGetModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer)
	{
		super(id, moduleRootLayer);
	}

	@RequiredReadAction
	@Override
	public void commit(@Nonnull NuGetModuleExtension mutableModuleExtension)
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
	public NuGetPackagesFile getPackagesFile()
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
		DomFileElement<NuGetPackagesFile> fileElement = DomManager.getDomManager(getProject()).getFileElement((XmlFile) maybeXmlFile, NuGetPackagesFile.class);
		return fileElement == null ? null : fileElement.getRootElement();
	}
}
