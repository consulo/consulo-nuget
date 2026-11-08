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
package consulo.nuget.manage;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.dotnet.packageManager.DotNetPackageManager;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.nuget.icon.NuGetIconGroup;
import consulo.project.Project;
import consulo.repository.ui.PackageManagementService;
import consulo.repository.ui.PackageManagementServiceProvider;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * The NuGet packages of a module - the target is the name of the module.
 */
@ExtensionImpl
public class NuGetPackageManagementServiceProvider implements PackageManagementServiceProvider {
    @Override
    public String getId() {
        return "nuget";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("NuGet");
    }

    @Override
    public Image getIcon() {
        return NuGetIconGroup.nuget();
    }

    @Override
    public @Nullable PackageManagementService create(Project project, String target) {
        return ReadAction.compute(() -> {
            Module module = ModuleManager.getInstance(project).findModuleByName(target);
            DotNetPackageManager packageManager = module == null ? null : DotNetPackageManager.find(module);
            return packageManager == null ? null : new NuGetPackageManagementService(module, packageManager);
        });
    }
}
