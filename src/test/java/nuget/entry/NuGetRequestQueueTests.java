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

package nuget.entry;

import com.intellij.util.io.HttpRequests;
import consulo.nuget.xml.module.extension.NuGetRequestQueue;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 15.10.2015
 */
public abstract class NuGetRequestQueueTests extends Assert
{
	private final NuGetRequestQueue testObj = new NuGetRequestQueue();

	@Test
	public void testNuGetOrg()
	{
		testRequest("https://nuget.org/api/v2/FindPackagesById()?id=%27NLog%27&includePrerelease=true");
	}

	@Test
	public void testDargonIo()
	{
		testRequest("https://nuget.dargon.io/FindPackagesById()?id=%27NLog%27&includePrerelease=true");
	}

	private void testRequest(String url) {
		final String kExpectedResult = "expected_result";
		String result = testObj.request(url, new HttpRequests.RequestProcessor<String>() {
			@Override
			public String process(@Nonnull HttpRequests.Request request) throws IOException
			{
				System.out.println("Request Successful:" + request.isSuccessful());
				return kExpectedResult;
			}
		});
		System.out.println("Expected result: '" + kExpectedResult + "' and got: '" + result + "' for url " +     url);
		assertEquals(kExpectedResult, result);
	}
}