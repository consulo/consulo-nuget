package consulo.nuget.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.dotnet.packageManager.DotNetPackageManager;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.nuget.manage.NuGetPackageManagementServiceProvider;
import consulo.repository.ui.PackagesEditorManager;
import consulo.nuget.icon.NuGetIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
@ActionImpl(id = "NuGet.ManageNuGetPackages", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_PROJECT_VIEW_POPUP)))
public class ManageNuGetPackagesAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public ManageNuGetPackagesAction() {
        super(LocalizeValue.localizeTODO("Manage NuGet Packages"));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Module module = e.getData(Module.KEY);
        if (module == null) {
            return;
        }
        PackagesEditorManager.getInstance(module.getProject()).open(NuGetPackageManagementServiceProvider.class, module.getName());
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return NuGetIconGroup.nuget();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Module module = e.getData(Module.KEY);
        e.getPresentation().setEnabledAndVisible(module != null && DotNetPackageManager.find(module) != null);
    }
}
