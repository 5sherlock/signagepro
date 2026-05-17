/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.Button
 *  javafx.scene.control.Separator
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.manager.MainApp;
import net.ybroad.dispy.manager.model.DispyData;

public class RootLayoutController {
    @FXML
    private Button loginButton;
    @FXML
    private Button logoutButton;
    @FXML
    private Button addButton;
    @FXML
    private Button dropButton;
    @FXML
    private Button renameButton;
    @FXML
    private Button unionButton;
    @FXML
    private Button copyButton;
    @FXML
    private Button exportButton;
    @FXML
    private Separator adminSeparator;
    @FXML
    private Button apkButton;
    @FXML
    private Button cutButton;
    @FXML
    private Button shellButton;
    @FXML
    private Button exitButton;
    @FXML
    private Button restartButton;
    @FXML
    private Button rebootButton;
    @FXML
    private Button aboutButton;
    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.unionButton.setVisible(false);
        this.unionButton.setManaged(false);
        this.dropButton.setVisible(false);
        this.dropButton.setManaged(false);
        this.adminSeparator.setVisible(false);
        this.apkButton.setVisible(false);
        this.cutButton.setVisible(false);
        this.shellButton.setVisible(false);
        this.exitButton.setVisible(false);
        this.restartButton.setVisible(false);
        this.rebootButton.setVisible(false);
    }

    public void connectionChanged(boolean bl, UserData userData) {
        boolean bl2 = userData != null;
        boolean bl3 = bl2 && userData.name.startsWith("admin");
        this.loginButton.setDisable(bl);
        this.logoutButton.setDisable(!bl);
        this.addButton.setDisable(!bl);
        this.unionButton.setDisable(!bl);
        this.unionButton.setVisible(bl && bl3);
        this.unionButton.setManaged(bl && bl3);
        this.adminSeparator.setVisible(bl && bl2);
        this.dropButton.setVisible(bl && bl3);
        this.dropButton.setManaged(bl && bl3);
        this.apkButton.setVisible(bl && bl3);
        this.apkButton.setManaged(bl && bl3);
        this.cutButton.setVisible(bl && userData.name.equals("admin"));
        this.cutButton.setManaged(bl && userData.name.equals("admin"));
        this.shellButton.setVisible(bl && userData.name.equals("admin"));
        this.shellButton.setManaged(bl && userData.name.equals("admin"));
        this.shellButton.setDisable(!bl);
        this.exitButton.setVisible(bl && bl3);
        this.exitButton.setManaged(bl && bl3);
        this.restartButton.setVisible(bl && bl2);
        this.rebootButton.setVisible(bl && bl2);
        if (!bl) {
            this.loginButton.requestFocus();
        }
    }

    public void selectionChanged(DispyData dispyData) {
        this.renameButton.setDisable(dispyData == null);
        this.dropButton.setDisable(dispyData == null);
        this.copyButton.setDisable(dispyData == null);
        this.exportButton.setDisable(dispyData == null);
        this.apkButton.setDisable(dispyData != null && !dispyData.getOnoffBoolean());
        this.cutButton.setDisable(dispyData != null && !dispyData.getOnoffBoolean());
        this.shellButton.setDisable(dispyData != null && (!dispyData.getOnoffBoolean() || dispyData.getVersionInt() < 66));
        this.exitButton.setDisable(dispyData == null || !dispyData.getOnoffBoolean());
        this.restartButton.setDisable(dispyData == null || !dispyData.getOnoffBoolean() || dispyData.getVersionInt() < 36);
        this.rebootButton.setDisable(dispyData == null || !dispyData.getOnoffBoolean() || dispyData.getVersionInt() < 36);
    }

    @FXML
    private void handleLogin() {
        this.mainApp.showLoginDialog();
    }

    @FXML
    private void handleLogout() {
        this.mainApp.serverLogout(true);
    }

    @FXML
    private void handleAdd() {
        this.mainApp.showAddDialog();
    }

    @FXML
    private void handleDrop() {
        this.mainApp.showDropDialog();
    }

    @FXML
    private void handleRename() {
        this.mainApp.showRenameDialog();
    }

    @FXML
    private void handleUnion() {
        this.mainApp.showAddUnionDialog();
    }

    @FXML
    private void handleCopy() {
        this.mainApp.showCopyDialog();
    }

    @FXML
    private void handleExport() {
        this.mainApp.showExportDialog();
    }

    @FXML
    private void handleApk() {
        this.mainApp.clientApk();
    }

    @FXML
    private void handleCut() {
        this.mainApp.clientCut();
    }

    @FXML
    private void handleShell() {
        this.mainApp.showShellDialog(false);
    }

    @FXML
    private void handleShellServer() {
        this.mainApp.showShellDialog(true);
    }

    @FXML
    private void handleExit() {
        this.mainApp.clientExit();
    }

    @FXML
    private void handleRestart() {
        this.mainApp.clientRestart();
    }

    @FXML
    private void handleReboot() {
        this.mainApp.clientReboot();
    }

    @FXML
    private void handleAbout() {
        this.mainApp.openUrl("https://ybroad.net/yboard/");
    }
}

