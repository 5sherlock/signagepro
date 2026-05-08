/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.Label
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DimensionCheckController {
    @FXML
    private Label file;
    @FXML
    private Label size;
    private Stage dialogStage;
    private boolean ok = false;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setFileSize(String string, String string2) {
        this.file.setText(string);
        this.size.setText(string2);
    }

    @FXML
    private void handleOk() {
        this.ok = true;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.ok = false;
        this.dialogStage.close();
    }

    public boolean getResult() {
        return this.ok;
    }
}

