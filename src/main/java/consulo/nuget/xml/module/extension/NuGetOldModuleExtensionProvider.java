package consulo.nuget.xml.module.extension;

import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.nuget.icon.NuGetIconGroup;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 08/01/2023
 */
public abstract class NuGetOldModuleExtensionProvider implements ModuleExtensionProvider<NuGetOldModuleExtension>
{
	@Nonnull
	@Override
	public LocalizeValue getName()
	{
		return LocalizeValue.of("NuGet");
	}

	@Nonnull
	@Override
	public Image getIcon()
	{
		return NuGetIconGroup.nuget();
	}

	@Nonnull
	@Override
	public ModuleExtension<NuGetOldModuleExtension> createImmutableExtension(@Nonnull ModuleRootLayer moduleRootLayer)
	{
		return new NuGetOldModuleExtension(getId(), moduleRootLayer);
	}

	@Nonnull
	@Override
	public MutableModuleExtension<NuGetOldModuleExtension> createMutableExtension(@Nonnull ModuleRootLayer moduleRootLayer)
	{
		return new NuGetOldMutableModuleExtension(getId(), moduleRootLayer);
	}
}
