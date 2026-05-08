/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.value.ChangeListener
 *  javafx.beans.value.ObservableValue
 *  javafx.event.EventHandler
 *  javafx.fxml.FXML
 *  javafx.scene.canvas.Canvas
 *  javafx.scene.control.Label
 *  javafx.scene.control.RadioButton
 *  javafx.scene.control.TextField
 *  javafx.scene.control.Toggle
 *  javafx.scene.control.ToggleGroup
 *  javafx.scene.shape.Line
 *  javafx.stage.Stage
 *  javafx.stage.WindowEvent
 */
package net.ybroad.dispy.manager.view;

import java.util.Arrays;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.ybroad.dispy.manager.util.CanvasDrawer;
import net.ybroad.dispy.manager.util.Lang;

public class CellDialogController2 {
    @FXML
    private Canvas canvas;
    @FXML
    private Label sizeLabel;
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
    private RadioButton radio10;
    @FXML
    private RadioButton radio11;
    @FXML
    private RadioButton radio12;
    @FXML
    private RadioButton radio13;
    @FXML
    private RadioButton radio14;
    @FXML
    private RadioButton radio15;
    @FXML
    private RadioButton radio16;
    @FXML
    private TextField field0;
    @FXML
    private TextField field1;
    @FXML
    private TextField field2;
    @FXML
    private TextField field3;
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
    private int typeOrigin;
    private int[] sizeOrigin;
    private boolean okClicked = false;

    @FXML
    private void initialize() {
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
        this.radio10.setToggleGroup(this.group);
        this.radio11.setToggleGroup(this.group);
        this.radio12.setToggleGroup(this.group);
        this.radio13.setToggleGroup(this.group);
        this.radio14.setToggleGroup(this.group);
        this.radio15.setToggleGroup(this.group);
        this.radio16.setToggleGroup(this.group);
        this.group.selectedToggleProperty().addListener((ChangeListener)new ChangeListener<Toggle>(){

            public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle toggle2) {
                if (toggle2 == CellDialogController2.this.radio1) {
                    CellDialogController2.this.type = 1;
                    CellDialogController2.this.vLine0.setVisible(true);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(true);
                    CellDialogController2.this.hLine1.setVisible(false);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(true);
                    CellDialogController2.this.hLine3.setVisible(false);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 100;
                        ((CellDialogController2)CellDialogController2.this).size[1] = (CellDialogController2.this.height - CellDialogController2.this.size[0]) / 3;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[1] = nArray[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.size[1];
                        int[] nArray2 = CellDialogController2.this.size;
                        nArray2[2] = nArray2[2] - CellDialogController2.this.size[2] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.height - CellDialogController2.this.size[0] - CellDialogController2.this.size[1] - CellDialogController2.this.size[2];
                    }
                } else if (toggle2 == CellDialogController2.this.radio2) {
                    CellDialogController2.this.type = 2;
                    CellDialogController2.this.vLine0.setVisible(true);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(false);
                    CellDialogController2.this.hLine1.setVisible(true);
                    CellDialogController2.this.vLine2.setVisible(false);
                    CellDialogController2.this.hLine2.setVisible(true);
                    CellDialogController2.this.vLine3.setVisible(false);
                    CellDialogController2.this.hLine3.setVisible(true);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 100;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.width / 3;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[1] = nArray[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.width / 3;
                        int[] nArray3 = CellDialogController2.this.size;
                        nArray3[2] = nArray3[2] - CellDialogController2.this.size[2] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.width - CellDialogController2.this.size[1] - CellDialogController2.this.size[2];
                    }
                } else if (toggle2 == CellDialogController2.this.radio3) {
                    CellDialogController2.this.type = 3;
                    CellDialogController2.this.vLine0.setVisible(true);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(false);
                    CellDialogController2.this.hLine1.setVisible(true);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(true);
                    CellDialogController2.this.hLine3.setVisible(false);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 100;
                        ((CellDialogController2)CellDialogController2.this).size[3] = 100;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.width / 2;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[1] = nArray[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.height - CellDialogController2.this.size[0] - CellDialogController2.this.size[3];
                    }
                } else if (toggle2 == CellDialogController2.this.radio4) {
                    CellDialogController2.this.type = 4;
                    CellDialogController2.this.vLine0.setVisible(true);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(false);
                    CellDialogController2.this.hLine1.setVisible(true);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(true);
                    CellDialogController2.this.hLine3.setVisible(false);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 100;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.width / 2;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[1] = nArray[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = (CellDialogController2.this.height - CellDialogController2.this.size[0]) / 2;
                        int[] nArray4 = CellDialogController2.this.size;
                        nArray4[2] = nArray4[2] - CellDialogController2.this.size[2] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.height - CellDialogController2.this.size[0] - CellDialogController2.this.size[2];
                    }
                } else if (toggle2 == CellDialogController2.this.radio5) {
                    CellDialogController2.this.type = 5;
                    CellDialogController2.this.vLine0.setVisible(true);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(false);
                    CellDialogController2.this.hLine1.setVisible(true);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(true);
                    CellDialogController2.this.hLine3.setVisible(false);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 100;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.width / 2;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[1] = nArray[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = (CellDialogController2.this.height - CellDialogController2.this.size[0]) / 2;
                        int[] nArray5 = CellDialogController2.this.size;
                        nArray5[2] = nArray5[2] - CellDialogController2.this.size[2] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.height - CellDialogController2.this.size[0] - CellDialogController2.this.size[2];
                    }
                } else if (toggle2 == CellDialogController2.this.radio6) {
                    CellDialogController2.this.type = 6;
                    CellDialogController2.this.vLine0.setVisible(true);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(false);
                    CellDialogController2.this.hLine1.setVisible(true);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(true);
                    CellDialogController2.this.hLine3.setVisible(false);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 200;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.width / 2;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[1] = nArray[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = (CellDialogController2.this.height - CellDialogController2.this.size[0]) / 2;
                        int[] nArray6 = CellDialogController2.this.size;
                        nArray6[2] = nArray6[2] - CellDialogController2.this.size[2] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.height - CellDialogController2.this.size[0] - CellDialogController2.this.size[2];
                    }
                } else if (toggle2 == CellDialogController2.this.radio7) {
                    CellDialogController2.this.type = 7;
                    CellDialogController2.this.vLine0.setVisible(true);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(false);
                    CellDialogController2.this.hLine1.setVisible(true);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(false);
                    CellDialogController2.this.hLine3.setVisible(true);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 200;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.width / 2;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[1] = nArray[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.height - CellDialogController2.this.size[0];
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.width - CellDialogController2.this.size[1];
                    }
                } else if (toggle2 == CellDialogController2.this.radio8) {
                    CellDialogController2.this.type = 8;
                    CellDialogController2.this.vLine0.setVisible(true);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(true);
                    CellDialogController2.this.hLine1.setVisible(false);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(false);
                    CellDialogController2.this.hLine3.setVisible(true);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 200;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[0] = nArray[0] - CellDialogController2.this.size[0] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[1] = (CellDialogController2.this.height - CellDialogController2.this.size[0]) / 2;
                        int[] nArray7 = CellDialogController2.this.size;
                        nArray7[1] = nArray7[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.height - CellDialogController2.this.size[0] - CellDialogController2.this.size[1];
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.width / 2;
                        int[] nArray8 = CellDialogController2.this.size;
                        nArray8[3] = nArray8[3] - CellDialogController2.this.size[3] % 10;
                    }
                } else if (toggle2 == CellDialogController2.this.radio9) {
                    CellDialogController2.this.type = 9;
                    CellDialogController2.this.vLine0.setVisible(true);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(true);
                    CellDialogController2.this.hLine1.setVisible(false);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(false);
                    CellDialogController2.this.hLine3.setVisible(true);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 200;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[0] = nArray[0] - CellDialogController2.this.size[0] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[1] = (CellDialogController2.this.height - CellDialogController2.this.size[0]) / 2;
                        int[] nArray9 = CellDialogController2.this.size;
                        nArray9[1] = nArray9[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.height - CellDialogController2.this.size[0] - CellDialogController2.this.size[1];
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.width / 2;
                        int[] nArray10 = CellDialogController2.this.size;
                        nArray10[3] = nArray10[3] - CellDialogController2.this.size[3] % 10;
                    }
                } else if (toggle2 == CellDialogController2.this.radio10) {
                    CellDialogController2.this.type = 10;
                    CellDialogController2.this.vLine0.setVisible(false);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(true);
                    CellDialogController2.this.hLine1.setVisible(false);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(true);
                    CellDialogController2.this.hLine3.setVisible(false);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = -1;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.height / 3;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.height / 3;
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.height - CellDialogController2.this.size[1] - CellDialogController2.this.size[2];
                    }
                } else if (toggle2 == CellDialogController2.this.radio11) {
                    CellDialogController2.this.type = 11;
                    CellDialogController2.this.vLine0.setVisible(false);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(false);
                    CellDialogController2.this.hLine1.setVisible(true);
                    CellDialogController2.this.vLine2.setVisible(false);
                    CellDialogController2.this.hLine2.setVisible(true);
                    CellDialogController2.this.vLine3.setVisible(false);
                    CellDialogController2.this.hLine3.setVisible(true);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = -1;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.width / 3;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.width / 3;
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.width - CellDialogController2.this.size[1] - CellDialogController2.this.size[2];
                    }
                } else if (toggle2 == CellDialogController2.this.radio12) {
                    CellDialogController2.this.type = 12;
                    CellDialogController2.this.vLine0.setVisible(false);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(false);
                    CellDialogController2.this.hLine1.setVisible(true);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(true);
                    CellDialogController2.this.hLine3.setVisible(false);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = -1;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.width / 2;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.height - 100;
                        ((CellDialogController2)CellDialogController2.this).size[3] = 100;
                    }
                } else if (toggle2 == CellDialogController2.this.radio13) {
                    CellDialogController2.this.type = 13;
                    CellDialogController2.this.vLine0.setVisible(false);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(true);
                    CellDialogController2.this.hLine1.setVisible(false);
                    CellDialogController2.this.vLine2.setVisible(false);
                    CellDialogController2.this.hLine2.setVisible(true);
                    CellDialogController2.this.vLine3.setVisible(true);
                    CellDialogController2.this.hLine3.setVisible(false);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = -1;
                        ((CellDialogController2)CellDialogController2.this).size[1] = 100;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.width / 2;
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.height - 100;
                    }
                } else if (toggle2 == CellDialogController2.this.radio14) {
                    CellDialogController2.this.type = 14;
                    CellDialogController2.this.vLine0.setVisible(false);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(true);
                    CellDialogController2.this.hLine1.setVisible(false);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(false);
                    CellDialogController2.this.hLine3.setVisible(true);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = -1;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.height / 2;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.height - CellDialogController2.this.size[1];
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.width / 2;
                    }
                } else if (toggle2 == CellDialogController2.this.radio15) {
                    CellDialogController2.this.type = 15;
                    CellDialogController2.this.vLine0.setVisible(false);
                    CellDialogController2.this.hLine0.setVisible(false);
                    CellDialogController2.this.vLine1.setVisible(true);
                    CellDialogController2.this.hLine1.setVisible(false);
                    CellDialogController2.this.vLine2.setVisible(true);
                    CellDialogController2.this.hLine2.setVisible(false);
                    CellDialogController2.this.vLine3.setVisible(false);
                    CellDialogController2.this.hLine3.setVisible(true);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = -1;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.height / 2;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.height - CellDialogController2.this.size[1];
                        ((CellDialogController2)CellDialogController2.this).size[3] = CellDialogController2.this.width / 2;
                    }
                } else if (toggle2 == CellDialogController2.this.radio16) {
                    CellDialogController2.this.type = 16;
                    CellDialogController2.this.vLine0.setVisible(false);
                    CellDialogController2.this.hLine0.setVisible(true);
                    CellDialogController2.this.vLine1.setVisible(true);
                    CellDialogController2.this.hLine1.setVisible(false);
                    CellDialogController2.this.vLine2.setVisible(false);
                    CellDialogController2.this.hLine2.setVisible(true);
                    CellDialogController2.this.vLine3.setVisible(false);
                    CellDialogController2.this.hLine3.setVisible(true);
                    if (toggle != null) {
                        ((CellDialogController2)CellDialogController2.this).size[0] = 300;
                        ((CellDialogController2)CellDialogController2.this).size[3] = 300;
                        ((CellDialogController2)CellDialogController2.this).size[1] = CellDialogController2.this.height / 2;
                        int[] nArray = CellDialogController2.this.size;
                        nArray[1] = nArray[1] - CellDialogController2.this.size[1] % 10;
                        ((CellDialogController2)CellDialogController2.this).size[2] = CellDialogController2.this.width - CellDialogController2.this.size[0] - CellDialogController2.this.size[3];
                    }
                }
                CanvasDrawer.drawCanvas(CellDialogController2.this.canvas, CellDialogController2.this.type, CellDialogController2.this.size, CellDialogController2.this.width, CellDialogController2.this.height);
                CellDialogController2.this.field0.setDisable(CellDialogController2.this.size[0] < 0);
                CellDialogController2.this.field0.setText(CellDialogController2.this.size[0] < 0 ? Lang.getString("size.full") : String.valueOf(CellDialogController2.this.size[0]));
                CellDialogController2.this.field1.setText(String.valueOf(CellDialogController2.this.size[1]));
                CellDialogController2.this.field2.setText(String.valueOf(CellDialogController2.this.size[2]));
                CellDialogController2.this.field3.setText(String.valueOf(CellDialogController2.this.size[3]));
            }
        });
        CanvasDrawer.createListener(this.canvas, new CanvasDrawer.CavasInfoListener(){

            @Override
            public void sizeChanged(int[] nArray) {
                ((CellDialogController2)CellDialogController2.this).size[0] = nArray[0];
                ((CellDialogController2)CellDialogController2.this).size[1] = nArray[1];
                ((CellDialogController2)CellDialogController2.this).size[2] = nArray[2];
                ((CellDialogController2)CellDialogController2.this).size[3] = nArray[3];
                CanvasDrawer.drawCanvas(CellDialogController2.this.canvas, CellDialogController2.this.type, CellDialogController2.this.size, CellDialogController2.this.width, CellDialogController2.this.height);
                CellDialogController2.this.field0.setDisable(CellDialogController2.this.size[0] < 0);
                CellDialogController2.this.field0.setText(CellDialogController2.this.size[0] < 0 ? Lang.getString("size.full") : String.valueOf(CellDialogController2.this.size[0]));
                CellDialogController2.this.field1.setText(String.valueOf(CellDialogController2.this.size[1]));
                CellDialogController2.this.field2.setText(String.valueOf(CellDialogController2.this.size[2]));
                CellDialogController2.this.field3.setText(String.valueOf(CellDialogController2.this.size[3]));
            }
        });
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
        stage.setOnCloseRequest((EventHandler)new EventHandler<WindowEvent>(){

            public void handle(WindowEvent windowEvent) {
                CanvasDrawer.deleteListener(CellDialogController2.this.canvas);
            }
        });
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
        if (n < 73) {
            this.radio10.setDisable(true);
            this.radio11.setDisable(true);
            this.radio12.setDisable(true);
            this.radio13.setDisable(true);
            this.radio14.setDisable(true);
            this.radio15.setDisable(true);
        }
        if (n < 85) {
            this.radio16.setDisable(true);
        }
    }

    public void setScreen(int n, int n2) {
        this.width = n;
        this.height = n2;
        this.sizeLabel.setText(n + " \u00d7 " + n2);
        if (n < n2) {
            this.canvas.setWidth(this.canvas.getWidth() * (double)n / (double)n2);
        } else if (n2 < n) {
            this.canvas.setHeight(this.canvas.getHeight() * (double)n2 / (double)n);
        }
        this.dialogStage.sizeToScene();
    }

    public void setCell(int n, int[] nArray) {
        this.type = this.typeOrigin = n;
        this.sizeOrigin = nArray;
        this.size = Arrays.copyOf(nArray, nArray.length);
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
                break;
            }
            case 10: {
                this.radio10.setSelected(true);
                break;
            }
            case 11: {
                this.radio11.setSelected(true);
                break;
            }
            case 12: {
                this.radio12.setSelected(true);
                break;
            }
            case 13: {
                this.radio13.setSelected(true);
                break;
            }
            case 14: {
                this.radio14.setSelected(true);
                break;
            }
            case 15: {
                this.radio15.setSelected(true);
            }
            case 16: {
                this.radio16.setSelected(true);
            }
        }
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
            String string = this.field0.getText().replaceAll("[^\\d]", "");
            this.size[0] = string.isEmpty() ? 0 : Integer.parseInt(string);
        } else if (this.field1.isFocused()) {
            String string = this.field1.getText().replaceAll("[^\\d]", "");
            this.size[1] = string.isEmpty() ? 0 : Integer.parseInt(string);
        } else if (this.field2.isFocused()) {
            String string = this.field2.getText().replaceAll("[^\\d]", "");
            this.size[2] = string.isEmpty() ? 0 : Integer.parseInt(string);
        } else if (this.field3.isFocused()) {
            String string = this.field3.getText().replaceAll("[^\\d]", "");
            this.size[3] = string.isEmpty() ? 0 : Integer.parseInt(string);
        }
        CanvasDrawer.drawCanvas(this.canvas, this.type, this.size, this.width, this.height);
    }

    @FXML
    private void handleRevert() {
        this.group.selectToggle(null);
        this.setCell(this.typeOrigin, this.sizeOrigin);
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

