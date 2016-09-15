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

/**
* @author VISTALL
* @since 23.02.2015
*/
public class NuGetDependency
{
	private final String myId;
	private final NuGetDependencyVersionInfo myDependencyVersionInfo;
	private final String myFramework;

	public NuGetDependency(String id, NuGetDependencyVersionInfo dependencyVersionInfo, String framework)
	{
		myId = id;
		myDependencyVersionInfo = dependencyVersionInfo;
		myFramework = framework;
	}

	public String getId()
	{
		return myId;
	}

	public NuGetDependencyVersionInfo getDependencyVersionInfo()
	{
		return myDependencyVersionInfo;
	}

	public String getFramework()
	{
		return myFramework;
	}
}
