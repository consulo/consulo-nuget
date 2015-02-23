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

/**
 * @author VISTALL
 * @since 23.02.2015
 */
public class NuGetPackageEntry
{
	private final String myId;
	private final String myVersion;
	private final String myContentType;
	private final String myContentUrl;
	private final List<NuGetDependency> myDependencies;
	private final String myRepoUrl;

	public NuGetPackageEntry(String id, String version, String contentType, String contentUrl, List<NuGetDependency> dependencies, String repoUrl)
	{
		myId = id;
		myVersion = version;
		myContentType = contentType;
		myContentUrl = contentUrl;
		myDependencies = dependencies;
		myRepoUrl = repoUrl;
	}

	public String getRepoUrl()
	{
		return myRepoUrl;
	}

	public List<NuGetDependency> getDependencies()
	{
		return myDependencies;
	}

	public String getContentType()
	{
		return myContentType;
	}

	public String getContentUrl()
	{
		return myContentUrl;
	}

	public String getId()
	{
		return myId;
	}

	public String getVersion()
	{
		return myVersion;
	}
}
