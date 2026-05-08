/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.Button
 *  javafx.scene.control.Label
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.ybroad.dispy.manager.model.DispyData;

public class ReserveDialogController {
    @FXML
    private Label time0Label;
    @FXML
    private Label time1Label;
    @FXML
    private Label time2Label;
    @FXML
    private Label time3Label;
    @FXML
    private Label item0Label;
    @FXML
    private Label item1Label;
    @FXML
    private Label item2Label;
    @FXML
    private Label item3Label;
    @FXML
    private Button edit0Button;
    @FXML
    private Button edit1Button;
    @FXML
    private Button edit2Button;
    @FXML
    private Button edit3Button;
    private Stage dialogStage;
    private int selected = -1;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setData(DispyData dispyData, int n) {
        this.time0Label.setText(dispyData.getReserve(n, 0));
        this.time1Label.setText(dispyData.getReserve(n, 1));
        this.time2Label.setText(dispyData.getReserve(n, 2));
        this.time3Label.setText(dispyData.getReserve(n, 3));
        this.item0Label.setText(dispyData.getReserveItem(n, 0));
        this.item1Label.setText(dispyData.getReserveItem(n, 1));
        this.item2Label.setText(dispyData.getReserveItem(n, 2));
        this.item3Label.setText(dispyData.getReserveItem(n, 3));
    }

    public int getSelected() {
        return this.selected;
    }

    @FXML
    private void handleEdit0() {
        this.selected = 0;
        this.dialogStage.close();
    }

    @FXML
    private void handleEdit1() {
        this.selected = 1;
        this.dialogStage.close();
    }

    @FXML
    private void handleEdit2() {
        this.selected = 2;
        this.dialogStage.close();
    }

    @FXML
    private void handleEdit3() {
        this.selected = 3;
        this.dialogStage.close();
    }

    @FXML
    private void handleOk() {
        this.dialogStage.close();
    }
}

