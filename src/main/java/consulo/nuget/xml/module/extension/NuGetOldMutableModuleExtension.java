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

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.language.XmlFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetOldMutableModuleExtension extends NuGetOldModuleExtension implements MutableModuleExtension<NuGetOldModuleExtension> {
    public NuGetOldMutableModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    public void setConfigFilePath(String path) {
        if (StringUtil.isEmptyOrSpaces(path)) {
            myConfigFileUrl = null;
        } else {
            myConfigFileUrl = VirtualFileManager.constructUrl(StandardFileSystems.FILE_PROTOCOL, path);
        }
    }

    @RequiredUIAccess
    @Nullable
    @Override
    public Component createConfigurationComponent(@Nonnull Disposable disposable, @Nonnull Runnable runnable) {
        FileChooserTextBoxBuilder.Controller configFileBox = FileChooserTextBoxBuilder.create(getProject())
                .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFileDescriptor(XmlFileType.INSTANCE))
                .dialogTitle(LocalizeValue.localizeTODO("Select File"))
                .dialogDescription(LocalizeValue.localizeTODO("Select NuGet package config file"))
                .uiDisposable(disposable)
                .build();

        String defaultConfigPath = VirtualFileUtil.urlToPath(getModule().getModuleDirUrl() + "/" + PACKAGES_CONFIG);
        configFileBox.getComponent().setPlaceholder(LocalizeValue.of(defaultConfigPath));

        String configFileUrl = getConfigFileUrl();
        if (!StringUtil.isEmpty(configFileUrl)) {
            configFileBox.setValue(FileUtil.toSystemDependentName(VirtualFileUtil.urlToPath(configFileUrl)), false);
        }

        configFileBox.getComponent().addValueListener(event -> setConfigFilePath(FileUtil.toSystemIndependentName(configFileBox.getValue())));

        FormBuilder formBuilder = FormBuilder.create();
        formBuilder.addLabeled(LocalizeValue.localizeTODO("Config file:"), configFileBox.getComponent());
        return formBuilder.build();
    }

    @Override
    public void setEnabled(boolean b) {
        myIsEnabled = b;
    }

    @Override
    public boolean isModified(@Nonnull NuGetOldModuleExtension nuGetModuleExtension) {
        return myIsEnabled != nuGetModuleExtension.isEnabled() || !Comparing.equal(myConfigFileUrl, nuGetModuleExtension.myConfigFileUrl);
    }
}
