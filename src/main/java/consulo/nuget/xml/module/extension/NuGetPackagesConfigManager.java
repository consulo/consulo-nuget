/*
 * Copyright 2013-2026 consulo.io
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
import consulo.annotation.component.ExtensionImpl;
import consulo.dotnet.packageManager.DotNetPackageManager;
import consulo.dotnet.packageManager.DotNetPackageReference;
import consulo.language.editor.WriteCommandAction;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.nuget.xml.dom.NuGetXmlPackage;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The packages of a module with a packages.config: an entry is written, then the packages are downloaded into the module.
 */
@ExtensionImpl
public class NuGetPackagesConfigManager implements DotNetPackageManager {
    // the framework of an entry when no other entry tells the one of the module
    private static final String DEFAULT_FRAMEWORK = "net45";

    @RequiredReadAction
    @Override
    public boolean isApplicable(Module module) {
        return ModuleUtilCore.getExtension(module, NuGetOldModuleExtension.class) != null;
    }

    @RequiredReadAction
    @Override
    public List<DotNetPackageReference> getPackages(Module module) {
        NuGetXmlPackagesFile packagesFile = findPackagesFile(module);
        if (packagesFile == null) {
            return List.of();
        }
        List<DotNetPackageReference> references = new ArrayList<>();
        for (NuGetXmlPackage xmlPackage : packagesFile.getPackages()) {
            String id = xmlPackage.getId().getValue();
            if (id != null && !id.isBlank()) {
                references.add(new DotNetPackageReference(id.trim(), xmlPackage.getVersion().getValue()));
            }
        }
        return references;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> install(Module module, String id, String version) {
        NuGetOldModuleExtension extension = ModuleUtilCore.getExtension(module, NuGetOldModuleExtension.class);
        if (extension == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No NuGet extension of module " + module.getName()));
        }
        try {
            WriteCommandAction.runWriteCommandAction(module.getProject(), () -> {
                NuGetXmlPackagesFile packagesFile = requirePackagesFile(module);
                NuGetXmlPackage existing = find(packagesFile, id);
                if (existing != null) {
                    existing.getVersion().setValue(version);
                    return;
                }
                NuGetXmlPackage xmlPackage = packagesFile.addPackage();
                xmlPackage.getId().setValue(id);
                xmlPackage.getVersion().setValue(version);
                xmlPackage.getTargetFramework().setValue(targetFramework(packagesFile));
            });
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        extension.update();
        return CompletableFuture.completedFuture(null);
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> uninstall(Module module, String id) {
        NuGetOldModuleExtension extension = ModuleUtilCore.getExtension(module, NuGetOldModuleExtension.class);
        if (extension == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No NuGet extension of module " + module.getName()));
        }
        try {
            WriteCommandAction.runWriteCommandAction(module.getProject(), () -> {
                NuGetXmlPackage existing = find(requirePackagesFile(module), id);
                if (existing != null) {
                    existing.undefine();
                }
            });
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        extension.update();
        return CompletableFuture.completedFuture(null);
    }

    private static @Nullable NuGetXmlPackage find(NuGetXmlPackagesFile packagesFile, String id) {
        for (NuGetXmlPackage xmlPackage : packagesFile.getPackages()) {
            if (id.equalsIgnoreCase(xmlPackage.getId().getValue())) {
                return xmlPackage;
            }
        }
        return null;
    }

    /**
     * The framework the other entries are for - the one of the module.
     */
    private static String targetFramework(NuGetXmlPackagesFile packagesFile) {
        for (NuGetXmlPackage xmlPackage : packagesFile.getPackages()) {
            String framework = xmlPackage.getTargetFramework().getValue();
            if (framework != null && !framework.isBlank()) {
                return framework;
            }
        }
        return DEFAULT_FRAMEWORK;
    }

    @RequiredReadAction
    private static @Nullable NuGetXmlPackagesFile findPackagesFile(Module module) {
        NuGetOldModuleExtension extension = ModuleUtilCore.getExtension(module, NuGetOldModuleExtension.class);
        return extension == null ? null : extension.getPackagesFile();
    }

    @RequiredReadAction
    private static NuGetXmlPackagesFile requirePackagesFile(Module module) {
        NuGetXmlPackagesFile packagesFile = findPackagesFile(module);
        if (packagesFile == null) {
            throw new IllegalStateException("No " + NuGetOldModuleExtension.PACKAGES_CONFIG + " of module " + module.getName());
        }
        return packagesFile;
    }
}
