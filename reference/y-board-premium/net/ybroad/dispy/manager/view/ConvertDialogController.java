/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.fxml.FXML
 *  javafx.scene.control.Button
 *  javafx.scene.control.Label
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableRow
 *  javafx.scene.control.TableView
 *  javafx.scene.control.cell.CheckBoxTableCell
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;
import net.ybroad.dispy.lib.PlayData;
import net.ybroad.dispy.manager.model.PlaylistData;
import net.ybroad.dispy.manager.util.Lang;

public class ConvertDialogController {
    @FXML
    TableView<PlaylistData> tableView;
    @FXML
    private TableColumn<PlaylistData, String> nameColumn;
    @FXML
    private TableColumn<PlaylistData, Boolean> checkColumn;
    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label info;
    private Stage dialogStage;
    private ObservableList<PlaylistData> data = FXCollections.observableArrayList();
    private ObservableList<PlaylistData> checked = FXCollections.observableArrayList();
    private boolean result;
    private boolean isWindow;

    @FXML
    private void initialize() {
        this.nameColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).dataProperty());
        this.checkColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).checkProperty());
        this.checkColumn.setCellFactory(tableColumn -> new CheckBoxTableCell<PlaylistData, Boolean>(){

            public void updateItem(Boolean bl, boolean bl2) {
                super.updateItem((Object)bl, bl2);
                TableRow tableRow = this.getTableRow();
                this.setEditable(true);
                if (tableRow.getItem() != null && !bl2) {
                    switch (((PlayData)tableRow.getItem()).type) {
                        case "video": {
                            if (((PlaylistData)tableRow.getItem()).getData().endsWith("mp4")) break;
                            this.setEditable(false);
                            break;
                        }
                        default: {
                            this.setEditable(false);
                        }
                    }
                }
            }
        });
        this.tableView.setItems(this.data);
        this.info.setText(Lang.getString("convert.info"));
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setTargetList(boolean bl, List<PlaylistData> list) {
        this.isWindow = bl;
        this.data.addAll(list);
        for (PlaylistData playlistData : list) {
            playlistData.setChecked(true);
        }
    }

    public ObservableList<PlaylistData> getCheckedList() {
        return this.checked;
    }

    public boolean getResult() {
        return this.result;
    }

    @FXML
    private void handleOk() {
        this.checked.addAll(this.data);
        for (int i = this.checked.size() - 1; i >= 0; --i) {
            PlaylistData playlistData = (PlaylistData)this.checked.get(i);
            if (playlistData.getChecked()) continue;
            this.checked.remove((Object)playlistData);
        }
        this.dialogStage.close();
        this.result = true;
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
        this.result = false;
    }
}

