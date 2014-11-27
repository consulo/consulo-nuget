/*
 * Copyright 2013-2014 must-be.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mustbe.consulo.nuget.module.extension;

import gnu.trove.THashMap;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.nuget.dom.NuGetPackage;
import org.mustbe.consulo.nuget.util.NuPkgUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
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
import com.intellij.platform.templates.github.DownloadUtil;
import com.intellij.ui.EditorNotifications;
import lombok.val;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetRepositoryWorker
{
	public static class PackageInfo
	{
		private String myId;
		private String myVersion;
		private String myTargetFramework;

		public PackageInfo(String id, String version, String targetFramework)
		{
			myId = id;
			myVersion = version;
			myTargetFramework = targetFramework;
		}

		public String getId()
		{
			return myId;
		}

		public String getVersion()
		{
			return myVersion;
		}

		public String getTargetFramework()
		{
			return myTargetFramework;
		}
	}

	public static final String PACKAGES_DIR = "packages";
	public static final String NUGET_LIBRARY_PREFIX = "nuget: ";
	public static final String NUGET_URL = "http://nuget.org/api/v2/package/%s/%s";

	private final NuGetModuleExtension myExtension;

	private AtomicBoolean myProgress = new AtomicBoolean();

	public NuGetRepositoryWorker(NuGetModuleExtension extension)
	{
		myExtension = extension;
	}

	public void forceUpdate()
	{
		if(isUpdateInProgress())
		{
			return;
		}

		val packagesFile = myExtension.getPackagesFile();
		if(packagesFile == null)
		{
			return;
		}

		myProgress.set(true);

		EditorNotifications.updateAll();

		val module = myExtension.getModule();
		new Task.Backgroundable(myExtension.getProject(), "Updating NuGet dependencies", false)
		{
			@Override
			public void run(@NotNull ProgressIndicator indicator)
			{
				Map<String, PackageInfo> packageMap = ApplicationManager.getApplication().runReadAction(new Computable
						.NotNullCachedComputable<Map<String, PackageInfo>>()
				{
					@NotNull
					@Override
					protected Map<String, PackageInfo> internalCompute()
					{
						Map<String, PackageInfo> map = new TreeMap<String, PackageInfo>();
						for(NuGetPackage nuGetPackage : packagesFile.getPackages())
						{
							String idValue = nuGetPackage.getId().getValue();
							String versionValue = nuGetPackage.getVersion().getValue();
							String targetFrameworkValue = nuGetPackage.getTargetFramework().getValue();
							if(idValue == null || versionValue == null || targetFrameworkValue == null)
							{
								continue;
							}
							map.put(idValue + "." + versionValue, new PackageInfo(idValue, versionValue, targetFrameworkValue));
						}
						return map;
					}
				});

				removeInvalidDependenciesFromFileSystem(packageMap, indicator);
				removeInvalidDependenciesFromModule(packageMap, indicator);

				if(packageMap.isEmpty())
				{
					return;
				}

				Map<PackageInfo, VirtualFile> refreshQueue = new THashMap<PackageInfo, VirtualFile>();

				indicator.setText("Downloading dependencies...");
				File packageDir = new File(module.getModuleDirPath(), PACKAGES_DIR);
				for(Map.Entry<String, PackageInfo> entry : packageMap.entrySet())
				{
					String key = entry.getKey();
					PackageInfo value = entry.getValue();
					String url = String.format(NUGET_URL, value.getId(), value.getVersion());

					val extractDir = new File(packageDir, key);
					val downloadTarget = new File(extractDir, key + ".nupkg");

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

	private void removeInvalidDependenciesFromFileSystem(final Map<String, PackageInfo> packages, ProgressIndicator indicator)
	{
		indicator.setText("NuGet: removing old dependencies from file system");

		VirtualFile moduleDir = myExtension.getModule().getModuleDir();
		assert moduleDir != null;

		val dir = moduleDir.findFileByRelativePath(PACKAGES_DIR);
		if(dir == null)
		{
			return;
		}

		invokeWriteAction(new Runnable()
		{
			@Override
			public void run()
			{
				for(VirtualFile virtualFile : dir.getChildren())
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

	private void removeInvalidDependenciesFromModule(Map<String, PackageInfo> packages, ProgressIndicator indicator)
	{
		indicator.setText("NuGet: removing old dependencies from module");

		final ModifiableRootModel modifiableModel = ApplicationManager.getApplication().runReadAction(new Computable<ModifiableRootModel>()
		{
			@Override
			public ModifiableRootModel compute()
			{
				return ModuleRootManager.getInstance(myExtension.getModule()).getModifiableModel();
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

	private void addDependenciesToModule(Map<PackageInfo, VirtualFile> packages, ProgressIndicator indicator)
	{
		indicator.setText("NuGet: add dependencies to module");

		final ModifiableRootModel modifiableModel = ApplicationManager.getApplication().runReadAction(new Computable<ModifiableRootModel>()
		{
			@Override
			public ModifiableRootModel compute()
			{
				return ModuleRootManager.getInstance(myExtension.getModule()).getModifiableModel();
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

			VirtualFile targetFrameworkLib = libraryDirectory.findFileByRelativePath(packageInfo.getTargetFramework());
			if(targetFrameworkLib == null)
			{
				continue;
			}

			addLibraryFiles(packageInfo, targetFrameworkLib, modifiableModel);
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
