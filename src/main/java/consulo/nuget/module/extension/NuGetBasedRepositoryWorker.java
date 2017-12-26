package consulo.nuget.module.extension;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.BundleBase;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.ModuleLibraryOrderEntryImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.io.DownloadUtil;
import com.intellij.util.io.HttpRequests;
import consulo.annotations.RequiredReadAction;
import consulo.dotnet.dll.DotNetModuleFileType;
import consulo.nuget.api.NuGetDependency;
import consulo.nuget.api.NuGetPackageEntry;
import consulo.nuget.api.NuGetPackageEntryParser;
import consulo.nuget.api.NuGetTargetFrameworkInfo;
import consulo.nuget.api.NuGetVersion;
import consulo.nuget.util.NuPkgUtil;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.DocumentationOrderRootType;
import consulo.vfs.util.ArchiveVfsUtil;

/**
 * @author VISTALL
 * @since 22.02.2015
 */
public abstract class NuGetBasedRepositoryWorker
{
	private static final Logger LOGGER = Logger.getInstance(NuGetBasedRepositoryWorker.class);

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

	private static final String ourPackagesFilterPattern = "{0}/Packages()?$filter=Id%20eq%20%27{1}%27&includePrerelease=true";

	protected final AtomicBoolean myProgress = new AtomicBoolean();
	protected final Module myModule;

	public NuGetBasedRepositoryWorker(Module module)
	{
		myModule = module;
	}

	@Nullable
	protected abstract String getPackagesDirPath();

	@RequiredReadAction
	protected abstract void loadDefinedPackages(@NotNull Consumer<PackageInfo> packageInfoConsumer);

	@NotNull
	public String getNameAndVersionSeparator()
	{
		return ".";
	}

	@NotNull
	public NuGetRepositoryManager getRepositoryManager()
	{
		return new NuGetRepositoryManager()
		{
			@NotNull
			@Override
			public List<String> getRepositories()
			{
				return Collections.singletonList("http://nuget.org/api/v2");
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
			public void run(@NotNull ProgressIndicator indicator)
			{
				try
				{
					final Map<String, PackageInfo> packageMap = new LinkedHashMap<String, PackageInfo>();
					final Consumer<PackageInfo> packageInfoConsumer = new Consumer<PackageInfo>()
					{
						@Override
						public void consume(PackageInfo packageInfo)
						{
							String key = packageInfo.getId() + getNameAndVersionSeparator() + packageInfo.getVersion();
							if(!packageMap.containsKey(key))
							{
								packageMap.put(key, packageInfo);
							}
						}
					};

					ApplicationManager.getApplication().runReadAction(new Runnable()
					{
						@Override
						public void run()
						{
							loadDefinedPackages(packageInfoConsumer);
						}
					});

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
						String downloadUrl = packageEntry == null ? null : packageEntry.getContentUrl();
						if(downloadUrl == null)
						{
							Notifications.Bus.notify(new Notification("NuGet", "Warning", "Package is not resolved with id: " + value.getId() +
									" and version: " + value.getVersion(), NotificationType.WARNING));
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

								Notifications.Bus.notify(new Notification("NuGet", "Warning", "Fail to download dependency with id: " + value.getId() +
										" " +
										"and version: " + value.getVersion(), NotificationType.WARNING));
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
					NuGetBasedRepositoryWorker.LOGGER.error(e);
				}
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

	public void resolveDependencies(@NotNull ProgressIndicator indicator,
			@NotNull NuGetRequestQueue requestQueue,
			@NotNull NuGetRepositoryManager manager,
			@NotNull Consumer<PackageInfo> packageInfoConsumer,
			@NotNull Map<String, PackageInfo> map)
	{
		PackageInfo[] packageInfos = map.values().toArray(new PackageInfo[map.size()]);

		for(PackageInfo packageInfo : packageInfos)
		{
			resolveDependenciesImpl(indicator, requestQueue, manager, packageInfoConsumer, map, packageInfo);
		}
	}

	private void resolveDependenciesImpl(@NotNull ProgressIndicator indicator,
			@NotNull NuGetRequestQueue requestQueue,
			@NotNull NuGetRepositoryManager manager,
			@NotNull Consumer<PackageInfo> packageInfoConsumer,
			@NotNull Map<String, PackageInfo> map,
			@NotNull PackageInfo packageInfo)
	{
		NuGetPackageEntry packageEntry = packageInfo.getPackageEntry();
		if(packageEntry == null)
		{
			return;
		}

		List<NuGetDependency> dependencies = packageEntry.getDependencies();

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
			packageInfoConsumer.consume(newPackageInfo);

			resolveDependenciesImpl(indicator, requestQueue, manager, packageInfoConsumer, map, newPackageInfo);
		}
	}

	private boolean isAlreadyInstalled(@NotNull Map<String, PackageInfo> packageInfoMap, @NotNull NuGetDependency dependency)
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
	protected NuGetPackageEntry resolvePackageEntry(@NotNull final NuGetRepositoryManager repositoryManager,
			@NotNull final ProgressIndicator indicator,
			@NotNull final NuGetRequestQueue requestQueue,
			@NotNull final String id,
			@NotNull final String version)
	{
		return requestPackageEntries(repositoryManager, indicator, requestQueue, id).get(version);
	}

	@Nullable
	private Collection<String> getVersionsForId(@NotNull final NuGetRepositoryManager repositoryManager,
			@NotNull final ProgressIndicator indicator,
			@NotNull NuGetRequestQueue requestQueue,
			@NotNull String id)
	{
		return requestPackageEntries(repositoryManager, indicator, requestQueue, id).keySet();
	}

	@NotNull
	protected Map<String, NuGetPackageEntry> requestPackageEntries(@NotNull final NuGetRepositoryManager repositoryManager,
			@NotNull final ProgressIndicator indicator,
			@NotNull final NuGetRequestQueue requestQueue,
			@NotNull final String id)
	{
		for(final String url : repositoryManager.getRepositories())
		{
			try
			{
				indicator.setText("NuGet: Getting info about " + id + " package from " + url);

				Map<String, NuGetPackageEntry> map = requestQueue.request(BundleBase.format(ourPackagesFilterPattern, url, id), new HttpRequests.RequestProcessor<Map<String, NuGetPackageEntry>>()

				{
					@Override
					public Map<String, NuGetPackageEntry> process(@NotNull HttpRequests.Request request) throws IOException
					{
						if(!request.isSuccessful())
						{
							return null;
						}
						try
						{
							Element rootElement = JDOMUtil.loadDocument(request.readBytes(indicator)).getRootElement();
							return NuGetPackageEntryParser.parse(rootElement, id, url);
						}
						catch(JDOMException e)
						{
							throw new IOException(e);
						}
					}
				});

				if(map == null)
				{
					continue;
				}

				return map;
			}
			finally
			{
				indicator.setText(null);
			}
		}
		return Collections.emptyMap();
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

		new WriteAction<Object>()
		{
			@Override
			protected void run(Result<Object> result) throws Throwable
			{
				modifiableRootModel.commit();
			}
		}.execute();
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

		new WriteAction<Object>()
		{
			@Override
			protected void run(Result<Object> result) throws Throwable
			{
				modifiableModel.commit();
			}
		}.execute();

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

		new WriteAction<Object>()
		{
			@Override
			protected void run(Result<Object> result) throws Throwable
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
		}.execute();
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
