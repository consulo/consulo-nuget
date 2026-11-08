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

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.dotnet.packageManager.DotNetPackageManager;
import consulo.dotnet.packageManager.DotNetPackageReference;
import consulo.module.Module;
import consulo.nuget.api.v3.ApiV3;
import consulo.repository.ui.InstalledPackage;
import consulo.repository.ui.PackageInstallScope;
import consulo.repository.ui.PackageManagementServiceEx;
import consulo.repository.ui.RepoPackage;
import consulo.repository.ui.SearchablePackageManagementService;
import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The packages of a module from a NuGet repository, referenced the way the project system of the module keeps them.
 */
public class NuGetPackageManagementService extends PackageManagementServiceEx implements SearchablePackageManagementService {
    public static final String NUGET_ORG = "https://api.nuget.org/v3/";

    private final Module myModule;
    private final DotNetPackageManager myPackageManager;
    // the descriptions of the packages the search returned
    private final Map<String, String> myDescriptions = new ConcurrentHashMap<>();

    public NuGetPackageManagementService(Module module, DotNetPackageManager packageManager) {
        myModule = module;
        myPackageManager = packageManager;
    }

    @Override
    public List<RepoPackage> getAllPackages() throws IOException {
        return List.of();
    }

    @Override
    public List<RepoPackage> getPackages(String searchQuery, int from, int to) {
        try {
            List<RepoPackage> packages = ApiV3.search(new EmptyProgressIndicator(), NUGET_ORG, searchQuery, from, to - from);
            for (RepoPackage repoPackage : packages) {
                if (repoPackage instanceof NuGetRepoPackageV3 packageV3) {
                    myDescriptions.put(repoPackage.getName(), StringUtil.notNullize(packageV3.getDescription()));
                }
            }
            return packages;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getPageSize() {
        return 50;
    }

    @Override
    public List<? extends InstalledPackage> getInstalledPackagesList() throws IOException {
        List<DotNetPackageReference> references = ReadAction.compute(() -> myModule.isDisposed() ? List.of() : myPackageManager.getPackages(myModule));
        List<InstalledPackage> packages = new ArrayList<>(references.size());
        for (DotNetPackageReference reference : references) {
            packages.add(new InstalledPackage(reference.id(), reference.version()));
        }
        return packages;
    }

    @Override
    public void installPackage(RepoPackage repoPackage, @Nullable String version, @Nullable PackageInstallScope scope, @Nullable String extraOptions, Listener listener) {
        install(repoPackage.getName(), version != null ? version : repoPackage.getLatestVersion(), listener);
    }

    @Override
    public void installPackage(RepoPackage repoPackage, @Nullable String version, boolean forceUpgrade, @Nullable String extraOptions, Listener listener, boolean installToUser) {
        install(repoPackage.getName(), version != null ? version : repoPackage.getLatestVersion(), listener);
    }

    @Override
    public void updatePackage(InstalledPackage installedPackage, @Nullable String version, Listener listener) {
        install(installedPackage.getName(), version, listener);
    }

    @Override
    public void uninstallPackages(List<InstalledPackage> installedPackages, Listener listener) {
        for (InstalledPackage installedPackage : installedPackages) {
            String id = installedPackage.getName();
            listener.operationStarted(id);
            whenDone(myPackageManager.uninstall(myModule, id), id, listener);
        }
    }

    private void install(String id, @Nullable String version, Listener listener) {
        listener.operationStarted(id);
        if (version != null) {
            whenDone(myPackageManager.install(myModule, id, version), id, listener);
            return;
        }
        // the version of the latest release - the repository is asked for it in the background, the reference is written in the ui thread
        UIAccess uiAccess = UIAccess.current();
        Application.get().executeOnPooledThread(() -> {
            String latest;
            try {
                latest = latestRelease(ApiV3.versions(new EmptyProgressIndicator(), NUGET_ORG, id));
            }
            catch (IOException e) {
                listener.operationFinished(id, ErrorDescription.fromMessage(StringUtil.notNullize(e.getMessage(), e.getClass().getName())));
                return;
            }
            if (latest == null) {
                listener.operationFinished(id, ErrorDescription.fromMessage("No versions of " + id));
                return;
            }
            uiAccess.give(() -> whenDone(myPackageManager.install(myModule, id, latest), id, listener));
        });
    }

    private static void whenDone(CompletableFuture<?> future, String id, Listener listener) {
        future.whenComplete((result, error) -> listener.operationFinished(id, error == null ? null : ErrorDescription.fromMessage(
            StringUtil.notNullize(error.getMessage(), error.getClass().getName()))));
    }

    @Override
    public AsyncResult<List<String>> fetchPackageVersions(String packageName) {
        return async(() -> {
            List<String> versions = new ArrayList<>(ApiV3.versions(new EmptyProgressIndicator(), NUGET_ORG, packageName));
            Collections.reverse(versions);
            return versions;
        });
    }

    @Override
    public AsyncResult<String> fetchPackageDetails(String packageName) {
        String description = myDescriptions.get(packageName);
        if (description != null) {
            return AsyncResult.resolved(toHtml(description));
        }
        return async(() -> {
            for (RepoPackage repoPackage : ApiV3.search(new EmptyProgressIndicator(), NUGET_ORG, "packageid:" + packageName, 0, 1)) {
                if (repoPackage instanceof NuGetRepoPackageV3 packageV3 && packageName.equalsIgnoreCase(repoPackage.getName())) {
                    String text = StringUtil.notNullize(packageV3.getDescription());
                    myDescriptions.put(packageName, text);
                    return toHtml(text);
                }
            }
            return "";
        });
    }

    @Override
    public boolean shouldFetchLatestVersionsForOnlyInstalledPackages() {
        return true;
    }

    @Override
    public AsyncResult<String> fetchLatestVersion(InstalledPackage pkg) {
        return async(() -> latestRelease(ApiV3.versions(new EmptyProgressIndicator(), NUGET_ORG, pkg.getName())));
    }

    /**
     * The newest version which is not a prerelease, or the newest one if all are.
     */
    private static @Nullable String latestRelease(List<String> versions) {
        for (int i = versions.size() - 1; i >= 0; i--) {
            if (!versions.get(i).contains("-")) {
                return versions.get(i);
            }
        }
        return versions.isEmpty() ? null : versions.get(versions.size() - 1);
    }

    private static String toHtml(String description) {
        return StringUtil.escapeXmlEntities(description).replace("\n", "<br/>");
    }

    private static <T> AsyncResult<T> async(Callable<T> callable) {
        AsyncResult<T> result = AsyncResult.undefined();
        Application.get().executeOnPooledThread(() -> {
            try {
                result.setDone(callable.call());
            }
            catch (Exception e) {
                result.rejectWithThrowable(e);
            }
        });
        return result;
    }
}
