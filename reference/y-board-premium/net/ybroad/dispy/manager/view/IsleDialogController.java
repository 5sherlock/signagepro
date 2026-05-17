/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.Button
 *  javafx.scene.control.TextField
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.ybroad.dispy.lib.IsleData;
import net.ybroad.dispy.manager.model.IsleServerData;

public class IsleDialogController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField idField;
    @FXML
    private TextField limitField;
    @FXML
    private TextField keyField;
    @FXML
    private Button removeButton;
    private Stage dialogStage;
    private IsleServerData origin;
    private IsleData isle;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setIsleServer(IsleServerData isleServerData) {
        this.origin = isleServerData;
        if (isleServerData == null) {
            this.removeButton.setVisible(false);
        } else {
            this.nameField.setText(isleServerData.getName());
            this.idField.setText(isleServerData.getId());
            this.idField.setEditable(false);
            this.limitField.setText(isleServerData.getLimit());
            this.keyField.setText(isleServerData.getKey());
            this.removeButton.setVisible(true);
        }
    }

    public void setRemovable(boolean bl) {
        this.removeButton.setVisible(bl);
    }

    public IsleData getIsle() {
        return this.isle;
    }

    @FXML
    private void handleOk() {
        String string = this.nameField.getText();
        String string2 = this.idField.getText();
        int n = 0;
        try {
            n = Integer.parseInt(this.limitField.getText());
        }
        catch (Exception exception) {
            return;
        }
        if (n <= 0) {
            return;
        }
        if (string.isEmpty()) {
            return;
        }
        if (string2.isEmpty()) {
            return;
        }
        if (this.origin != null && n < this.origin.getLimitInt()) {
            return;
        }
        this.isle = new IsleData();
        this.isle.id = string2;
        this.isle.name = string;
        this.isle.limit = n;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.isle = null;
        this.dialogStage.close();
    }

    @FXML
    private void handleRemove() {
        this.isle = new IsleData();
        this.isle.id = this.origin.getId();
        this.isle.name = this.origin.getName();
        this.isle.limit = -1;
        this.dialogStage.close();
    }
}

