package org.mustbe.consulo.nuget.module.extension;

import gnu.trove.THashMap;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.consulo.lombok.annotations.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.dotnet.util.ArrayUtil2;
import org.mustbe.consulo.nuget.api.NuGetCompareType;
import org.mustbe.consulo.nuget.api.NuGetDependency;
import org.mustbe.consulo.nuget.api.NuGetDependencyVersionInfo;
import org.mustbe.consulo.nuget.api.NuGetDependencyVersionInfoWithBounds;
import org.mustbe.consulo.nuget.api.NuGetPackageEntry;
import org.mustbe.consulo.nuget.api.NuGetSimpleDependencyVersionInfo;
import org.mustbe.consulo.nuget.api.NuGetVersion;
import org.mustbe.consulo.nuget.util.NuPkgUtil;
import com.google.gson.Gson;
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
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.util.ArchiveVfsUtil;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.io.DownloadUtil;
import com.intellij.util.io.HttpRequests;
import lombok.val;

/**
 * @author VISTALL
 * @since 22.02.2015
 */
@Logger
public abstract class NuGetBasedRepositoryWorker
{
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

	private static final Namespace ourAtomNamespace = Namespace.getNamespace("http://www.w3.org/2005/Atom");
	private static final Namespace ourDataServicesMetadataNamespace = Namespace.getNamespace("m",
			"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata");
	private static final Namespace ourDataServicesNamespace = Namespace.getNamespace("d", "http://schemas.microsoft.com/ado/2007/08/dataservices");

	private static final String ourPackagesPattern = "%s/Packages(Id='%s',Version='%s')";
	private static final String ourVersionsPattern = "%s/package-versions/%s";

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
					val packageMap = new TreeMap<String, PackageInfo>();
					val packageInfoConsumer = new Consumer<PackageInfo>()
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

					for(PackageInfo packageInfo : packageMap.values())
					{
						packageInfo.setPackageEntry(resolvePackageEntry(repositoryManager, indicator, packageInfo.myId, packageInfo.myVersion));
					}

					resolveDependencies(indicator, repositoryManager, packageInfoConsumer, packageMap);

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

						NuGetPackageEntry packageEntry = value.getPackageEntry();
						String downloadUrl = packageEntry == null ? null : packageEntry.getContentUrl();
						if(downloadUrl == null)
						{
							Notifications.Bus.notify(new Notification("NuGet", "Warning", "Package is not resolved with id: " + value.getId() +
									" and version: " + value.getVersion(), NotificationType.WARNING));
							continue;
						}

						val extractDir = new File(packageDir, key);
						val downloadTarget = new File(extractDir, value.getId() + "." + value.getVersion() + ".nupkg");

						FileUtil.createParentDirs(downloadTarget);

						try
						{
							DownloadUtil.downloadContentToFile(indicator, downloadUrl, downloadTarget);

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

							Notifications.Bus.notify(new Notification("NuGet", "Warning", "Fail to download dependency with id: " + value.getId() +
							" " +
									"and version: " + value.getVersion(), NotificationType.WARNING));
						}
					}

					RefreshQueue.getInstance().refresh(false, true, null, refreshQueue.values());

					addDependenciesToModule(refreshQueue, indicator);
				}
				catch(Exception e)
				{
					LOGGER.error(e);
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
			@NotNull NuGetRepositoryManager manager,
			@NotNull Consumer<PackageInfo> packageInfoConsumer,
			@NotNull Map<String, PackageInfo> map)
	{
		PackageInfo[] packageInfos = map.values().toArray(new PackageInfo[map.size()]);

		for(PackageInfo packageInfo : packageInfos)
		{
			resolveDependenciesImpl(indicator, manager, packageInfoConsumer, map, packageInfo);
		}
	}

	private void resolveDependenciesImpl(@NotNull ProgressIndicator indicator,
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

			String[] versionsForId = getVersionsForId(indicator, packageEntry.getRepoUrl(), dependency.getId());
			if(versionsForId != null)
			{
				versionsForId = ArrayUtil.reverseArray(versionsForId);

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
				newPackageInfo.setPackageEntry(resolvePackageEntry(manager, indicator, dependency.getId(), correctVersion));
				packageInfoConsumer.consume(newPackageInfo);

				resolveDependenciesImpl(indicator, manager, packageInfoConsumer, map, newPackageInfo);
			}
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
			@NotNull final String id,
			@NotNull final String version)
	{
		for(String url : repositoryManager.getRepositories())
		{
			try
			{
				indicator.setText("NuGet: Getting info about " + id + ":" + version + " package from " + url);
				Element element = HttpRequests.request(String.format(ourPackagesPattern, url, id, version)).accept("*/*").gzip(false).connect(new
																																					  HttpRequests.RequestProcessor<Element>()
				{
					@Override
					public Element process(@NotNull HttpRequests.Request request) throws IOException
					{
						if(!request.isSuccessful())
						{
							return null;
						}
						try
						{
							return JDOMUtil.loadDocument(request.readBytes(indicator)).getRootElement();
						}
						catch(JDOMException e)
						{
							throw new IOException(e);
						}
					}
				});

				if(!"entry".equals(element.getName()))
				{
					return null;
				}

				String contentType = null;
				String contentUrl = null;
				Element content = element.getChild("content", ourAtomNamespace);
				if(content != null)
				{
					contentType = content.getAttributeValue("type");
					contentUrl = content.getAttributeValue("src");
				}

				List<NuGetDependency> dependencies = new SmartList<NuGetDependency>();

				Element properties = element.getChild("properties", ourDataServicesMetadataNamespace);
				if(properties != null)
				{
					Element dependenciesElement = properties.getChild("Dependencies", ourDataServicesNamespace);
					if(dependenciesElement != null)
					{
						String textTrim = dependenciesElement.getTextTrim();
						List<String> split = StringUtil.split(textTrim, "|");

						for(String dependencyData : split)
						{
							StringTokenizer tokenizer = new StringTokenizer(dependencyData, ":");
							String depId = tokenizer.nextToken();
							String versionInfo = tokenizer.nextToken();
							String frameworkName = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;

							NuGetDependencyVersionInfo dependencyVersionInfo = null;
							if(versionInfo.contains(","))
							{
								List<String> versionList = StringUtil.split(versionInfo, ",");
								NuGetCompareType minCompare = NuGetCompareType.EQ;
								NuGetCompareType maxCompare = NuGetCompareType.EQ;

								NuGetVersion minVersion = null;
								NuGetVersion maxVersion = null;

								String min = ArrayUtil2.safeGet(versionList, 0);
								if(min != null)
								{
									min = min.trim();
									minCompare = toCompare(min.charAt(0));
									if(minCompare != NuGetCompareType.EQ)
									{
										min = min.substring(1, min.length());
									}
									minVersion = NuGetVersion.parseVersion(min);
								}
								String max = ArrayUtil2.safeGet(versionList, 1);
								if(max != null)
								{
									max = max.trim();
									maxCompare = toCompare(max.charAt(max.length() - 1));
									if(maxCompare != NuGetCompareType.EQ)
									{
										max = max.substring(0, max.length() - 1);
									}
									maxVersion = NuGetVersion.parseVersion(max);
								}
								dependencyVersionInfo = new NuGetDependencyVersionInfoWithBounds(minCompare, minVersion, maxCompare, maxVersion);
							}
							else
							{
								dependencyVersionInfo = new NuGetSimpleDependencyVersionInfo(NuGetVersion.parseVersion(versionInfo));
							}
							dependencies.add(new NuGetDependency(depId, dependencyVersionInfo, frameworkName));
						}
					}
				}

				return new NuGetPackageEntry(id, version, contentType, contentUrl, dependencies, url);
			}
			catch(IOException e)
			{
				LOGGER.error(e);
			}
			finally
			{
				indicator.setText(null);
			}
		}
		return null;
	}

	@Nullable
	private String[] getVersionsForId(@NotNull final ProgressIndicator indicator, @NotNull String url, @NotNull String id)
	{
		try
		{
			indicator.setText("NuGet: getting versions for " + id + " package from " + url);
			return HttpRequests.request(String.format(ourVersionsPattern, url, id)).accept("*/*").gzip(false).connect(new HttpRequests
					.RequestProcessor<String[]>()
			{
				@Override
				public String[] process(@NotNull HttpRequests.Request request) throws IOException
				{
					if(!request.isSuccessful())
					{
						return null;
					}
					return new Gson().fromJson(request.getReader(indicator), String[].class);
				}
			});
		}
		catch(IOException e)
		{
			LOGGER.error(e);
			return null;
		}
		finally
		{
			indicator.setText(null);
		}
	}

	private NuGetCompareType toCompare(char c)
	{
		switch(c)
		{
			case '[':
				return NuGetCompareType.GTEQ;
			case '(':
				return NuGetCompareType.GT;
			case ']':
				return NuGetCompareType.LTEQ;
			case ')':
				return NuGetCompareType.LT;
		}
		return NuGetCompareType.EQ;
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
		indicator.setText(null);
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
		indicator.setText(null);
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
		indicator.setText(null);
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
