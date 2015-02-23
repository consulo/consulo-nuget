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

package org.mustbe.consulo.nuget.api;

import java.util.List;

import org.consulo.lombok.annotations.Logger;
import org.mustbe.consulo.dotnet.util.ArrayUtil2;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author VISTALL
 * @since 23.02.2015
 */
@Logger
public class NuGetVersion
{

	public static NuGetVersion parseVersion(String v)
	{
		try
		{
			List<String> split = StringUtil.split(v, ".");
			final String lastNumberTemp = ArrayUtil2.safeGet(split, 2);
			String bugfixNumber = lastNumberTemp;
			Type type = Type.release;
			int build = 0;
			if(lastNumberTemp != null && lastNumberTemp.contains("-"))
			{
				int i = lastNumberTemp.indexOf('-');

				bugfixNumber = lastNumberTemp.substring(0, i);

				String buildInfo = lastNumberTemp.substring(i + 1, lastNumberTemp.length());
				if(buildInfo.startsWith("beta-"))
				{
					type = Type.beta;
					build = Integer.parseInt(buildInfo.substring(5, buildInfo.length()));
				}
				else if(buildInfo.startsWith("beta"))
				{
					type = Type.beta;
					build = Integer.parseInt(buildInfo.substring(4, buildInfo.length()));
				}
				else
				{
					build = Integer.parseInt(buildInfo);
				}
			}
			int bugfix = 0;
			if(bugfixNumber != null)
			{
				bugfix = Integer.parseInt(bugfixNumber);
			}
			return new NuGetVersion(Integer.parseInt(split.get(0)), Integer.parseInt(split.get(1)), bugfix, type, build);
		}
		catch(Exception e)
		{
			LOGGER.error("Problem with parsing version '" + v + "'", e);
			return new NuGetVersion(0, 0, 0);
		}
	}

	public enum Type
	{
		beta,
		release,
	}

	public final int major;
	public final int minor;
	public final int bugfix;

	public final Type type;
	public final int build;

	public NuGetVersion(int major, int minor, int bugfix)
	{
		this(major, minor, bugfix, Type.release, 0);
	}

	public NuGetVersion(int major, int minor, int bugfix, Type type, int build)
	{
		this.bugfix = bugfix;
		this.minor = minor;
		this.major = major;
		this.type = type;
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

		result = this.type.ordinal() - version.type.ordinal();
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
}
