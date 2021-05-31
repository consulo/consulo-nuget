package consulo.nuget.api.v3;

import com.google.gson.annotations.SerializedName;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public class Catalog
{
	public static class Item
	{
		@SerializedName("@id")
		public String id;

		@SerializedName("@type")
		public String type;
	}

	public Item[] items;
}
