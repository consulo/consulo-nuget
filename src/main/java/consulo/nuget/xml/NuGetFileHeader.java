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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.nuget.xml.module.extension.NuGetOldModuleExtension;
import consulo.nuget.xml.module.extension.NuGetOldMutableModuleExtension;
import consulo.nuget.xml.module.extension.NuGetRepositoryWorker;
import consulo.nuget.xml.module.extension.NuGetXmlPackagesFile;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.ide.highlighter.XmlFileType;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 24.11.14
 */
@ExtensionImpl
public class NuGetFileHeader implements EditorNotificationProvider {
    private final Project myProject;

    @Inject
    public NuGetFileHeader(Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public String getId() {
        return "nuget-config-file";
    }

    @RequiredReadAction
    @Nullable
    @Override
    public EditorNotificationBuilder buildNotification(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor, @Nonnull Supplier<EditorNotificationBuilder> supplier) {
        if (file.getFileType() != XmlFileType.INSTANCE) {
            return null;
        }

        final Module moduleForPsiElement = ModuleUtilCore.findModuleForFile(file, myProject);
        if (moduleForPsiElement == null) {
            return null;
        }

        NuGetOldModuleExtension extension = ModuleUtilCore.getExtension(moduleForPsiElement, NuGetOldModuleExtension.class);
        if (extension == null) {
            return null;
        }

        if (!Comparing.equal(file, extension.getConfigFile())) {
            return null;
        }

        NuGetXmlPackagesFile packagesFile = extension.getPackagesFile();
        if (packagesFile == null) {
            return null;
        }

        EditorNotificationBuilder builder = supplier.get();
        builder.withText(LocalizeValue.localizeTODO("NuGet"));

        final NuGetRepositoryWorker worker = extension.getWorker();

        if (!worker.isUpdateInProgress()) {
            builder.withAction(LocalizeValue.localizeTODO("Update Packages"), (e) -> worker.forceUpdate());
            builder.withAction(LocalizeValue.localizeTODO("Manage Packages"), "NuGet.ManageNuGetPackages");
            builder.withAction(LocalizeValue.localizeTODO("Remove NuGet Support"), e -> {

                final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(moduleForPsiElement).getModifiableModel();

                worker.cancelTasks();

                NuGetOldMutableModuleExtension mutableModuleExtension = modifiableModel.getExtension(NuGetOldMutableModuleExtension.class);
                assert mutableModuleExtension != null;
                mutableModuleExtension.setEnabled(false);

                ApplicationManager.getApplication().runWriteAction(() -> {
                    modifiableModel.commit();
                    EditorNotifications.updateAll();
                });
            });
            return builder;
        }
        return null;
    }
}
