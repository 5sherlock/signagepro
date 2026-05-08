/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 *  javafx.fxml.FXML
 *  javafx.scene.control.Button
 *  javafx.scene.control.CheckBox
 *  javafx.scene.control.ColorPicker
 *  javafx.scene.control.ComboBox
 *  javafx.scene.control.RadioButton
 *  javafx.scene.control.Spinner
 *  javafx.scene.control.TextField
 *  javafx.scene.control.Toggle
 *  javafx.scene.control.ToggleGroup
 *  javafx.scene.paint.Color
 *  javafx.stage.FileChooser
 *  javafx.stage.FileChooser$ExtensionFilter
 *  javafx.stage.Stage
 *  javafx.stage.Window
 */
package net.ybroad.dispy.manager.view;

import java.io.File;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.ybroad.dispy.manager.MainApp;
import net.ybroad.dispy.manager.util.Lang;

public class ScreenDialogController {
    @FXML
    private TextField logoField;
    @FXML
    private Button logoButton;
    private String logoAlternative;
    @FXML
    private TextField fileField;
    @FXML
    private Button fileButton;
    @FXML
    private CheckBox clock;
    @FXML
    private ComboBox<String> clockCell;
    @FXML
    private ColorPicker clockColor;
    @FXML
    private ComboBox<String> clockHor;
    @FXML
    private ComboBox<String> clockVer;
    @FXML
    private Spinner<Integer> spinW;
    @FXML
    private Spinner<Integer> spinH;
    @FXML
    private RadioButton rotate0;
    @FXML
    private RadioButton rotate90;
    @FXML
    private RadioButton rotate180;
    @FXML
    private RadioButton rotate270;
    private ToggleGroup rotateGroup = new ToggleGroup();
    @FXML
    private Button onoffButton;
    private Stage dialogStage;
    private MainApp mainApp;
    private boolean ok = false;

    @FXML
    private void initialize() {
        this.clockCell.getItems().addAll((Object[])new String[]{"#0", "#1", "#2", "#3", Lang.getString("screen.clock.cell.world")});
        this.clockHor.getItems().addAll((Object[])new String[]{Lang.getString("screen.clock.hor.left"), Lang.getString("screen.clock.hor.center"), Lang.getString("screen.clock.hor.right")});
        this.clockVer.getItems().addAll((Object[])new String[]{Lang.getString("screen.clock.ver.top"), Lang.getString("screen.clock.ver.center"), Lang.getString("screen.clock.ver.bottom")});
        this.rotate0.setToggleGroup(this.rotateGroup);
        this.rotate90.setToggleGroup(this.rotateGroup);
        this.rotate180.setToggleGroup(this.rotateGroup);
        this.rotate270.setToggleGroup(this.rotateGroup);
    }

    public void setDialogStage(Stage stage, MainApp mainApp) {
        this.dialogStage = stage;
        this.mainApp = mainApp;
    }

    public void setLogo(String string) {
        if (this.logoField != null) {
            this.logoField.setText(string);
        } else {
            this.logoAlternative = string;
        }
    }

    public String getLogo() {
        if (this.logoField != null) {
            return this.logoField.getText();
        }
        return this.logoAlternative;
    }

    public void setBackground(String string) {
        this.fileField.setText(string);
    }

    public String getBackground() {
        return this.fileField.getText();
    }

    public void setClock(boolean bl, String string, String string2, int n) {
        this.clock.setSelected(bl);
        if (string == null || string.isEmpty() || string.equals("null")) {
            this.clockColor.setDisable(true);
        } else {
            this.clockColor.setDisable(false);
            this.clockColor.setValue((Object)Color.valueOf((String)("#" + string.subSequence(2, 8) + string.subSequence(0, 2))));
        }
        if (string2 == null) {
            this.clockCell.getSelectionModel().select(0);
            this.clockHor.getSelectionModel().select(2);
            this.clockVer.getSelectionModel().select(1);
            this.clockCell.setDisable(true);
            this.clockHor.setDisable(true);
            this.clockVer.setDisable(true);
        } else {
            switch (string2.charAt(0)) {
                case '0': {
                    this.clockCell.getSelectionModel().select(0);
                    break;
                }
                case '1': {
                    this.clockCell.getSelectionModel().select(1);
                    break;
                }
                case '2': {
                    this.clockCell.getSelectionModel().select(2);
                    break;
                }
                case '3': {
                    this.clockCell.getSelectionModel().select(3);
                    break;
                }
                default: {
                    this.clockCell.getSelectionModel().select(4);
                }
            }
            switch (string2.charAt(1)) {
                case 'l': {
                    this.clockHor.getSelectionModel().select(0);
                    break;
                }
                case 'c': {
                    this.clockHor.getSelectionModel().select(1);
                    break;
                }
                default: {
                    this.clockHor.getSelectionModel().select(2);
                }
            }
            switch (string2.charAt(2)) {
                case 't': {
                    this.clockVer.getSelectionModel().select(0);
                    break;
                }
                case 'b': {
                    this.clockVer.getSelectionModel().select(2);
                    break;
                }
                default: {
                    this.clockVer.getSelectionModel().select(1);
                }
            }
        }
        switch (n) {
            case 0: {
                this.rotateGroup.selectToggle((Toggle)this.rotate0);
                break;
            }
            case 90: {
                this.rotateGroup.selectToggle((Toggle)this.rotate90);
                break;
            }
            case 180: {
                this.rotateGroup.selectToggle((Toggle)this.rotate180);
                break;
            }
            case 270: {
                this.rotateGroup.selectToggle((Toggle)this.rotate270);
                break;
            }
            default: {
                this.rotateGroup.selectToggle((Toggle)this.rotate0);
                this.rotate90.setDisable(true);
                this.rotate180.setDisable(true);
                this.rotate270.setDisable(true);
            }
        }
    }

    public boolean getClock() {
        return this.clock.isSelected();
    }

    public String getClockPos() {
        if (this.clockCell.isDisabled()) {
            return null;
        }
        String string = "";
        switch (this.clockCell.getSelectionModel().getSelectedIndex()) {
            case 0: {
                string = string + "0";
                break;
            }
            case 1: {
                string = string + "1";
                break;
            }
            case 2: {
                string = string + "2";
                break;
            }
            case 3: {
                string = string + "3";
                break;
            }
            default: {
                string = string + "w";
            }
        }
        switch (this.clockHor.getSelectionModel().getSelectedIndex()) {
            case 0: {
                string = string + "l";
                break;
            }
            case 1: {
                string = string + "c";
                break;
            }
            default: {
                string = string + "r";
            }
        }
        switch (this.clockVer.getSelectionModel().getSelectedIndex()) {
            case 0: {
                string = string + "t";
                break;
            }
            case 2: {
                string = string + "b";
                break;
            }
            default: {
                string = string + "c";
            }
        }
        return string;
    }

    public String getClockColor() {
        Color color = (Color)this.clockColor.getValue();
        int n = (int)(255.0 * color.getOpacity());
        int n2 = (int)(255.0 * color.getRed());
        int n3 = (int)(255.0 * color.getGreen());
        int n4 = (int)(255.0 * color.getBlue());
        return String.format("%02x%02x%02x%02x", n, n2, n3, n4);
    }

    public void setNum(int n, int n2) {
        this.spinW.getValueFactory().setValue((Object)n);
        this.spinH.getValueFactory().setValue((Object)n2);
    }

    public int getNumW() {
        int n = (Integer)this.spinW.getValue();
        if (n < 1) {
            n = 1;
        }
        return n;
    }

    public int getRotate() {
        Toggle toggle = this.rotateGroup.getSelectedToggle();
        if (toggle == this.rotate90) {
            return 90;
        }
        if (toggle == this.rotate180) {
            return 180;
        }
        if (toggle == this.rotate270) {
            return 270;
        }
        return 0;
    }

    public int getNumH() {
        int n = (Integer)this.spinH.getValue();
        if (n < 1) {
            n = 1;
        }
        return n;
    }

    public boolean isOkClicked() {
        return this.ok;
    }

    public void setOnOffShow(boolean bl) {
        this.onoffButton.setVisible(bl);
    }

    @FXML
    private void handleLogo() {
        File file;
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add((Object)new FileChooser.ExtensionFilter("Image (*.jpg, *.png)", new String[]{"*.jpg", "*.jepg", "*.png", "*.JPG", "*.JPEG", "*.PNG"}));
        File file2 = new File(this.logoField.getText()).getParentFile();
        if (file2 != null && file2.isDirectory()) {
            fileChooser.setInitialDirectory(file2);
        }
        if ((file = fileChooser.showOpenDialog((Window)this.dialogStage)) == null) {
            this.logoField.setText("");
        } else {
            this.logoField.setText(file.getAbsolutePath());
            this.logoField.home();
            this.logoField.end();
        }
    }

    @FXML
    private void handleFile() {
        File file;
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add((Object)new FileChooser.ExtensionFilter("Image (*.jpg, *.png)", new String[]{"*.jpg", "*.jepg", "*.png", "*.JPG", "*.JPEG", "*.PNG"}));
        File file2 = new File(this.fileField.getText()).getParentFile();
        if (file2 != null && file2.isDirectory()) {
            fileChooser.setInitialDirectory(file2);
        }
        if ((file = fileChooser.showOpenDialog((Window)this.dialogStage)) == null) {
            this.fileField.setText("");
        } else {
            this.fileField.setText(file.getAbsolutePath());
            this.fileField.home();
            this.fileField.end();
        }
    }

    @FXML
    private void handleOk() {
        this.ok = true;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }

    @FXML
    private void handleOnOff() {
        this.dialogStage.close();
        Platform.runLater(() -> this.mainApp.showOnOffDialog());
    }
}

