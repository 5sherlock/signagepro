/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.event.EventHandler
 *  javafx.fxml.FXML
 *  javafx.scene.control.Button
 *  javafx.scene.control.ComboBox
 *  javafx.scene.control.DatePicker
 *  javafx.scene.control.Label
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
 *  javafx.scene.control.TextArea
 *  javafx.scene.control.TextField
 *  javafx.scene.input.DragEvent
 *  javafx.scene.input.Dragboard
 *  javafx.scene.input.TransferMode
 *  javafx.scene.layout.AnchorPane
 *  javafx.scene.layout.HBox
 *  javafx.stage.FileChooser
 *  javafx.stage.FileChooser$ExtensionFilter
 *  javafx.stage.Stage
 *  javafx.stage.Window
 *  javafx.stage.WindowEvent
 *  javafx.util.StringConverter
 */
package net.ybroad.dispy.manager.view;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.model.PlaylistData;
import net.ybroad.dispy.manager.util.Lang;

public class PlaylistDialogController
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
    private ComboBox<String> typeCombo;
    @FXML
    private TextField timeField;
    @FXML
    private TextField fontField;
    @FXML
    private ComboBox<String> showCombo1;
    @FXML
    private ComboBox<String> showCombo2;
    @FXML
    private ComboBox<String> showCombo3;
    @FXML
    private ComboBox<String> showCombo4;
    @FXML
    private Button fileButton;
    @FXML
    private TextField fileLabel;
    @FXML
    private TextArea textArea;
    @FXML
    private ComboBox<String> fromCombo;
    @FXML
    private Label dndLabel;
    @FXML
    private HBox reserveBox;
    @FXML
    private Label reserveLabel;
    @FXML
    private DatePicker reserveDate;
    @FXML
    private ComboBox<String> reserveTime;
    private DispyData dispy;
    private Stage dialogStage;
    private ObservableList<PlaylistData> playlist = FXCollections.observableArrayList();
    private static final String[] TYPE_TEXT = new String[]{Lang.getString("play.type.video"), Lang.getString("play.type.image"), Lang.getString("play.type.pdf"), Lang.getString("play.type.ppt"), Lang.getString("play.type.text1"), Lang.getString("play.type.text2"), Lang.getString("play.type.web"), Lang.getString("play.type.style1"), Lang.getString("play.type.sync1"), Lang.getString("play.type.sync2"), Lang.getString("play.type.sync3"), Lang.getString("play.stype.ync4"), Lang.getString("play.type.sync5"), Lang.getString("play.type.sync6"), Lang.getString("play.type.sync7"), Lang.getString("play.type.sync8")};
    private static final String[] DAY_OF_WEEK = new String[]{Lang.getString("week.mon"), Lang.getString("week.tue"), Lang.getString("week.wed"), Lang.getString("week.thu"), Lang.getString("week.fri"), Lang.getString("week.sat"), Lang.getString("week.sun")};
    private static final String[] HOUR_OF_DAY_START = new String[]{"00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"};
    private static final String[] HOUR_OF_DAY_END = new String[]{"00:59", "01:59", "02:59", "03:59", "04:59", "05:59", "06:59", "07:59", "08:59", "09:59", "10:59", "11:59", "12:59", "13:59", "14:59", "15:59", "16:59", "17:59", "18:59", "19:59", "20:59", "21:59", "22:59", "23:59"};
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
    private static final int TYPE_SYNC4 = 11;
    private static final int TYPE_SYNC5 = 12;
    private static final int TYPE_SYNC6 = 13;
    private static final int TYPE_SYNC7 = 14;
    private static final int TYPE_SYNC8 = 15;
    private FileChooser fileChooser = new FileChooser();
    private FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("MPEG-4 video (*.mp4)", new String[]{"*.mp4", "*.MP4"});
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
        this.tableView.setItems(this.playlist);
        this.tableView.getSelectionModel().selectedItemProperty().addListener((observableValue, playlistData, playlistData2) -> {
            if (playlistData2 != null) {
                this.fillForm((PlaylistData)playlistData2);
            }
        });
        this.typeCombo.getItems().addAll((Object[])TYPE_TEXT);
        this.typeCombo.getSelectionModel().selectedItemProperty().addListener((observableValue, string, string2) -> this.typeChanged((String)string2));
        this.typeCombo.getSelectionModel().select(0);
        this.showCombo1.getItems().addAll((Object[])DAY_OF_WEEK);
        this.showCombo2.getItems().addAll((Object[])DAY_OF_WEEK);
        this.showCombo3.getItems().addAll((Object[])HOUR_OF_DAY_START);
        this.showCombo4.getItems().addAll((Object[])HOUR_OF_DAY_END);
        this.reserveTime.getItems().addAll((Object[])HOUR_OF_DAY_START);
        this.reserveDate.setConverter((StringConverter)new StringConverter<LocalDate>(){
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
        });
        this.fromCombo.getSelectionModel().selectedItemProperty().addListener((observableValue, string, string2) -> this.fromChanged());
    }

    private void fillForm(PlaylistData playlistData) {
        this.typeCombo.getSelectionModel().select((Object)playlistData.getType());
        switch (playlistData.getTypeCode()) {
            case "video": 
            case "image": 
            case "pdf": 
            case "ppt": {
                this.timeField.setText(String.valueOf(playlistData.getAttrTime()));
                this.showCombo1.getSelectionModel().select(playlistData.getShowWeekStart());
                this.showCombo2.getSelectionModel().select(playlistData.getShowWeekEnd());
                this.showCombo3.getSelectionModel().select(playlistData.getShowStart());
                this.showCombo4.getSelectionModel().select(playlistData.getShowEnd());
                this.fileLabel.setText(playlistData.getDataFile().getAbsolutePath());
                this.fileLabel.end();
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
                break;
            }
        }
    }

    private void typeChanged(String string) {
        if (string == null) {
            return;
        }
        if (string.equals(TYPE_TEXT[0]) || string.equals(TYPE_TEXT[1]) || string.equals(TYPE_TEXT[2]) || string.equals(TYPE_TEXT[3])) {
            this.timeField.setVisible(true);
            this.fontField.setVisible(false);
            this.showCombo1.setVisible(true);
            this.showCombo2.setVisible(true);
            this.showCombo3.setVisible(true);
            this.showCombo4.setVisible(true);
            this.fileLabel.setVisible(true);
            this.fileButton.setVisible(true);
            this.textArea.setVisible(false);
            this.fileLabel.setText("");
        } else if (string.equals(TYPE_TEXT[4]) || string.equals(TYPE_TEXT[5]) || string.equals(TYPE_TEXT[6])) {
            this.timeField.setVisible(true);
            this.fontField.setVisible(true);
            this.showCombo1.setVisible(true);
            this.showCombo2.setVisible(true);
            this.showCombo3.setVisible(true);
            this.showCombo4.setVisible(true);
            this.fileLabel.setVisible(false);
            this.fileButton.setVisible(false);
            this.textArea.setVisible(true);
        } else {
            this.timeField.setVisible(false);
            this.fontField.setVisible(false);
            this.showCombo1.setVisible(false);
            this.showCombo2.setVisible(false);
            this.showCombo3.setVisible(false);
            this.showCombo4.setVisible(false);
            this.fileLabel.setVisible(false);
            this.fileButton.setVisible(false);
            this.textArea.setVisible(false);
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
        stage.setOnCloseRequest((EventHandler)this);
        this.rootView.setOnDragEntered((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                PlaylistDialogController.this.dndLabel.setVisible(true);
            }
        });
        this.rootView.setOnDragExited((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                PlaylistDialogController.this.dndLabel.setVisible(false);
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
                    for (File file : dragboard.getFiles()) {
                        String string = file.getAbsolutePath();
                        if (string.toLowerCase().endsWith(".mp4")) {
                            PlaylistDialogController.this.typeCombo.getSelectionModel().select(0);
                            PlaylistDialogController.this.fileLabel.setText(string);
                            PlaylistDialogController.this.fileLabel.end();
                            PlaylistDialogController.this.recentDir = file.getParentFile();
                            PlaylistDialogController.this.handleAdd();
                            continue;
                        }
                        if (string.toLowerCase().endsWith(".jpg") || string.toLowerCase().endsWith(".jepg") || string.toLowerCase().endsWith("png")) {
                            PlaylistDialogController.this.typeCombo.getSelectionModel().select(1);
                            PlaylistDialogController.this.fileLabel.setText(string);
                            PlaylistDialogController.this.fileLabel.end();
                            PlaylistDialogController.this.recentDir = file.getParentFile();
                            PlaylistDialogController.this.handleAdd();
                            continue;
                        }
                        if (string.toLowerCase().endsWith(".pdf")) {
                            PlaylistDialogController.this.typeCombo.getSelectionModel().select(2);
                            PlaylistDialogController.this.fileLabel.setText(string);
                            PlaylistDialogController.this.fileLabel.end();
                            PlaylistDialogController.this.recentDir = file.getParentFile();
                            PlaylistDialogController.this.handleAdd();
                            continue;
                        }
                        if (!string.toLowerCase().endsWith(".ppt") && !string.toLowerCase().endsWith(".pptx")) continue;
                        PlaylistDialogController.this.typeCombo.getSelectionModel().select(3);
                        PlaylistDialogController.this.fileLabel.setText(string);
                        PlaylistDialogController.this.fileLabel.end();
                        PlaylistDialogController.this.recentDir = file.getParentFile();
                        PlaylistDialogController.this.handleAdd();
                    }
                }
                dragEvent.setDropCompleted(bl);
                dragEvent.consume();
            }
        });
    }

    public void setDispyData(DispyData dispyData) {
        this.dispy = dispyData;
    }

    public void setPlaylist(List<PlaylistData> list) {
        this.playlist.clear();
        this.playlist.addAll(list);
    }

    private void setPlaylistNew(List<PlaylistData> list) {
        this.playlist.clear();
        this.playlist.addAll(list);
        for (PlaylistData playlistData : this.playlist) {
            playlistData.setState("new");
            playlistData.changed = true;
        }
    }

    public void setReserve(int n, int n2) {
        this.reserveBox.setVisible(n2 >= 0);
        if (n2 >= 0) {
            this.fromCombo.getItems().add((Object)Lang.getString("play.from.play", n));
            for (int i = 0; i < 4; ++i) {
                if (i == n2) continue;
                this.fromCombo.getItems().add((Object)Lang.getString("play.from.reserve", n, i + 1));
            }
        } else {
            this.fromCombo.setVisible(false);
        }
    }

    private void fromChanged() {
        int n = this.fromCombo.getSelectionModel().getSelectedIndex();
        if (n >= 0) {
            String string = (String)this.fromCombo.getItems().get(n);
            int n2 = string.indexOf(35);
            int n3 = string.lastIndexOf(45);
            if (n3 < 0) {
                int n4 = Integer.parseInt(string.substring(n2 + 1, n2 + 2));
                this.setPlaylistNew(this.dispy.getPlaylist(n4));
                this.setReserveTime(0L);
            } else {
                int n5 = Integer.parseInt(string.substring(n2 + 1, n2 + 2));
                int n6 = Integer.parseInt(string.substring(n3 + 2, n3 + 3)) - 1;
                this.setPlaylistNew(this.dispy.getReservelist(n5, n6));
                this.setReserveTime(this.dispy.getReserveTime(n5, n6));
            }
            Platform.runLater((Runnable)new Runnable(){

                @Override
                public void run() {
                    PlaylistDialogController.this.fromCombo.getSelectionModel().clearSelection();
                }
            });
        }
    }

    public ObservableList<PlaylistData> getPlaylist() {
        return this.playlist;
    }

    public void setReserveTime(long l) {
        if (l > 0L) {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTimeInMillis(l);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(gregorianCalendar.toInstant(), ZoneId.systemDefault());
            this.reserveDate.setValue((Object)localDateTime.toLocalDate());
            this.reserveTime.getSelectionModel().select(localDateTime.getHour());
        } else {
            this.reserveDate.setValue(null);
            this.reserveTime.getSelectionModel().clearSelection();
        }
    }

    public long getReserveTime() {
        LocalDate localDate = (LocalDate)this.reserveDate.getValue();
        if (localDate != null) {
            long l;
            long l2;
            int n = this.reserveTime.getSelectionModel().getSelectedIndex();
            if (n < 0) {
                n = 0;
            }
            if ((l2 = (l = localDate.atTime(n, 0, 0).atZone(ZoneId.systemDefault()).toEpochSecond()) - System.currentTimeMillis()) > 300L && l2 > 5184000L) {
                return l;
            }
            return 0L;
        }
        return 0L;
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
            this.fileLabel.setText(file.getAbsolutePath());
            this.fileLabel.end();
            this.recentDir = file.getParentFile();
        }
    }

    @FXML
    private void handleAdd() {
        PlaylistData playlistData = this._makePlaylistData(true);
        if (playlistData != null) {
            playlistData.setState("new");
            playlistData.changed = true;
            this.playlist.add((Object)playlistData);
            this.tableView.getSelectionModel().select(this.playlist.size() - 1);
        }
    }

    @FXML
    private void handleEdit() {
        PlaylistData playlistData;
        File file;
        PlaylistData playlistData2;
        int n = this.tableView.getSelectionModel().getSelectedIndex();
        if (n >= 0 && (playlistData2 = this._makePlaylistData((file = (playlistData = (PlaylistData)this.playlist.get(n)).getDataFile()) != null && file.exists())) != null) {
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

    private PlaylistData _makePlaylistData(boolean bl) {
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
                file = new File(this.fileLabel.getText());
                string2 = this.timeField.getText();
                if (bl && (!file.exists() || !this._fileSizeCheck(file))) break;
                n2 = Integer.parseInt(string2);
                playlistData = new PlaylistData("video");
                playlistData.setDataFile(file);
                playlistData.setAttrInt(n2);
                playlistData.setShowInt(n4, n5, n6, n7);
                break;
            }
            case 1: {
                file = new File(this.fileLabel.getText());
                string2 = this.timeField.getText();
                if (bl && (!file.exists() || !this._fileSizeCheck(file)) || (n2 = Integer.parseInt(string2)) <= 0) break;
                playlistData = new PlaylistData("image");
                playlistData.setDataFile(file);
                playlistData.setAttrInt(n2);
                playlistData.setShowInt(n4, n5, n6, n7);
                break;
            }
            case 2: {
                file = new File(this.fileLabel.getText());
                string2 = this.timeField.getText();
                if (bl && (!file.exists() || !this._fileSizeCheck(file)) || (n2 = Integer.parseInt(string2)) <= 0) break;
                playlistData = new PlaylistData("pdf");
                playlistData.setDataFile(file);
                playlistData.setAttrInt(n2);
                playlistData.setShowInt(n4, n5, n6, n7);
                break;
            }
            case 3: {
                file = new File(this.fileLabel.getText());
                string2 = this.timeField.getText();
                if (bl && (!file.exists() || !this._fileSizeCheck(file)) || (n2 = Integer.parseInt(string2)) <= 0) break;
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
                if (string.isEmpty() || string2.isEmpty() || string3.isEmpty()) break;
                n2 = Integer.parseInt(string2);
                n3 = Integer.parseInt(string3);
                if (n2 <= 0 || n3 <= 0) break;
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
                if (string.isEmpty() || string2.isEmpty() || string3.isEmpty()) break;
                n2 = Integer.parseInt(string2);
                n3 = Integer.parseInt(string3);
                if (n2 <= 0 || n3 <= 0) break;
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
                if (string.isEmpty() || string2.isEmpty() || string3.isEmpty()) break;
                n2 = Integer.parseInt(string2);
                n3 = Integer.parseInt(string3);
                if (n2 <= 0 || n3 <= 0) break;
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
                if (string.isEmpty() || string2.isEmpty() || string3.isEmpty()) break;
                n2 = Integer.parseInt(string2);
                n3 = Integer.parseInt(string3);
                if (n2 <= 0 || n3 <= 0) break;
                playlistData = new PlaylistData("style1");
                playlistData.setDataText(string);
                playlistData.setAttrInt(n2, n3);
                playlistData.setShowInt(n4, n5, n6, n7);
                break;
            }
            case 8: 
            case 9: 
            case 10: 
            case 11: 
            case 12: 
            case 13: 
            case 14: 
            case 15: {
                playlistData = new PlaylistData("sync" + (n - 8 + 1));
            }
        }
        return playlistData;
    }

    private boolean _fileSizeCheck(File file) {
        if (this.dispy.getOwner().startsWith("admin")) {
            return true;
        }
        long l = file.length();
        if (l > 0x6400000L) {
            return false;
        }
        for (PlaylistData playlistData : this.playlist) {
            if (!playlistData.getType().equals("video") && !playlistData.getType().equals("image") && !playlistData.getType().equals("pdf") && !playlistData.getType().equals("ppt")) continue;
            l += playlistData.getDataFile().length();
        }
        return l <= 314572800L;
    }

    @FXML
    private void handleUp() {
        int n = this.tableView.getSelectionModel().getSelectedIndex();
        if (n > 0) {
            this.playlist.add(n - 1, this.playlist.remove(n));
            this.tableView.getSelectionModel().select(n - 1);
        }
    }

    @FXML
    private void handleDown() {
        int n = this.tableView.getSelectionModel().getSelectedIndex();
        if (n >= 0 && n < this.playlist.size() - 1) {
            this.playlist.add(n + 1, this.playlist.remove(n));
            this.tableView.getSelectionModel().select(n + 1);
        }
    }

    @FXML
    private void handleRemove() {
        PlaylistData playlistData = (PlaylistData)this.tableView.getSelectionModel().getSelectedItem();
        this.playlist.remove((Object)playlistData);
    }

    @FXML
    private void handleRemoveAll() {
        this.playlist.clear();
    }

    @FXML
    private void handleOk() {
        if (this.reserveBox.isVisible() && this.getReserveTime() == 0L) {
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
}

