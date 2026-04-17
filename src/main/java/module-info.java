/**
 * @author VISTALL
 * @since 08/01/2023
 */
module consulo.nuget {
    requires consulo.ide.api;

    requires consulo.language.editor.api;
    requires consulo.external.service.api;
    requires consulo.repository.ui.api;
    requires consulo.http.api;
    requires consulo.ui.ex.awt.api;

    requires consulo.application.api;
    requires consulo.application.content.api;
    requires consulo.component.api;
    requires consulo.disposer.api;
    requires consulo.file.chooser.api;
    requires consulo.file.editor.api;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.project.api;
    requires consulo.project.ui.api;
    requires consulo.ui.api;
    requires consulo.logging.api;
    requires consulo.localize.api;
    requires consulo.virtual.file.system.api;
    requires consulo.util.collection;
    requires consulo.util.concurrent;
    requires consulo.util.io;
    requires consulo.util.jdom;
    requires consulo.util.lang;

    requires com.intellij.xml.api;
    requires com.intellij.xml.dom.api;
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