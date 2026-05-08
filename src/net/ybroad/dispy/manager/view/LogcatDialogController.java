/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.Label
 *  javafx.scene.control.TextArea
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class LogcatDialogController {
    @FXML
    private Label label;
    @FXML
    private TextArea text;
    private Stage dialogStage;
    private String id;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    @FXML
    private void handleOk() {
        this.dialogStage.close();
    }

    public void setDispyId(String string) {
        this.id = string;
    }

    public String getDispyId() {
        return this.id;
    }

    public void setLogs(String[] stringArray) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String string : stringArray) {
            stringBuilder.append(string).append("\n");
        }
        this.text.setText(stringBuilder.toString());
        this.label.setVisible(false);
    }

    public void close() {
        this.dialogStage.close();
    }
}

