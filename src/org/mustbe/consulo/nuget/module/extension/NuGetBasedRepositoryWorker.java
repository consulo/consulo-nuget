package org.mustbe.consulo.nuget.module.extension;

import gnu.trove.THashMap;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.nuget.util.NuPkgUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.ModuleLibraryOrderEntryImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.types.BinariesOrderRootType;
import com.intellij.openapi.roots.types.DocumentationOrderRootType;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.util.ArchiveVfsUtil;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.io.DownloadUtil;
import lombok.val;

/**
 * @author VISTALL
 * @since 22.02.2015
 */
public abstract class NuGetBasedRepositoryWorker
{
	public static class PackageInfo
	{
		private String myId;
		private String myVersion;
		private String[] myTargetFrameworks;

		public PackageInfo(String id, String version, String... targetFramework)
		{
			myId = id;
			myVersion = version;
			myTargetFrameworks = targetFramework;
		}

		public String getId()
		{
			return myId;
		}

		public String getVersion()
		{
			return myVersion;
		}

		public String[] getTargetFrameworks()
		{
			return myTargetFrameworks;
		}
	}

	public static final String NUGET_LIBRARY_PREFIX = "nuget: ";
	public static final String NUGET_URL = "http://nuget.org/api/v2/package/%s/%s";
	public static final String PACKAGES_DIR = "packages";

	protected final AtomicBoolean myProgress = new AtomicBoolean();
	protected final Module myModule;

	public NuGetBasedRepositoryWorker(Module module)
	{
		myModule = module;
	}

	@Nullable
	protected abstract String getPackagesDirPath();

	@NotNull
	@RequiredReadAction
	protected abstract Map<String, PackageInfo> getPackagesInfo();

	public void forceUpdate()
	{
		if(isUpdateInProgress())
		{
			return;
		}

		myProgress.set(true);

		EditorNotifications.updateAll();

		new Task.Backgroundable(myModule.getProject(), "Updating NuGet dependencies", false)
		{
			@Override
			public void run(@NotNull ProgressIndicator indicator)
			{
				Map<String, PackageInfo> packageMap = ApplicationManager.getApplication().runReadAction(new Computable<Map<String, PackageInfo>>()
				{
					@Override
					public Map<String, PackageInfo> compute()
					{
						return getPackagesInfo();
					}
				});

				removeInvalidDependenciesFromFileSystem(packageMap, indicator);
				removeInvalidDependenciesFromModule(packageMap, indicator);

				if(packageMap.isEmpty())
				{
					return;
				}

				String packagesDir = getPackagesDirPath();
				if(packagesDir == null)
				{
					return;
				}
				Map<PackageInfo, VirtualFile> refreshQueue = new THashMap<PackageInfo, VirtualFile>();

				indicator.setText("NuGet: Downloading dependencies...");
				File packageDir = new File(packagesDir);
				for(Map.Entry<String, PackageInfo> entry : packageMap.entrySet())
				{
					String key = entry.getKey();
					PackageInfo value = entry.getValue();
					String url = String.format(NUGET_URL, value.getId(), value.getVersion());

					val extractDir = new File(packageDir, key);
					val downloadTarget = new File(extractDir, value.getId() + "." + value.getVersion() + ".nupkg");

					FileUtil.createParentDirs(downloadTarget);

					try
					{
						DownloadUtil.downloadContentToFile(indicator, url, downloadTarget);

						VirtualFile extractDirFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(extractDir);
						if(extractDirFile == null)
						{
							continue;
						}

						refreshQueue.put(value, extractDirFile);
						indicator.setText("NuGet: extracting: " + downloadTarget.getPath());
						NuPkgUtil.extract(downloadTarget, extractDir, new FilenameFilter()
						{
							@Override
							public boolean accept(File dir, String name)
							{
								File parentFile = dir.getParentFile();
								return parentFile != null && parentFile.getName().equals("lib") && FileUtil.filesEqual(extractDir,
										parentFile.getParentFile());
							}
						}, true);
					}
					catch(IOException e)
					{
						FileUtil.delete(downloadTarget);

						Notifications.Bus.notify(new Notification("NuGet", "Warning", "Fail to download dependency with id: " + value.getId() + " " +
								"and version: " + value.getVersion(), NotificationType.WARNING));
					}
				}

				RefreshQueue.getInstance().refresh(false, true, null, refreshQueue.values());

				addDependenciesToModule(refreshQueue, indicator);
			}

			@Override
			public void onSuccess()
			{
				myProgress.set(false);
				EditorNotifications.updateAll();
			}

			@Override
			public void onCancel()
			{
				cancelTasks();
			}
		}.queue();
	}

	protected void addDependenciesToModule(Map<PackageInfo, VirtualFile> packages, ProgressIndicator indicator)
	{
		indicator.setText("NuGet: add dependencies to module");

		final ModifiableRootModel modifiableModel = ApplicationManager.getApplication().runReadAction(new Computable<ModifiableRootModel>()
		{
			@Override
			public ModifiableRootModel compute()
			{
				return ModuleRootManager.getInstance(myModule).getModifiableModel();
			}
		});

		for(Map.Entry<PackageInfo, VirtualFile> entry : packages.entrySet())
		{
			PackageInfo packageInfo = entry.getKey();
			VirtualFile packageDir = entry.getValue();

			VirtualFile libraryDirectory = packageDir.findFileByRelativePath("lib");
			if(libraryDirectory == null)
			{
				continue;
			}

			if(addLibraryFiles(packageInfo, libraryDirectory, modifiableModel))
			{
				continue;
			}

			for(String targetFramework : packageInfo.getTargetFrameworks())
			{
				VirtualFile targetFrameworkLib = libraryDirectory.findFileByRelativePath(targetFramework);
				if(targetFrameworkLib == null)
				{
					continue;
				}

				addLibraryFiles(packageInfo, targetFrameworkLib, modifiableModel);
				break;
			}
		}

		invokeWriteAction(new Runnable()
		{
			@Override
			public void run()
			{
				modifiableModel.commit();
			}
		});
	}

	private static boolean addLibraryFiles(PackageInfo packageInfo, VirtualFile libraryDir, ModifiableRootModel modifiableRootModel)
	{
		String libraryName = packageInfo.getId() + ".dll";
		String docFileName = packageInfo.getId() + ".xml";

		VirtualFile libraryFile = libraryDir.findFileByRelativePath(libraryName);
		if(libraryFile != null)
		{
			VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(libraryFile);
			if(archiveRootForLocalFile == null)
			{
				return false;
			}
			LibraryTable moduleLibraryTable = modifiableRootModel.getModuleLibraryTable();

			Library library = moduleLibraryTable.createLibrary(NUGET_LIBRARY_PREFIX + packageInfo.getId() + "." + packageInfo.getVersion());
			Library.ModifiableModel modifiableModel = library.getModifiableModel();
			modifiableModel.addRoot(archiveRootForLocalFile, BinariesOrderRootType.getInstance());

			VirtualFile docFile = libraryDir.findFileByRelativePath(docFileName);
			if(docFile != null)
			{
				modifiableModel.addRoot(docFile, DocumentationOrderRootType.getInstance());
			}
			modifiableModel.commit();
			return true;
		}
		return false;
	}

	protected void removeInvalidDependenciesFromModule(Map<String, PackageInfo> packages, ProgressIndicator indicator)
	{
		indicator.setText("NuGet: removing old dependencies from module");

		final ModifiableRootModel modifiableModel = ApplicationManager.getApplication().runReadAction(new Computable<ModifiableRootModel>()
		{
			@Override
			public ModifiableRootModel compute()
			{
				return ModuleRootManager.getInstance(myModule).getModifiableModel();
			}
		});

		OrderEntry[] orderEntries = modifiableModel.getOrderEntries();
		for(OrderEntry orderEntry : orderEntries)
		{
			if(!(orderEntry instanceof ModuleLibraryOrderEntryImpl))
			{
				continue;
			}

			String libraryName = ((ModuleLibraryOrderEntryImpl) orderEntry).getLibraryName();
			if(libraryName != null)
			{
				int i = libraryName.indexOf(NUGET_LIBRARY_PREFIX);
				if(i != -1)
				{
					String idAndVersion = libraryName.substring(i, libraryName.length());

					PackageInfo nuGetPackage = packages.get(idAndVersion);
					if(nuGetPackage == null)
					{
						modifiableModel.removeOrderEntry(orderEntry);
					}
				}
			}
		}

		invokeWriteAction(new Runnable()
		{
			@Override
			public void run()
			{
				modifiableModel.commit();
			}
		});
	}

	protected void removeInvalidDependenciesFromFileSystem(final Map<String, PackageInfo> packages, ProgressIndicator indicator)
	{
		indicator.setText("NuGet: removing old dependencies from file system");

		val dir = getPackagesDirPath();
		if(dir == null)
		{
			return;
		}

		invokeWriteAction(new Runnable()
		{
			@Override
			public void run()
			{
				VirtualFile packagesDir = LocalFileSystem.getInstance().findFileByPath(getPackagesDirPath());
				if(packagesDir == null)
				{
					return;
				}
				for(VirtualFile virtualFile : packagesDir.getChildren())
				{
					if(!virtualFile.isDirectory())
					{
						continue;
					}

					PackageInfo nuGetPackage = packages.get(virtualFile.getName());
					if(nuGetPackage == null)
					{
						try
						{
							virtualFile.delete(null);
						}
						catch(IOException e)
						{
							//
						}
					}
				}
			}
		});
	}

	public static void invokeWriteAction(final Runnable runnable)
	{
		ApplicationManager.getApplication().invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				ApplicationManager.getApplication().runWriteAction(runnable);
			}
		});
	}

	public void cancelTasks()
	{
		myProgress.set(false);
		EditorNotifications.updateAll();
	}

	public boolean isUpdateInProgress()
	{
		return myProgress.get();
	}
}
