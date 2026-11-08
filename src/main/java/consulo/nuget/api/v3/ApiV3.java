package consulo.nuget.api.v3;

import com.google.gson.Gson;
import consulo.application.progress.ProgressIndicator;
import consulo.http.HttpRequests;
import consulo.nuget.manage.NuGetRepoPackageV3;
import consulo.repository.ui.RepoPackage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public class ApiV3
{
	public static List<RepoPackage> list(ProgressIndicator indicator, String repositoryUrl, String q)
	{
		try
		{
			return search(indicator, repositoryUrl, q, 0, 20);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Searches the repository by the search service of its index.
	 *
	 * @param skip the count of the results to skip
	 * @param take the count of the results to return
	 */
	public static List<RepoPackage> search(ProgressIndicator indicator, String repositoryUrl, String q, int skip, int take) throws IOException
	{
		List<RepoPackage> list = new ArrayList<>();
		Index index = Index.call(repositoryUrl, indicator);
		Index.Resource searchQuery = Arrays.stream(index.resources).filter(it -> it.type.startsWith("SearchQueryService")).findFirst().orElse(null);
		if(searchQuery == null)
		{
			return list;
		}
		String url = searchQuery.id + "?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&skip=" + skip + "&take=" + take;
		SearchQueryService searchQueryService = new Gson().fromJson(HttpRequests.request(url).readString(indicator), SearchQueryService.class);
		if(searchQueryService.data != null)
		{
			for(SearchQueryService.Data data : searchQueryService.data)
			{
				list.add(new NuGetRepoPackageV3(data, repositoryUrl));
			}
		}
		return list;
	}

	/**
	 * The versions of the package, the oldest first, from the package base address of the repository.
	 */
	public static List<String> versions(ProgressIndicator indicator, String repositoryUrl, String id) throws IOException
	{
		Index index = Index.call(repositoryUrl, indicator);
		String packageBase = index.getURL(Index.PackageBaseAddress);
		String json = HttpRequests.request(packageBase + id.toLowerCase(Locale.ROOT) + "/index.json").readString(indicator);
		PackageBaseAddressIndex addressIndex = new Gson().fromJson(json, PackageBaseAddressIndex.class);
		return addressIndex == null || addressIndex.versions == null ? List.of() : List.of(addressIndex.versions);
	}
}
