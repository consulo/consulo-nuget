package consulo.nuget.api.v3;

import com.google.gson.Gson;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.io.HttpRequests;
import com.intellij.webcore.packaging.RepoPackage;
import consulo.nuget.manage.NuGetRepoPackageV3;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public class ApiV3
{
	public static List<RepoPackage> list(ProgressIndicator indicator, String repositoryUrl, String q)
	{
		List<RepoPackage> list = new ArrayList<>();

		Gson gson = new Gson();
		try
		{
			Index index = Index.call(repositoryUrl, indicator);

			Index.Resource searchQuery = Arrays.stream(index.resources).filter(it -> it.type.startsWith("SearchQueryService")).findFirst().get();

			String qJson = HttpRequests.request(searchQuery.id + "?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)).readString(indicator);

			SearchQueryService searchQueryService = gson.fromJson(qJson, SearchQueryService.class);

			for(SearchQueryService.Data data : searchQueryService.data)
			{
				list.add(new NuGetRepoPackageV3(data, null));
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return list;
	}
}
