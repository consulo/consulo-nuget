package consulo.nuget.manage;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.CatchingConsumer;
import com.intellij.webcore.packaging.InstalledPackage;
import com.intellij.webcore.packaging.PackageManagementServiceEx;
import com.intellij.webcore.packaging.RepoPackage;
import consulo.nuget.api.v3.ApiV3;
import consulo.nuget.module.extension.NuGetModuleExtension;
import consulo.nuget.module.extension.NuGetPackageInfo;
import consulo.packagesView.SearchablePackageManagementService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public class NuGetPackageManagmentService extends PackageManagementServiceEx implements SearchablePackageManagementService
{
	private final Map<String, NuGetRepoPackageV3> myLastPackages = new HashMap<>();

	private final NuGetModuleExtension<?> myExtension;

	public NuGetPackageManagmentService(NuGetModuleExtension<?> extension)
	{
		myExtension = extension;
	}

	@Override
	public void updatePackage(@Nonnull InstalledPackage installedPackage, @Nullable String s, @Nonnull Listener listener)
	{

	}

	@Override
	public void fetchLatestVersion(@Nonnull InstalledPackage installedPackage, @Nonnull CatchingConsumer<String, Exception> catchingConsumer)
	{

	}

	@Nonnull
	@Override
	public List<RepoPackage> getAllPackages() throws IOException
	{
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<RepoPackage> reloadAllPackages() throws IOException
	{
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<? extends InstalledPackage> getInstalledPackagesList() throws ExecutionException
	{
		List<NuGetPackageInfo> installedPackages = ReadAction.compute(() -> myExtension.getInstalledPackages());

		return installedPackages.stream().map(it -> new InstalledPackage(it.id(), it.version())).collect(Collectors.toList());
	}

	@Override
	public void installPackage(RepoPackage repoPackage,
							   @Nullable String version,
							   boolean forceUpgrade,
							   @Nullable String extraOptions,
							   Listener listener,
							   boolean installToUser)
	{
		NuGetRepoPackageV3 entry = (NuGetRepoPackageV3) repoPackage;

		listener.operationStarted(repoPackage.getName());

		myExtension.installPackage(new NuGetPackageInfo(entry.getName(), entry.getLatestVersion(), "net45"));

		myExtension.update();

		listener.operationFinished(repoPackage.getName(), null);
	}

	@Override
	public void uninstallPackages(List<InstalledPackage> list, Listener listener)
	{

	}

	@Override
	public void fetchPackageVersions(String id, CatchingConsumer<List<String>, Exception> catchingConsumer)
	{
		try
		{
			NuGetRepoPackageV3 packageV3 = myLastPackages.get(id);
			if(packageV3 == null)
			{
				catchingConsumer.accept(List.of());
			}
			else
			{
				catchingConsumer.accept(Arrays.stream(packageV3.getVersions()).map(it -> it.version).collect(Collectors.toList()));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void fetchPackageDetails(String id, CatchingConsumer<String, Exception> catchingConsumer)
	{
		try
		{
			NuGetRepoPackageV3 packageV3 = myLastPackages.get(id);
			if(packageV3 == null)
			{
				catchingConsumer.accept("");
			}
			else
			{
				catchingConsumer.accept(packageV3.getDescription());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Nonnull
	@Override
	public List<RepoPackage> getPackages(@Nonnull String query, int i, int i1)
	{
		try
		{
			List<RepoPackage> list = ApiV3.list(ProgressManager.getGlobalProgressIndicator(), "https://api.nuget.org/v3/", query);
			for(RepoPackage repoPackage : list)
			{
				myLastPackages.put(repoPackage.getName(), (NuGetRepoPackageV3) repoPackage);
			}
			return list;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return List.of();
	}

	@Override
	public int getPageSize()
	{
		return 20;
	}
}
