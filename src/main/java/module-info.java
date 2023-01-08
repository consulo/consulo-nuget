/**
 * @author VISTALL
 * @since 08/01/2023
 */
module consulo.nuget
{
	requires consulo.ide.api;

	requires com.intellij.xml;
	requires consulo.dotnet.api;

	requires com.google.gson;

	// TODO [VISTALL] remove in future
	requires java.desktop;

	exports consulo.nuget;
	exports consulo.nuget.action;
	exports consulo.nuget.api;
	exports consulo.nuget.api.v3;
	exports consulo.nuget.icon;
	exports consulo.nuget.manage;
	exports consulo.nuget.module.extension;
	exports consulo.nuget.util;
	exports consulo.nuget.xml;
	exports consulo.nuget.xml.dom;
	exports consulo.nuget.xml.module.extension;

	opens consulo.nuget.xml.dom to com.intellij.xml;
}