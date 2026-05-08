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

public class MemberEditDialogController {
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
    public static final int ACTION_EDIT = 1;
    public static final int ACTION_UNREGISTER = 2;
    private int clickAction = 0;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setUser(UserData userData) {
        this.data = new UserData();
        this.data.name = userData.name;
        this.data.member = true;
        this.nameField.setText(userData.name);
        if (userData.limit > 0) {
            this.limitField.setText(String.valueOf(userData.limit));
        }
        this.memoArea.setText(userData.memo.replace("\\n", "\n"));
    }

    public UserData getUser() {
        return this.data;
    }

    public int getClickAction() {
        return this.clickAction;
    }

    @FXML
    private void handleOk() {
        String string = this.nameField.getText();
        if (string.isEmpty()) {
            return;
        }
        this.data.name = string;
        this.data.pw = this.pwField.getText();
        try {
            this.data.limit = Integer.parseInt(this.limitField.getText());
        }
        catch (Exception exception) {
            this.data.limit = 0;
        }
        this.data.memo = this.memoArea.getText().replace("\n", "\\n");
        this.clickAction = 1;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }

    @FXML
    private void handleUnregister() {
        this.data.member = false;
        this.clickAction = 2;
        this.dialogStage.close();
    }
}

