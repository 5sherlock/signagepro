/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.PasswordField
 *  javafx.scene.control.TextArea
 *  javafx.scene.control.TextField
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.ybroad.dispy.lib.UserData;

public class RegisterDialogController {
    @FXML
    private TextField nameField;
    @FXML
    private PasswordField pwField;
    @FXML
    private TextField limitField;
    @FXML
    private TextArea memoArea;
    private Stage dialogStage;
    private UserData data;
    public static final int ACTION_CANCEL = 0;
    public static final int ACTION_LOGIN = 1;
    public static final int ACTION_REGISTER = 2;
    private int clickAction = 0;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setUser(UserData userData) {
        this.data = userData;
        this.nameField.setText(userData.name);
        userData.member = false;
        if (userData.name.isEmpty()) {
            this.nameField.requestFocus();
        } else {
            this.pwField.requestFocus();
        }
    }

    public UserData getUser() {
        return this.data;
    }

    public int getClickAction() {
        return this.clickAction;
    }

    @FXML
    private void handleCancel() {
        this.data.name = this.nameField.getText();
        this.dialogStage.close();
    }

    @FXML
    private void handleRegister() {
        this.data.name = this.nameField.getText();
        this.data.pw = this.pwField.getText();
        this.data.member = true;
        try {
            this.data.limit = Integer.parseInt(this.limitField.getText());
        }
        catch (Exception exception) {
            this.data.limit = 0;
        }
        this.data.memo = this.memoArea.getText().replace("\n", "\\n");
        this.clickAction = 2;
        this.dialogStage.close();
    }
}

