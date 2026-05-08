/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.event.ActionEvent
 *  javafx.event.EventHandler
 *  javafx.fxml.FXML
 *  javafx.scene.Node
 *  javafx.scene.control.Button
 *  javafx.scene.control.CheckBox
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
 *  javafx.scene.control.cell.CheckBoxTableCell
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;
import net.ybroad.dispy.manager.model.DispyData;

public class CopyDialogController {
    @FXML
    TableView<DispyData> tableView;
    @FXML
    private TableColumn<DispyData, String> nameColumn;
    @FXML
    private TableColumn<DispyData, Boolean> checkColumn;
    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    private Stage dialogStage;
    private ObservableList<DispyData> data = FXCollections.observableArrayList();
    private ObservableList<DispyData> checked = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        this.nameColumn.setCellValueFactory(cellDataFeatures -> ((DispyData)cellDataFeatures.getValue()).nameProperty());
        this.checkColumn.setCellValueFactory(cellDataFeatures -> ((DispyData)cellDataFeatures.getValue()).checkProperty());
        this.checkColumn.setCellFactory(CheckBoxTableCell.forTableColumn(this.checkColumn));
        final CheckBox checkBox = new CheckBox();
        checkBox.setOnAction((EventHandler)new EventHandler<ActionEvent>(){

            public void handle(ActionEvent actionEvent) {
                CopyDialogController.this.checkAll(checkBox.isSelected());
            }
        });
        this.checkColumn.setGraphic((Node)checkBox);
        this.tableView.setItems(this.data);
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setTargetList(ObservableList<DispyData> observableList) {
        for (DispyData dispyData : observableList) {
            dispyData.setChecked(false);
        }
        this.data.addAll(observableList);
    }

    public ObservableList<DispyData> getCheckedList() {
        return this.checked;
    }

    private void checkAll(boolean bl) {
        for (DispyData dispyData : this.data) {
            dispyData.setChecked(bl);
        }
    }

    @FXML
    private void handleOk() {
        this.checked.addAll(this.data);
        for (int i = this.checked.size() - 1; i >= 0; --i) {
            DispyData dispyData = (DispyData)this.checked.get(i);
            if (dispyData.getChecked()) continue;
            this.checked.remove((Object)dispyData);
        }
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }
}

