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

package org.mustbe.consulo.nuget;

import org.jetbrains.annotations.NotNull;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

/**
 * @author VISTALL
 * @since 24.11.14
 */
public class NuGetFileTypeFactory extends FileTypeFactory
{
	@Deprecated
	public static final String PACKAGES_CONFIG = "packages.config";

	@Override
	public void createFileTypes(@NotNull FileTypeConsumer consumer)
	{
		//FIXME [VISTALL] currenly its a problem - due we need always mark *.config files as XML
		consumer.consume(XmlFileType.INSTANCE, new ExtensionFileNameMatcher("config"));
	}
}

