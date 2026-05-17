/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.fxml.FXML
 *  javafx.scene.control.DatePicker
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
 *  javafx.stage.Stage
 *  javafx.util.StringConverter
 */
package net.ybroad.dispy.manager.view;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import net.ybroad.dispy.manager.MainApp;
import net.ybroad.dispy.manager.model.HistoryData;

public class HistoryDialogController {
    @FXML
    private TableView<HistoryData> tableView;
    @FXML
    private TableColumn<HistoryData, String> dataColumn;
    @FXML
    private TableColumn<HistoryData, String> sumColumn;
    private ObservableList<HistoryData> history = FXCollections.observableArrayList();
    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    private MainApp mainApp;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        this.tableView.setItems(this.history);
        this.dataColumn.setCellValueFactory(cellDataFeatures -> ((HistoryData)cellDataFeatures.getValue()).data);
        this.sumColumn.setCellValueFactory(cellDataFeatures -> ((HistoryData)cellDataFeatures.getValue()).sum);
        final String string = "yyyy-MM-dd";
        this.fromPicker.setPromptText(string.toLowerCase());
        this.toPicker.setPromptText(string.toLowerCase());
        StringConverter<LocalDate> stringConverter = new StringConverter<LocalDate>(){
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
        };
        this.fromPicker.setConverter((StringConverter)stringConverter);
        this.toPicker.setConverter((StringConverter)stringConverter);
        this.fromPicker.setValue((Object)LocalDate.now());
        this.toPicker.setValue((Object)LocalDate.now());
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setHistoryData(ArrayList<HistoryData> arrayList) {
        this.history.addAll(arrayList);
    }

    @FXML
    private void handleSubmit() {
        LocalDate localDate = (LocalDate)this.fromPicker.getValue();
        LocalDate localDate2 = (LocalDate)this.toPicker.getValue();
        if (localDate != null && localDate2 != null) {
            long l;
            this.history.clear();
            ObservableList observableList = this.tableView.getColumns();
            while (observableList.size() > 2) {
                observableList.remove(2);
            }
            long l2 = localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond() / 86400L;
            if (l2 <= (l = localDate2.atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond() / 86400L) && l - l2 <= 61L) {
                Object object;
                int n = 0;
                while ((long)n <= l - l2) {
                    object = localDate.plusDays(n);
                    String string = String.format("%02d%02d%02d", ((LocalDate)object).getYear() % 100, ((LocalDate)object).getMonthValue(), ((LocalDate)object).getDayOfMonth());
                    String string2 = String.format("%02d-%02d", ((LocalDate)object).getMonthValue(), ((LocalDate)object).getDayOfMonth());
                    TableColumn tableColumn = new TableColumn(string2);
                    tableColumn.setStyle("-fx-alignment: center;");
                    tableColumn.setMinWidth(45.0);
                    tableColumn.setMaxWidth(45.0);
                    tableColumn.setCellValueFactory(cellDataFeatures -> ((HistoryData)cellDataFeatures.getValue()).getValue(string));
                    this.tableView.getColumns().add((Object)tableColumn);
                    ++n;
                }
                String string = String.format("%02d%02d%02d", localDate.getYear() % 100, localDate.getMonthValue(), localDate.getDayOfMonth());
                object = String.format("%02d%02d%02d", localDate2.getYear() % 100, localDate2.getMonthValue(), localDate2.getDayOfMonth());
                this.mainApp.requestHistory(string, (String)object);
            }
        }
    }

    @FXML
    private void handleOk() {
        this.dialogStage.close();
    }

    public void close() {
        this.dialogStage.close();
    }
}

