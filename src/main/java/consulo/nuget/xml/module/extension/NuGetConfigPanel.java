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

package consulo.nuget.xml.module.extension;

import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.ide.highlighter.XmlFileType;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

/**
 * @author VISTALL
 * @since 26.12.14
 */
public class NuGetConfigPanel extends JPanel
{
	public NuGetConfigPanel(final NuGetOldMutableModuleExtension moduleExtension)
	{
		super(new VerticalFlowLayout(VerticalFlowLayout.TOP));

		final JBTextField textField = new JBTextField();

		textField.getEmptyText().setText(VirtualFileUtil.urlToPath(moduleExtension.getModule().getModuleDirUrl() + "/" + NuGetOldModuleExtension.PACKAGES_CONFIG));

		String configFileUrl = moduleExtension.getConfigFileUrl();
		if(!StringUtil.isEmpty(configFileUrl))
		{
			textField.setText(FileUtil.toSystemDependentName(VirtualFileUtil.urlToPath(configFileUrl)));
		}

		TextFieldWithBrowseButton browseButton = new TextFieldWithBrowseButton(textField);
		browseButton.addBrowseFolderListener("Select File", "Select NuGet package config file", moduleExtension.getProject(),
				FileChooserDescriptorFactory.createSingleFileDescriptor(XmlFileType.INSTANCE), new TextComponentAccessor<JTextField>()
		{
			@Override
			public String getText(JTextField component)
			{
				return FileUtil.toSystemDependentName(component.getText());
			}

			@Override
			public void setText(JTextField component, String text)
			{
				component.setText(FileUtil.toSystemDependentName(text));
			}
		});
		textField.getDocument().addDocumentListener(new DocumentAdapter()
		{
			@Override
			protected void textChanged(DocumentEvent e)
			{
				moduleExtension.setConfigFilePath(FileUtil.toSystemIndependentName(textField.getText()));
			}
		});
		add(LabeledComponent.create(browseButton, "Config file:"));
	}
}
