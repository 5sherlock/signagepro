/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.javafx.scene.control.skin.TableViewSkin
 *  com.sun.javafx.scene.control.skin.VirtualFlow
 *  javafx.application.Platform
 *  javafx.beans.binding.Bindings
 *  javafx.beans.value.ChangeListener
 *  javafx.beans.value.ObservableValue
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.event.ActionEvent
 *  javafx.event.EventHandler
 *  javafx.fxml.FXML
 *  javafx.scene.Scene
 *  javafx.scene.canvas.Canvas
 *  javafx.scene.control.Alert
 *  javafx.scene.control.Alert$AlertType
 *  javafx.scene.control.Button
 *  javafx.scene.control.CheckBox
 *  javafx.scene.control.ComboBox
 *  javafx.scene.control.DatePicker
 *  javafx.scene.control.Label
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
 *  javafx.scene.control.TextArea
 *  javafx.scene.control.TextField
 *  javafx.scene.image.Image
 *  javafx.scene.input.DragEvent
 *  javafx.scene.input.Dragboard
 *  javafx.scene.input.MouseEvent
 *  javafx.scene.input.TransferMode
 *  javafx.scene.layout.AnchorPane
 *  javafx.stage.FileChooser
 *  javafx.stage.FileChooser$ExtensionFilter
 *  javafx.stage.Modality
 *  javafx.stage.Stage
 *  javafx.stage.Window
 *  javafx.stage.WindowEvent
 *  javafx.util.StringConverter
 */
package net.ybroad.dispy.manager.view;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import net.ybroad.dispy.lib.Log;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.manager.MainApp;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.model.PlaylistData;
import net.ybroad.dispy.manager.util.CanvasDrawer;
import net.ybroad.dispy.manager.util.Lang;

public class PlaylistDialog2Controller
implements EventHandler<WindowEvent> {
    @FXML
    private AnchorPane rootView;
    @FXML
    private TableView<PlaylistData> tableView0;
    @FXML
    private TableColumn<PlaylistData, String> stateColumn0;
    @FXML
    private TableColumn<PlaylistData, String> typeColumn0;
    @FXML
    private TableColumn<PlaylistData, String> attrColumn0;
    @FXML
    private TableColumn<PlaylistData, String> showColumn0;
    @FXML
    private TableColumn<PlaylistData, String> dataColumn0;
    @FXML
    private TableView<PlaylistData> tableView1;
    @FXML
    private TableColumn<PlaylistData, String> stateColumn1;
    @FXML
    private TableColumn<PlaylistData, String> typeColumn1;
    @FXML
    private TableColumn<PlaylistData, String> attrColumn1;
    @FXML
    private TableColumn<PlaylistData, String> showColumn1;
    @FXML
    private TableColumn<PlaylistData, String> dataColumn1;
    @FXML
    private TableView<PlaylistData> tableView2;
    @FXML
    private TableColumn<PlaylistData, String> stateColumn2;
    @FXML
    private TableColumn<PlaylistData, String> typeColumn2;
    @FXML
    private TableColumn<PlaylistData, String> attrColumn2;
    @FXML
    private TableColumn<PlaylistData, String> showColumn2;
    @FXML
    private TableColumn<PlaylistData, String> dataColumn2;
    @FXML
    private TableView<PlaylistData> tableView3;
    @FXML
    private TableColumn<PlaylistData, String> stateColumn3;
    @FXML
    private TableColumn<PlaylistData, String> typeColumn3;
    @FXML
    private TableColumn<PlaylistData, String> attrColumn3;
    @FXML
    private TableColumn<PlaylistData, String> showColumn3;
    @FXML
    private TableColumn<PlaylistData, String> dataColumn3;
    @FXML
    private Button up0;
    @FXML
    private Button down0;
    @FXML
    private Button remove0;
    @FXML
    private Button removeall0;
    @FXML
    private Button up1;
    @FXML
    private Button down1;
    @FXML
    private Button remove1;
    @FXML
    private Button removeall1;
    @FXML
    private Button up2;
    @FXML
    private Button down2;
    @FXML
    private Button remove2;
    @FXML
    private Button removeall2;
    @FXML
    private Button up3;
    @FXML
    private Button down3;
    @FXML
    private Button remove3;
    @FXML
    private Button removeall3;
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
    private DatePicker openDate;
    @FXML
    private ComboBox<String> openTime;
    @FXML
    private ComboBox<String> openMin;
    @FXML
    private Button openButton;
    @FXML
    private DatePicker tillDate;
    @FXML
    private ComboBox<String> tillTime;
    @FXML
    private ComboBox<String> tillMin;
    @FXML
    private Button tillButton;
    @FXML
    private TextField fileField;
    @FXML
    private Button fileButton;
    @FXML
    private Label fileLabel;
    @FXML
    private TextArea textArea;
    @FXML
    private Canvas canvas;
    @FXML
    private CheckBox check0;
    @FXML
    private CheckBox check1;
    @FXML
    private CheckBox check2;
    @FXML
    private CheckBox check3;
    @FXML
    private Label reserveLabel;
    @FXML
    private DatePicker reserveDate;
    @FXML
    private ComboBox<String> reserveTime;
    @FXML
    private Label reserveColon;
    @FXML
    private ComboBox<String> reserveMin;
    @FXML
    private Label fromLabel;
    @FXML
    private ComboBox<String> fromCombo;
    @FXML
    private Label play0Label;
    @FXML
    private Label play1Label;
    @FXML
    private Label play2Label;
    @FXML
    private Label play3Label;
    @FXML
    private Label dndLabel;
    private Stage dialogStage;
    private MainApp mainApp;
    private UserData user;
    private DispyData dispy;
    private ArrayList<ObservableList<PlaylistData>> playlist = new ArrayList();
    private ArrayList<TableView<PlaylistData>> tableView = new ArrayList();
    private ArrayList<CheckBox> check = new ArrayList();
    private ObservableList<PlaylistData> playlist0 = FXCollections.observableArrayList();
    private ObservableList<PlaylistData> playlist1 = FXCollections.observableArrayList();
    private ObservableList<PlaylistData> playlist2 = FXCollections.observableArrayList();
    private ObservableList<PlaylistData> playlist3 = FXCollections.observableArrayList();
    private static final String[] TYPE_TEXT = new String[]{Lang.getString("play.type.video"), Lang.getString("play.type.image"), Lang.getString("play.type.pdf"), Lang.getString("play.type.ppt"), Lang.getString("play.type.text1"), Lang.getString("play.type.text2"), Lang.getString("play.type.web"), Lang.getString("play.type.style1"), Lang.getString("play.type.sync1"), Lang.getString("play.type.sync2"), Lang.getString("play.type.sync3"), Lang.getString("play.type.sync.n", "N"), Lang.getString("play.type.synclan1"), Lang.getString("play.type.synclan2"), Lang.getString("play.type.synclan3"), Lang.getString("play.type.synclan.n", "N"), Lang.getString("play.type.individual")};
    private static final int TYPE_VIDEO = 0;
    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_PDF = 2;
    private static final int TYPE_PPT = 3;
    private static final int TYPE_TEXT1 = 4;
    private static final int TYPE_TEXT2 = 5;
    private static final int TYPE_WEB = 6;
    private static final int TYPE_STYLE1 = 7;
    private static final int TYPE_SYNC1 = 8;
    private static final int TYPE_SYNC2 = 9;
    private static final int TYPE_SYNC3 = 10;
    private static final int TYPE_SYNC_N = 11;
    private static final int TYPE_SYNC_LAN1 = 12;
    private static final int TYPE_SYNC_LAN2 = 13;
    private static final int TYPE_SYNC_LAN3 = 14;
    private static final int TYPE_SYNC_LAN_N = 15;
    private static final int TYPE_INDIVISUAL = 16;
    private static final String[] DAY_OF_WEEK = new String[]{Lang.getString("week.mon"), Lang.getString("week.tue"), Lang.getString("week.wed"), Lang.getString("week.thu"), Lang.getString("week.fri"), Lang.getString("week.sat"), Lang.getString("week.sun")};
    private static final String[] HOUR_OF_DAY_START = new String[]{"00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"};
    private static final String[] HOUR_OF_DAY_END = new String[]{"00:59", "01:59", "02:59", "03:59", "04:59", "05:59", "06:59", "07:59", "08:59", "09:59", "10:59", "11:59", "12:59", "13:59", "14:59", "15:59", "16:59", "17:59", "18:59", "19:59", "20:59", "21:59", "22:59", "23:59"};
    private static final String[] HOUR_OF_DAY = new String[]{"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"};
    private static final String[] MINUTE_OF_HOUR = new String[]{"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59"};
    private static final StringConverter<LocalDate> DATE_FORMATTER = new StringConverter<LocalDate>(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        public String toString(LocalDate localDate) {
            if (localDate != null) {
                return this.dateFormatter.format(localDate);
            }
            return "";
        }

        public LocalDate fromString(String string) {
            if (string != null && !string.isEmpty()) {
                return LocalDate.parse(string, this.dateFormatter);
            }
            return null;
        }
    };
    private FileChooser fileChooser = new FileChooser();
    private FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("Video (*.mp4, *.mpg, *.mpeg, *.avi, *.mkv, *.wmv, *.mov)", new String[]{"*.mp4", "*.mpg", "*.mpeg", "*.avi", "*.mkv", "*.wmv", "*.mov", "*.MP4", "*.MPG", "*.MPEG", "*.AVI", "*.MKV", "*.WMV", "*.MOV"});
    private FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("Image (*.jpg, *.png)", new String[]{"*.jpg", "*.jepg", "*.png", "*.JPG", "*.JPEG", "*.PNG"});
    private FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF (*.pdf)", new String[]{"*.pdf", "*.PDF"});
    private FileChooser.ExtensionFilter pptFilter = new FileChooser.ExtensionFilter("PPT (*.ppt, *.pptx)", new String[]{"*.ppt", "*.pptx", "*.PPT", "*.PPTX"});
    private File recentDir = null;

    @FXML
    private void initialize() {
        this.stateColumn0.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).stateProperty());
        this.typeColumn0.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).typeProperty());
        this.attrColumn0.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).attrProperty());
        this.showColumn0.setCellValueFactory(cellDataFeatures -> Bindings.concat((Object[])new Object[]{((PlaylistData)cellDataFeatures.getValue()).showProperty(), ((PlaylistData)cellDataFeatures.getValue()).openProperty(), ((PlaylistData)cellDataFeatures.getValue()).tillProperty()}));
        this.dataColumn0.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).dataProperty());
        this.stateColumn1.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).stateProperty());
        this.typeColumn1.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).typeProperty());
        this.attrColumn1.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).attrProperty());
        this.showColumn1.setCellValueFactory(cellDataFeatures -> Bindings.concat((Object[])new Object[]{((PlaylistData)cellDataFeatures.getValue()).showProperty(), ((PlaylistData)cellDataFeatures.getValue()).openProperty(), ((PlaylistData)cellDataFeatures.getValue()).tillProperty()}));
        this.dataColumn1.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).dataProperty());
        this.stateColumn2.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).stateProperty());
        this.typeColumn2.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).typeProperty());
        this.attrColumn2.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).attrProperty());
        this.showColumn2.setCellValueFactory(cellDataFeatures -> Bindings.concat((Object[])new Object[]{((PlaylistData)cellDataFeatures.getValue()).showProperty(), ((PlaylistData)cellDataFeatures.getValue()).openProperty(), ((PlaylistData)cellDataFeatures.getValue()).tillProperty()}));
        this.dataColumn2.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).dataProperty());
        this.stateColumn3.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).stateProperty());
        this.typeColumn3.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).typeProperty());
        this.attrColumn3.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).attrProperty());
        this.showColumn3.setCellValueFactory(cellDataFeatures -> Bindings.concat((Object[])new Object[]{((PlaylistData)cellDataFeatures.getValue()).showProperty(), ((PlaylistData)cellDataFeatures.getValue()).openProperty(), ((PlaylistData)cellDataFeatures.getValue()).tillProperty()}));
        this.dataColumn3.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).dataProperty());
        this.tableView0.setItems(this.playlist0);
        this.tableView1.setItems(this.playlist1);
        this.tableView2.setItems(this.playlist2);
        this.tableView3.setItems(this.playlist3);
        this.tableView0.getSelectionModel().selectedItemProperty().addListener((ChangeListener)new ChangeListener<PlaylistData>(){

            public void changed(ObservableValue<? extends PlaylistData> observableValue, PlaylistData playlistData, PlaylistData playlistData2) {
                if (playlistData != null && playlistData2 != null) {
                    PlaylistDialog2Controller.this.fillForm(playlistData2);
                    try {
                        VirtualFlow virtualFlow = (VirtualFlow)((TableViewSkin)PlaylistDialog2Controller.this.tableView0.getSkin()).getChildren().get(1);
                        int n = virtualFlow.getFirstVisibleCell().getIndex();
                        int n2 = virtualFlow.getLastVisibleCell().getIndex();
                        int n3 = PlaylistDialog2Controller.this.tableView0.getItems().indexOf((Object)playlistData2);
                        if (n3 < n || n2 < n3) {
                            PlaylistDialog2Controller.this.tableView0.scrollTo((Object)playlistData2);
                        }
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                    }
                }
            }
        });
        this.tableView1.getSelectionModel().selectedItemProperty().addListener((ChangeListener)new ChangeListener<PlaylistData>(){

            public void changed(ObservableValue<? extends PlaylistData> observableValue, PlaylistData playlistData, PlaylistData playlistData2) {
                if (playlistData != null && playlistData2 != null) {
                    PlaylistDialog2Controller.this.fillForm(playlistData2);
                    try {
                        VirtualFlow virtualFlow = (VirtualFlow)((TableViewSkin)PlaylistDialog2Controller.this.tableView1.getSkin()).getChildren().get(1);
                        int n = virtualFlow.getFirstVisibleCell().getIndex();
                        int n2 = virtualFlow.getLastVisibleCell().getIndex();
                        int n3 = PlaylistDialog2Controller.this.tableView1.getItems().indexOf((Object)playlistData2);
                        if (n3 < n || n2 < n3) {
                            PlaylistDialog2Controller.this.tableView1.scrollTo((Object)playlistData2);
                        }
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                    }
                }
            }
        });
        this.tableView2.getSelectionModel().selectedItemProperty().addListener((ChangeListener)new ChangeListener<PlaylistData>(){

            public void changed(ObservableValue<? extends PlaylistData> observableValue, PlaylistData playlistData, PlaylistData playlistData2) {
                if (playlistData != null && playlistData2 != null) {
                    PlaylistDialog2Controller.this.fillForm(playlistData2);
                    try {
                        VirtualFlow virtualFlow = (VirtualFlow)((TableViewSkin)PlaylistDialog2Controller.this.tableView2.getSkin()).getChildren().get(1);
                        int n = virtualFlow.getFirstVisibleCell().getIndex();
                        int n2 = virtualFlow.getLastVisibleCell().getIndex();
                        int n3 = PlaylistDialog2Controller.this.tableView2.getItems().indexOf((Object)playlistData2);
                        if (n3 < n || n2 < n3) {
                            PlaylistDialog2Controller.this.tableView2.scrollTo((Object)playlistData2);
                        }
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                    }
                }
            }
        });
        this.tableView3.getSelectionModel().selectedItemProperty().addListener((ChangeListener)new ChangeListener<PlaylistData>(){

            public void changed(ObservableValue<? extends PlaylistData> observableValue, PlaylistData playlistData, PlaylistData playlistData2) {
                if (playlistData != null && playlistData2 != null) {
                    PlaylistDialog2Controller.this.fillForm(playlistData2);
                    try {
                        VirtualFlow virtualFlow = (VirtualFlow)((TableViewSkin)PlaylistDialog2Controller.this.tableView3.getSkin()).getChildren().get(1);
                        int n = virtualFlow.getFirstVisibleCell().getIndex();
                        int n2 = virtualFlow.getLastVisibleCell().getIndex();
                        int n3 = PlaylistDialog2Controller.this.tableView3.getItems().indexOf((Object)playlistData2);
                        if (n3 < n || n2 < n3) {
                            PlaylistDialog2Controller.this.tableView3.scrollTo((Object)playlistData2);
                        }
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                    }
                }
            }
        });
        this.tableView0.focusedProperty().addListener((observableValue, bl, bl2) -> {
            if (bl2.booleanValue()) {
                PlaylistData playlistData = (PlaylistData)this.tableView0.getSelectionModel().getSelectedItem();
                if (playlistData == null && !this.tableView0.getItems().isEmpty()) {
                    playlistData = (PlaylistData)this.tableView0.getItems().get(0);
                    this.tableView0.getSelectionModel().select(0);
                }
                this.fillForm(playlistData);
                this.check0.setSelected(true);
                this.handleCheck(new ActionEvent((Object)this.check0, null));
            }
        });
        this.tableView1.focusedProperty().addListener((observableValue, bl, bl2) -> {
            if (bl2.booleanValue()) {
                PlaylistData playlistData = (PlaylistData)this.tableView1.getSelectionModel().getSelectedItem();
                if (playlistData == null && !this.tableView1.getItems().isEmpty()) {
                    playlistData = (PlaylistData)this.tableView1.getItems().get(0);
                    this.tableView1.getSelectionModel().select(0);
                }
                this.fillForm(playlistData);
                this.check1.setSelected(true);
                this.handleCheck(new ActionEvent((Object)this.check1, null));
            }
        });
        this.tableView2.focusedProperty().addListener((observableValue, bl, bl2) -> {
            if (bl2.booleanValue()) {
                PlaylistData playlistData = (PlaylistData)this.tableView2.getSelectionModel().getSelectedItem();
                if (playlistData == null && !this.tableView2.getItems().isEmpty()) {
                    playlistData = (PlaylistData)this.tableView2.getItems().get(0);
                    this.tableView2.getSelectionModel().select(0);
                }
                this.fillForm(playlistData);
                this.check2.setSelected(true);
                this.handleCheck(new ActionEvent((Object)this.check2, null));
            }
        });
        this.tableView3.focusedProperty().addListener((observableValue, bl, bl2) -> {
            if (bl2.booleanValue()) {
                PlaylistData playlistData = (PlaylistData)this.tableView3.getSelectionModel().getSelectedItem();
                if (playlistData == null && !this.tableView3.getItems().isEmpty()) {
                    playlistData = (PlaylistData)this.tableView3.getItems().get(0);
                    this.tableView3.getSelectionModel().select(0);
                }
                this.fillForm(playlistData);
                this.check3.setSelected(true);
                this.handleCheck(new ActionEvent((Object)this.check3, null));
            }
        });
        this.typeCombo.getItems().addAll((Object[])TYPE_TEXT);
        this.typeCombo.getSelectionModel().selectedItemProperty().addListener((observableValue, string, string2) -> this.typeChanged((String)string2));
        this.typeCombo.getSelectionModel().select(0);
        this.showCombo1.getItems().addAll((Object[])DAY_OF_WEEK);
        this.showCombo2.getItems().addAll((Object[])DAY_OF_WEEK);
        this.showCombo3.getItems().addAll((Object[])HOUR_OF_DAY_START);
        this.showCombo4.getItems().addAll((Object[])HOUR_OF_DAY_END);
        this.openDate.setConverter(DATE_FORMATTER);
        this.openTime.getItems().addAll((Object[])HOUR_OF_DAY);
        this.openMin.getItems().addAll((Object[])MINUTE_OF_HOUR);
        this.tillDate.setConverter(DATE_FORMATTER);
        this.tillTime.getItems().addAll((Object[])HOUR_OF_DAY);
        this.tillMin.getItems().addAll((Object[])MINUTE_OF_HOUR);
        this.reserveTime.getItems().addAll((Object[])HOUR_OF_DAY);
        this.reserveMin.getItems().addAll((Object[])MINUTE_OF_HOUR);
        this.reserveDate.setConverter(DATE_FORMATTER);
        this.fromCombo.getSelectionModel().selectedItemProperty().addListener((observableValue, string, string2) -> this.fromChanged());
        this.playlist.add(this.playlist0);
        this.playlist.add(this.playlist1);
        this.playlist.add(this.playlist2);
        this.playlist.add(this.playlist3);
        this.tableView.add(this.tableView0);
        this.tableView.add(this.tableView1);
        this.tableView.add(this.tableView2);
        this.tableView.add(this.tableView3);
        this.check.add(this.check0);
        this.check.add(this.check1);
        this.check.add(this.check2);
        this.check.add(this.check3);
    }

    private void fillForm(PlaylistData playlistData) {
        if (playlistData == null) {
            return;
        }
        String string = playlistData.getTypeCode();
        if (string.startsWith("synclan")) {
            this.typeCombo.getSelectionModel().select((Object)Lang.getString("play.type.synclan.n", "N"));
        } else if (string.startsWith("sync")) {
            this.typeCombo.getSelectionModel().select((Object)Lang.getString("play.type.sync.n", "N"));
        } else {
            this.typeCombo.getSelectionModel().select((Object)playlistData.getType());
        }
        long l = playlistData.getOpen();
        long l2 = playlistData.getTill();
        switch (string) {
            case "video": 
            case "image": 
            case "pdf": 
            case "ppt": {
                LocalDateTime localDateTime;
                GregorianCalendar gregorianCalendar;
                this.timeField.setText(String.valueOf(playlistData.getAttrTime()));
                this.showCombo1.getSelectionModel().select(playlistData.getShowWeekStart());
                this.showCombo2.getSelectionModel().select(playlistData.getShowWeekEnd());
                this.showCombo3.getSelectionModel().select(playlistData.getShowStart());
                this.showCombo4.getSelectionModel().select(playlistData.getShowEnd());
                if (l > 0L) {
                    gregorianCalendar = new GregorianCalendar();
                    gregorianCalendar.setTimeInMillis(l);
                    localDateTime = LocalDateTime.ofInstant(gregorianCalendar.toInstant(), ZoneId.systemDefault());
                    this.openDate.setValue((Object)localDateTime.toLocalDate());
                    this.openTime.getSelectionModel().select(localDateTime.getHour());
                    this.openMin.getSelectionModel().select(localDateTime.getMinute());
                } else {
                    this.openDate.setValue(null);
                    this.openTime.getSelectionModel().clearSelection();
                    this.openMin.getSelectionModel().clearSelection();
                }
                if (l2 > 0L) {
                    gregorianCalendar = new GregorianCalendar();
                    gregorianCalendar.setTimeInMillis(l2);
                    localDateTime = LocalDateTime.ofInstant(gregorianCalendar.toInstant(), ZoneId.systemDefault());
                    this.tillDate.setValue((Object)localDateTime.toLocalDate());
                    this.tillTime.getSelectionModel().select(localDateTime.getHour());
                    this.tillMin.getSelectionModel().select(localDateTime.getMinute());
                } else {
                    this.tillDate.setValue(null);
                    this.tillTime.getSelectionModel().clearSelection();
                    this.tillMin.getSelectionModel().clearSelection();
                }
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
                LocalDateTime localDateTime;
                GregorianCalendar gregorianCalendar;
                this.timeField.setText(String.valueOf(playlistData.getAttrTime()));
                this.fontField.setText(String.valueOf(playlistData.getAttrFont()));
                this.showCombo1.getSelectionModel().select(playlistData.getShowWeekStart());
                this.showCombo2.getSelectionModel().select(playlistData.getShowWeekEnd());
                this.showCombo3.getSelectionModel().select(playlistData.getShowStart());
                this.showCombo4.getSelectionModel().select(playlistData.getShowEnd());
                if (l > 0L) {
                    gregorianCalendar = new GregorianCalendar();
                    gregorianCalendar.setTimeInMillis(l);
                    localDateTime = LocalDateTime.ofInstant(gregorianCalendar.toInstant(), ZoneId.systemDefault());
                    this.openDate.setValue((Object)localDateTime.toLocalDate());
                    this.openTime.getSelectionModel().select(localDateTime.getHour());
                    this.openMin.getSelectionModel().select(localDateTime.getMinute());
                } else {
                    this.openDate.setValue(null);
                    this.openTime.getSelectionModel().clearSelection();
                    this.openMin.getSelectionModel().clearSelection();
                }
                if (l2 > 0L) {
                    gregorianCalendar = new GregorianCalendar();
                    gregorianCalendar.setTimeInMillis(l2);
                    localDateTime = LocalDateTime.ofInstant(gregorianCalendar.toInstant(), ZoneId.systemDefault());
                    this.tillDate.setValue((Object)localDateTime.toLocalDate());
                    this.tillTime.getSelectionModel().select(localDateTime.getHour());
                    this.tillMin.getSelectionModel().select(localDateTime.getMinute());
                } else {
                    this.tillDate.setValue(null);
                    this.tillTime.getSelectionModel().clearSelection();
                    this.tillMin.getSelectionModel().clearSelection();
                }
                this.textArea.setText(playlistData.getDataText().replace("\\n", "\n"));
                break;
            }
            case "individual": {
                this.timeField.setText("");
                this.fontField.setText("");
                this.showCombo1.getSelectionModel().select(-1);
                this.showCombo2.getSelectionModel().select(-1);
                this.showCombo3.getSelectionModel().select(-1);
                this.showCombo4.getSelectionModel().select(-1);
                this.openDate.setValue(null);
                this.openTime.getSelectionModel().clearSelection();
                this.openMin.getSelectionModel().clearSelection();
                this.tillDate.setValue(null);
                this.tillTime.getSelectionModel().clearSelection();
                this.tillMin.getSelectionModel().clearSelection();
                this.textArea.setText("");
                break;
            }
            default: {
                String string2 = "";
                if (string.startsWith("synclan")) {
                    string2 = string.substring("synclan".length());
                } else if (string.startsWith("sync")) {
                    string2 = string.substring("sync".length());
                }
                this.textArea.setText(string2);
            }
        }
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
            this.openDate.setVisible(true);
            this.openTime.setVisible(true);
            this.openMin.setVisible(true);
            this.openButton.setVisible(true);
            this.tillDate.setVisible(true);
            this.tillTime.setVisible(true);
            this.tillMin.setVisible(true);
            this.tillButton.setVisible(true);
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
            this.openDate.setVisible(true);
            this.openTime.setVisible(true);
            this.openMin.setVisible(true);
            this.openButton.setVisible(true);
            this.tillDate.setVisible(true);
            this.tillTime.setVisible(true);
            this.tillMin.setVisible(true);
            this.tillButton.setVisible(true);
            this.fileField.setVisible(false);
            this.fileButton.setVisible(false);
            this.fileLabel.setVisible(false);
            this.textArea.setVisible(true);
        } else if (string.equals(TYPE_TEXT[11]) || string.equals(TYPE_TEXT[15])) {
            this.timeField.setVisible(false);
            this.fontField.setVisible(false);
            this.attrDivider.setVisible(false);
            this.showCombo1.setVisible(false);
            this.showCombo2.setVisible(false);
            this.showCombo3.setVisible(false);
            this.showCombo4.setVisible(false);
            this.openDate.setVisible(false);
            this.openTime.setVisible(false);
            this.openMin.setVisible(false);
            this.openButton.setVisible(false);
            this.tillDate.setVisible(false);
            this.tillTime.setVisible(false);
            this.tillMin.setVisible(false);
            this.tillButton.setVisible(false);
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
            this.openDate.setVisible(false);
            this.openTime.setVisible(false);
            this.openMin.setVisible(false);
            this.openButton.setVisible(false);
            this.tillDate.setVisible(false);
            this.tillTime.setVisible(false);
            this.tillMin.setVisible(false);
            this.tillButton.setVisible(false);
            this.fileField.setVisible(false);
            this.fileButton.setVisible(false);
            this.fileLabel.setVisible(false);
            this.textArea.setVisible(false);
        }
    }

    private void fromChanged() {
        int n = this.fromCombo.getSelectionModel().getSelectedIndex();
        this.fromChanged(n);
        Platform.runLater((Runnable)new Runnable(){

            @Override
            public void run() {
                PlaylistDialog2Controller.this.fromCombo.getSelectionModel().clearSelection();
            }
        });
    }

    private void fromChanged(int n) {
        block3: {
            block2: {
                if (n != 0) break block2;
                for (int i = 0; i < 4; ++i) {
                    this.setPlaylist(i, this.dispy.getPlaylist(i));
                }
                break block3;
            }
            if (n <= 0) break block3;
            for (int i = 0; i < 4; ++i) {
                this.setPlaylist(i, this.dispy.getReservelist(i, n - 1));
            }
        }
    }

    private void setPlaylist(int n, List<PlaylistData> list) {
        for (int i = list.size() - 1; i >= 0; --i) {
            PlaylistData playlistData = list.get(i);
            if (!Objects.equals(playlistData.getTypeCode(), "individual")) continue;
            playlistData.individualItems = new ArrayList();
            for (String string : playlistData.text.split("_")) {
                PlaylistData playlistData2 = list.remove(i + 1);
                playlistData2.individualOwner = this.mainApp.getDispyData(string);
                playlistData.individualItems.add(playlistData2);
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (PlaylistData playlistData3 : playlistData.individualItems) {
                if (stringBuilder.length() == 0) {
                    stringBuilder.append("(" + playlistData.individualItems.size() + ") " + playlistData3.getData());
                    continue;
                }
                stringBuilder.append(", " + playlistData3.getData());
            }
            if (stringBuilder.length() == 0) {
                stringBuilder.append("(0)");
            }
            playlistData.setData(stringBuilder.toString());
        }
        this.playlist.get(n).clear();
        this.playlist.get(n).addAll(list);
    }

    private void setReserveTime(long l) {
        if (l > 0L) {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTimeInMillis(l);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(gregorianCalendar.toInstant(), ZoneId.systemDefault());
            this.reserveDate.setValue((Object)localDateTime.toLocalDate());
            this.reserveTime.getSelectionModel().select(localDateTime.getHour());
            this.reserveMin.getSelectionModel().select(localDateTime.getMinute());
        } else {
            this.reserveDate.setValue(null);
            this.reserveTime.getSelectionModel().clearSelection();
            this.reserveMin.getSelectionModel().clearSelection();
        }
    }

    public long getReserveTime() {
        LocalDate localDate = (LocalDate)this.reserveDate.getValue();
        if (localDate != null) {
            long l;
            long l2;
            int n;
            int n2 = this.reserveTime.getSelectionModel().getSelectedIndex();
            if (n2 < 0) {
                n2 = 0;
            }
            if ((n = this.reserveMin.getSelectionModel().getSelectedIndex()) < 0) {
                n = 0;
            }
            if ((l2 = (l = localDate.atTime(n2, n, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L) - System.currentTimeMillis()) > 300000L) {
                return l;
            }
            return 0L;
        }
        return 0L;
    }

    public void setDialogStage(Stage stage, MainApp mainApp) {
        this.dialogStage = stage;
        this.mainApp = mainApp;
        stage.setOnCloseRequest((EventHandler)this);
        this.rootView.setOnDragEntered((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                PlaylistDialog2Controller.this.dndLabel.setVisible(true);
            }
        });
        this.rootView.setOnDragExited((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                PlaylistDialog2Controller.this.dndLabel.setVisible(false);
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
                                PlaylistDialog2Controller.this.typeCombo.getSelectionModel().select(0);
                                PlaylistDialog2Controller.this.timeField.setText("0");
                                PlaylistDialog2Controller.this.fileField.setText(string);
                                PlaylistDialog2Controller.this.fileField.home();
                                PlaylistDialog2Controller.this.fileField.end();
                                PlaylistDialog2Controller.this.recentDir = file.getParentFile();
                                PlaylistDialog2Controller.this.handleAdd(null);
                                continue;
                            }
                            if (string2.endsWith(".jpg") || string2.endsWith(".jpeg") || string2.endsWith("png")) {
                                PlaylistDialog2Controller.this.typeCombo.getSelectionModel().select(1);
                                PlaylistDialog2Controller.this.timeField.setText("30");
                                PlaylistDialog2Controller.this.fileField.setText(string);
                                PlaylistDialog2Controller.this.fileField.home();
                                PlaylistDialog2Controller.this.fileField.end();
                                PlaylistDialog2Controller.this.recentDir = file.getParentFile();
                                PlaylistDialog2Controller.this.handleAdd(null);
                                continue;
                            }
                            if (string2.endsWith(".pdf")) {
                                PlaylistDialog2Controller.this.typeCombo.getSelectionModel().select(2);
                                PlaylistDialog2Controller.this.timeField.setText("30");
                                PlaylistDialog2Controller.this.fileField.setText(string);
                                PlaylistDialog2Controller.this.fileField.home();
                                PlaylistDialog2Controller.this.fileField.end();
                                PlaylistDialog2Controller.this.recentDir = file.getParentFile();
                                PlaylistDialog2Controller.this.handleAdd(null);
                                continue;
                            }
                            if (!string2.endsWith(".ppt") && !string2.endsWith(".pptx")) continue;
                            PlaylistDialog2Controller.this.typeCombo.getSelectionModel().select(3);
                            PlaylistDialog2Controller.this.timeField.setText("30");
                            PlaylistDialog2Controller.this.fileField.setText(string);
                            PlaylistDialog2Controller.this.fileField.home();
                            PlaylistDialog2Controller.this.fileField.end();
                            PlaylistDialog2Controller.this.recentDir = file.getParentFile();
                            PlaylistDialog2Controller.this.handleAdd(null);
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
        boolean bl = userData.name.equals("admin");
        this.play0Label.setVisible(bl);
        this.play1Label.setVisible(bl);
        this.play2Label.setVisible(bl);
        this.play3Label.setVisible(bl);
    }

    public void setDispyData(DispyData dispyData) {
        this.dispy = dispyData;
        if (dispyData.getVersionInt() < 80 || !dispyData.getId().startsWith("u")) {
            this.typeCombo.getItems().remove(16);
        }
        if (dispyData.getVersionInt() >= 86 || dispyData.getId().startsWith("w") && dispyData.getVersionInt() >= 53) {
            this.openDate.setDisable(false);
            this.openTime.setDisable(false);
            this.openMin.setDisable(false);
            this.openButton.setDisable(false);
        } else {
            this.openDate.setDisable(true);
            this.openTime.setDisable(true);
            this.openMin.setDisable(true);
            this.openButton.setDisable(true);
        }
        if (dispyData.getVersionInt() >= 82 || dispyData.getId().startsWith("w") && dispyData.getVersionInt() >= 53) {
            this.tillDate.setDisable(false);
            this.tillTime.setDisable(false);
            this.tillMin.setDisable(false);
            this.tillButton.setDisable(false);
        } else {
            this.tillDate.setDisable(true);
            this.tillTime.setDisable(true);
            this.tillMin.setDisable(true);
            this.tillButton.setDisable(true);
        }
        CanvasDrawer.drawCanvas(this.canvas, dispyData.getTypeInt(), dispyData.getCellInt(), dispyData.getWidth(), dispyData.getHeight());
    }

    public void setPlaylist(int n, Image image) {
        if (n < 0) {
            this.reserveLabel.setVisible(false);
            this.reserveDate.setVisible(false);
            this.reserveTime.setVisible(false);
            this.reserveColon.setVisible(false);
            this.reserveMin.setVisible(false);
            this.fromLabel.setVisible(false);
            this.fromCombo.setVisible(false);
            for (int i = 0; i < 4; ++i) {
                this.setPlaylist(i, this.dispy.getPlaylist(i));
            }
            this.play0Label.setText(this.dispy.play[0]);
            this.play1Label.setText(this.dispy.play[1]);
            this.play2Label.setText(this.dispy.play[2]);
            this.play3Label.setText(this.dispy.play[3]);
        } else {
            Scene scene;
            int n2;
            this.reserveLabel.setVisible(true);
            this.reserveDate.setVisible(true);
            this.reserveTime.setVisible(true);
            this.reserveColon.setVisible(true);
            this.reserveMin.setVisible(true);
            for (n2 = 0; n2 < 4; ++n2) {
                long l = this.dispy.getReserveTime(n2, n);
                if (l <= 0L) continue;
                this.setReserveTime(l);
                break;
            }
            this.fromCombo.getItems().add((Object)Lang.getString("reserve.from.play"));
            for (n2 = 0; n2 < 4; ++n2) {
                this.fromCombo.getItems().add((Object)Lang.getString("reserve.from.reserve", n2 + 1, this.dispy.getReserve(n2)));
            }
            n2 = 1;
            for (int i = 0; i < 4; ++i) {
                scene = this.dispy.getReservelist(i, n);
                this.setPlaylist(i, (List<PlaylistData>)scene);
                n2 &= scene.isEmpty();
            }
            if (n2 != 0) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setTitle(this.dialogStage.getTitle());
                if (n == 0) {
                    alert.setHeaderText(Lang.getString("reserve.from.message", Lang.getString("reserve.from.play")));
                } else {
                    alert.setHeaderText(Lang.getString("reserve.from.message", Lang.getString("reserve.from.reserve", n, "")));
                }
                scene = alert.getDialogPane().getScene();
                ((Stage)scene.getWindow()).getIcons().add((Object)image);
                Lang.setStyleSheet(scene);
                alert.showAndWait();
                if (n == 0) {
                    for (int i = 0; i < 4; ++i) {
                        this.setPlaylist(i, this.dispy.getPlaylist(i));
                    }
                } else {
                    for (int i = 0; i < 4; ++i) {
                        this.setPlaylist(i, this.dispy.getReservelist(i, n - 1));
                    }
                }
            }
            this.play0Label.setText(this.dispy.reserve[0][n]);
            this.play1Label.setText(this.dispy.reserve[1][n]);
            this.play2Label.setText(this.dispy.reserve[2][n]);
            this.play3Label.setText(this.dispy.reserve[3][n]);
        }
    }

    public ArrayList<ObservableList<PlaylistData>> getPlaylist() {
        if (this.playlist != null) {
            for (ObservableList<PlaylistData> observableList : this.playlist) {
                for (int i = observableList.size() - 1; i >= 0; --i) {
                    PlaylistData playlistData = (PlaylistData)observableList.get(i);
                    if (!Objects.equals(playlistData.getTypeCode(), "individual")) continue;
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int j = 0; j < playlistData.individualItems.size(); ++j) {
                        PlaylistData playlistData2 = playlistData.individualItems.get(j);
                        observableList.add(i + 1 + j, (Object)playlistData2);
                        if (stringBuilder.length() > 0) {
                            stringBuilder.append("_");
                        }
                        stringBuilder.append(playlistData2.individualOwner.getId());
                    }
                    playlistData.setDataText(stringBuilder.toString());
                }
            }
        }
        return this.playlist;
    }

    @FXML
    private void handleOpenDelete() {
        this.openDate.setValue(null);
        this.openTime.getSelectionModel().clearSelection();
        this.openMin.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleTillDelete() {
        this.tillDate.setValue(null);
        this.tillTime.getSelectionModel().clearSelection();
        this.tillMin.getSelectionModel().clearSelection();
    }

    private PlaylistData _makePlaylistData(boolean bl, boolean bl2, boolean bl3) {
        try {
            int n;
            int n2;
            int n3 = this.typeCombo.getSelectionModel().getSelectedIndex();
            File file = null;
            String string = null;
            int n4 = 0;
            String string2 = null;
            int n5 = 0;
            String string3 = null;
            int n6 = 0;
            int n7 = this.showCombo1.getSelectionModel().getSelectedIndex();
            int n8 = this.showCombo2.getSelectionModel().getSelectedIndex();
            int n9 = this.showCombo3.getSelectionModel().getSelectedIndex();
            int n10 = this.showCombo4.getSelectionModel().getSelectedIndex();
            long l = 0L;
            long l2 = 0L;
            LocalDate localDate = (LocalDate)this.openDate.getValue();
            if (localDate != null) {
                n2 = this.openTime.getSelectionModel().getSelectedIndex();
                if (n2 < 0) {
                    n2 = 0;
                }
                if ((n = this.openMin.getSelectionModel().getSelectedIndex()) < 0) {
                    n = 0;
                }
                l = localDate.atTime(n2, n, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
            }
            if ((localDate = (LocalDate)this.tillDate.getValue()) != null) {
                n2 = this.tillTime.getSelectionModel().getSelectedIndex();
                if (n2 < 0) {
                    n2 = 0;
                }
                if ((n = this.tillMin.getSelectionModel().getSelectedIndex()) < 0) {
                    n = 0;
                }
                l2 = localDate.atTime(n2, n, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
            }
            PlaylistData playlistData = null;
            switch (n3) {
                case 0: {
                    file = new File(this.fileField.getText());
                    if (bl && !this._fileSizeCheck(file)) break;
                    string2 = this.timeField.getText();
                    try {
                        n5 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n5 < 0) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (bl2 && !this._dimensionCheck(file) || !this.user.name.startsWith("admin") && this.dispy.mbps > 0 && bl3 && !this._bitrateCheck(file)) break;
                    playlistData = new PlaylistData("video");
                    playlistData.setDataFile(file);
                    playlistData.setAttrInt(n5);
                    playlistData.setShowInt(n7, n8, n9, n10);
                    playlistData.setOpen(l);
                    playlistData.setTill(l2);
                    break;
                }
                case 1: {
                    file = new File(this.fileField.getText());
                    if (bl && !this._fileSizeCheck(file)) break;
                    string2 = this.timeField.getText();
                    try {
                        n5 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n5 <= 0) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (bl2 && !this._dimensionCheck(file)) break;
                    playlistData = new PlaylistData("image");
                    playlistData.setDataFile(file);
                    playlistData.setAttrInt(n5);
                    playlistData.setShowInt(n7, n8, n9, n10);
                    playlistData.setOpen(l);
                    playlistData.setTill(l2);
                    break;
                }
                case 2: {
                    file = new File(this.fileField.getText());
                    if (bl && !this._fileSizeCheck(file)) break;
                    string2 = this.timeField.getText();
                    try {
                        n5 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n5 <= 0) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    playlistData = new PlaylistData("pdf");
                    playlistData.setDataFile(file);
                    playlistData.setAttrInt(n5);
                    playlistData.setShowInt(n7, n8, n9, n10);
                    playlistData.setOpen(l);
                    playlistData.setTill(l2);
                    break;
                }
                case 3: {
                    file = new File(this.fileField.getText());
                    if (bl && !this._fileSizeCheck(file)) break;
                    string2 = this.timeField.getText();
                    try {
                        n5 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n5 <= 0) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    playlistData = new PlaylistData("ppt");
                    playlistData.setDataFile(file);
                    playlistData.setAttrInt(n5);
                    playlistData.setShowInt(n7, n8, n9, n10);
                    playlistData.setOpen(l);
                    playlistData.setTill(l2);
                    break;
                }
                case 4: {
                    string = this.textArea.getText();
                    string2 = this.timeField.getText();
                    string3 = this.fontField.getText();
                    if (string.isEmpty()) break;
                    try {
                        n5 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    try {
                        n6 = Integer.parseInt(string3);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n6 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    if (n5 <= 0) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n6 <= 0) {
                        n6 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    playlistData = new PlaylistData("text1");
                    playlistData.setDataText(string);
                    playlistData.setAttrInt(n5, n6);
                    playlistData.setShowInt(n7, n8, n9, n10);
                    playlistData.setOpen(l);
                    playlistData.setTill(l2);
                    break;
                }
                case 5: {
                    string = this.textArea.getText();
                    string2 = this.timeField.getText();
                    string3 = this.fontField.getText();
                    if (string.isEmpty()) break;
                    try {
                        n5 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    try {
                        n6 = Integer.parseInt(string3);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n6 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    if (n5 <= 0) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n6 <= 0) {
                        n6 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    playlistData = new PlaylistData("text2");
                    playlistData.setDataText(string);
                    playlistData.setAttrInt(n5, n6);
                    playlistData.setShowInt(n7, n8, n9, n10);
                    playlistData.setOpen(l);
                    playlistData.setTill(l2);
                    break;
                }
                case 6: {
                    if (this.dispy.getVersionInt() < 36) break;
                    string = this.textArea.getText();
                    string2 = this.timeField.getText();
                    string3 = this.fontField.getText();
                    if (string.isEmpty()) break;
                    try {
                        n5 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    try {
                        n6 = Integer.parseInt(string3);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n6 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    if (n5 < 30) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n6 <= 0) {
                        n6 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    playlistData = new PlaylistData("web");
                    playlistData.setDataText(string);
                    playlistData.setAttrInt(n5, n6);
                    playlistData.setShowInt(n7, n8, n9, n10);
                    playlistData.setOpen(l);
                    playlistData.setTill(l2);
                    break;
                }
                case 7: {
                    if (this.dispy.getVersionInt() < 33) break;
                    string = this.textArea.getText();
                    string2 = this.timeField.getText();
                    string3 = this.fontField.getText();
                    if (string.isEmpty()) break;
                    try {
                        n5 = Integer.parseInt(string2);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    try {
                        n6 = Integer.parseInt(string3);
                    }
                    catch (NumberFormatException numberFormatException) {
                        n6 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    if (n5 < 30) {
                        n5 = 30;
                        this.timeField.setText(String.valueOf(30));
                    }
                    if (n6 <= 0) {
                        n6 = 50;
                        this.fontField.setText(String.valueOf(50));
                    }
                    playlistData = new PlaylistData("style1");
                    playlistData.setDataText(string);
                    playlistData.setAttrInt(n5, n6);
                    playlistData.setShowInt(n7, n8, n9, n10);
                    playlistData.setOpen(l);
                    playlistData.setTill(l2);
                    break;
                }
                case 8: 
                case 9: 
                case 10: {
                    playlistData = new PlaylistData("sync" + (n3 - 8 + 1));
                    break;
                }
                case 11: {
                    string = this.textArea.getText();
                    n4 = Integer.parseInt(string);
                    if (0 >= n4 || n4 >= 1000) break;
                    playlistData = new PlaylistData("sync", n4);
                    break;
                }
                case 12: 
                case 13: 
                case 14: {
                    if (!this.dispy.getId().startsWith("w") && this.dispy.getVersionInt() < 60 || this.dispy.getId().startsWith("w") && this.dispy.getVersionInt() < 53) break;
                    playlistData = new PlaylistData("synclan" + (n3 - 12 + 1));
                    break;
                }
                case 15: {
                    if (!this.dispy.getId().startsWith("w") && this.dispy.getVersionInt() < 60 || this.dispy.getId().startsWith("w") && this.dispy.getVersionInt() < 53 || 0 >= (n4 = Integer.parseInt(string = this.textArea.getText())) || n4 >= 1000) break;
                    playlistData = new PlaylistData("synclan", n4);
                    break;
                }
                case 16: {
                    playlistData = new PlaylistData("individual");
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

    @FXML
    private void handleCheck(ActionEvent actionEvent) {
        Object object = actionEvent.getSource();
        if (this.dispy.getVersionInt() < 30 || object != actionEvent.getTarget()) {
            for (CheckBox checkBox : this.check) {
                if (object == checkBox) continue;
                checkBox.setSelected(false);
            }
        }
    }

    /*
     * Enabled aggressive block sorting
     */
    @FXML
    private void handleAdd(ActionEvent actionEvent) {
        int n = -1;
        PlaylistData playlistData = null;
        int n2 = 0;
        while (true) {
            block16: {
                PlaylistData playlistData2;
                if (n2 >= 4) {
                    return;
                }
                if (!this.check.get(n2).isSelected() || (playlistData2 = this._makePlaylistData(true, false, true)) == null) break block16;
                switch (playlistData2.getTypeCode()) {
                    case "video": 
                    case "image": 
                    case "pdf": 
                    case "ppt": {
                        File file = playlistData2.getDataFile();
                        if (file != null && file.isFile() && file.length() > 0L) break;
                        break block16;
                    }
                    case "individual": {
                        playlistData2 = this.mainApp.showIndivisualDialog(this.dialogStage, playlistData2);
                        if (playlistData2 == null) break block16;
                    }
                }
                playlistData2.setState("new");
                playlistData2.changed = true;
                this.playlist.get(n2).add((Object)playlistData2);
                this.tableView.get(n2).getSelectionModel().select(this.playlist.get(n2).size() - 1);
                this.fillForm(playlistData2);
                if (!(this.dispy.getVersionInt() < 30 || playlistData2.getTypeCode().equals("pdf") || playlistData2.getTypeCode().equals("ppt") || playlistData2.getTypeCode().startsWith("sync"))) {
                    if (n < 0) {
                        n = n2;
                        playlistData = playlistData2;
                    } else {
                        playlistData.addExpands(n2, playlistData2);
                        playlistData2.setExpanded(n, playlistData);
                    }
                }
            }
            ++n2;
        }
    }

    @FXML
    private void handleEdit(ActionEvent actionEvent) {
        for (int i = 0; i < 4; ++i) {
            PlaylistData playlistData;
            PlaylistData playlistData2;
            int n;
            if (!this.check.get(i).isSelected() || (n = this.tableView.get(i).getSelectionModel().getSelectedIndex()) < 0 || (playlistData2 = (PlaylistData)this.playlist.get(i).get(n)).getExpandedData() != null) continue;
            if (Objects.equals(playlistData2.getTypeCode(), "individual")) {
                playlistData = this.mainApp.showIndivisualDialog(this.dialogStage, playlistData2);
            } else {
                File file = playlistData2.getDataFile();
                boolean bl = file != null && file.exists();
                playlistData = this._makePlaylistData(bl, false, bl);
                if (playlistData != null && Objects.equals(playlistData.getTypeCode(), "individual")) {
                    playlistData = this.mainApp.showIndivisualDialog(this.dialogStage, playlistData);
                }
            }
            if (playlistData == null) continue;
            playlistData.setState(playlistData2.getStateCode());
            playlistData.setState("edited");
            if (playlistData.file != null && !playlistData.file.equals(((PlaylistData)this.playlist.get((int)i).get((int)n)).file)) {
                playlistData.changed = true;
            }
            block19: for (int j = i + 1; j < 4; ++j) {
                for (PlaylistData playlistData3 : this.playlist.get(j)) {
                    int n2;
                    int n3;
                    if (playlistData3.getExpandedData() != playlistData2) continue;
                    playlistData3.setType(playlistData.getTypeCode());
                    switch (playlistData3.getTypeCode()) {
                        case "text1": 
                        case "text2": 
                        case "web": 
                        case "style1": {
                            playlistData3.setAttrInt(playlistData.getAttrTime(), playlistData.getAttrFont());
                            break;
                        }
                        default: {
                            playlistData3.setAttrInt(playlistData.getAttrTime());
                        }
                    }
                    playlistData3.setShowInt(playlistData.getShowWeekStart(), playlistData.getShowWeekEnd(), playlistData.getShowStart(), playlistData.getShowEnd());
                    long l = 0L;
                    long l2 = 0L;
                    LocalDate localDate = (LocalDate)this.openDate.getValue();
                    if (localDate != null) {
                        n3 = this.openTime.getSelectionModel().getSelectedIndex();
                        if (n3 < 0) {
                            n3 = 0;
                        }
                        if ((n2 = this.openMin.getSelectionModel().getSelectedIndex()) < 0) {
                            n2 = 0;
                        }
                        l = localDate.atTime(n3, n2, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
                    }
                    if ((localDate = (LocalDate)this.tillDate.getValue()) != null) {
                        n3 = this.tillTime.getSelectionModel().getSelectedIndex();
                        if (n3 < 0) {
                            n3 = 0;
                        }
                        if ((n2 = this.tillMin.getSelectionModel().getSelectedIndex()) < 0) {
                            n2 = 0;
                        }
                        l2 = localDate.atTime(n3, n2, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
                    }
                    playlistData.setOpen(l);
                    playlistData.setTill(l2);
                    switch (playlistData3.getTypeCode()) {
                        case "text1": 
                        case "text2": 
                        case "web": 
                        case "style1": {
                            playlistData3.setDataText(playlistData.getDataText());
                            break;
                        }
                        default: {
                            if (playlistData3.getTypeCode().startsWith("sync")) break;
                            playlistData3.setDataFile(playlistData.getDataFile());
                        }
                    }
                    playlistData3.setState(playlistData.getStateCode());
                    playlistData3.setExpanded(i, playlistData);
                    playlistData.addExpands(j, playlistData3);
                    this.tableView.get(j).getSelectionModel().select(this.playlist.get(j).indexOf((Object)playlistData3));
                    continue block19;
                }
            }
            this.playlist.get(i).remove(n);
            this.playlist.get(i).add(n, (Object)playlistData);
            this.tableView.get(i).getSelectionModel().select(n);
        }
    }

    @FXML
    private void handleUp(ActionEvent actionEvent) {
        int n;
        int n2 = -1;
        if (actionEvent.getSource() == this.up0) {
            n2 = 0;
        } else if (actionEvent.getSource() == this.up1) {
            n2 = 1;
        } else if (actionEvent.getSource() == this.up2) {
            n2 = 2;
        } else if (actionEvent.getSource() == this.up3) {
            n2 = 3;
        }
        if (n2 >= 0 && (n = this.tableView.get(n2).getSelectionModel().getSelectedIndex()) > 0) {
            this.playlist.get(n2).add(n - 1, this.playlist.get(n2).remove(n));
            this.tableView.get(n2).getSelectionModel().select(n - 1);
        }
    }

    @FXML
    private void handleDown(ActionEvent actionEvent) {
        int n;
        int n2 = -1;
        if (actionEvent.getSource() == this.down0) {
            n2 = 0;
        } else if (actionEvent.getSource() == this.down1) {
            n2 = 1;
        } else if (actionEvent.getSource() == this.down2) {
            n2 = 2;
        } else if (actionEvent.getSource() == this.down3) {
            n2 = 3;
        }
        if (n2 >= 0 && (n = this.tableView.get(n2).getSelectionModel().getSelectedIndex()) >= 0 && n < this.playlist.get(n2).size() - 1) {
            this.playlist.get(n2).add(n + 1, this.playlist.get(n2).remove(n));
            this.tableView.get(n2).getSelectionModel().select(n + 1);
        }
    }

    @FXML
    private void handleRemove(ActionEvent actionEvent) {
        PlaylistData playlistData;
        int n = -1;
        if (actionEvent.getSource() == this.remove0) {
            n = 0;
        } else if (actionEvent.getSource() == this.remove1) {
            n = 1;
        } else if (actionEvent.getSource() == this.remove2) {
            n = 2;
        } else if (actionEvent.getSource() == this.remove3) {
            n = 3;
        }
        if (n >= 0 && (playlistData = (PlaylistData)this.tableView.get(n).getSelectionModel().getSelectedItem()) != null && playlistData.getExpandedData() == null) {
            int[] nArray = playlistData.getExpandsCell();
            if (nArray != null) {
                block0: for (int i = n + 1; i < nArray.length; ++i) {
                    if (nArray[i] == 0) continue;
                    for (PlaylistData playlistData2 : this.playlist.get(i)) {
                        if (playlistData2.getExpandedData() != playlistData) continue;
                        this.playlist.get(i).remove((Object)playlistData2);
                        continue block0;
                    }
                }
            }
            this.playlist.get(n).remove((Object)playlistData);
        }
    }

    @FXML
    private void handleRemoveAll(ActionEvent actionEvent) {
        int n = -1;
        if (actionEvent.getSource() == this.removeall0) {
            n = 0;
        } else if (actionEvent.getSource() == this.removeall1) {
            n = 1;
        } else if (actionEvent.getSource() == this.removeall2) {
            n = 2;
        } else if (actionEvent.getSource() == this.removeall3) {
            n = 3;
        }
        if (n >= 0) {
            for (int i = this.playlist.get(n).size() - 1; i >= 0; --i) {
                PlaylistData playlistData = (PlaylistData)this.playlist.get(n).get(i);
                if (playlistData.getExpandedData() != null) continue;
                int[] nArray = playlistData.getExpandsCell();
                if (nArray != null) {
                    block1: for (int j = n + 1; j < nArray.length; ++j) {
                        if (nArray[j] == 0) continue;
                        for (PlaylistData playlistData2 : this.playlist.get(j)) {
                            if (playlistData2.getExpandedData() != playlistData) continue;
                            this.playlist.get(j).remove((Object)playlistData2);
                            continue block1;
                        }
                    }
                }
                this.playlist.get(n).remove((Object)playlistData);
            }
        }
    }

    @FXML
    private void handleClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            Object object = mouseEvent.getSource();
            for (int i = 0; i < this.dispy.play.length; ++i) {
                TableView<PlaylistData> tableView = this.tableView.get(i);
                if (object != tableView) continue;
                PlaylistData playlistData = (PlaylistData)tableView.getSelectionModel().getSelectedItem();
                if (playlistData == null || playlistData.changed) break;
                switch (playlistData.getTypeCode()) {
                    case "video": 
                    case "image": {
                        this.mainApp.showMediainfoDialog(this.dispy.play[i], playlistData.getData());
                    }
                }
                break;
            }
        }
    }

    @FXML
    private void handleOk() {
        if (this.reserveLabel.isVisible() && this.getReserveTime() == 0L) {
            return;
        }
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.playlist = null;
        this.dialogStage.close();
    }

    public void handle(WindowEvent windowEvent) {
        this.playlist = null;
    }

    public void close() {
        this.playlist = null;
        this.dialogStage.close();
    }
}

