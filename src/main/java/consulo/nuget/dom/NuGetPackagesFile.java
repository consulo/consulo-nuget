/*
 * Copyright 2013-2014 must-be.org
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

package consulo.nuget.dom;

import java.util.List;

import javax.annotation.Nonnull;
import com.intellij.util.xml.DefinesXml;
import com.intellij.util.xml.DomElement;

/**
 * @author VISTALL
 * @since 24.11.14
 */
@DefinesXml
public interface NuGetPackagesFile extends DomElement
{
	@Nonnull
	List<NuGetPackage> getPackages();
}
