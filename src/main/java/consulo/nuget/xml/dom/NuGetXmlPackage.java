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

package consulo.nuget.xml.dom;

import consulo.xml.util.xml.Attribute;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.GenericAttributeValue;
import consulo.xml.util.xml.Required;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public interface NuGetXmlPackage extends DomElement
{
	@Nonnull
	@Attribute
	@Required
	GenericAttributeValue<String> getId();

	@Nonnull
	@Attribute
	@Required
	GenericAttributeValue<String> getVersion();

	@Nonnull
	@Attribute("targetFramework")
	@Required
	GenericAttributeValue<String> getTargetFramework();
}
