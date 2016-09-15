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

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.util.text.StringUtil;
import consulo.dotnet.util.ArrayUtil2;

/**
 * @author VISTALL
 * @since 16.03.2015
 */
public class NuGetDependencyVersionInfoParser
{
	@NotNull
	public static NuGetDependencyVersionInfo parse(String versionInfo)
	{
		NuGetDependencyVersionInfo dependencyVersionInfo = null;
		assert !versionInfo.isEmpty() : versionInfo;
		if(versionInfo.charAt(0) == '[' || versionInfo.charAt(0) == '(')
		{
			int indexOfComma = versionInfo.indexOf(',');

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

				if(indexOfComma == -1)
				{
					min = min.substring(0, min.length() - 1);
				}
				minVersion = NuGetVersion.parseVersion(min);
			}
			String max = ArrayUtil2.safeGet(versionList, 1);
			if(max != null)
			{
				maxCompare = toCompare(max.charAt(max.length() - 1));
				if(maxCompare != NuGetCompareType.EQ)
				{
					max = max.substring(0, max.length() - 1);
				}

				boolean undefinedVersion = max.length() == 1 && max.charAt(0) == ' ';
				if(!undefinedVersion)
				{
					maxVersion = NuGetVersion.parseVersion(max);
				}
				else
				{
					maxVersion = new NuGetVersion(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
				}
			}
			else if(indexOfComma == -1)
			{
				// if no separator max == min, version like [1.0]
				maxVersion = minVersion;
			}
			dependencyVersionInfo = new NuGetDependencyVersionInfoWithBounds(minCompare, minVersion, maxCompare, maxVersion);
		}
		else
		{
			NuGetVersion version = NuGetVersion.parseVersion(versionInfo);
			dependencyVersionInfo = new NuGetSimpleDependencyVersionInfo(version);
		}
		return dependencyVersionInfo;
	}

	private static NuGetCompareType toCompare(char c)
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
}
