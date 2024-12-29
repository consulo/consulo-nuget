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

import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 16.03.2015
 */
public class NuGetTargetFrameworkInfo
{
	public static NuGetTargetFrameworkInfo parse(@Nonnull String targetFramework)
	{
		return new NuGetTargetFrameworkInfo(StringUtil.split(targetFramework, "+"));
	}

	private List<String> myTargetFrameworks;

	public NuGetTargetFrameworkInfo(List<String> targetFrameworks)
	{
		myTargetFrameworks = targetFrameworks;
	}

	public boolean accept(@Nonnull String framework)
	{
		return myTargetFrameworks.contains(framework);
	}
}
