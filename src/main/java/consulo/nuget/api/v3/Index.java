package consulo.nuget.api.v3;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import consulo.application.progress.ProgressIndicator;
import consulo.http.HttpRequests;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public class Index
{
	public static final String PackageBaseAddress = "PackageBaseAddress";

	public static class Resource
	{
		@SerializedName("@id")
		public String id;

		@SerializedName("@type")
		public String type;

		public String comment;
	}
	
	public String version;
	public Resource[] resources;

	public String getURL(String typePrefix)
	{
		return Arrays.stream(resources).filter(it -> it.type.startsWith(typePrefix)).findFirst().get().id;
	}

	public static Index call(String repositoryUrl, ProgressIndicator indicator) throws IOException
	{
		String json = HttpRequests.request(repositoryUrl + "index.json").readString(indicator);

		return new Gson().fromJson(json, Index.class);
	}

}