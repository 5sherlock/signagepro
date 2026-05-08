/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.fxml.FXML
 *  javafx.geometry.Pos
 *  javafx.scene.control.TableCell
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
 *  javafx.stage.Stage
 *  javafx.util.Callback
 */
package net.ybroad.dispy.manager.view;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.ybroad.dispy.manager.model.ScheduleData;

public class ScheduleDialog2Controller {
    @FXML
    private TableView<ScheduleData> tableView;
    @FXML
    private TableColumn<ScheduleData, String> cellColumn;
    @FXML
    private TableColumn<ScheduleData, String> typeColumn;
    @FXML
    private TableColumn<ScheduleData, String> dataColumn;
    @FXML
    private TableColumn<ScheduleData, String> dateColumn;
    private ObservableList<ScheduleData> data = FXCollections.observableArrayList();
    private int date;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        this.tableView.setItems(this.data);
        this.tableView.getSelectionModel().setCellSelectionEnabled(true);
        this.cellColumn.setCellValueFactory(cellDataFeatures -> ((ScheduleData)cellDataFeatures.getValue()).cellProperty());
        this.typeColumn.setCellValueFactory(cellDataFeatures -> ((ScheduleData)cellDataFeatures.getValue()).typeProperty());
        this.dataColumn.setCellValueFactory(cellDataFeatures -> ((ScheduleData)cellDataFeatures.getValue()).dataProperty());
        this.cellColumn.setCellFactory((Callback)new Callback<TableColumn<ScheduleData, String>, TableCell<ScheduleData, String>>(){

            public TableCell<ScheduleData, String> call(TableColumn<ScheduleData, String> tableColumn) {
                return new TableCell<ScheduleData, String>(){

                    public void updateItem(String string, boolean bl) {
                        super.updateItem((Object)string, bl);
                        this.setText(bl ? "" : string);
                        this.setAlignment(Pos.CENTER);
                        if (string == null) {
                            this.setStyle("");
                        } else {
                            switch (string) {
                                case "#0": {
                                    this.setStyle("-fx-text-fill: white; -fx-background-color: rgb(25, 171, 144);");
                                    break;
                                }
                                case "#1": {
                                    this.setStyle("-fx-text-fill: white; -fx-background-color: rgb(155, 82, 160);");
                                    break;
                                }
                                case "#2": {
                                    this.setStyle("-fx-text-fill: white; -fx-background-color: rgb(255, 153, 0);");
                                    break;
                                }
                                case "#3": {
                                    this.setStyle("-fx-text-fill: white; -fx-background-color: rgb(59, 154, 218);");
                                    break;
                                }
                                default: {
                                    this.setStyle("");
                                }
                            }
                        }
                    }
                };
            }
        });
        Callback<TableColumn<ScheduleData, String>, TableCell<ScheduleData, String>> callback = new Callback<TableColumn<ScheduleData, String>, TableCell<ScheduleData, String>>(){

            public TableCell<ScheduleData, String> call(TableColumn<ScheduleData, String> tableColumn) {
                TableCell<ScheduleData, String> tableCell = new TableCell<ScheduleData, String>(){

                    public void updateItem(String string, boolean bl) {
                        super.updateItem((Object)string, bl);
                        if (string == null || bl) {
                            this.setDisable(true);
                            this.setStyle("--fx-border-width:0; fx-background-color:white;");
                        } else {
                            this.setStyle("-fx-border-width:0; -fx-background-insets:0 1 1 0; -fx-background-color:" + string + ";");
                        }
                    }
                };
                return tableCell;
            }
        };
        for (int i = 0; i < 24; ++i) {
            int n = i;
            TableColumn tableColumn = new TableColumn(String.valueOf(i));
            tableColumn.setCellValueFactory(cellDataFeatures -> ((ScheduleData)cellDataFeatures.getValue()).hourProperty(this.date, n));
            tableColumn.setCellFactory((Callback)callback);
            tableColumn.setStyle("-fx-font-size: 9pt;");
            tableColumn.setMinWidth(18.0);
            tableColumn.setMaxWidth(18.0);
            this.dateColumn.getColumns().add((Object)tableColumn);
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setData(ObservableList<ScheduleData> observableList, int n) {
        this.data.addAll(observableList);
        this.date = n;
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, n);
        calendar.set(10, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        String string = new SimpleDateFormat("MM-dd (EE)").format(calendar.getTime());
        this.dateColumn.setText(string);
        this.dialogStage.setTitle(this.dialogStage.getTitle() + " - " + string);
    }

    @FXML
    private void handleOk() {
        this.dialogStage.close();
    }
}

