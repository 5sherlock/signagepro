/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 *  javafx.fxml.FXML
 *  javafx.scene.control.Label
 *  javafx.scene.layout.AnchorPane
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class WaitDialogController {
    @FXML
    private AnchorPane pane;
    @FXML
    private Label title;
    @FXML
    private Label content;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        this.title.setText("");
        this.content.setText("");
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void set(String string, String string2) {
        Platform.runLater(() -> {
            this.title.setText(string);
            this.content.setText(string2);
            this.dialogStage.sizeToScene();
        });
    }

    public void show() {
        this.dialogStage.show();
    }

    public void hide() {
        this.dialogStage.hide();
    }
}

