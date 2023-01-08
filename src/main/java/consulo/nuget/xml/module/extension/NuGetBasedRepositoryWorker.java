package consulo.nuget.xml.module.extension;

import com.google.gson.Gson;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.function.Computable;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.DocumentationOrderRootType;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.dotnet.dll.DotNetModuleFileType;
import consulo.fileEditor.EditorNotifications;
import consulo.ide.util.DownloadUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.nuget.NuGetNotificationGroup;
import consulo.nuget.api.*;
import consulo.nuget.api.v3.Index;
import consulo.nuget.api.v3.PackageBaseAddressIndex;
import consulo.nuget.util.NuPkgUtil;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.xml.ide.highlighter.XmlFileType;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 22.02.2015
 */
public abstract class NuGetBasedRepositoryWorker
{
	private static final Logger LOG = Logger.getInstance(NuGetBasedRepositoryWorker.class);

	public static class PackageInfo
	{
		private String myId;
		private String myVersion;
		private String[] myTargetFrameworks;

		private NuGetPackageEntry myPackageEntry;

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

		public NuGetPackageEntry getPackageEntry()
		{
			return myPackageEntry;
		}

		public void setPackageEntry(NuGetPackageEntry packageEntry)
		{
			myPackageEntry = packageEntry;
		}

		@Override
		public boolean equals(Object o)
		{
			if(this == o)
			{
				return true;
			}
			if(o == null || getClass() != o.getClass())
			{
				return false;
			}

			PackageInfo that = (PackageInfo) o;

			if(myId != null ? !myId.equals(that.myId) : that.myId != null)
			{
				return false;
			}
			if(myVersion != null ? !myVersion.equals(that.myVersion) : that.myVersion != null)
			{
				return false;
			}

			return true;
		}

		@Override
		public int hashCode()
		{
			int result = myId != null ? myId.hashCode() : 0;
			result = 31 * result + (myVersion != null ? myVersion.hashCode() : 0);
			return result;
		}
	}

	public static final String NUGET_LIBRARY_PREFIX = "nuget: ";
	public static final String PACKAGES_DIR = "packages";

	protected final AtomicBoolean myProgress = new AtomicBoolean();
	protected final Module myModule;

	public NuGetBasedRepositoryWorker(Module module)
	{
		myModule = module;
	}

	@Nullable
	protected abstract String getPackagesDirPath();

	@RequiredReadAction
	protected abstract void loadDefinedPackages(@Nonnull Consumer<PackageInfo> packageInfoConsumer);

	@Nonnull
	public String getNameAndVersionSeparator()
	{
		return ".";
	}

	@Nonnull
	public NuGetRepositoryManager getRepositoryManager()
	{
		return new NuGetRepositoryManager()
		{
			@Nonnull
			@Override
			public List<String> getRepositories()
			{
				return List.of("https://api.nuget.org/v3/");
			}
		};
	}

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
			public void run(@Nonnull ProgressIndicator indicator)
			{
				try
				{
					final Map<String, PackageInfo> packageMap = new LinkedHashMap<String, PackageInfo>();
					final Consumer<PackageInfo> packageInfoConsumer = packageInfo -> {
						String key = packageInfo.getId() + getNameAndVersionSeparator() + packageInfo.getVersion();
						if(!packageMap.containsKey(key))
						{
							packageMap.put(key, packageInfo);
						}
					};

					ApplicationManager.getApplication().runReadAction(() -> loadDefinedPackages(packageInfoConsumer));

					NuGetRepositoryManager repositoryManager = getRepositoryManager();

					NuGetRequestQueue requestQueue = new NuGetRequestQueue();

					for(PackageInfo packageInfo : packageMap.values())
					{
						packageInfo.setPackageEntry(resolvePackageEntry(repositoryManager, indicator, requestQueue, packageInfo.myId, packageInfo.myVersion));
					}

					resolveDependencies(indicator, requestQueue, repositoryManager, packageInfoConsumer, packageMap);

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
					Map<PackageInfo, VirtualFile> refreshQueue = new LinkedHashMap<PackageInfo, VirtualFile>();

					indicator.setText("NuGet: Downloading dependencies...");
					File packageDir = new File(packagesDir);
					for(Map.Entry<String, PackageInfo> entry : packageMap.entrySet())
					{
						String key = entry.getKey();
						PackageInfo value = entry.getValue();

						NuGetPackageEntry packageEntry = value.getPackageEntry();
						String downloadUrl = packageEntry == null ? null : packageEntry.contentUrl();
						if(downloadUrl == null)
						{
							Notifications.Bus.notify(new Notification(NuGetNotificationGroup.GROUP, "Warning", "Package is not resolved with id: " + value.getId() + " and version: " + value.getVersion(), NotificationType.WARNING));
							continue;
						}

						final File extractDir = new File(packageDir, key);
						if(!extractDir.exists())
						{
							File downloadTarget = new File(extractDir, value.getId() + "." + value.getVersion() + ".nupkg");

							FileUtil.createParentDirs(downloadTarget);

							try
							{
								DownloadUtil.downloadContentToFile(indicator, downloadUrl, downloadTarget);

								indicator.setText("NuGet: extracting: " + downloadTarget.getPath());
								NuPkgUtil.extract(downloadTarget, extractDir, new FilenameFilter()
								{
									@Override
									public boolean accept(File dir, String name)
									{
										File parentFile = dir.getParentFile();
										return parentFile != null && parentFile.getName().equals("lib") && FileUtil.filesEqual(extractDir, parentFile.getParentFile());
									}
								}, true);
							}
							catch(IOException e)
							{
								FileUtil.delete(downloadTarget);

								Notifications.Bus.notify(new Notification(NuGetNotificationGroup.GROUP, "Warning", "Fail to download dependency with id: " + value.getId() + " " + "and version: " + value.getVersion(), NotificationType.WARNING));
							}
						}

						VirtualFile extractDirFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(extractDir);
						if(extractDirFile == null)
						{
							continue;
						}

						refreshQueue.put(value, extractDirFile);
					}

					RefreshQueue.getInstance().refresh(false, true, null, refreshQueue.values());

					addDependenciesToModule(refreshQueue, indicator);
				}
				catch(Exception e)
				{
					NuGetBasedRepositoryWorker.LOG.error(e);
				}
			}

			@RequiredUIAccess
			@Override
			public void onSuccess()
			{
				myProgress.set(false);
				EditorNotifications.updateAll();
			}

			@RequiredUIAccess
			@Override
			public void onCancel()
			{
				cancelTasks();
			}
		}.queue();
	}

	public void resolveDependencies(@Nonnull ProgressIndicator indicator, @Nonnull NuGetRequestQueue requestQueue, @Nonnull NuGetRepositoryManager manager, @Nonnull Consumer<PackageInfo> packageInfoConsumer, @Nonnull Map<String, PackageInfo> map)
	{
		PackageInfo[] packageInfos = map.values().toArray(new PackageInfo[map.size()]);

		for(PackageInfo packageInfo : packageInfos)
		{
			resolveDependenciesImpl(indicator, requestQueue, manager, packageInfoConsumer, map, packageInfo);
		}
	}

	private void resolveDependenciesImpl(@Nonnull ProgressIndicator indicator, @Nonnull NuGetRequestQueue requestQueue, @Nonnull NuGetRepositoryManager manager, @Nonnull Consumer<PackageInfo> packageInfoConsumer, @Nonnull Map<String, PackageInfo> map, @Nonnull PackageInfo packageInfo)
	{
		NuGetPackageEntry packageEntry = packageInfo.getPackageEntry();
		if(packageEntry == null)
		{
			return;
		}

		List<NuGetDependency> dependencies = packageEntry.dependencies();

		for(NuGetDependency dependency : dependencies)
		{
			if(isAlreadyInstalled(map, dependency))
			{
				continue;
			}

			Collection<String> temp = getVersionsForId(manager, indicator, requestQueue, dependency.getId());
			String[] versionsForId = ArrayUtil.reverseArray(ArrayUtil.toStringArray(temp));

			String correctVersion = null;
			for(String s : versionsForId)
			{
				if(dependency.getDependencyVersionInfo().is(NuGetVersion.parseVersion(s)))
				{
					correctVersion = s;
					break;
				}
			}

			if(correctVersion == null)
			{
				continue;
			}

			String[] frameworks;
			if(dependency.getFramework() == null)
			{
				frameworks = packageInfo.getTargetFrameworks();
			}
			else
			{
				frameworks = new String[]{dependency.getFramework()};
			}
			PackageInfo newPackageInfo = new PackageInfo(dependency.getId(), correctVersion, frameworks);
			newPackageInfo.setPackageEntry(resolvePackageEntry(manager, indicator, requestQueue, dependency.getId(), correctVersion));
			packageInfoConsumer.accept(newPackageInfo);

			resolveDependenciesImpl(indicator, requestQueue, manager, packageInfoConsumer, map, newPackageInfo);
		}
	}

	private boolean isAlreadyInstalled(@Nonnull Map<String, PackageInfo> packageInfoMap, @Nonnull NuGetDependency dependency)
	{
		for(PackageInfo packageInfo : packageInfoMap.values())
		{
			if(packageInfo.getId().equals(dependency.getId()))
			{
				return true;
			}
		}
		return false;
	}

	@Nullable
	protected NuGetPackageEntry resolvePackageEntry(@Nonnull final NuGetRepositoryManager repositoryManager, @Nonnull final ProgressIndicator indicator, @Nonnull final NuGetRequestQueue requestQueue, @Nonnull final String id, @Nonnull final String version)
	{
		for(final String url : repositoryManager.getRepositories())
		{
			try
			{
				indicator.setText("NuGet: Getting info about " + id + ":" + version + " package from " + url);

				try
				{
					Index index = Index.call(url, indicator);

					String packBaseUrl = index.getURL(Index.PackageBaseAddress);

					String resultUrl = packBaseUrl + id.toLowerCase(Locale.ROOT) + "/" + version.toLowerCase(Locale.ROOT) + "/" + id.toLowerCase(Locale.ROOT) + ".nuspec";

					return requestQueue.request(resultUrl, request -> {
						try
						{
							Element rootElement = JDOMUtil.loadDocument(request.readBytes(indicator)).getRootElement();
							return NuGetPackageEntryParser.parseSingle(rootElement, packBaseUrl, url);
						}
						catch(JDOMException e)
						{
							throw new IOException(e);
						}
					});
				}
				catch(IOException e)
				{
					LOG.warn(e);
				}
			}
			finally
			{
				indicator.setTextValue(LocalizeValue.of());
			}
		}

		return null;
	}

	@Nullable
	private Collection<String> getVersionsForId(@Nonnull final NuGetRepositoryManager repositoryManager, @Nonnull final ProgressIndicator indicator, @Nonnull NuGetRequestQueue requestQueue, @Nonnull String id)
	{
		for(final String url : repositoryManager.getRepositories())
		{
			try
			{
				indicator.setText("NuGet: Getting info about " + id + " package from " + url);

				try
				{
					Index index = Index.call(url, indicator);

					String packBase = index.getURL(Index.PackageBaseAddress);

					PackageBaseAddressIndex i = requestQueue.request(packBase + id.toLowerCase(Locale.ROOT) + "/index.json", request -> {
						return new Gson().fromJson(request.readString(indicator), PackageBaseAddressIndex.class);
					});

					return List.of(i.versions);
				}
				catch(IOException e)
				{
					LOG.warn(e);
				}
			}
			finally
			{
				indicator.setText(null);
			}
		}
		return List.of();
	}

	protected void addDependenciesToModule(Map<PackageInfo, VirtualFile> packages, ProgressIndicator indicator)
	{
		indicator.setText("NuGet: add dependencies to module");

		final ModifiableRootModel modifiableRootModel = ApplicationManager.getApplication().runReadAction(new Computable<ModifiableRootModel>()
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

			LibraryTable moduleLibraryTable = modifiableRootModel.getModuleLibraryTable();
			Library library = moduleLibraryTable.createLibrary(NUGET_LIBRARY_PREFIX + packageInfo.getId() + "." + packageInfo.getVersion());
			Library.ModifiableModel modifiableModel = library.getModifiableModel();

			// we dont added from root
			if(!addLibraryFiles(libraryDirectory, modifiableModel))
			{
				VirtualFile[] children = libraryDirectory.getChildren();
				mainLoop:
				for(VirtualFile versionFrameworkLib : children)
				{
					if(versionFrameworkLib.isDirectory())
					{
						NuGetTargetFrameworkInfo targetFrameworkInfo = NuGetTargetFrameworkInfo.parse(versionFrameworkLib.getName());

						for(String targetFramework : packageInfo.getTargetFrameworks())
						{
							if(targetFrameworkInfo.accept(targetFramework))
							{
								addLibraryFiles(versionFrameworkLib, modifiableModel);
								break mainLoop;
							}
						}
					}
				}

			}

			modifiableModel.commit();
		}

		WriteAction.runAndWait(modifiableRootModel::commit);

		indicator.setText(null);
	}

	private static boolean addLibraryFiles(VirtualFile libraryDir, Library.ModifiableModel modifiableModel)
	{
		boolean added = false;
		for(VirtualFile virtualFile : libraryDir.getChildren())
		{
			if(virtualFile.getFileType() == DotNetModuleFileType.INSTANCE)
			{
				VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile);
				if(archiveRootForLocalFile == null)
				{
					continue;
				}
				added = true;
				modifiableModel.addRoot(archiveRootForLocalFile, BinariesOrderRootType.getInstance());
			}
			else if(virtualFile.getFileType() == XmlFileType.INSTANCE)
			{
				modifiableModel.addRoot(virtualFile, DocumentationOrderRootType.getInstance());
			}
		}

		return added;
	}

	protected void removeInvalidDependenciesFromModule(Map<String, PackageInfo> packages, ProgressIndicator indicator)
	{
		indicator.setText("NuGet: removing old dependencies from module");

		final ModifiableRootModel modifiableModel = ApplicationManager.getApplication().runReadAction((Computable<ModifiableRootModel>) () -> ModuleRootManager.getInstance(myModule).getModifiableModel());

		OrderEntry[] orderEntries = modifiableModel.getOrderEntries();
		for(OrderEntry orderEntry : orderEntries)
		{
			if(!(orderEntry instanceof LibraryOrderEntry))
			{
				continue;
			}

			LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;
			if(!libraryOrderEntry.isModuleLevel())
			{
				continue;
			}

			String libraryName = libraryOrderEntry.getLibraryName();
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

		WriteAction.runAndWait(modifiableModel::commit);

		indicator.setText(null);
	}

	protected void removeInvalidDependenciesFromFileSystem(final Map<String, PackageInfo> packages, ProgressIndicator indicator)
	{
		indicator.setText("NuGet: removing old dependencies from file system");

		final String dir = getPackagesDirPath();
		if(dir == null)
		{
			return;
		}

		WriteAction.runAndWait(() -> {
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
		});
		indicator.setText(null);
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
