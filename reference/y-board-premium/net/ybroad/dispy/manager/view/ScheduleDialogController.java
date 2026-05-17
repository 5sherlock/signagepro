/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.event.EventHandler
 *  javafx.fxml.FXML
 *  javafx.geometry.Pos
 *  javafx.scene.control.TableCell
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
 *  javafx.scene.input.MouseEvent
 *  javafx.stage.Stage
 *  javafx.util.Callback
 */
package net.ybroad.dispy.manager.view;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.ybroad.dispy.lib.PlayData;
import net.ybroad.dispy.manager.MainApp;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.model.PlaylistData;
import net.ybroad.dispy.manager.model.ScheduleData;

public class ScheduleDialogController {
    @FXML
    private TableView<ScheduleData> tableView;
    @FXML
    private TableColumn<ScheduleData, String> cellColumn;
    @FXML
    private TableColumn<ScheduleData, String> typeColumn;
    @FXML
    private TableColumn<ScheduleData, String> dataColumn;
    private ObservableList<ScheduleData> data = FXCollections.observableArrayList();
    private Stage dialogStage;
    private MainApp mainApp;

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
                            this.setStyle("-fx-border-width:0; -fx-background-insets:0 1 1 0; -fx-background-color:" + string.split("/")[1] + ";");
                        }
                    }
                };
                tableCell.addEventHandler(MouseEvent.MOUSE_CLICKED, (EventHandler)new EventHandler<MouseEvent>((TableCell)tableCell){
                    final /* synthetic */ TableCell val$cell;
                    {
                        this.val$cell = tableCell;
                    }

                    public void handle(MouseEvent mouseEvent) {
                        ScheduleDialogController.this.mainApp.showScheduleDialog2((ObservableList<ScheduleData>)ScheduleDialogController.this.data, Integer.parseInt(((String)this.val$cell.getItem()).split("/")[0]));
                    }
                });
                return tableCell;
            }
        };
        Calendar calendar = Calendar.getInstance();
        int n = calendar.get(2);
        ArrayList<TableColumn> arrayList = new ArrayList<TableColumn>();
        arrayList.add(new TableColumn(calendar.getDisplayName(2, 1, Locale.getDefault())));
        for (int i = 0; i < 60; ++i) {
            TableColumn tableColumn;
            int n2 = i;
            int n3 = calendar.get(2);
            if (n3 < n) {
                n3 += 12;
            }
            if (arrayList.size() > n3 - n) {
                tableColumn = (TableColumn)arrayList.get(n3 - n);
            } else {
                tableColumn = new TableColumn(calendar.getDisplayName(2, 1, Locale.getDefault()));
                arrayList.add(tableColumn);
            }
            int n4 = calendar.get(5);
            int n5 = calendar.get(7);
            TableColumn tableColumn2 = new TableColumn(String.valueOf(n4));
            tableColumn2.setCellValueFactory(cellDataFeatures -> ((ScheduleData)cellDataFeatures.getValue()).dayProperty(n2));
            tableColumn2.setCellFactory((Callback)callback);
            if (n5 == 7) {
                tableColumn2.setStyle("-fx-font-size: 9pt; -fx-background-color: lightblue;");
            } else if (n5 == 1) {
                tableColumn2.setStyle("-fx-font-size: 9pt; -fx-background-color: lightpink;");
            } else {
                tableColumn2.setStyle("-fx-font-size: 9pt;");
            }
            tableColumn2.setMinWidth(18.0);
            tableColumn2.setMaxWidth(18.0);
            tableColumn.getColumns().add((Object)tableColumn2);
            calendar.add(5, 1);
        }
        this.tableView.getColumns().addAll(arrayList);
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setData(DispyData dispyData) {
        for (int i = 0; i < 4; ++i) {
            ArrayList<ScheduleData> arrayList = new ArrayList<ScheduleData>();
            for (PlaylistData playlistData : dispyData.getPlaylist(i)) {
                if (((PlayData)playlistData).type.startsWith("sync")) continue;
                long l = Math.max(Long.parseLong(dispyData.play[i]), playlistData.getOpen() > 0L ? playlistData.getOpen() : 0L);
                long l2 = playlistData.getTill() > 0L ? playlistData.getTill() : Long.MAX_VALUE;
                arrayList.add(new ScheduleData(playlistData, i, 0, l, l2));
            }
            for (int j = 0; j < 4; ++j) {
                List<PlaylistData> list = dispyData.getReservelist(i, j);
                if (list.isEmpty()) continue;
                for (ScheduleData scheduleData : arrayList) {
                    PlaylistData playlistData = null;
                    for (PlaylistData playlistData2 : list) {
                        if (!scheduleData.getData().equals(playlistData2.getData())) continue;
                        playlistData = playlistData2;
                        break;
                    }
                    if (playlistData == null) {
                        scheduleData.addTime(dispyData.getReserveTime(i, j), dispyData.getReserveTime(i, j));
                        scheduleData.addShow(null);
                        continue;
                    }
                    scheduleData.addTime(dispyData.getReserveTime(i, j), Long.MAX_VALUE);
                    scheduleData.addShow(playlistData);
                }
                for (PlaylistData playlistData : list) {
                    boolean bl = false;
                    for (ScheduleData scheduleData : arrayList) {
                        if (!scheduleData.getData().equals(playlistData.getData())) continue;
                        bl = true;
                        break;
                    }
                    if (bl || ((PlayData)playlistData).type.startsWith("sync")) continue;
                    long l = Math.max(dispyData.getReserveTime(i, j), playlistData.getOpen() > 0L ? playlistData.getOpen() : 0L);
                    long l3 = playlistData.getTill() > 0L ? playlistData.getTill() : Long.MAX_VALUE;
                    arrayList.add(new ScheduleData(playlistData, i, j + 1, l, l3));
                }
            }
            this.data.addAll(arrayList);
        }
    }

    @FXML
    private void handleOk() {
        this.dialogStage.close();
    }
}

