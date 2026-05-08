/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.TextArea
 *  javafx.scene.control.TextField
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.ybroad.dispy.manager.MainApp;

public class ShellDialogController {
    @FXML
    private TextField cmd;
    @FXML
    private TextArea result;
    private Stage dialogStage;
    private MainApp mainApp;
    private String id;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage, MainApp mainApp) {
        this.dialogStage = stage;
        this.mainApp = mainApp;
    }

    @FXML
    private void handleSend() {
        String string = this.cmd.getText();
        if (this.result.getText().isEmpty()) {
            this.result.appendText("cmd > " + string);
        } else {
            this.result.appendText("\ncmd > " + string);
        }
        this.mainApp.sendShell(this.id, string);
        this.cmd.setText("");
    }

    @FXML
    private void handleOk() {
        this.dialogStage.close();
    }

    public void setDispyId(String string) {
        this.id = string;
    }

    public String getDispyId() {
        if (this.id == null) {
            return "null";
        }
        return this.id;
    }

    public void setResult(String string) {
        String string2 = this.result.getText();
        if (string2.isEmpty()) {
            this.result.setText(string);
        } else {
            this.result.appendText("\n" + string);
        }
    }

    public void close() {
        this.dialogStage.close();
    }
}

