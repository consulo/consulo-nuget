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

import org.junit.Assert;
import org.junit.Test;
import consulo.nuget.api.NuGetCompareType;
import consulo.nuget.api.NuGetDependencyVersionInfoParser;
import consulo.nuget.api.NuGetDependencyVersionInfoWithBounds;
import consulo.nuget.api.NuGetVersion;

/**
 * @author VISTALL
 * @since 23.02.2015
 */
public abstract class VersionTest extends Assert
{
	@Test
	public void testDependency()
	{
		assertTrue(new NuGetDependencyVersionInfoWithBounds(NuGetCompareType.EQ, new NuGetVersion(1, 2, 3), null, null).is(new NuGetVersion(1, 2,
				3)));

		assertFalse(new NuGetDependencyVersionInfoWithBounds(NuGetCompareType.GT, new NuGetVersion(1, 2, 3), null, null).is(new NuGetVersion(1, 2,
				3)));

		assertTrue(new NuGetDependencyVersionInfoWithBounds(NuGetCompareType.GTEQ, new NuGetVersion(1, 2, 3), null, null).is(new NuGetVersion(1, 2,
				3)));

		assertTrue(new NuGetDependencyVersionInfoWithBounds(NuGetCompareType.GT, new NuGetVersion(1, 2, 3), null, null).is(new NuGetVersion(1, 2,
				4)));

		assertTrue(new NuGetDependencyVersionInfoWithBounds(NuGetCompareType.GTEQ, new NuGetVersion(1, 2, 3), NuGetCompareType.LTEQ,
				new NuGetVersion(3, 0, 0)).is(new NuGetVersion(3, 0, 0)));

		assertFalse(new NuGetDependencyVersionInfoWithBounds(NuGetCompareType.GTEQ, new NuGetVersion(1, 2, 3), NuGetCompareType.LT,
				new NuGetVersion(3, 0, 0)).is(new NuGetVersion(3, 0, 0)));

		assertTrue(new NuGetDependencyVersionInfoWithBounds(NuGetCompareType.GTEQ, new NuGetVersion(1, 2, 3), NuGetCompareType.LTEQ,
				new NuGetVersion(3, 0, 0)).is(new NuGetVersion(2, 0, 0)));
	}

	@Test
	public void testBuildNumber()
	{
		assertTrue(NuGetVersion.parseVersion("0.6.4033103-beta").equals(new NuGetVersion(0, 6, 4033103, NuGetVersion.BuildType.beta, 0)));
		assertTrue(NuGetVersion.parseVersion("4.0.10-beta2223").equals(new NuGetVersion(4, 0, 10, NuGetVersion.BuildType.beta, 2223)));
		assertTrue(NuGetVersion.parseVersion("4.0.10-beta-2223").equals(new NuGetVersion(4, 0, 10, NuGetVersion.BuildType.beta, 2223)));
		assertTrue(NuGetVersion.parseVersion("4.0.10-alpha2223").equals(new NuGetVersion(4, 0, 10, NuGetVersion.BuildType.alpha, 2223)));
		assertTrue(NuGetVersion.parseVersion("4.0.10-alpha-2223").equals(new NuGetVersion(4, 0, 10, NuGetVersion.BuildType.alpha, 2223)));
		assertTrue(NuGetVersion.parseVersion("1.0.0-beta1-20141031-01'").equals(new NuGetVersion(1, 0, 0, NuGetVersion.BuildType.beta, 1)));
		assertTrue(NuGetVersion.parseVersion("3.0").equals(new NuGetVersion(3, 0, 0)));
	}

	@Test
	public void testDependencyVersion()
	{
		NuGetDependencyVersionInfoParser.parse("[1.0.0]");
		NuGetDependencyVersionInfoParser.parse("[1.0.0, )");
	}
}
