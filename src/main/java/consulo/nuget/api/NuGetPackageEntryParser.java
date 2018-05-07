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

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.jdom.Element;
import org.jdom.Namespace;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;

/**
 * @author VISTALL
 * @since 16.03.2015
 */
public class NuGetPackageEntryParser
{
	private static final Namespace ourAtomNamespace = Namespace.getNamespace("http://www.w3.org/2005/Atom");
	private static final Namespace ourDataServicesMetadataNamespace = Namespace.getNamespace("m",
			"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata");
	private static final Namespace ourDataServicesNamespace = Namespace.getNamespace("d", "http://schemas.microsoft.com/ado/2007/08/dataservices");

	@Nonnull
	public static Map<String, NuGetPackageEntry> parse(@Nonnull Element rootElement, @Nonnull String id, @Nonnull String repoUrl)
	{
		Map<String, NuGetPackageEntry> map = new TreeMap<String, NuGetPackageEntry>();

		List<Element> rootChildren = rootElement.getChildren("entry", ourAtomNamespace);
		for(Element element : rootChildren)
		{
			if(!"entry".equals(element.getName()))
			{
				continue;
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

						NuGetDependencyVersionInfo dependencyVersionInfo = NuGetDependencyVersionInfoParser.parse(versionInfo);
						dependencies.add(new NuGetDependency(depId, dependencyVersionInfo, frameworkName));
					}
				}

				Element versionElement = properties.getChild("Version", ourDataServicesNamespace);
				assert versionElement != null;
				NuGetPackageEntry entry = new NuGetPackageEntry(id, versionElement.getText(), contentType, contentUrl, dependencies, repoUrl);
				map.put(entry.getVersion(), entry);
			}
		}
		return map;
	}
}
