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

/**
 * @author VISTALL
 * @since 16.03.2015
 */
public class NuGetTargetFrameworkInfo
{
	public static NuGetTargetFrameworkInfo parse(@NotNull String targetFramework)
	{
		return new NuGetTargetFrameworkInfo(StringUtil.split(targetFramework, "+"));
	}

	private List<String> myTargetFrameworks;

	public NuGetTargetFrameworkInfo(List<String> targetFrameworks)
	{
		myTargetFrameworks = targetFrameworks;
	}

	public boolean accept(@NotNull String framework)
	{
		return myTargetFrameworks.contains(framework);
	}
}
