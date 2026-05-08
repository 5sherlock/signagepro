/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.CheckBox
 *  javafx.scene.control.TextArea
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class MessageDialogController {
    @FXML
    private CheckBox login;
    @FXML
    private CheckBox now;
    @FXML
    private TextArea text;
    private Stage dialogStage;
    private boolean ok = false;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public boolean getLogin() {
        return this.login.isSelected();
    }

    public boolean getNow() {
        return this.now.isSelected();
    }

    public String getText() {
        return this.text.getText();
    }

    public void setText(String string) {
        this.text.setText(string);
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

