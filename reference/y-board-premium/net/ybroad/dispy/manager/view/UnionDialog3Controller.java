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

public class UnionDialog3Controller {
    @FXML
    private TextField pxmm;
    @FXML
    private TextField devXpx;
    @FXML
    private TextField devYpx;
    @FXML
    private TextField devWpx;
    @FXML
    private TextField devHpx;
    @FXML
    private TextField worldWpx;
    @FXML
    private TextField worldHpx;
    @FXML
    private TextField devXmm;
    @FXML
    private TextField devYmm;
    @FXML
    private TextField devWmm;
    @FXML
    private TextField devHmm;
    @FXML
    private TextField worldWmm;
    @FXML
    private TextField worldHmm;
    private Stage dialogStage;
    private DispyData data;
    private boolean clickOk;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setData(DispyData dispyData) {
        this.data = dispyData;
        this.pxmm.setText(String.format("%f", dispyData.unionf));
        this.devXpx.setText(String.format("%d", dispyData.unionx));
        this.devYpx.setText(String.format("%d", dispyData.uniony));
        this.devWpx.setText(String.format("%d", dispyData.width));
        this.devHpx.setText(String.format("%d", dispyData.height));
        this.worldWpx.setText(String.format("%d", dispyData.unionw));
        this.worldHpx.setText(String.format("%d", dispyData.unionh));
        this.devXmm.setText(String.format("%.1f", (double)dispyData.unionx / dispyData.unionf));
        this.devYmm.setText(String.format("%.1f", (double)dispyData.uniony / dispyData.unionf));
        this.devWmm.setText(String.format("%.1f", (double)dispyData.width / dispyData.unionf));
        this.devHmm.setText(String.format("%.1f", (double)dispyData.height / dispyData.unionf));
        this.worldWmm.setText(String.format("%.1f", (double)dispyData.unionw / dispyData.unionf));
        this.worldHmm.setText(String.format("%.1f", (double)dispyData.unionh / dispyData.unionf));
    }

    public boolean isClickOk() {
        return this.clickOk;
    }

    public DispyData getResult() {
        return this.data;
    }

    @FXML
    private void refresh() {
        double d;
        double d2;
        int n;
        int n2;
        double d3;
        double d4;
        double d5;
        try {
            d5 = Double.parseDouble(this.pxmm.getText());
        }
        catch (Exception exception) {
            d5 = 0.0;
        }
        try {
            d4 = Double.parseDouble(this.devXmm.getText());
        }
        catch (Exception exception) {
            d4 = 0.0;
        }
        try {
            d3 = Double.parseDouble(this.devYmm.getText());
        }
        catch (Exception exception) {
            d3 = 0.0;
        }
        try {
            n2 = Integer.parseInt(this.devWpx.getText());
        }
        catch (Exception exception) {
            n2 = 0;
        }
        try {
            n = Integer.parseInt(this.devHpx.getText());
        }
        catch (Exception exception) {
            n = 0;
        }
        try {
            d2 = Double.parseDouble(this.worldWmm.getText());
        }
        catch (Exception exception) {
            d2 = 0.0;
        }
        try {
            d = Double.parseDouble(this.worldHmm.getText());
        }
        catch (Exception exception) {
            d = 0.0;
        }
        this.devXpx.setText(String.format("%d", (int)(d4 * d5)));
        this.devYpx.setText(String.format("%d", (int)(d3 * d5)));
        this.devWmm.setText(d5 == 0.0 ? "?" : String.format("%.1f", (double)n2 / d5));
        this.devHmm.setText(d5 == 0.0 ? "?" : String.format("%.1f", (double)n / d5));
        this.worldWpx.setText(String.format("%d", (int)(d2 * d5)));
        this.worldHpx.setText(String.format("%d", (int)(d * d5)));
    }

    @FXML
    private void handleOk() {
        try {
            this.data.unionx = Integer.parseInt(this.devXpx.getText());
            this.data.uniony = Integer.parseInt(this.devYpx.getText());
            this.data.unionw = Integer.parseInt(this.worldWpx.getText());
            this.data.unionh = Integer.parseInt(this.worldHpx.getText());
            this.data.unionf = Double.parseDouble(this.pxmm.getText());
            if (this.data.unionf == 0.0) {
                return;
            }
        }
        catch (Exception exception) {
            return;
        }
        this.clickOk = true;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }

    public void close() {
        this.dialogStage.close();
    }
}

