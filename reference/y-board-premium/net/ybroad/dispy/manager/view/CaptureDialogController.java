/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.Label
 *  javafx.scene.image.Image
 *  javafx.scene.image.ImageView
 *  javafx.scene.input.Clipboard
 *  javafx.scene.input.ClipboardContent
 *  javafx.scene.shape.Rectangle
 *  javafx.stage.FileChooser
 *  javafx.stage.FileChooser$ExtensionFilter
 *  javafx.stage.Stage
 *  javafx.stage.Window
 */
package net.ybroad.dispy.manager.view;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.ybroad.dispy.lib.Log;
import net.ybroad.dispy.lib.Util;

public class CaptureDialogController {
    @FXML
    private Label label;
    @FXML
    private ImageView image;
    @FXML
    private Rectangle border;
    private Stage dialogStage;
    private String id;
    private File file;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    @FXML
    private void handleSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add((Object)new FileChooser.ExtensionFilter("Image (*.jpg, *.jpeg)", new String[]{"*.jpg", "*.jpeg"}));
        File file = fileChooser.showSaveDialog((Window)this.dialogStage);
        if (file != null) {
            try {
                Util.cp(this.file, file);
            }
            catch (IOException iOException) {
                Log.out(iOException);
            }
        }
    }

    @FXML
    private void handleCopy() {
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putImage(this.image.getImage());
        Clipboard.getSystemClipboard().setContent((Map)clipboardContent);
    }

    @FXML
    private void handleOk() {
        this.dialogStage.close();
    }

    public void setImageSize(int n, int n2) {
        this.image.setFitWidth((double)n);
        this.image.setFitHeight((double)n2);
        this.border.setWidth((double)n);
        this.border.setHeight((double)n2);
    }

    public void setDispyId(String string) {
        this.id = string;
    }

    public String getDispyId() {
        return this.id;
    }

    public void setSnapImage(File file) {
        this.file = file;
        String string = file.toURI().toString();
        Log.out("Snapshot uri: " + string);
        this.image.setImage(new Image(string));
        this.label.setVisible(false);
    }

    public void close() {
        this.dialogStage.close();
    }
}

