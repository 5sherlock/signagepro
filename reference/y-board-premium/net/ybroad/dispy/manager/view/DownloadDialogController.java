/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.event.EventHandler
 *  javafx.fxml.FXML
 *  javafx.scene.control.Button
 *  javafx.scene.control.Label
 *  javafx.scene.control.ProgressBar
 *  javafx.stage.Stage
 *  javafx.stage.WindowEvent
 */
package net.ybroad.dispy.manager.view;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class DownloadDialogController {
    @FXML
    private Label name;
    @FXML
    private ProgressBar progress;
    @FXML
    private Label value;
    @FXML
    private Button open;
    @FXML
    private Button find;
    @FXML
    private Button ok;
    private Stage dialogStage;
    private File file;
    private String path;

    @FXML
    private void initialize() {
        this.open.setDisable(true);
        this.find.setDisable(true);
        this.ok.setDisable(true);
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
        stage.setOnCloseRequest((EventHandler)new EventHandler<WindowEvent>(){

            public void handle(WindowEvent windowEvent) {
                windowEvent.consume();
            }
        });
    }

    public void setFile(File file) {
        this.name.setText(file.getName());
        this.file = file;
        this.path = file.getAbsolutePath();
    }

    @FXML
    private void handleOk() {
        this.dialogStage.close();
    }

    public void close() {
        this.dialogStage.close();
    }

    public void setPercent(String string, double d) {
        if (Objects.equals(this.path, string)) {
            if (d == Double.MAX_VALUE) {
                this.progress.setProgress(1.0);
                this.value.setText("Finished");
                this.open.setDisable(false);
                this.find.setDisable(false);
                this.ok.setDisable(false);
                this.dialogStage.setOnCloseRequest(null);
            } else {
                this.progress.setProgress(d / 100.0);
                this.value.setText(String.format("%.1f %%", d));
            }
        }
    }

    @FXML
    private void handleOpen() {
        try {
            Desktop.getDesktop().open(this.file);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    @FXML
    private void handleFind() {
        try {
            Desktop.getDesktop().open(this.file.getParentFile());
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }
}

