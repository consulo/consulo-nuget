package consulo.nuget.api.v3;

import com.google.gson.annotations.SerializedName;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public class Page
{
	public static class Item
	{
		@SerializedName("@id")
		public String id;

		@SerializedName("@type")
		public String type;

		@SerializedName("nuget:id")
		public String nugetId;

		@SerializedName("nuget:version")
		public String nugetVersion;
	}

	public Item[] items;
}
