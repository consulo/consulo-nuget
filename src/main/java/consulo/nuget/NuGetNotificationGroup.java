package consulo.nuget;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 08/01/2023
 */
@ExtensionImpl
public class NuGetNotificationGroup implements NotificationGroupContributor
{
	public static final NotificationGroup GROUP = NotificationGroup.balloonGroup("NuGet", LocalizeValue.of("NuGet"));

	@Override
	public void contribute(@Nonnull Consumer<NotificationGroup> consumer)
	{
		consumer.accept(GROUP);
	}
}
