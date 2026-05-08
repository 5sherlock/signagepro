/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.DatePicker
 *  javafx.stage.Stage
 *  javafx.util.StringConverter
 */
package net.ybroad.dispy.manager.view;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class LicenseDialogController {
    @FXML
    private DatePicker datePicker;
    private Stage dialogStage;
    private long limit = 0L;

    @FXML
    private void initialize() {
        final String string = "yyyy-MM-dd";
        this.datePicker.setPromptText(string.toLowerCase());
        this.datePicker.setConverter((StringConverter)new StringConverter<LocalDate>(){
            DateTimeFormatter dateFormatter;
            {
                this.dateFormatter = DateTimeFormatter.ofPattern(string);
            }

            public String toString(LocalDate localDate) {
                if (localDate != null) {
                    return this.dateFormatter.format(localDate);
                }
                return "";
            }

            public LocalDate fromString(String string2) {
                if (string2 != null && !string2.isEmpty()) {
                    return LocalDate.parse(string2, this.dateFormatter);
                }
                return null;
            }
        });
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public long getLimit() {
        return this.limit;
    }

    @FXML
    private void handleOk() {
        LocalDate localDate = (LocalDate)this.datePicker.getValue();
        if (localDate != null) {
            this.limit = localDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
        }
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }
}

