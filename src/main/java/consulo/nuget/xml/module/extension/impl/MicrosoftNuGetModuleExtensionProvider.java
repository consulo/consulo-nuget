package consulo.nuget.xml.module.extension.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.nuget.xml.module.extension.NuGetOldModuleExtensionProvider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 08/01/2023
 */
@ExtensionImpl
public class MicrosoftNuGetModuleExtensionProvider extends NuGetOldModuleExtensionProvider
{
	@Nonnull
	@Override
	public String getId()
	{
		return "microsoft-nuget";
	}

	@Nullable
	@Override
	public String getParentId()
	{
		return "microsoft-dotnet";
	}
}