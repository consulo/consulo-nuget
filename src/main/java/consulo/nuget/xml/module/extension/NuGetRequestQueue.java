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

package consulo.nuget.xml.module.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16.03.2015
 */
public class NuGetRequestQueue
{
	private static final Logger LOGGER = Logger.getInstance(NuGetRequestQueue.class);

	private Map<String, Object> myCache = new HashMap<String, Object>();

	@SuppressWarnings("unchecked")
	public <T> T request(String url, HttpRequests.RequestProcessor<T> requestProcessor)
	{
		Object o = myCache.get(url);
		if(o != null)
		{
			return (T) o;
		}

		T value = null;
		try
		{
			value = HttpRequests.request(url).accept("*/*").gzip(false).connect(requestProcessor);
		}
		catch(IOException e)
		{
			LOGGER.warn("Failed to execute url: " + url, e);
		}

		myCache.put(url, value);
		return value;
	}
}
