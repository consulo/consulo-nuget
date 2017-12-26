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

import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 23.02.2015
 */
public class NuGetDependencyVersionInfoWithBounds implements NuGetDependencyVersionInfo
{
	private NuGetCompareType myMinCompareType;
	private NuGetVersion myMinVersion;
	private NuGetCompareType myMaxCompareType;
	private NuGetVersion myMaxVersion;

	public NuGetDependencyVersionInfoWithBounds(NuGetCompareType minCompareType,
			NuGetVersion minVersion,
			NuGetCompareType maxCompareType,
			NuGetVersion maxVersion)
	{
		myMinCompareType = minCompareType;
		myMinVersion = minVersion;
		myMaxCompareType = maxCompareType;
		myMaxVersion = maxVersion;
	}

	@Override
	public boolean is(@NotNull NuGetVersion version)
	{
		if(myMinCompareType != null)
		{
			if(!compare(myMinCompareType, myMinVersion, version))
			{
				return false;
			}
		}

		if(myMaxCompareType != null)
		{
			if(!compare(myMaxCompareType, myMaxVersion, version))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean compare(@NotNull NuGetCompareType compareType, @NotNull NuGetVersion v1, @NotNull NuGetVersion v2)
	{
		int compareValue = v2.compareTo(v1);
		switch(compareType)
		{
			case GT:
				return compareValue > 0;
			case GTEQ:
				return compareValue >= 0;
			case EQ:
				return compareValue == 0;
			case LTEQ:
				return compareValue <= 0;
			case LT:
				return compareValue < 0;
		}
		return false;
	}

	@Override
	public String toString()
	{
		return "NuGetDependencyVersionInfoWithBounds{" +
				"myMinCompareType=" + myMinCompareType +
				", myMinVersion=" + myMinVersion +
				", myMaxCompareType=" + myMaxCompareType +
				", myMaxVersion=" + myMaxVersion +
				'}';
	}
}
