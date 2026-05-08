/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 *  javafx.fxml.FXML
 *  javafx.scene.control.CheckBox
 *  javafx.scene.control.ComboBox
 *  javafx.scene.control.PasswordField
 *  javafx.scene.control.TextField
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.ybroad.dispy.lib.Log;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.manager.util.Lang;

public class LoginDialogController {
    @FXML
    private ComboBox<String> serverCombo;
    @FXML
    private TextField nameField;
    @FXML
    private PasswordField pwField;
    @FXML
    private CheckBox autoCheck;
    private Stage dialogStage;
    private UserData data;
    public static final int ACTION_CANCEL = 0;
    public static final int ACTION_LOGIN = 1;
    public static final int ACTION_REGISTER = 2;
    private int clickAction = 0;

    @FXML
    private void initialize() {
        if (this.serverCombo != null) {
            String string = Lang.getString("login.server.default");
            String string2 = Lang.getString("login.server.custom");
            this.serverCombo.getItems().addAll((Object[])new String[]{string, string2});
            this.serverCombo.getSelectionModel().selectedItemProperty().addListener((observableValue, string3, string4) -> Platform.runLater(() -> {
                if (string4 == null) {
                    this.serverCombo.getEditor().setText("");
                    this.serverCombo.getEditor().setEditable(true);
                } else if (string4.equals(string)) {
                    this.serverCombo.getEditor().setEditable(false);
                } else if (string4.equals(string2)) {
                    this.serverCombo.getSelectionModel().clearSelection();
                }
            }));
            try {
                File file = new File("server.txt");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(file), "UTF-8"));
                String string5 = bufferedReader.readLine();
                bufferedReader.close();
                if (string5.contains(":")) {
                    string5 = string5.substring(0, string5.indexOf(":"));
                }
                this.serverCombo.getEditor().setText(string5);
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (this.serverCombo.getEditor().getText().isEmpty()) {
                this.serverCombo.getSelectionModel().select(0);
            } else {
                this.serverCombo.getEditor().setEditable(false);
            }
        }
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
    private void handleLogin() {
        if (this.serverCombo != null) {
            try {
                File file = new File("server.txt");
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(file), "UTF-8"));
                if (this.serverCombo.getSelectionModel().getSelectedIndex() == 0) {
                    bufferedWriter.write("");
                } else {
                    String string = this.serverCombo.getEditor().getText();
                    if (!string.contains(":")) {
                        string = string + ":10080";
                    }
                    bufferedWriter.write(string);
                }
                bufferedWriter.close();
            }
            catch (Exception exception) {
                Log.out(exception);
            }
        }
        this.data.name = this.nameField.getText();
        this.data.pw = this.pwField.getText();
        if (this.autoCheck.isSelected()) {
            LoginDialogController.saveUser(this.data);
        }
        this.data.member = false;
        if (this.data.name.isEmpty() || this.data.pw.isEmpty()) {
            return;
        }
        this.clickAction = 1;
        this.dialogStage.close();
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
        this.clickAction = 2;
        this.dialogStage.close();
    }

    public static void saveUser(UserData userData) {
        File file = new File("u1.dat");
        String string = userData.name + "\n" + userData.pw;
        byte[] byArray = string.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < byArray.length; ++i) {
            int n = i;
            byArray[n] = (byte)(byArray[n] ^ 57 + byArray.length - i);
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);){
            fileOutputStream.write(byArray.length);
            fileOutputStream.write(byArray);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static UserData loadUser() {
        UserData userData = null;
        File file = new File("u1.dat");
        try (FileInputStream fileInputStream = new FileInputStream(file);){
            int n = fileInputStream.read();
            byte[] byArray = new byte[n];
            fileInputStream.read(byArray);
            for (int i = 0; i < byArray.length; ++i) {
                int n2 = i;
                byArray[n2] = (byte)(byArray[n2] ^ 57 + byArray.length - i);
            }
            String string = new String(byArray, StandardCharsets.UTF_8);
            String[] stringArray = string.split("\n");
            userData = new UserData();
            userData.name = stringArray[0];
            if (stringArray.length > 1) {
                userData.pw = stringArray[1];
            }
        }
        catch (Exception exception) {
            userData = null;
        }
        return userData;
    }
}

