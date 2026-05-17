/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.Button
 *  javafx.scene.control.Label
 *  javafx.scene.control.TextArea
 *  javafx.stage.FileChooser
 *  javafx.stage.Stage
 *  javafx.stage.Window
 */
package net.ybroad.dispy.manager.view;

import java.io.File;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.ybroad.dispy.manager.MainApp;

public class MediainfoDialogController {
    @FXML
    private Label name;
    @FXML
    private TextArea info;
    @FXML
    private Button download;
    private Stage dialogStage;
    private MainApp mainApp;
    private String play;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage, MainApp mainApp) {
        this.dialogStage = stage;
        this.mainApp = mainApp;
    }

    public void setInfo(String string, String string2, String string3) {
        this.play = string;
        this.name.setText(string2);
        this.info.setText(string3);
    }

    public void setShowDownload(boolean bl) {
        this.download.setVisible(bl);
    }

    @FXML
    private void handleOk() {
        this.dialogStage.close();
    }

    @FXML
    private void handleDownload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(this.name.getText());
        File file = fileChooser.showSaveDialog((Window)this.dialogStage);
        if (file != null) {
            this.mainApp.showDownloadDialog(this.play, this.name.getText(), file);
        }
    }

    public void close() {
        this.dialogStage.close();
    }
}

