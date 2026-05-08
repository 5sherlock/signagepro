/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.animation.KeyFrame
 *  javafx.animation.KeyValue
 *  javafx.animation.Timeline
 *  javafx.application.Platform
 *  javafx.beans.value.ObservableValue
 *  javafx.collections.ListChangeListener
 *  javafx.collections.ListChangeListener$Change
 *  javafx.collections.ObservableList
 *  javafx.collections.transformation.FilteredList
 *  javafx.collections.transformation.SortedList
 *  javafx.fxml.FXML
 *  javafx.geometry.Pos
 *  javafx.scene.canvas.Canvas
 *  javafx.scene.control.Button
 *  javafx.scene.control.ComboBox
 *  javafx.scene.control.Label
 *  javafx.scene.control.ProgressBar
 *  javafx.scene.control.TableCell
 *  javafx.scene.control.TableColumn
 *  javafx.scene.control.TableView
 *  javafx.scene.control.Tooltip
 *  javafx.scene.layout.GridPane
 *  javafx.scene.layout.HBox
 *  javafx.scene.layout.VBox
 *  javafx.util.Callback
 *  javafx.util.Duration
 */
package net.ybroad.dispy.manager.view;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Duration;
import net.ybroad.dispy.lib.Log;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.manager.MainApp;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.util.CanvasDrawer;
import net.ybroad.dispy.manager.util.Lang;

public class DispyOverviewController {
    @FXML
    private ComboBox<String> dispyGroup;
    @FXML
    private Button historyButton;
    @FXML
    private TableView<DispyData> dispyTable;
    @FXML
    private TableColumn<DispyData, String> nameColumn;
    @FXML
    private TableColumn<DispyData, String> stateColumn;
    @FXML
    private TableColumn<DispyData, String> touchColumn;
    @FXML
    private Label idLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private Label versionLabel;
    @FXML
    private Label infoLabel;
    @FXML
    private Label specLabel;
    @FXML
    private Label freeLabel;
    @FXML
    private Label stateLabel;
    @FXML
    private Label ipLabel;
    @FXML
    private Label touchLabel;
    @FXML
    private Label licenseLabel;
    @FXML
    private Label sizeLabel;
    @FXML
    private Label typeLabel;
    @FXML
    private Label cell0Label;
    @FXML
    private Label cell1Label;
    @FXML
    private Label cell2Label;
    @FXML
    private Label cell3Label;
    @FXML
    private Label playLabel;
    @FXML
    private Label play0Label;
    @FXML
    private Label play1Label;
    @FXML
    private Label play2Label;
    @FXML
    private Label play3Label;
    @FXML
    private Label reserve0Label;
    @FXML
    private Label reserve1Label;
    @FXML
    private Label reserve2Label;
    @FXML
    private Label reserve3Label;
    @FXML
    private Label reserve00Label;
    @FXML
    private Label reserve01Label;
    @FXML
    private Label reserve02Label;
    @FXML
    private Label reserve03Label;
    @FXML
    private Label reserve10Label;
    @FXML
    private Label reserve11Label;
    @FXML
    private Label reserve12Label;
    @FXML
    private Label reserve13Label;
    @FXML
    private Label reserve20Label;
    @FXML
    private Label reserve21Label;
    @FXML
    private Label reserve22Label;
    @FXML
    private Label reserve23Label;
    @FXML
    private Label reserve30Label;
    @FXML
    private Label reserve31Label;
    @FXML
    private Label reserve32Label;
    @FXML
    private Label reserve33Label;
    @FXML
    private Label bgmLabel;
    @FXML
    private Button licenseButton;
    @FXML
    private Button renewButton;
    @FXML
    private Button screenButton;
    @FXML
    private Canvas cellCanvas;
    @FXML
    private Button cellButton;
    @FXML
    private Button snapButton;
    @FXML
    private Button screencapButton;
    @FXML
    private Button logcatButton;
    @FXML
    private Button masterButton;
    @FXML
    private Button slaveButton;
    @FXML
    private Button unionButton;
    @FXML
    private Button playButton;
    @FXML
    private Button reserve0Button;
    @FXML
    private Button reserve1Button;
    @FXML
    private Button reserve2Button;
    @FXML
    private Button reserve3Button;
    @FXML
    private Button bgmButton;
    @FXML
    private Button replace01Button;
    @FXML
    private Button replace12Button;
    @FXML
    private Button replace23Button;
    @FXML
    private Button replace02Button;
    @FXML
    private Button replace03Button;
    @FXML
    private Button replace13Button;
    @FXML
    private Button replaceButton;
    @FXML
    private GridPane replacePane;
    @FXML
    private Button scheduleButton;
    @FXML
    private VBox progressBox;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private HBox shutter;
    private MainApp mainApp;
    private UserData user;
    private FilteredList<DispyData> filteredData;
    private SortedList<DispyData> sortedData;

    @FXML
    private void initialize() {
        this.dispyGroup.valueProperty().addListener((observableValue, string, string2) -> Platform.runLater(() -> this.groupChanged((String)string2)));
        this.nameColumn.setCellValueFactory(cellDataFeatures -> ((DispyData)cellDataFeatures.getValue()).nameProperty());
        this.stateColumn.setCellValueFactory(cellDataFeatures -> ((DispyData)cellDataFeatures.getValue()).onoffProperty());
        this.touchColumn.setCellValueFactory(cellDataFeatures -> ((DispyData)cellDataFeatures.getValue()).touchProperty());
        final String string3 = Lang.getString("overview.connect");
        this.stateColumn.setCellFactory((Callback)new Callback<TableColumn<DispyData, String>, TableCell<DispyData, String>>(){

            public TableCell<DispyData, String> call(TableColumn<DispyData, String> tableColumn) {
                return new TableCell<DispyData, String>(){

                    public void updateItem(String string, boolean bl) {
                        super.updateItem((Object)string, bl);
                        this.setText(bl ? "" : string);
                        this.setAlignment(Pos.CENTER);
                        if (string == null) {
                            this.setStyle("");
                        } else if (string.equals(string3)) {
                            this.setStyle("-fx-background-insets:0 1 1 0; -fx-background-color: palegreen;");
                        } else {
                            this.setStyle("-fx-background-insets:0 1 1 0; -fx-background-color: lightpink;");
                        }
                    }
                };
            }
        });
        this.showDispyDetails(null);
        this.dispyTable.getSelectionModel().selectedItemProperty().addListener((observableValue, dispyData, dispyData2) -> this.showDispyDetails((DispyData)dispyData2));
        this.shutter.setVisible(true);
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        ObservableList<DispyData> observableList = mainApp.getDispyData();
        observableList.addListener((ListChangeListener)new ListChangeListener<DispyData>(){

            public void onChanged(ListChangeListener.Change<? extends DispyData> change) {
                DispyOverviewController.this.groupReset();
            }
        });
        this.filteredData = new FilteredList(observableList, dispyData -> true);
        this.sortedData = new SortedList(this.filteredData);
        this.sortedData.comparatorProperty().bind((ObservableValue)this.dispyTable.comparatorProperty());
        this.dispyTable.setItems(this.sortedData);
        this.filteredData.addListener((ListChangeListener)new ListChangeListener<DispyData>(){

            public void onChanged(ListChangeListener.Change<? extends DispyData> change) {
                DispyOverviewController.this.dispyGroup.setDisable(DispyOverviewController.this.filteredData.isEmpty());
                DispyOverviewController.this.historyButton.setDisable(DispyOverviewController.this.filteredData.isEmpty());
            }
        });
    }

    public void setUserData(UserData userData) {
        this.user = userData;
    }

    public void showDispyDetails(DispyData dispyData) {
        int n;
        if (dispyData != null) {
            this.idLabel.setText("id: " + dispyData.getId());
            this.nameLabel.setText(dispyData.getName());
            if (this.user.name.startsWith("admin") || this.user.name.endsWith("#")) {
                this.versionLabel.setText("ver: " + dispyData.getVersion() + " / " + dispyData.getOwner());
            } else {
                this.versionLabel.setText("ver: " + dispyData.getVersion());
            }
            String string = dispyData.info.isEmpty() ? Lang.getString("app.noinfo") : dispyData.info.replace("\\n", "\n");
            n = string.indexOf("\u02fd");
            if (n < 0) {
                this.infoLabel.setText(string);
                this.infoLabel.setTooltip(null);
            } else {
                this.infoLabel.setText(string.substring(0, n));
                this.infoLabel.setTooltip(this.makeTooltip(string.substring(n + 1)));
            }
            this.specLabel.setText(dispyData.spec.isEmpty() ? Lang.getString("app.noinfo") : dispyData.spec);
            this.freeLabel.setText(dispyData.free < 0L ? Lang.getString("app.noinfo") : (dispyData.free > 0x40000000L ? BigDecimal.valueOf((float)dispyData.free / 1024.0f / 1024.0f / 1024.0f).setScale(2, RoundingMode.FLOOR).floatValue() + "G" : BigDecimal.valueOf((float)dispyData.free / 1024.0f / 1024.0f).setScale(2, RoundingMode.FLOOR).floatValue() + "M"));
            this.stateLabel.setText(dispyData.getOnoff());
            if (dispyData.getOnoffBoolean()) {
                this.stateLabel.setStyle("-fx-background-color: palegreen;");
            } else {
                this.stateLabel.setStyle("-fx-background-color: lightpink;");
            }
            if (dispyData.getId().startsWith("u")) {
                if (dispyData.connect + dispyData.disconnect == 0L) {
                    this.ipLabel.setText("");
                } else {
                    this.ipLabel.setText("(" + dispyData.connect + "/" + (dispyData.connect + dispyData.disconnect) + ")");
                }
            } else {
                this.ipLabel.setText(dispyData.getIp());
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String string2 = dispyData.connect > 0L ? simpleDateFormat.format(dispyData.connect) : Lang.getString("app.noinfo");
            String string3 = dispyData.disconnect > 0L ? simpleDateFormat.format(dispyData.disconnect) : Lang.getString("app.noinfo");
            String string4 = !dispyData.reason.isEmpty() ? dispyData.reason : Lang.getString("app.noinfo");
            this.touchLabel.setText(dispyData.getTouch());
            if (dispyData.getId().startsWith("u")) {
                this.touchLabel.setTooltip(null);
            } else {
                this.touchLabel.setTooltip(this.makeTooltip(Lang.getString("overview.last", string2, string3, string4)));
            }
            this.licenseLabel.setText(dispyData.getLicense());
            long l = dispyData.getLicenseLong() - System.currentTimeMillis();
            if (l < 0L) {
                this.licenseLabel.setStyle("-fx-background-color: red; -fx-text-fill: white;");
                this.renewButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
            } else if (l < 864000000L) {
                this.licenseLabel.setStyle("-fx-background-color: lightpink; -fx-text-fill: white;");
                this.renewButton.setStyle("-fx-background-color: lightpink; -fx-text-fill: white;");
            } else {
                this.licenseLabel.setStyle("-fx-background-color: transparent;");
                this.renewButton.setStyle("");
            }
            CanvasDrawer.drawCanvas(this.cellCanvas, dispyData.getTypeInt(), dispyData.getCellInt(), dispyData.getWidth(), dispyData.getHeight());
            this.sizeLabel.setText(dispyData.getSize());
            this.typeLabel.setText(dispyData.getTypeString());
            this.cell0Label.setText(dispyData.getCell0());
            this.cell1Label.setText(dispyData.getCell1());
            this.cell2Label.setText(dispyData.getCell2());
            this.cell3Label.setText(dispyData.getCell3());
            this.playLabel.setText(dispyData.getPlayTime());
            this.play0Label.setText(dispyData.getPlay0());
            this.play1Label.setText(dispyData.getPlay1());
            this.play2Label.setText(dispyData.getPlay2());
            this.play3Label.setText(dispyData.getPlay3());
            this.play0Label.setTooltip(this.makeTooltip(dispyData.getPlayItem0()));
            this.play1Label.setTooltip(this.makeTooltip(dispyData.getPlayItem1()));
            this.play2Label.setTooltip(this.makeTooltip(dispyData.getPlayItem2()));
            this.play3Label.setTooltip(this.makeTooltip(dispyData.getPlayItem3()));
            this.reserve0Label.setText(dispyData.getReserve(0));
            this.reserve1Label.setText(dispyData.getReserve(1));
            this.reserve2Label.setText(dispyData.getReserve(2));
            this.reserve3Label.setText(dispyData.getReserve(3));
            this.reserve00Label.setText(dispyData.getReserveItem(0, 0));
            this.reserve01Label.setText(dispyData.getReserveItem(1, 0));
            this.reserve02Label.setText(dispyData.getReserveItem(2, 0));
            this.reserve03Label.setText(dispyData.getReserveItem(3, 0));
            this.reserve10Label.setText(dispyData.getReserveItem(0, 1));
            this.reserve11Label.setText(dispyData.getReserveItem(1, 1));
            this.reserve12Label.setText(dispyData.getReserveItem(2, 1));
            this.reserve13Label.setText(dispyData.getReserveItem(3, 1));
            this.reserve20Label.setText(dispyData.getReserveItem(0, 2));
            this.reserve21Label.setText(dispyData.getReserveItem(1, 2));
            this.reserve22Label.setText(dispyData.getReserveItem(2, 2));
            this.reserve23Label.setText(dispyData.getReserveItem(3, 2));
            this.reserve30Label.setText(dispyData.getReserveItem(0, 3));
            this.reserve31Label.setText(dispyData.getReserveItem(1, 3));
            this.reserve32Label.setText(dispyData.getReserveItem(2, 3));
            this.reserve33Label.setText(dispyData.getReserveItem(3, 3));
            this.reserve00Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(0, 0)));
            this.reserve01Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(1, 0)));
            this.reserve02Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(2, 0)));
            this.reserve03Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(3, 0)));
            this.reserve10Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(0, 1)));
            this.reserve11Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(1, 1)));
            this.reserve12Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(2, 1)));
            this.reserve13Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(3, 1)));
            this.reserve20Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(0, 2)));
            this.reserve21Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(1, 2)));
            this.reserve22Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(2, 2)));
            this.reserve23Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(3, 2)));
            this.reserve30Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(0, 3)));
            this.reserve31Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(1, 3)));
            this.reserve32Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(2, 3)));
            this.reserve33Label.setTooltip(this.makeTooltip(dispyData.getReserveItemItem(3, 3)));
            this.bgmLabel.setText(dispyData.getBgm());
            this.bgmLabel.setTooltip(this.makeTooltip(dispyData.getBgmItem()));
        } else {
            this.idLabel.setText("");
            this.nameLabel.setText("");
            this.versionLabel.setText("");
            this.infoLabel.setText("");
            this.infoLabel.setTooltip(null);
            this.specLabel.setText("");
            this.freeLabel.setText("");
            this.stateLabel.setText("");
            this.stateLabel.setStyle("-fx-background-color: transparent;");
            this.ipLabel.setText("");
            this.touchLabel.setText("");
            this.touchLabel.setTooltip(null);
            this.licenseLabel.setText("");
            this.licenseLabel.setStyle("-fx-background-color: transparent;");
            this.renewButton.setStyle("");
            CanvasDrawer.drawCanvas(this.cellCanvas, 0, null, 0, 0);
            this.sizeLabel.setText("");
            this.typeLabel.setText("");
            this.cell0Label.setText("");
            this.cell1Label.setText("");
            this.cell2Label.setText("");
            this.cell3Label.setText("");
            this.playLabel.setText("");
            this.play0Label.setText("");
            this.play1Label.setText("");
            this.play2Label.setText("");
            this.play3Label.setText("");
            this.play0Label.setTooltip(null);
            this.play1Label.setTooltip(null);
            this.play2Label.setTooltip(null);
            this.play3Label.setTooltip(null);
            this.reserve0Label.setText("");
            this.reserve1Label.setText("");
            this.reserve2Label.setText("");
            this.reserve3Label.setText("");
            this.reserve00Label.setText("");
            this.reserve01Label.setText("");
            this.reserve02Label.setText("");
            this.reserve03Label.setText("");
            this.reserve10Label.setText("");
            this.reserve11Label.setText("");
            this.reserve12Label.setText("");
            this.reserve13Label.setText("");
            this.reserve20Label.setText("");
            this.reserve21Label.setText("");
            this.reserve22Label.setText("");
            this.reserve23Label.setText("");
            this.reserve30Label.setText("");
            this.reserve31Label.setText("");
            this.reserve32Label.setText("");
            this.reserve33Label.setText("");
            this.reserve00Label.setTooltip(null);
            this.reserve01Label.setTooltip(null);
            this.reserve02Label.setTooltip(null);
            this.reserve03Label.setTooltip(null);
            this.reserve10Label.setTooltip(null);
            this.reserve11Label.setTooltip(null);
            this.reserve12Label.setTooltip(null);
            this.reserve13Label.setTooltip(null);
            this.reserve20Label.setTooltip(null);
            this.reserve21Label.setTooltip(null);
            this.reserve22Label.setTooltip(null);
            this.reserve23Label.setTooltip(null);
            this.reserve30Label.setTooltip(null);
            this.reserve31Label.setTooltip(null);
            this.reserve32Label.setTooltip(null);
            this.reserve33Label.setTooltip(null);
            this.bgmLabel.setText("");
            this.bgmLabel.setTooltip(null);
        }
        this.cell0Label.getGraphic().setVisible(this.cell0Label.getText().length() > 0);
        this.cell1Label.getGraphic().setVisible(this.cell1Label.getText().length() > 0);
        this.cell2Label.getGraphic().setVisible(this.cell2Label.getText().length() > 0);
        this.cell3Label.getGraphic().setVisible(this.cell3Label.getText().length() > 0);
        boolean bl = dispyData == null;
        int n2 = n = dispyData == null || !dispyData.getOnoffBoolean() ? 1 : 0;
        if (dispyData != null && dispyData.getId().charAt(0) == 'u') {
            this.screenButton.setDisable(false);
            this.screenButton.setUserData((Object)"");
        } else {
            this.screenButton.setDisable(n != 0 || dispyData.getVersionInt() < 33);
            this.screenButton.setUserData(null);
        }
        this.cellButton.setDisable(bl);
        this.snapButton.setDisable(bl);
        this.playButton.setDisable(bl);
        this.reserve0Button.setDisable(bl);
        this.reserve1Button.setDisable(bl);
        this.reserve2Button.setDisable(bl);
        this.reserve3Button.setDisable(bl);
        this.replace01Button.setDisable(bl);
        this.replace12Button.setDisable(bl);
        this.replace23Button.setDisable(bl);
        this.replace02Button.setDisable(bl);
        this.replace03Button.setDisable(bl);
        this.replace13Button.setDisable(bl);
        this.replaceButton.setDisable(bl);
        this.replacePane.setVisible(false);
        this.scheduleButton.setDisable(bl);
        this.bgmButton.setDisable(bl || dispyData.getVersionInt() < 77);
        this.snapButton.setDisable(n != 0);
        this.screencapButton.setDisable(n != 0 || dispyData.getVersionInt() < 71);
        this.logcatButton.setDisable(n != 0 || !dispyData.getId().startsWith("w") && dispyData.getVersionInt() < 68 || dispyData.getId().startsWith("w") && dispyData.getVersionInt() < 53);
        this.masterButton.setDisable(n != 0 || !dispyData.getId().startsWith("w") && dispyData.getVersionInt() < 73 || dispyData.getId().startsWith("w") && dispyData.getVersionInt() < 53);
        this.slaveButton.setDisable(n != 0 || !dispyData.getId().startsWith("w") && dispyData.getVersionInt() < 73 || dispyData.getId().startsWith("w") && dispyData.getVersionInt() < 53);
        this.unionButton.setDisable(n != 0 || dispyData.getVersionInt() < 75);
        this.licenseButton.setVisible(this.user != null && this.user.name.startsWith("admin") && dispyData != null);
        this.screencapButton.setVisible(this.user != null && this.user.name.equals("admin") && dispyData != null);
        this.logcatButton.setVisible(this.user != null && this.user.name.equals("admin") && dispyData != null);
        this.masterButton.setVisible(this.user != null && this.user.name.equals("admin") && dispyData != null);
        this.slaveButton.setVisible(this.user != null && this.user.name.equals("admin") && dispyData != null);
        this.unionButton.setVisible(this.user != null && this.user.name.equals("admin") && dispyData != null);
        this.progressBox.setVisible(false);
        this.shutter.setVisible(false);
        if (this.mainApp != null) {
            this.mainApp.setSelected(dispyData);
        }
    }

    public void showShutter(boolean bl) {
        this.shutter.setVisible(bl);
    }

    public void setRenewUrl(String string) {
        if (string == null || this.user.name.startsWith("admin")) {
            this.renewButton.setVisible(false);
        } else {
            this.renewButton.setVisible(!this.licenseButton.isVisible());
        }
        this.renewButton.setUserData((Object)string);
    }

    public void updateProgress(String string, int n, int n2, double d) {
        boolean bl = d >= 0.0 && n <= n2;
        this.progressBox.setVisible(bl);
        if (bl) {
            this.progressBar.setProgress(d * 0.01);
            this.progressLabel.setText(Lang.getString(string, n, n2, (int)d));
        }
    }

    public List<DispyData> getGroupList() {
        return this.sortedData;
    }

    public void groupReset() {
        Platform.runLater(() -> {
            this.dispyGroup.getItems().clear();
            this.dispyGroup.getItems().add((Object)Lang.getString("overview.group.all"));
            this.dispyGroup.getItems().add((Object)Lang.getString("overview.group.none"));
            ArrayList<String> arrayList = new ArrayList<String>();
            for (DispyData object : this.filteredData.getSource()) {
                for (String string : object.group.split(",")) {
                    if (string == null || (string = string.trim()).isEmpty() || arrayList.contains(string)) continue;
                    arrayList.add(string);
                }
            }
            Collections.sort(arrayList);
            for (String string : arrayList) {
                this.dispyGroup.getItems().add((Object)string);
            }
            this.dispyGroup.getSelectionModel().select(0);
        });
    }

    private void groupChanged(String string) {
        if (this.filteredData == null) {
            return;
        }
        this.filteredData.setPredicate(dispyData -> {
            int n = this.dispyGroup.getSelectionModel().getSelectedIndex();
            if (n <= 0) {
                return true;
            }
            if (n == 1) {
                return dispyData.group == null || dispyData.group.isEmpty();
            }
            for (String string2 : dispyData.group.split(",")) {
                if (!string.equals(string2.trim())) continue;
                return true;
            }
            return false;
        });
    }

    @FXML
    private void handleLicense() {
        this.mainApp.showLicenseDialog();
    }

    @FXML
    private void handleRenew() {
        String string = (String)this.renewButton.getUserData();
        if (string != null) {
            this.mainApp.openUrl(string);
        }
    }

    @FXML
    private void handleScreen() {
        if (this.screenButton.getUserData() == null) {
            this.mainApp.showScreenDialog();
        } else {
            this.mainApp.showUnionDialog();
        }
    }

    @FXML
    private void handleCell() {
        this.mainApp.showCellDialog();
    }

    @FXML
    private void handleSnap() {
        this.mainApp.showSnapDialog(false);
    }

    @FXML
    private void handleScreenCap() {
        this.mainApp.showSnapDialog(true);
    }

    @FXML
    private void handleLogcat() {
        this.mainApp.showLogcatDialog();
    }

    @FXML
    private void handleMaster() {
        this.mainApp.showLanMasterDialog();
    }

    @FXML
    private void handleSlave() {
        this.mainApp.showLanSlaveDialog();
    }

    @FXML
    private void handleUnion() {
        this.mainApp.showUnionDialog2();
    }

    @FXML
    private void handlePlay() {
        this.mainApp.showPlaylistDialog();
    }

    @FXML
    private void handleReserve0() {
        this.mainApp.showReserveDialog(0);
    }

    @FXML
    private void handleReserve1() {
        this.mainApp.showReserveDialog(1);
    }

    @FXML
    private void handleReserve2() {
        this.mainApp.showReserveDialog(2);
    }

    @FXML
    private void handleReserve3() {
        this.mainApp.showReserveDialog(3);
    }

    @FXML
    private void handleBGM() {
        this.mainApp.showBgmDialog();
    }

    @FXML
    private void handleReplace01() {
        this.mainApp.replacePlaylist(0, 1);
    }

    @FXML
    private void handleReplace12() {
        this.mainApp.replacePlaylist(1, 2);
    }

    @FXML
    private void handleReplace23() {
        this.mainApp.replacePlaylist(2, 3);
    }

    @FXML
    private void handleReplace02() {
        this.mainApp.replacePlaylist(0, 2);
    }

    @FXML
    private void handleReplace03() {
        this.mainApp.replacePlaylist(0, 3);
    }

    @FXML
    private void handleReplace13() {
        this.mainApp.replacePlaylist(1, 3);
    }

    @FXML
    private void handleReplaceShow() {
        if (!this.replaceButton.isDisable()) {
            this.replacePane.setVisible(true);
        }
    }

    @FXML
    private void handleReplaceHide() {
        this.replacePane.setVisible(false);
    }

    @FXML
    private void handleSchedule() {
        this.mainApp.showScheduleDialog();
    }

    @FXML
    private void handleHistory() {
        this.mainApp.showHistoryDialog((List<DispyData>)this.filteredData);
    }

    private Tooltip makeTooltip(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        Tooltip tooltip = new Tooltip(string);
        try {
            Field field = tooltip.getClass().getDeclaredField("BEHAVIOR");
            field.setAccessible(true);
            Object object = field.get(tooltip);
            Field field2 = object.getClass().getDeclaredField("hideTimer");
            field2.setAccessible(true);
            Timeline timeline = (Timeline)field2.get(object);
            timeline.getKeyFrames().clear();
            timeline.getKeyFrames().add((Object)new KeyFrame(Duration.INDEFINITE, new KeyValue[0]));
        }
        catch (Exception exception) {
            Log.out(exception);
        }
        return tooltip;
    }
}

