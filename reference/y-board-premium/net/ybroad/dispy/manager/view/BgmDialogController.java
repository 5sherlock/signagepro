/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.javafx.scene.control.skin.TableViewSkin
 *  com.sun.javafx.scene.control.skin.VirtualFlow
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
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
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
import java.io.File;
import java.util.ArrayList;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import net.ybroad.dispy.lib.PlayData;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.model.PlaylistData;
import net.ybroad.dispy.manager.util.Lang;

public class BgmDialogController
implements EventHandler<WindowEvent> {
    @FXML
    private AnchorPane rootView;
    @FXML
    private TableView<PlaylistData> tableView;
    @FXML
    private TableColumn<PlaylistData, String> stateColumn;
    @FXML
    private TableColumn<PlaylistData, String> showColumn;
    @FXML
    private TableColumn<PlaylistData, String> dataColumn;
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
    private Label playLabel;
    @FXML
    private Label dndLabel;
    private Stage dialogStage;
    private ObservableList<PlaylistData> playlist = FXCollections.observableArrayList();
    private static final String[] DAY_OF_WEEK = new String[]{Lang.getString("week.mon"), Lang.getString("week.tue"), Lang.getString("week.wed"), Lang.getString("week.thu"), Lang.getString("week.fri"), Lang.getString("week.sat"), Lang.getString("week.sun")};
    private static final String[] HOUR_OF_DAY_START = new String[]{"00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"};
    private static final String[] HOUR_OF_DAY_END = new String[]{"00:59", "01:59", "02:59", "03:59", "04:59", "05:59", "06:59", "07:59", "08:59", "09:59", "10:59", "11:59", "12:59", "13:59", "14:59", "15:59", "16:59", "17:59", "18:59", "19:59", "20:59", "21:59", "22:59", "23:59"};
    private FileChooser fileChooser = new FileChooser();
    private FileChooser.ExtensionFilter fileFilter = new FileChooser.ExtensionFilter("Music (*.mp3)", new String[]{"*.mp3", "*.MP3"});
    private File recentDir = null;

    @FXML
    private void initialize() {
        this.stateColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).stateProperty());
        this.showColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).showProperty());
        this.dataColumn.setCellValueFactory(cellDataFeatures -> ((PlaylistData)cellDataFeatures.getValue()).dataProperty());
        this.tableView.setItems(this.playlist);
        this.tableView.getSelectionModel().selectedItemProperty().addListener((ChangeListener)new ChangeListener<PlaylistData>(){

            public void changed(ObservableValue<? extends PlaylistData> observableValue, PlaylistData playlistData, PlaylistData playlistData2) {
                if (playlistData2 != null) {
                    BgmDialogController.this.fillForm(playlistData2);
                    try {
                        VirtualFlow virtualFlow = (VirtualFlow)((TableViewSkin)BgmDialogController.this.tableView.getSkin()).getChildren().get(1);
                        int n = virtualFlow.getFirstVisibleCell().getIndex();
                        int n2 = virtualFlow.getLastVisibleCell().getIndex();
                        int n3 = BgmDialogController.this.tableView.getItems().indexOf((Object)playlistData2);
                        if (n3 < n || n2 < n3) {
                            BgmDialogController.this.tableView.scrollTo((Object)playlistData2);
                        }
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                    }
                }
            }
        });
        this.showCombo1.getItems().addAll((Object[])DAY_OF_WEEK);
        this.showCombo2.getItems().addAll((Object[])DAY_OF_WEEK);
        this.showCombo3.getItems().addAll((Object[])HOUR_OF_DAY_START);
        this.showCombo4.getItems().addAll((Object[])HOUR_OF_DAY_END);
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
        stage.setOnCloseRequest((EventHandler)this);
        this.rootView.setOnDragEntered((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                BgmDialogController.this.dndLabel.setVisible(true);
            }
        });
        this.rootView.setOnDragExited((EventHandler)new EventHandler<DragEvent>(){

            public void handle(DragEvent dragEvent) {
                BgmDialogController.this.dndLabel.setVisible(false);
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
                        String string2 = string.toLowerCase();
                        if (!string2.endsWith(".mp3")) continue;
                        BgmDialogController.this.fileField.setText(string);
                        BgmDialogController.this.fileField.home();
                        BgmDialogController.this.fileField.end();
                        BgmDialogController.this.recentDir = file.getParentFile();
                        BgmDialogController.this.handleAdd(null);
                    }
                }
                dragEvent.setDropCompleted(bl);
                dragEvent.consume();
            }
        });
    }

    public void setUserData(UserData userData) {
        boolean bl = userData.name.equals("admin");
        this.playLabel.setVisible(bl);
    }

    public void setDispyData(DispyData dispyData) {
        this.playlist.clear();
        this.playlist.addAll(dispyData.getBgmlist());
        this.playLabel.setText(dispyData.bgm);
    }

    public ArrayList<PlayData> getPlaylist() {
        if (this.playlist == null) {
            return null;
        }
        ArrayList<PlayData> arrayList = new ArrayList<PlayData>();
        for (PlaylistData playlistData : this.playlist) {
            arrayList.add(playlistData);
        }
        return arrayList;
    }

    private void fillForm(PlaylistData playlistData) {
        if (playlistData == null) {
            return;
        }
        this.showCombo1.getSelectionModel().select(playlistData.getShowWeekStart());
        this.showCombo2.getSelectionModel().select(playlistData.getShowWeekEnd());
        this.showCombo3.getSelectionModel().select(playlistData.getShowStart());
        this.showCombo4.getSelectionModel().select(playlistData.getShowEnd());
        this.fileField.setText(playlistData.getDataFile().getAbsolutePath());
        this.fileField.home();
        this.fileField.end();
        File file = playlistData.getDataFile().getParentFile();
        if (file != null && file.exists()) {
            this.recentDir = file;
        }
    }

    @FXML
    private void handleFile() {
        this.fileChooser.getExtensionFilters().clear();
        this.fileChooser.getExtensionFilters().add((Object)this.fileFilter);
        this.fileChooser.setInitialDirectory(this.recentDir);
        File file = this.fileChooser.showOpenDialog((Window)this.dialogStage);
        if (file != null) {
            this.fileField.setText(file.getAbsolutePath());
            this.fileField.home();
            this.fileField.end();
            this.recentDir = file.getParentFile();
        }
    }

    private PlaylistData _makePlaylistData(boolean bl) {
        try {
            File file = new File(this.fileField.getText());
            int n = this.showCombo1.getSelectionModel().getSelectedIndex();
            int n2 = this.showCombo2.getSelectionModel().getSelectedIndex();
            int n3 = this.showCombo3.getSelectionModel().getSelectedIndex();
            int n4 = this.showCombo4.getSelectionModel().getSelectedIndex();
            PlaylistData playlistData = new PlaylistData("music");
            playlistData.setDataFile(file);
            playlistData.setShowInt(n, n2, n3, n4);
            return playlistData;
        }
        catch (Exception exception) {
            Log.out(exception);
            return null;
        }
    }

    @FXML
    private void handleAdd(ActionEvent actionEvent) {
        PlaylistData playlistData = this._makePlaylistData(true);
        if (playlistData != null) {
            File file = playlistData.getDataFile();
            if (file == null || !file.isFile() || file.length() <= 0L) {
                return;
            }
            playlistData.setState("new");
            playlistData.changed = true;
            this.playlist.add((Object)playlistData);
            this.tableView.getSelectionModel().select(this.playlist.size() - 1);
        }
    }

    @FXML
    private void handleEdit(ActionEvent actionEvent) {
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
        this.playlist.remove((Object)playlistData);
    }

    @FXML
    private void handleRemoveAll(ActionEvent actionEvent) {
        this.playlist.clear();
    }

    @FXML
    private void handleOk() {
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

