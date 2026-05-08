/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.TextField
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.ybroad.dispy.manager.model.DispyData;

public class RenameDialogController {
    @FXML
    private TextField ownerField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField groupField;
    @FXML
    private TextField mbpsField;
    private Stage dialogStage;
    private DispyData data;
    private boolean clickOk = false;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public boolean isClickOk() {
        return this.clickOk;
    }

    public void setClient(DispyData dispyData) {
        this.data = dispyData;
        this.ownerField.setText(dispyData.getOwner());
        this.nameField.setText(dispyData.getName());
        this.groupField.setText(dispyData.group);
        this.mbpsField.setText(String.valueOf(dispyData.mbps));
    }

    public void setAllowRegister(boolean bl) {
        this.ownerField.setDisable(!bl);
        this.mbpsField.setDisable(!bl);
    }

    @FXML
    private void handleOk() {
        this.data.setOwner(this.ownerField.getText());
        this.data.setName(this.nameField.getText());
        this.data.group = this.groupField.getText();
        try {
            this.data.mbps = Integer.parseInt(this.mbpsField.getText());
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.clickOk = true;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }
}

