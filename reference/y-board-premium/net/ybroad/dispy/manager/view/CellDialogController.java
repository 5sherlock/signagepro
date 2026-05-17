/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.value.ChangeListener
 *  javafx.beans.value.ObservableValue
 *  javafx.fxml.FXML
 *  javafx.scene.canvas.Canvas
 *  javafx.scene.control.Label
 *  javafx.scene.control.RadioButton
 *  javafx.scene.control.TextField
 *  javafx.scene.control.Toggle
 *  javafx.scene.control.ToggleGroup
 *  javafx.scene.shape.Line
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import net.ybroad.dispy.manager.util.CanvasDrawer;
import net.ybroad.dispy.manager.util.Lang;

public class CellDialogController {
    @FXML
    private Canvas canvas;
    @FXML
    private RadioButton radio1;
    @FXML
    private RadioButton radio2;
    @FXML
    private RadioButton radio3;
    @FXML
    private RadioButton radio4;
    @FXML
    private RadioButton radio5;
    @FXML
    private RadioButton radio6;
    @FXML
    private RadioButton radio7;
    @FXML
    private RadioButton radio8;
    @FXML
    private RadioButton radio9;
    @FXML
    private TextField field0;
    @FXML
    private TextField field1;
    @FXML
    private TextField field2;
    @FXML
    private TextField field3;
    @FXML
    private Label maxLabel;
    @FXML
    private Line vLine0;
    @FXML
    private Line vLine1;
    @FXML
    private Line vLine2;
    @FXML
    private Line vLine3;
    @FXML
    private Line hLine0;
    @FXML
    private Line hLine1;
    @FXML
    private Line hLine2;
    @FXML
    private Line hLine3;
    private Stage dialogStage;
    private ToggleGroup group;
    private int width;
    private int height;
    private int type;
    private int[] size;
    private boolean okClicked = false;

    @FXML
    private void initialize() {
        this.maxLabel.setText("");
        this.group = new ToggleGroup();
        this.radio1.setToggleGroup(this.group);
        this.radio2.setToggleGroup(this.group);
        this.radio3.setToggleGroup(this.group);
        this.radio4.setToggleGroup(this.group);
        this.radio5.setToggleGroup(this.group);
        this.radio6.setToggleGroup(this.group);
        this.radio7.setToggleGroup(this.group);
        this.radio8.setToggleGroup(this.group);
        this.radio9.setToggleGroup(this.group);
        this.group.selectedToggleProperty().addListener((ChangeListener)new ChangeListener<Toggle>(){

            public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle toggle2) {
                if (toggle2 == CellDialogController.this.radio1) {
                    CellDialogController.this.type = 1;
                    CellDialogController.this.vLine0.setVisible(true);
                    CellDialogController.this.hLine0.setVisible(false);
                    CellDialogController.this.vLine1.setVisible(true);
                    CellDialogController.this.hLine1.setVisible(false);
                    CellDialogController.this.vLine2.setVisible(true);
                    CellDialogController.this.hLine2.setVisible(false);
                    CellDialogController.this.vLine3.setVisible(true);
                    CellDialogController.this.hLine3.setVisible(false);
                } else if (toggle2 == CellDialogController.this.radio2) {
                    CellDialogController.this.type = 2;
                    CellDialogController.this.vLine0.setVisible(true);
                    CellDialogController.this.hLine0.setVisible(false);
                    CellDialogController.this.vLine1.setVisible(false);
                    CellDialogController.this.hLine1.setVisible(true);
                    CellDialogController.this.vLine2.setVisible(false);
                    CellDialogController.this.hLine2.setVisible(true);
                    CellDialogController.this.vLine3.setVisible(false);
                    CellDialogController.this.hLine3.setVisible(true);
                } else if (toggle2 == CellDialogController.this.radio3) {
                    CellDialogController.this.type = 3;
                    CellDialogController.this.vLine0.setVisible(true);
                    CellDialogController.this.hLine0.setVisible(false);
                    CellDialogController.this.vLine1.setVisible(false);
                    CellDialogController.this.hLine1.setVisible(true);
                    CellDialogController.this.vLine2.setVisible(true);
                    CellDialogController.this.hLine2.setVisible(false);
                    CellDialogController.this.vLine3.setVisible(true);
                    CellDialogController.this.hLine3.setVisible(false);
                } else if (toggle2 == CellDialogController.this.radio4) {
                    CellDialogController.this.type = 4;
                    CellDialogController.this.vLine0.setVisible(true);
                    CellDialogController.this.hLine0.setVisible(false);
                    CellDialogController.this.vLine1.setVisible(false);
                    CellDialogController.this.hLine1.setVisible(true);
                    CellDialogController.this.vLine2.setVisible(true);
                    CellDialogController.this.hLine2.setVisible(false);
                    CellDialogController.this.vLine3.setVisible(true);
                    CellDialogController.this.hLine3.setVisible(false);
                } else if (toggle2 == CellDialogController.this.radio5) {
                    CellDialogController.this.type = 5;
                    CellDialogController.this.vLine0.setVisible(true);
                    CellDialogController.this.hLine0.setVisible(false);
                    CellDialogController.this.vLine1.setVisible(false);
                    CellDialogController.this.hLine1.setVisible(true);
                    CellDialogController.this.vLine2.setVisible(true);
                    CellDialogController.this.hLine2.setVisible(false);
                    CellDialogController.this.vLine3.setVisible(true);
                    CellDialogController.this.hLine3.setVisible(false);
                } else if (toggle2 == CellDialogController.this.radio6) {
                    CellDialogController.this.type = 6;
                    CellDialogController.this.vLine0.setVisible(true);
                    CellDialogController.this.hLine0.setVisible(false);
                    CellDialogController.this.vLine1.setVisible(false);
                    CellDialogController.this.hLine1.setVisible(true);
                    CellDialogController.this.vLine2.setVisible(true);
                    CellDialogController.this.hLine2.setVisible(false);
                    CellDialogController.this.vLine3.setVisible(true);
                    CellDialogController.this.hLine3.setVisible(false);
                } else if (toggle2 == CellDialogController.this.radio7) {
                    CellDialogController.this.type = 7;
                    CellDialogController.this.vLine0.setVisible(true);
                    CellDialogController.this.hLine0.setVisible(false);
                    CellDialogController.this.vLine1.setVisible(false);
                    CellDialogController.this.hLine1.setVisible(true);
                    CellDialogController.this.vLine2.setVisible(true);
                    CellDialogController.this.hLine2.setVisible(false);
                    CellDialogController.this.vLine3.setVisible(false);
                    CellDialogController.this.hLine3.setVisible(true);
                } else if (toggle2 == CellDialogController.this.radio8) {
                    CellDialogController.this.type = 8;
                    CellDialogController.this.vLine0.setVisible(true);
                    CellDialogController.this.hLine0.setVisible(false);
                    CellDialogController.this.vLine1.setVisible(true);
                    CellDialogController.this.hLine1.setVisible(false);
                    CellDialogController.this.vLine2.setVisible(true);
                    CellDialogController.this.hLine2.setVisible(false);
                    CellDialogController.this.vLine3.setVisible(false);
                    CellDialogController.this.hLine3.setVisible(true);
                } else if (toggle2 == CellDialogController.this.radio9) {
                    CellDialogController.this.type = 9;
                    CellDialogController.this.vLine0.setVisible(true);
                    CellDialogController.this.hLine0.setVisible(false);
                    CellDialogController.this.vLine1.setVisible(true);
                    CellDialogController.this.hLine1.setVisible(false);
                    CellDialogController.this.vLine2.setVisible(true);
                    CellDialogController.this.hLine2.setVisible(false);
                    CellDialogController.this.vLine3.setVisible(false);
                    CellDialogController.this.hLine3.setVisible(true);
                }
                CanvasDrawer.drawCanvas(CellDialogController.this.canvas, CellDialogController.this.type, CellDialogController.this.size, CellDialogController.this.width, CellDialogController.this.height);
            }
        });
        this.field0.focusedProperty().addListener((ChangeListener)new ChangeListener<Boolean>(){

            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean bl, Boolean bl2) {
                CellDialogController.this.showHint(bl2 != false ? CellDialogController.this.field0 : null);
            }
        });
        this.field1.focusedProperty().addListener((ChangeListener)new ChangeListener<Boolean>(){

            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean bl, Boolean bl2) {
                if (bl2.booleanValue()) {
                    CellDialogController.this.showHint(bl2 != false ? CellDialogController.this.field1 : null);
                }
            }
        });
        this.field2.focusedProperty().addListener((ChangeListener)new ChangeListener<Boolean>(){

            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean bl, Boolean bl2) {
                if (bl2.booleanValue()) {
                    CellDialogController.this.showHint(bl2 != false ? CellDialogController.this.field2 : null);
                }
            }
        });
        this.field3.focusedProperty().addListener((ChangeListener)new ChangeListener<Boolean>(){

            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean bl, Boolean bl2) {
                if (bl2.booleanValue()) {
                    CellDialogController.this.showHint(bl2 != false ? CellDialogController.this.field3 : null);
                }
            }
        });
    }

    private void showHint(TextField textField) {
        if (textField == this.field0) {
            int n = 0;
            switch (this.type) {
                case 1: {
                    n = this.height - this.size[1] - this.size[2] - this.size[3];
                    break;
                }
                case 2: {
                    n = this.height;
                    break;
                }
                case 3: {
                    n = this.height - this.size[2] - this.size[3];
                    break;
                }
                case 4: {
                    n = this.height - this.size[2] - this.size[3];
                    break;
                }
                case 5: {
                    n = this.height - this.size[2] - this.size[3];
                    break;
                }
                case 6: {
                    n = this.height - this.size[2] - this.size[3];
                    break;
                }
                case 7: {
                    n = this.height - this.size[2];
                    break;
                }
                case 8: {
                    n = this.height - this.size[1] - this.size[2];
                    break;
                }
                case 9: {
                    n = this.height - this.size[1] - this.size[2];
                }
            }
            this.maxLabel.setText(Lang.getString("size.max", String.valueOf(n)));
        } else if (textField == this.field1) {
            int n = 0;
            switch (this.type) {
                case 1: {
                    n = this.height - this.size[0] - this.size[2] - this.size[3];
                    break;
                }
                case 2: {
                    n = this.width - this.size[2] - this.size[3];
                    break;
                }
                case 3: {
                    n = this.width;
                    break;
                }
                case 4: {
                    n = this.width;
                    break;
                }
                case 5: {
                    n = this.width;
                    break;
                }
                case 6: {
                    n = this.width;
                    break;
                }
                case 7: {
                    n = this.width;
                    break;
                }
                case 8: {
                    n = this.height - this.size[0] - this.size[2];
                    break;
                }
                case 9: {
                    n = this.height - this.size[0] - this.size[2];
                }
            }
            this.maxLabel.setText(Lang.getString("size.max", String.valueOf(n)));
        } else if (textField == this.field2) {
            int n = 0;
            switch (this.type) {
                case 1: {
                    n = this.height - this.size[0] - this.size[1] - this.size[3];
                    break;
                }
                case 2: {
                    n = this.width - this.size[1] - this.size[3];
                    break;
                }
                case 3: {
                    n = this.height - this.size[0] - this.size[3];
                    break;
                }
                case 4: {
                    n = this.height - this.size[0] - this.size[3];
                    break;
                }
                case 5: {
                    n = this.height - this.size[0] - this.size[3];
                    break;
                }
                case 6: {
                    n = this.height - this.size[0] - this.size[3];
                    break;
                }
                case 7: {
                    n = this.height - this.size[0];
                    break;
                }
                case 8: {
                    n = this.height - this.size[0] - this.size[1];
                    break;
                }
                case 9: {
                    n = this.height - this.size[0] - this.size[1];
                }
            }
            this.maxLabel.setText(Lang.getString("size.max", String.valueOf(n)));
        } else if (textField == this.field3) {
            int n = 0;
            switch (this.type) {
                case 1: {
                    n = this.height - this.size[0] - this.size[1] - this.size[2];
                    break;
                }
                case 2: {
                    n = this.width - this.size[1] - this.size[2];
                    break;
                }
                case 3: {
                    n = this.height - this.size[0] - this.size[2];
                    break;
                }
                case 4: {
                    n = this.height - this.size[0] - this.size[2];
                    break;
                }
                case 5: {
                    n = this.height - this.size[0] - this.size[2];
                    break;
                }
                case 6: {
                    n = this.height - this.size[0] - this.size[2];
                    break;
                }
                case 7: {
                    n = this.width;
                    break;
                }
                case 8: {
                    n = this.width;
                    break;
                }
                case 9: {
                    n = this.width;
                }
            }
            this.maxLabel.setText(Lang.getString("size.max", String.valueOf(n)));
        } else {
            this.maxLabel.setText("");
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setVersion(int n) {
        if (n < 40) {
            this.radio4.setDisable(true);
            this.radio5.setDisable(true);
            this.radio6.setDisable(true);
            this.radio7.setDisable(true);
        }
        if (n < 45) {
            this.radio8.setDisable(true);
            this.radio9.setDisable(true);
        }
    }

    public void setScreen(int n, int n2) {
        this.width = n;
        this.height = n2;
    }

    public void setCell(int n, int[] nArray) {
        this.type = n;
        this.size = nArray;
        switch (n) {
            case 1: {
                this.radio1.setSelected(true);
                break;
            }
            case 2: {
                this.radio2.setSelected(true);
                break;
            }
            case 3: {
                this.radio3.setSelected(true);
                break;
            }
            case 4: {
                this.radio4.setSelected(true);
                break;
            }
            case 5: {
                this.radio5.setSelected(true);
                break;
            }
            case 6: {
                this.radio6.setSelected(true);
                break;
            }
            case 7: {
                this.radio7.setSelected(true);
                break;
            }
            case 8: {
                this.radio8.setSelected(true);
                break;
            }
            case 9: {
                this.radio9.setSelected(true);
            }
        }
        this.field0.setText(String.valueOf(nArray[0]));
        this.field1.setText(String.valueOf(nArray[1]));
        this.field2.setText(String.valueOf(nArray[2]));
        this.field3.setText(String.valueOf(nArray[3]));
    }

    public boolean isOkClicked() {
        return this.okClicked;
    }

    public int getType() {
        return this.type;
    }

    public int[] getSize() {
        return this.size;
    }

    @FXML
    private void handleEdit() {
        if (this.field0.isFocused()) {
            String string = this.field0.getText();
            this.size[0] = string.isEmpty() ? 0 : Integer.parseInt(string);
            this.showHint(this.field0);
        } else if (this.field1.isFocused()) {
            String string = this.field1.getText();
            this.size[1] = string.isEmpty() ? 0 : Integer.parseInt(string);
            this.showHint(this.field1);
        } else if (this.field2.isFocused()) {
            String string = this.field2.getText();
            this.size[2] = string.isEmpty() ? 0 : Integer.parseInt(string);
            this.showHint(this.field2);
        } else if (this.field3.isFocused()) {
            String string = this.field3.getText();
            this.size[3] = string.isEmpty() ? 0 : Integer.parseInt(string);
            this.showHint(this.field3);
        } else {
            this.showHint(null);
        }
        CanvasDrawer.drawCanvas(this.canvas, this.type, this.size, this.width, this.height);
    }

    @FXML
    private void handleOk() {
        this.okClicked = true;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }
}

