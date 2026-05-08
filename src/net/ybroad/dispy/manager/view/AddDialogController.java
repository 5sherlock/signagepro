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
import net.ybroad.dispy.manager.util.Lang;

public class AddDialogController {
    @FXML
    private TextField idField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField groupField;
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
    }

    public void setUnion(boolean bl) {
        this.idField.setDisable(true);
        this.idField.setText(Lang.getString("add.union"));
    }

    @FXML
    private void handleOk() {
        if (!this.idField.isDisabled()) {
            this.data.setId(this.idField.getText());
        }
        this.data.setName(this.nameField.getText());
        this.data.group = this.groupField.getText();
        this.clickOk = true;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }
}

