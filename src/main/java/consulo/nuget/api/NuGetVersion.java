/*
 * Copyright 2013-2015 must-be.org
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

package consulo.nuget.api;

import consulo.dotnet.util.ArrayUtil2;
import consulo.logging.Logger;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 23.02.2015
 */
public class NuGetVersion
{
	private static final Logger LOG = Logger.getInstance(NuGetVersion.class);

	public static NuGetVersion parseVersion(String v)
	{
		try
		{
			List<String> split = StringUtil.split(v, ".");
			final String lastNumberTemp = ArrayUtil2.safeGet(split, 2);
			String bugfixNumber = lastNumberTemp;
			BuildType buildType = null;
			int build = 0;

			if(lastNumberTemp != null && lastNumberTemp.contains("-"))
			{
				int i = lastNumberTemp.indexOf('-');

				bugfixNumber = lastNumberTemp.substring(0, i);

				String buildInfo = lastNumberTemp.substring(i + 1, lastNumberTemp.length());
				for(BuildType temp : BuildType.values())
				{
					if(buildInfo.startsWith(temp.name()))
					{
						buildType = temp;

						String afterBuildTypeText = buildInfo.substring(temp.name().length(), buildInfo.length());
						if(!afterBuildTypeText.isEmpty())
						{
							if(afterBuildTypeText.charAt(0) == '-')
							{
								afterBuildTypeText = afterBuildTypeText.substring(1, afterBuildTypeText.length());
							}
							int j = afterBuildTypeText.indexOf('-');
							if(j == -1)
							{
								build = Integer.parseInt(afterBuildTypeText);
							}
							else
							{
								build = Integer.parseInt(afterBuildTypeText.substring(0, j));
							}
						}
						break;
					}
				}

				if(buildType == null)
				{
					try
					{
						buildType = BuildType.__custom;
						build = Integer.parseInt(buildInfo);
					}
					catch(NumberFormatException e)
					{
						// if we failed we dont interest in it
					}
				}
			}
			int bugfix = 0;
			if(bugfixNumber != null)
			{
				bugfix = Integer.parseInt(bugfixNumber);
			}

			buildType = ObjectUtil.notNull(buildType, BuildType.___release);
			return new NuGetVersion(Integer.parseInt(split.get(0)), Integer.parseInt(split.get(1)), bugfix, buildType, build);
		}
		catch(Exception e)
		{
			LOG.error("Problem with parsing version '" + v + "'", e);
			return new NuGetVersion(0, 0, 0);
		}
	}

	public enum BuildType
	{
		alpha,
		beta,
		rc,
		__custom,
		___release
	}

	public final int major;
	public final int minor;
	public final int bugfix;

	public final BuildType myBuildType;
	public final int build;

	public NuGetVersion(int major, int minor, int bugfix)
	{
		this(major, minor, bugfix, BuildType.___release, 0);
	}

	public NuGetVersion(int major, int minor, int bugfix, @Nonnull BuildType buildType, int build)
	{
		this.bugfix = bugfix;
		this.minor = minor;
		this.major = major;
		this.myBuildType = buildType;
		this.build = build;
	}

	public int compareTo(NuGetVersion version)
	{
		int result;

		result = this.major - version.major;
		if(result != 0)
		{
			return result;
		}

		result = this.minor - version.minor;
		if(result != 0)
		{
			return result;
		}

		result = this.bugfix - version.bugfix;
		if(result != 0)
		{
			return result;
		}

		result = this.build - version.build;
		if(result != 0)
		{
			return result;
		}

		result = this.myBuildType.ordinal() - version.myBuildType.ordinal();
		if(result != 0)
		{
			return result;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof NuGetVersion && compareTo((NuGetVersion) obj) == 0;
	}

	@Override
	public String toString()
	{
		return "NuGetVersion{" +
				"major=" + major +
				", minor=" + minor +
				", bugfix=" + bugfix +
				", type=" + myBuildType +
				", build=" + build +
				'}';
	}
}
