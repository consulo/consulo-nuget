package consulo.nuget.manage;

import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.nuget.api.v3.ApiV3;
import consulo.nuget.module.extension.NuGetModuleExtension;
import consulo.nuget.module.extension.NuGetPackageInfo;
import consulo.repository.ui.InstalledPackage;
import consulo.repository.ui.PackageManagementServiceEx;
import consulo.repository.ui.RepoPackage;
import consulo.repository.ui.SearchablePackageManagementService;
import consulo.util.concurrent.AsyncResult;

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
	public List<? extends InstalledPackage> getInstalledPackagesList() throws IOException
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
	public AsyncResult<List<String>> fetchPackageVersions(String id)
	{
		AsyncResult<List<String>> result = AsyncResult.undefined();
		try
		{
			NuGetRepoPackageV3 packageV3 = myLastPackages.get(id);
			if(packageV3 == null)
			{
				result.setDone(List.of());
			}
			else
			{
				result.setDone(Arrays.stream(packageV3.getVersions()).map(it -> it.version).collect(Collectors.toList()));
			}
		}
		catch(Exception e)
		{
			result.rejectWithThrowable(e);
		}
		return result;
	}

	@Override
	public AsyncResult<String> fetchPackageDetails(String id)
	{
		AsyncResult<String> result = AsyncResult.undefined();
		try
		{
			NuGetRepoPackageV3 packageV3 = myLastPackages.get(id);
			if(packageV3 == null)
			{
				result.setDone("");
			}
			else
			{
				result.setDone(packageV3.getDescription());
			}
		}
		catch(Exception e)
		{
			result.rejectWithThrowable(e);
		}

		return result;
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
