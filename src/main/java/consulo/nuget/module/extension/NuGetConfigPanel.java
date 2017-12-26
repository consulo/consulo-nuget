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

package consulo.nuget.module.extension;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;

/**
 * @author VISTALL
 * @since 26.12.14
 */
public class NuGetConfigPanel extends JPanel
{
	public NuGetConfigPanel(final NuGetMutableModuleExtension moduleExtension)
	{
		super(new VerticalFlowLayout(VerticalFlowLayout.TOP));

		final JBTextField textField = new JBTextField();

		textField.getEmptyText().setText(VfsUtil.urlToPath(moduleExtension.getModule().getModuleDirUrl() + "/" + NuGetModuleExtension.PACKAGES_CONFIG));

		String configFileUrl = moduleExtension.getConfigFileUrl();
		if(!StringUtil.isEmpty(configFileUrl))
		{
			textField.setText(FileUtil.toSystemDependentName(VfsUtil.urlToPath(configFileUrl)));
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
