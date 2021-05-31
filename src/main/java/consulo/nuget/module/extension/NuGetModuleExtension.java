package consulo.nuget.module.extension;

import consulo.module.extension.ModuleExtension;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public interface NuGetModuleExtension<T extends NuGetModuleExtension<T>> extends ModuleExtension<T>
{
	@Nonnull
	List<NuGetPackageInfo> getInstalledPackages();

	void update();

	void installPackage(NuGetPackageInfo packageInfo);
}
