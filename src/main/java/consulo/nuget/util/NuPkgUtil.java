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

package consulo.nuget.util;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.URLUtil;
import com.intellij.util.io.ZipUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 27.11.14
 * <p/>
 * This class contains method for extracting .nupkg archives. All methods are looks like {@link ZipUtil} with one different,
 * directories can contains URL escaping, like 'net20%2Bsl-20' ill extract to 'net20+sl20'
 */
public class NuPkgUtil
{
	public static void extract(@Nonnull File file, @Nonnull File outputDir, @Nullable FilenameFilter filenameFilter) throws IOException
	{
		extract(file, outputDir, filenameFilter, true);
	}

	public static void extract(@Nonnull File file,
			@Nonnull File outputDir,
			@Nullable FilenameFilter filenameFilter,
			boolean overwrite) throws IOException
	{
		try (ZipFile zipFile = new ZipFile(file))
		{
			extract(zipFile, outputDir, filenameFilter, overwrite);
		}
	}

	public static void extract(final @Nonnull ZipFile zipFile, @Nonnull File outputDir, @Nullable FilenameFilter filenameFilter) throws IOException
	{
		extract(zipFile, outputDir, filenameFilter, true);
	}

	public static void extract(final @Nonnull ZipFile zipFile,
			@Nonnull File outputDir,
			@Nullable FilenameFilter filenameFilter,
			boolean overwrite) throws IOException
	{
		final Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while(entries.hasMoreElements())
		{
			ZipEntry entry = entries.nextElement();
			final File file = new File(outputDir, entry.getName());
			if(filenameFilter == null || filenameFilter.accept(file.getParentFile(), file.getName()))
			{
				extractEntry(entry, zipFile.getInputStream(entry), outputDir, overwrite);
			}
		}
	}

	public static void extractEntry(ZipEntry entry, final InputStream inputStream, File outputDir) throws IOException
	{
		extractEntry(entry, inputStream, outputDir, true);
	}

	public static void extractEntry(ZipEntry entry, final InputStream inputStream, File outputDir, boolean overwrite) throws IOException
	{
		final boolean isDirectory = entry.isDirectory();
		final String relativeName = entry.getName();
		final File file = new File(outputDir, URLUtil.unescapePercentSequences(relativeName));
		if(file.exists() && !overwrite)
		{
			return;
		}

		FileUtil.createParentDirs(file);
		if(isDirectory)
		{
			file.mkdir();
		}
		else
		{
			try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file)); BufferedInputStream is = new BufferedInputStream(inputStream))
			{
				FileUtil.copy(is, os);
			}
		}
	}
}
