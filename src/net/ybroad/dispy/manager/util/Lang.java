/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXMLLoader
 *  javafx.scene.Scene
 */
package net.ybroad.dispy.manager.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import net.ybroad.dispy.manager.util.ResourceBundleUtf8Control;

public class Lang {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("bundles.lang", new ResourceBundleUtf8Control());
    private static final String css = Lang.class.getResource(bundle.getString("app.css")).toExternalForm();

    public static void setResource(FXMLLoader fXMLLoader) {
        fXMLLoader.setResources(bundle);
    }

    public static void setStyleSheet(Scene scene) {
        scene.getStylesheets().add((Object)css);
    }

    public static String getString(String string) {
        try {
            return bundle.getString(string);
        }
        catch (Exception exception) {
            return string;
        }
    }

    public static String getString(String string, Object ... objectArray) {
        try {
            return MessageFormat.format(bundle.getString(string), objectArray);
        }
        catch (Exception exception) {
            return string;
        }
    }
}

