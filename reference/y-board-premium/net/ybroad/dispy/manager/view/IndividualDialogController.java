/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.javafx.scene.control.skin.TableViewSkin
 *  com.sun.javafx.scene.control.skin.VirtualFlow
 *  javafx.application.Platform
 *  javafx.beans.value.ChangeListener
 *  javafx.beans.value.ObservableValue
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.event.ActionEvent
 *  javafx.event.EventHandler
 *  javafx.fxml.FXML
 *  javafx.scene.control.Button
 *  javafx.scene.control.ComboBox
 *  javafx.scene.control.Label
 *  javafx.scene.control.ListCell
 *  javafx.scene.control.ListView
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
 *  javafx.scene.control.TextArea
 *  javafx.scene.control.TextField
 *  javafx.scene.input.DragEvent
 *  javafx.scene.input.Dragboard
 *  javafx.scene.input.TransferMode
 *  javafx.scene.layout.AnchorPane
 *  javafx.stage.FileChooser
 *  javafx.stage.FileChooser$ExtensionFilter
 *  javafx.stage.Stage
 *  javafx.stage.Window
 *  javafx.stage.WindowEvent
 */
package net.ybroad.dispy.manager.view;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import net.ybroad.dispy.lib.Log;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.manager.MainApp;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.model.PlaylistData;
import net.ybroad.dispy.manager.util.Lang;

public class IndividualDialogController
implements EventHandler<WindowEvent> {
    @FXML
    private AnchorPane rootView;
    @FXML
    private TableView<PlaylistData> tableView;
    @FXML
    private TableColumn<PlaylistData, String> stateColumn;
    @FXML
    private TableColumn<PlaylistData, String> typeColumn;
    @FXML
    private TableColumn<PlaylistData, String> attrColumn;
    @FXML
    private TableColumn<PlaylistData, String> showColumn;
    @FXML
    private TableColumn<PlaylistData, String> dataColumn;
    @FXML
    private TableColumn<PlaylistData, String> deviceColumn;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private TextField timeField;
    @FXML
    private TextField fontField;
    @FXML
    private Label attrDivider;
    @FXML
    private ComboBox<String> showCombo1;
    @FXML
    private ComboBox<String> showCombo2;
    @FXML
    private ComboBox<String> showCombo3;
    @FXML
    private ComboBox<String> showCombo4;
    @FXML
    private TextField fileField;
    @FXML
    private Button fileButton;
    @FXML
    private Label fileLabel;
    @FXML
    private TextArea textArea;
    @FXML
    private ListView<DispyData> deviceList;
    @FXML
    private Label dndLabel;
    private Stage dialogStage;
    private MainApp mainApp;
    private UserData user;
    private DispyData dispy;
    private PlaylistData individual;
    private ObservableList<PlaylistData> playlist = FXCollections.observableArrayList();
    private static final String[] TYPE_TEXT = new String[]{Lang.getString("play.type.video"), Lang.getString("play.type.image"), Lang.getString("play.type.pdf"), Lang.getString("play.type.ppt"), Lang.getString("play.type.text1"), Lang.getString("play.type.text2"), Lang.getString("play.type.web"), Lang.getString("play.type.style1")};
    private static final int TYPE_VIDEO = 0;
    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_PDF = 2;
    private static final int TYPE_PPT = 3;
    private static final int TYPE_TEXT1 = 4;
    private static final int TYPE_TEXT2 = 5;
    private static final int TYPE_WEB = 6;
    private static final int TYPE_STYLE1 = 7;
    private static final String[] DAY_OF_WEEK = new String[]{Lang.getString("week.mon"), Lang.getString("week.tue"), Lang.getString("week.wed"), Lang.getString("week.thu"), Lang.getString("week.fri"), Lang.getString("week.sat"), Lang.getString("week.sun")};
    private static final String[] HOUR_OF_DAY_START = new String[]{"00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"};
    private static final String[] HOUR_OF_DAY_END = new String[]{"00:59", "01:59", "02:59", "03:59", "04:59", "05:59", "06:59", "07:59", "08:59", "09:59", "10:59", "11:59", "12:59", "13:59", "14:59", "15:59", "16:59", "17:59", "18:59", "19:59", "20:59", "21:59", "22:59", "23:59"};
    private FileChooser fileChooser = new FileChooser();
    private FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("Video (*.mp4, *.mpg, *.mpeg, *.avi, *.mkv, *.wmv, *.mov)", new String[]{"*.mp4", "*.mpg", "*.mpeg", "*.avi", "*.mkv", "*.wmv", "*.mov", "*.MP4", "*.MPG", "*.MPEG", "*.AVI", "*.MKV", "*.WMV", "*.MOV"});
    private FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("Image (*.jpg, *.png)", new String[]{"*.jpg", "*.jepg", "*.png", "*.JPG", "*.JPEG", "*.PNG"});
    private FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF (*.pdf)", new String[]{"*.pdf", "*.PDF"});
    private FileChooser.ExtensionFilter pptFilter = new FileChooser.ExtensionFilter("PPT (*.ppt, *.pptx)", new String[]{"*.ppt", "*.pptx", "*.PPT", "*.PPTX"});
    private File recentDir = null;

    @FXML
    private void initialize() {
        this.stateColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).stateProperty());
        this.typeColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).typeProperty());
        this.attrColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).attrProperty());
        this.showColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).showProperty());
        this.dataColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).dataProperty());
        this.deviceColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).individualOwner.nameProperty());
        this.tableView.setItems(this.playlist);
        this.tableView.getSelectionModel().selectedItemProperty().addListener((ChangeListener)new ChangeListener<PlaylistData>(){

            public void changed(ObservableValue<? extends PlaylistData> observableValue, PlaylistData playlistData, PlaylistData playlistData2) {
                if (playlistData != null && playlistData2 != null) {
                    IndividualDialogController.this.fillForm(playlistData2);
                    try {
                        VirtualFlow virtualFlow = (VirtualFlow)((TableViewSkin)IndividualDialogController.this.tableView.getSkin()).getChildren().get(1);
                        int n = virtualFlow.getFirstVisibleCell().getIndex();
                        int n2 = virtualFlow.getLastVisibleCell().getIndex();
                        int n3 = IndividualDialogController.this.tableView.getItems().indexOf((Object)playlistData2);
                        if (n3 < n || n2 < n3) {
                            IndividualDialogController.this.tableView.scrollTo((Object)playlistData2);
                        }
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                    }
                }
            }
        });
        this.tableView.focusedProperty().addListener((observableValue, bl, bl2) -> {
            if (bl2.booleanValue()) {
                PlaylistData playlistData = (PlaylistData)this.tableView.getSelectionModel().getSelectedItem();
                if (playlistData == null && !this.tableView.getItems().isEmpty()) {
                    playlistData = (PlaylistData)this.tableView.getItems().get(0);
                    this.tableView.getSelectionModel().select(0);
                }
                this.fillForm(playlistData);
            }
        });
        this.typeCombo.getItems().addAll((Object[])TYPE_TEXT);
        this.typeCombo.getSelectionModel().selectedItemProperty().addListener((observableValue, string, string2) -> this.typeChanged((String)string2));
        this.typeCombo.getSelectionModel().select(0);
        this.showCombo1.getItems().addAll((Object[])DAY_OF_WEEK);
        this.showCombo2.getItems().addAll((Object[])DAY_OF_WEEK);
        this.showCombo3.getItems().addAll((Object[])HOUR_OF_DAY_START);
        this.showCombo4.getItems().addAll((Object[])HOUR_OF_DAY_END);
        this.deviceList.setCellFactory(listView -> new ListCell<DispyData>(){

            public void updateItem(DispyData dispyData, boolean bl) {
                super.updateItem((Object)dispyData, bl);
                if (bl) {
                    this.setText(null);
                } else {
                    this.setText(dispyData.getName());
                }
            }
        });
    }

    private void fillForm(PlaylistData playlistData) {
        if (playlistData == null) {
            return;
        }
        String string = playlistData.getTypeCode();
        this.typeCombo.getSelectionModel().select((Object)playlistData.getType());
        switch (string) {
            case "video": 
            case "image": 
            case "pdf": 
            case "ppt": {
                this.timeField.setText(String.valueOf(playlistData.getAttrTime()));
                this.showCombo1.getSelectionModel().select(playlistData.getShowWeekStart());
                this.showCombo2.getSelectionModel().select(playlistData.getShowWeekEnd());
                this.showCombo3.getSelectionModel().select(playlistData.getShowStart());
                this.showCombo4.getSelectionModel().select(playlistData.getShowEnd());
                this.fileField.setText(playlistData.getDataFile().getAbsolutePath());
                this.fileField.home();
                this.fileField.end();
                File file = playlistData.getDataFile().getParentFile();
                if (file == null || !file.exists()) break;
                this.recentDir = file;
                break;
            }
            case "text1": 
            case "text2": 
            case "web": 
            case "style1": {
                this.timeField.setText(String.valueOf(playlistData.getAttrTime()));
                this.fontField.setText(String.valueOf(playlistData.getAttrFont()));
                this.showCombo1.getSelectionModel().select(playlistData.getShowWeekStart());
                this.showCombo2.getSelectionModel().select(playlistData.getShowWeekEnd());
                this.showCombo3.getSelectionModel().select(playlistData.getShowStart());
                this.showCombo4.getSelectionModel().select(playlistData.getShowEnd());
                this.textArea.setText(playlistData.getDataText().replace("\\n", "\n"));
            }
        }
        this.deviceList.getSelectionModel().select((Object)playlistData.individualOwner);
    }

    private void typeChanged(String string) {
        if (string == null) {
            return;
        }
        if (string.equals(TYPE_TEXT[0]) || string.equals(TYPE_TEXT[1]) || string.equals(TYPE_TEXT[2]) || string.equals(TYPE_TEXT[3])) {
            this.timeField.setText(string.equals(TYPE_TEXT[0]) ? "0" : "30");
            this.timeField.setVisible(true);
            this.fontField.setVisible(false);
            this.attrDivider.setVisible(false);
            this.showCombo1.setVisible(true);
            this.showCombo2.setVisible(true);
            this.showCombo3.setVisible(true);
            this.showCombo4.setVisible(true);
            this.fileField.setVisible(true);
            this.fileButton.setVisible(true);
            this.fileLabel.setVisible(true);
            this.textArea.setVisible(false);
            this.fileField.setText("");
        } else if (string.equals(TYPE_TEXT[4]) || string.equals(TYPE_TEXT[5]) || string.equals(TYPE_TEXT[6]) || string.equals(TYPE_TEXT[7])) {
            this.timeField.setText("60");
            this.timeField.setVisible(true);
            this.fontField.setVisible(true);
            this.attrDivider.setVisible(true);
            this.showCombo1.setVisible(true);
            this.showCombo2.setVisible(true);
            this.showCombo3.setVisible(true);
            this.showCombo4.setVisible(true);
            this.fileField.setVisible(false);
            this.fileButton.setVisible(false);
            this.fileLabel.setVisible(false);
            this.textArea.setVisible(true);
        } else {
            this.timeField.setVisible(false);
            this.fontField.setVisible(false);
            this.attrDivider.setVisible(false);
            this.showCombo1.setVisible(false);
            this.showCombo2.setVisible(false);
            this.showCombo3.setVisible(false);
            this.showCombo4.setVisible(false);
            this.fileField.setVisible(false);
            this.fileButton.setVisible(false);
            this.fileLabel.setVisible(false);
            this.textArea.setVisible(false);
        }
    }

    public void setDialogStage(Stage stage, MainApp mainApp) {
        this.dialogStage = stage;
        this.mainApp = mainApp;
        stage.setOnCloseRequest((EventHandler)this);
        this.rootView.setOnDragEntered((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                IndividualDialogController.this.dndLabel.setVisible(true);
            }
        });
        this.rootView.setOnDragExited((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                IndividualDialogController.this.dndLabel.setVisible(false);
            }
        });
        this.rootView.setOnDragOver((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                Dragboard dragboard = dragEvent.getDragboard();
                if (dragboard.hasFiles()) {
                    dragEvent.acceptTransferModes(new TransferMode[]{TransferMode.LINK});
                } else {
                    dragEvent.consume();
                }
            }
        });
        this.rootView.setOnDragDropped((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                Dragboard dragboard = dragEvent.getDragboard();
                boolean bl = false;
                if (dragboard.hasFiles()) {
                    bl = true;
                    List list = dragboard.getFiles();
                    Platform.runLater(() -> {
                        for (File file : list) {
                            String string = file.getAbsolutePath();
                            String string2 = string.toLowerCase();
                            if (string2.endsWith(".mp4") || string2.endsWith(".mpg") || string2.endsWith(".mpeg") || string2.endsWith(".avi") || string2.endsWith(".mkv") || string2.endsWith(".wmv") || string2.endsWith(".mov")) {
                                IndividualDialogController.this.typeCombo.getSelectionModel().select(0);
                                IndividualDialogController.this.timeField.setText("0");
                                IndividualDialogController.this.fileField.setText(string);
                                IndividualDialogController.this.fileField.home();
                                IndividualDialogController.this.fileField.end();
                                IndividualDialogController.this.recentDir = file.getParentFile();
                                IndividualDialogController.this.handleAdd(null);
                                continue;
                            }
                            if (string2.endsWith(".jpg") || string2.endsWith(".jpeg") || string2.endsWith("png")) {
                                IndividualDialogController.this.typeCombo.getSelectionModel().select(1);
                                IndividualDialogController.this.timeField.setText("30");
                                IndividualDialogController.this.fileField.setText(string);
                                IndividualDialogController.this.fileField.home();
                                IndividualDialogController.this.fileField.end();
                                IndividualDialogController.this.recentDir = file.getParentFile();
                                IndividualDialogController.this.handleAdd(null);
                                continue;
                            }
                            if (string2.endsWith(".pdf")) {
                                IndividualDialogController.this.typeCombo.getSelectionModel().select(2);
                                IndividualDialogController.this.timeField.setText("30");
                                IndividualDialogController.this.fileField.setText(string);
                                IndividualDialogController.this.fileField.home();
                                IndividualDialogController.this.fileField.end();
                                IndividualDialogController.this.recentDir = file.getParentFile();
                                IndividualDialogController.this.handleAdd(null);
                                continue;
                            }
                            if (!string2.endsWith(".ppt") && !string2.endsWith(".pptx")) continue;
                            IndividualDialogController.this.typeCombo.getSelectionModel().select(3);
                            IndividualDialogController.this.timeField.setText("30");
                            IndividualDialogController.this.fileField.setText(string);
                            IndividualDialogController.this.fileField.home();
                            IndividualDialogController.this.fileField.end();
                            IndividualDialogController.this.recentDir = file.getParentFile();
                            IndividualDialogController.this.handleAdd(null);
                        }
                    });
                }
                dragEvent.setDropCompleted(bl);
                dragEvent.consume();
            }
        });
    }

    public void setUserData(UserData userData) {
        this.user = userData;
    }

    public void setDispyData(DispyData dispyData) {
        this.dispy = dispyData;
    }

    public void setUnionData(ObservableList<DispyData> observableList) {
        this.deviceList.setItems(observableList);
    }

    public void setPlaylistData(PlaylistData playlistData) {
        this.individual = playlistData;
        if (playlistData != null && playlistData.individualItems != null) {
            this.playlist.addAll(playlistData.individualItems);
        }
    }

    public void handle(WindowEvent windowEvent) {
        this.individual = null;
    }

    @FXML
    private void handleFile() {
        this.fileChooser.getExtensionFilters().clear();
        int n = this.typeCombo.getSelectionModel().getSelectedIndex();
        switch (n) {
            case 0: {
                this.fileChooser.getExtensionFilters().add((Object)this.videoFilter);
                break;
            }
            case 1: {
                this.fileChooser.getExtensionFilters().add((Object)this.imageFilter);
                break;
            }
            case 2: {
                this.fileChooser.getExtensionFilters().add((Object)this.pdfFilter);
                break;
            }
            case 3: {
                this.fileChooser.getExtensionFilters().add((Object)this.pptFilter);
            }
        }
        this.fileChooser.setInitialDirectory(this.recentDir);
        File file = this.fileChooser.showOpenDialog((Window)this.dialogStage);
        if (file != null) {
            this.fileField.setText(file.getAbsolutePath());
            this.fileField.home();
            this.fileField.end();
            this.recentDir = file.getParentFile();
        }
    }

    private PlaylistData _makePlaylistData(boolean bl, boolean bl2, boolean bl3) {
        try {
            int n = this.typeCombo.getSelectionModel().getSelectedIndex();
            File file = null;
            String string = null;
            String string2 = null;
            int n2 = 0;
            String string3 = null;
            int n3 = 0;
            int n4 = this.showCombo1.getSelectionModel().getSelectedIndex();
            int n5 = this.showCombo2.getSelectionModel().getSelectedIndex();
            int n6 = this.showCombo3.getSelectionModel().getSelectedIndex();
            int n7 = this.showCombo4.getSelectionModel().getSelectedIndex();
            PlaylistData playlistData = null;
            switch (n) {
                case 0: {
                    file = new File(this.fileField.getText());
                    if (bl && !this._fileSizeCheck(file)) break;
                    string2 = this.timeField.getText();
                    try {
                        n2 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n2 < 0) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (bl2 && !this._dimensionCheck(file) || !this.user.name.startsWith("admin") && this.dispy.mbps > 0 && bl3 && !this._bitrateCheck(file)) break;
                    playlistData = new PlaylistData("video");
                    playlistData.setDataFile(file);
                    playlistData.setAttrInt(n2);
                    playlistData.setShowInt(n4, n5, n6, n7);
                    break;
                }
                case 1: {
                    file = new File(this.fileField.getText());
                    if (bl && !this._fileSizeCheck(file)) break;
                    string2 = this.timeField.getText();
                    try {
                        n2 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n2 <= 0) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (bl2 && !this._dimensionCheck(file)) break;
                    playlistData = new PlaylistData("image");
                    playlistData.setDataFile(file);
                    playlistData.setAttrInt(n2);
                    playlistData.setShowInt(n4, n5, n6, n7);
                    break;
                }
                case 2: {
                    file = new File(this.fileField.getText());
                    if (bl && !this._fileSizeCheck(file)) break;
                    string2 = this.timeField.getText();
                    try {
                        n2 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n2 <= 0) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    playlistData = new PlaylistData("pdf");
                    playlistData.setDataFile(file);
                    playlistData.setAttrInt(n2);
                    playlistData.setShowInt(n4, n5, n6, n7);
                    break;
                }
                case 3: {
                    file = new File(this.fileField.getText());
                    if (bl && !this._fileSizeCheck(file)) break;
                    string2 = this.timeField.getText();
                    try {
                        n2 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n2 <= 0) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    playlistData = new PlaylistData("ppt");
                    playlistData.setDataFile(file);
                    playlistData.setAttrInt(n2);
                    playlistData.setShowInt(n4, n5, n6, n7);
                    break;
                }
                case 4: {
                    string = this.textArea.getText();
                    string2 = this.timeField.getText();
                    string3 = this.fontField.getText();
                    if (string.isEmpty()) break;
                    try {
                        n2 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    try {
                        n3 = Integer.parseInt(string3);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n3 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    if (n2 <= 0) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n3 <= 0) {
                        n3 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    playlistData = new PlaylistData("text1");
                    playlistData.setDataText(string);
                    playlistData.setAttrInt(n2, n3);
                    playlistData.setShowInt(n4, n5, n6, n7);
                    break;
                }
                case 5: {
                    string = this.textArea.getText();
                    string2 = this.timeField.getText();
                    string3 = this.fontField.getText();
                    if (string.isEmpty()) break;
                    try {
                        n2 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    try {
                        n3 = Integer.parseInt(string3);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n3 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    if (n2 <= 0) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n3 <= 0) {
                        n3 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    playlistData = new PlaylistData("text2");
                    playlistData.setDataText(string);
                    playlistData.setAttrInt(n2, n3);
                    playlistData.setShowInt(n4, n5, n6, n7);
                    break;
                }
                case 6: {
                    string = this.textArea.getText();
                    string2 = this.timeField.getText();
                    string3 = this.fontField.getText();
                    if (string.isEmpty()) break;
                    try {
                        n2 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    try {
                        n3 = Integer.parseInt(string3);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n3 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    if (n2 < 30) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n3 <= 0) {
                        n3 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    playlistData = new PlaylistData("web");
                    playlistData.setDataText(string);
                    playlistData.setAttrInt(n2, n3);
                    playlistData.setShowInt(n4, n5, n6, n7);
                    break;
                }
                case 7: {
                    string = this.textArea.getText();
                    string2 = this.timeField.getText();
                    string3 = this.fontField.getText();
                    if (string.isEmpty()) break;
                    try {
                        n2 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    try {
                        n3 = Integer.parseInt(string3);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n3 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    if (n2 < 30) {
                        n2 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n3 <= 0) {
                        n3 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    playlistData = new PlaylistData("style1");
                    playlistData.setDataText(string);
                    playlistData.setAttrInt(n2, n3);
                    playlistData.setShowInt(n4, n5, n6, n7);
                }
            }
            if (playlistData != null) {
                playlistData.individualOwner = (DispyData)this.deviceList.getSelectionModel().getSelectedItem();
                if (playlistData.individualOwner == null) {
                    return null;
                }
            }
            return playlistData;
        }
        catch (Exception exception) {
            Log.out(exception);
            return null;
        }
    }

    private boolean _bitrateCheck(File file) {
        String string = "MediaInfo.exe --output=JSON \"" + file.getAbsolutePath() + "\"";
        Log.out("bitrate check: " + string);
        try {
            String string2;
            Process process = Runtime.getRuntime().exec(new String[]{"MediaInfo.exe", "--output=JSON", file.getAbsolutePath()});
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int n = -1;
            while ((string2 = bufferedReader.readLine()) != null) {
                String string3 = "\"OverallBitRate\":\"";
                if (!string2.startsWith(string3)) continue;
                n = (int)(Long.parseLong(string2.substring(string3.length(), string2.lastIndexOf("\""))) / 1000000L);
                break;
            }
            bufferedReader.close();
            Log.out("bitrate check: " + n + " / " + this.dispy.mbps);
            if (0 <= n && n <= this.dispy.mbps) {
                return true;
            }
            this.mainApp.showBitrateCheckDialog(this.dialogStage, file, n, this.dispy.mbps);
        }
        catch (Exception exception) {
            Log.out(exception);
        }
        return false;
    }

    private boolean _dimensionCheck(File file) {
        String string = "ffmpeg.exe -i \"" + file.getAbsolutePath() + "\"";
        Log.out("dim check: " + string);
        try {
            String string2;
            Process process = Runtime.getRuntime().exec(new String[]{"ffmpeg.exe", "-i", file.getAbsolutePath()});
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            int n = 0;
            int n2 = 0;
            block4: while ((string2 = bufferedReader.readLine()) != null) {
                if (!string2.contains("Stream #") || !string2.contains("Video:")) continue;
                for (String string3 : string2.split(" ")) {
                    String[] stringArray;
                    if (string3.endsWith(",")) {
                        string3 = string3.substring(0, string3.length() - 1);
                    }
                    if ((stringArray = string3.split("x")).length != 2) continue;
                    try {
                        n = Integer.parseInt(stringArray[0]);
                        n2 = Integer.parseInt(stringArray[1]);
                    }
                    catch (Exception exception) {
                        n = 0;
                        n2 = 0;
                        continue;
                    }
                    if (n == 0 || n2 == 0) {
                        n = 0;
                        n2 = 0;
                        continue;
                    }
                    Log.out("dim check: " + string3);
                    break block4;
                }
            }
            bufferedReader.close();
            if (n > 0 && n2 > 0) {
                if (n * n2 < 2073600) {
                    return true;
                }
                if (this.mainApp.showDimensionCheckDialog(this.dialogStage, file, n, n2)) {
                    return true;
                }
            }
        }
        catch (Exception exception) {
            Log.out(exception);
        }
        return false;
    }

    private boolean _fileSizeCheck(File file) {
        return true;
    }

    @FXML
    private void handleAdd(ActionEvent actionEvent) {
        PlaylistData playlistData = this._makePlaylistData(true, false, true);
        if (playlistData != null) {
            switch (playlistData.getTypeCode()) {
                case "video": 
                case "image": 
                case "pdf": 
                case "ppt": {
                    File file = playlistData.getDataFile();
                    if (file != null && file.isFile() && file.length() > 0L) break;
                    return;
                }
            }
            playlistData.setState("new");
            playlistData.changed = true;
            this.playlist.add((Object)playlistData);
            this.tableView.getSelectionModel().select(this.playlist.size() - 1);
            this.fillForm(playlistData);
        }
    }

    @FXML
    private void handleEdit(ActionEvent actionEvent) {
        PlaylistData playlistData;
        File file;
        boolean bl;
        PlaylistData playlistData2;
        int n = this.tableView.getSelectionModel().getSelectedIndex();
        if (n >= 0 && (playlistData2 = this._makePlaylistData(bl = (file = (playlistData = (PlaylistData)this.playlist.get(n)).getDataFile()) != null && file.exists(), false, bl)) != null) {
            playlistData2.setState(playlistData.getStateCode());
            playlistData2.setState("edited");
            if (playlistData2.file != null && !playlistData2.file.equals(((PlaylistData)this.playlist.get((int)n)).file)) {
                playlistData2.changed = true;
            }
            this.playlist.remove(n);
            this.playlist.add(n, (Object)playlistData2);
            this.tableView.getSelectionModel().select(n);
        }
    }

    @FXML
    private void handleUp(ActionEvent actionEvent) {
        int n = this.tableView.getSelectionModel().getSelectedIndex();
        if (n > 0) {
            this.playlist.add(n - 1, this.playlist.remove(n));
            this.tableView.getSelectionModel().select(n - 1);
        }
    }

    @FXML
    private void handleDown(ActionEvent actionEvent) {
        int n = this.tableView.getSelectionModel().getSelectedIndex();
        if (n >= 0 && n < this.playlist.size() - 1) {
            this.playlist.add(n + 1, this.playlist.remove(n));
            this.tableView.getSelectionModel().select(n + 1);
        }
    }

    @FXML
    private void handleRemove(ActionEvent actionEvent) {
        PlaylistData playlistData = (PlaylistData)this.tableView.getSelectionModel().getSelectedItem();
        if (playlistData != null && playlistData.getExpandedData() == null) {
            this.playlist.remove((Object)playlistData);
        }
    }

    @FXML
    private void handleRemoveAll(ActionEvent actionEvent) {
        this.playlist.clear();
    }

    @FXML
    private void handleOk() {
        this.individual.individualItems = new ArrayList<PlaylistData>((Collection<PlaylistData>)this.playlist);
        StringBuilder stringBuilder = new StringBuilder();
        for (PlaylistData playlistData : this.individual.individualItems) {
            if (stringBuilder.length() == 0) {
                stringBuilder.append("(" + this.individual.individualItems.size() + ") " + playlistData.getData());
                continue;
            }
            stringBuilder.append(", " + playlistData.getData());
        }
        if (stringBuilder.length() == 0) {
            stringBuilder.append("(0)");
        }
        this.individual.setData(stringBuilder.toString());
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.individual = null;
        this.dialogStage.close();
    }

    public PlaylistData getPlaylist() {
        return this.individual;
    }
}

