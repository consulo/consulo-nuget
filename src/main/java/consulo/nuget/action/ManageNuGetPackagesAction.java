package consulo.nuget.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.webcore.packaging.ManagePackagesDialog;
import consulo.nuget.manage.NuGetPackageManagmentService;
import consulo.nuget.module.extension.NuGetModuleExtension;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
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

		new ManagePackagesDialog(module.getProject(), new NuGetPackageManagmentService(extension), null).showAsync();
	}

	@RequiredUIAccess
	@Override
	public void update(@Nonnull AnActionEvent e)
	{
		Module module = e.getData(CommonDataKeys.MODULE);
		e.getPresentation().setEnabledAndVisible(module != null && ModuleUtilCore.getExtension(module, NuGetModuleExtension.class) != null);
	}
}
