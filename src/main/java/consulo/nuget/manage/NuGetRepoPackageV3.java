package consulo.nuget.manage;

import consulo.nuget.api.v3.SearchQueryService;
import consulo.repository.ui.RepoPackage;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public class NuGetRepoPackageV3 extends RepoPackage
{
	@Nonnull
	private final SearchQueryService.Data myData;

	public NuGetRepoPackageV3(SearchQueryService.Data data, String repoUrl)
	{
		super(data.id, repoUrl, data.version);
		myData = data;
	}

	@Nonnull
	public SearchQueryService.DataVersion[] getVersions()
	{
		return myData.versions;
	}

	@Nonnull
	public String getDescription()
	{
		return myData.description;
	}
}
