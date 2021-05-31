package consulo.nuget.api.v3;

import com.google.gson.annotations.SerializedName;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public class SearchQueryService
{
	public static class Data
	{
		public String id;
		public String version;
		public String description;
		public String title;
		public String iconUrl;

		public DataVersion[] versions;
	}

	public static class DataVersion
	{
		public String version;

		@SerializedName("@id")
		public String $id;
	}

	public Data[] data;
}
