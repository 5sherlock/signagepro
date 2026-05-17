/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 *  javafx.collections.ObservableList
 *  javafx.collections.transformation.FilteredList
 *  javafx.fxml.FXML
 *  javafx.geometry.Pos
 *  javafx.geometry.Rectangle2D
 *  javafx.scene.control.Button
 *  javafx.scene.control.Separator
 *  javafx.scene.control.TableCell
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
 *  javafx.scene.control.TextField
 *  javafx.scene.control.ToolBar
 *  javafx.stage.Stage
 *  javafx.util.Callback
 */
package net.ybroad.dispy.manager.view;

import java.io.Serializable;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.manager.MainApp;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.model.IsleServerData;
import net.ybroad.dispy.manager.model.MemberData;
import net.ybroad.dispy.manager.util.Lang;

public class MemberDialogController {
    @FXML
    private ToolBar topToolbar;
    @FXML
    private Button allButton;
    @FXML
    private Button noneButton;
    @FXML
    private TextField filterText;
    @FXML
    private Separator adminSeparator;
    @FXML
    private Button messageButton;
    @FXML
    private TableView<MemberData> tableView;
    @FXML
    private TableColumn<MemberData, String> nameColumn;
    @FXML
    private TableColumn<MemberData, String> deviceColumn;
    @FXML
    private TableColumn<MemberData, String> stateColumn;
    @FXML
    private TableColumn<MemberData, String> touchColumn;
    @FXML
    private ToolBar bottomToolbar;
    @FXML
    private Button registerButton;
    @FXML
    private Button editButton;
    @FXML
    private Button isleNewButton;
    @FXML
    private Button isleFixButton;
    private MainApp mainApp;
    private Stage dialogStage;
    private FilteredList<MemberData> filteredMembers;
    private boolean updateCountPandding = false;
    private ObservableList<DispyData> updateCountDispyData;
    private ObservableList<MemberData> updateCountMemberData;

    @FXML
    private void initialize() {
        this.nameColumn.setCellValueFactory(cellDataFeatures -> ((MemberData)cellDataFeatures.getValue()).nameProperty());
        this.deviceColumn.setCellValueFactory(cellDataFeatures -> ((MemberData)cellDataFeatures.getValue()).deviceProperty());
        this.stateColumn.setCellValueFactory(cellDataFeatures -> ((MemberData)cellDataFeatures.getValue()).onoffProperty());
        this.touchColumn.setCellValueFactory(cellDataFeatures -> ((MemberData)cellDataFeatures.getValue()).touchProperty());
        this.deviceColumn.setCellFactory((Callback)new Callback<TableColumn<MemberData, String>, TableCell<MemberData, String>>(){

            public TableCell<MemberData, String> call(TableColumn<MemberData, String> tableColumn) {
                return new TableCell<MemberData, String>(){

                    public void updateItem(String string, boolean bl) {
                        super.updateItem((Object)string, bl);
                        this.setText(bl ? "" : string);
                        this.setAlignment(Pos.CENTER);
                    }
                };
            }
        });
        final String string = Lang.getString("isle.server");
        final String string2 = Lang.getString("member.connect");
        this.stateColumn.setCellFactory((Callback)new Callback<TableColumn<MemberData, String>, TableCell<MemberData, String>>(){

            public TableCell<MemberData, String> call(TableColumn<MemberData, String> tableColumn) {
                return new TableCell<MemberData, String>(){

                    public void updateItem(String string, boolean bl) {
                        super.updateItem((Object)string, bl);
                        this.setText(bl ? "" : string);
                        this.setAlignment(Pos.CENTER);
                        if (string == null) {
                            this.setStyle("");
                        } else if (string.equals(string)) {
                            this.setStyle("-fx-background-insets:0 1 1 0; -fx-background-color: lightgray;");
                        } else if (string.equals(string2)) {
                            this.setStyle("-fx-background-insets:0 1 1 0; -fx-background-color: palegreen;");
                        } else {
                            this.setStyle("-fx-background-insets:0 1 1 0; -fx-background-color: lightpink;");
                        }
                    }
                };
            }
        });
        this.tableView.getSelectionModel().selectedItemProperty().addListener((observableValue, memberData, memberData2) -> this.handleSelect((MemberData)memberData2));
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setUser(UserData userData) {
        if (userData.name.equals("admin")) {
            this.adminSeparator.setVisible(true);
            this.messageButton.setVisible(true);
        } else {
            this.adminSeparator.setVisible(false);
            this.messageButton.setVisible(false);
        }
        if (userData.name.startsWith("admin")) {
            this.noneButton.setVisible(true);
            this.bottomToolbar.setVisible(true);
            this.bottomToolbar.setManaged(true);
        } else {
            this.noneButton.setVisible(false);
            this.bottomToolbar.setVisible(false);
            this.bottomToolbar.setManaged(false);
        }
    }

    public void setMember(ObservableList<MemberData> observableList) {
        this.filteredMembers = new FilteredList(observableList, memberData -> true);
        this.tableView.setItems((ObservableList)this.filteredMembers);
        this.allButton.setText(this.allButton.getText() + " (0)");
        this.noneButton.setText(this.noneButton.getText() + " (0)");
    }

    public void updateCount(ObservableList<DispyData> observableList, ObservableList<MemberData> observableList2) {
        this.updateCountDispyData = observableList;
        this.updateCountMemberData = observableList2;
        if (!this.updateCountPandding) {
            this.updateCountPandding = true;
            Platform.runLater(() -> {
                this._updateCount(this.updateCountDispyData, this.updateCountMemberData);
                this.updateCountPandding = false;
            });
        }
    }

    private void _updateCount(ObservableList<DispyData> observableList, ObservableList<MemberData> observableList2) {
        Object object2;
        int n = 0;
        int n2 = 0;
        for (Object object2 : observableList) {
            if (!((DispyData)object2).getOnoffBoolean()) continue;
            if (((DispyData)object2).getOwner().isEmpty()) {
                ++n2;
            }
            ++n;
        }
        int n3 = 0;
        for (MemberData memberData : observableList2) {
            if (!(memberData instanceof IsleServerData)) continue;
            n3 += memberData.getLimitInt();
        }
        object2 = this.allButton.getText();
        int n4 = ((String)object2).indexOf(40);
        int n5 = ((String)object2).indexOf(41);
        this.allButton.setText(((String)object2).substring(0, n4 + 1) + n + "/" + (observableList.size() + n3) + ((String)object2).substring(n5));
        int n6 = 0;
        for (Serializable serializable : observableList) {
            if (!((DispyData)serializable).getOwner().isEmpty()) continue;
            ++n6;
        }
        object2 = this.noneButton.getText();
        n4 = ((String)object2).indexOf(40);
        n5 = ((String)object2).indexOf(41);
        this.noneButton.setText(((String)object2).substring(0, n4 + 1) + n2 + "/" + n6 + ((String)object2).substring(n5));
        for (Serializable serializable : this.tableView.getItems()) {
            int n7 = 0;
            int n8 = 0;
            for (DispyData dispyData : observableList) {
                if (dispyData.getOwner().equals(((MemberData)serializable).getName())) {
                    if (dispyData.getOnoffBoolean()) {
                        ++n7;
                    }
                    ++n8;
                }
                ((MemberData)serializable).setDevice(n7 + "/" + n8);
            }
        }
    }

    public String getCurrent() {
        MemberData memberData = (MemberData)this.tableView.getSelectionModel().getSelectedItem();
        if (memberData == null) {
            return "";
        }
        return memberData.getName();
    }

    private void handleSelect(MemberData memberData) {
        if (memberData == null) {
            this.mainApp.setFilter(null);
            this.editButton.setDisable(true);
            this.isleFixButton.setDisable(true);
        } else {
            this.mainApp.setFilter(memberData.getName());
            if (memberData instanceof IsleServerData) {
                this.isleFixButton.setDisable(false);
                this.editButton.setDisable(true);
            } else {
                this.isleFixButton.setDisable(true);
                this.editButton.setDisable(false);
            }
        }
    }

    @FXML
    private void handleAll() {
        this.tableView.getSelectionModel().clearSelection();
        this.mainApp.setFilter(null);
    }

    @FXML
    private void handleNone() {
        this.tableView.getSelectionModel().clearSelection();
        this.mainApp.setFilter("");
    }

    @FXML
    private void handleSearchClear() {
        this.filterText.setText("");
        this.handleSearch();
    }

    @FXML
    private void handleSearch() {
        String string = this.filterText.getText().toLowerCase();
        this.filteredMembers.setPredicate(memberData -> memberData.getName().toLowerCase().contains(string));
    }

    @FXML
    private void handleRegister() {
        this.mainApp.showRegisterDialog();
    }

    @FXML
    private void handleEdit() {
        MemberData memberData = (MemberData)this.tableView.getSelectionModel().getSelectedItem();
        if (!(memberData instanceof IsleServerData)) {
            this.mainApp.showMemberEditDialog(memberData);
        }
    }

    @FXML
    private void handleMessage() {
        this.mainApp.showSendMessageDialog();
    }

    @FXML
    private void handleIsleNew() {
        this.mainApp.showIsleDialog(null);
    }

    @FXML
    private void handleIsleFix() {
        MemberData memberData = (MemberData)this.tableView.getSelectionModel().getSelectedItem();
        if (memberData instanceof IsleServerData) {
            this.mainApp.showIsleDialog((IsleServerData)memberData);
        }
    }

    public void setPosition(Rectangle2D rectangle2D, Stage stage) {
        stage.setX((rectangle2D.getWidth() - stage.getWidth() + this.dialogStage.getWidth()) / 2.0);
        this.dialogStage.setX(stage.getX() - this.dialogStage.getWidth());
        this.dialogStage.setY(stage.getY());
    }

    public void close() {
        this.dialogStage.close();
    }
}

