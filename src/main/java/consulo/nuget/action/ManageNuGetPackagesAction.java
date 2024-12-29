package consulo.nuget.action;

import consulo.annotation.component.ActionImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.nuget.icon.NuGetIconGroup;
import consulo.nuget.manage.NuGetPackageManagmentService;
import consulo.nuget.module.extension.NuGetModuleExtension;
import consulo.repository.ui.RepositoryDialogFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
@ActionImpl(id = "NuGet.ManageNuGetPackages")
public class ManageNuGetPackagesAction extends DumbAwareAction
{
	@RequiredUIAccess
	@Override
	public void actionPerformed(@Nonnull AnActionEvent e)
	{
		Module module = e.getData(CommonDataKeys.MODULE);
		if(module == null)
		{
			return;
		}

		NuGetModuleExtension extension = ModuleUtilCore.getExtension(module, NuGetModuleExtension.class);
		if(extension == null)
		{
			return;
		}

		RepositoryDialogFactory repositoryDialogFactory = module.getProject().getInstance(RepositoryDialogFactory.class);

		repositoryDialogFactory.showManagePackagesDialogAsync(new NuGetPackageManagmentService(extension), null);
	}

	@Nullable
	@Override
	protected Image getTemplateIcon()
	{
		return NuGetIconGroup.nuget();
	}

	@RequiredUIAccess
	@Override
	public void update(@Nonnull AnActionEvent e)
	{
		Module module = e.getData(CommonDataKeys.MODULE);
		e.getPresentation().setEnabledAndVisible(module != null && ModuleUtilCore.getExtension(module, NuGetModuleExtension.class) != null);
	}
}
