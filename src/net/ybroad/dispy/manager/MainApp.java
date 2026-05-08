/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Application
 *  javafx.application.Platform
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.collections.transformation.FilteredList
 *  javafx.concurrent.Task
 *  javafx.event.EventHandler
 *  javafx.fxml.FXMLLoader
 *  javafx.geometry.Rectangle2D
 *  javafx.scene.Node
 *  javafx.scene.Parent
 *  javafx.scene.Scene
 *  javafx.scene.control.Alert
 *  javafx.scene.control.Alert$AlertType
 *  javafx.scene.control.ButtonBar$ButtonData
 *  javafx.scene.control.ButtonType
 *  javafx.scene.control.Label
 *  javafx.scene.image.Image
 *  javafx.scene.layout.AnchorPane
 *  javafx.scene.layout.BorderPane
 *  javafx.stage.DirectoryChooser
 *  javafx.stage.Modality
 *  javafx.stage.Screen
 *  javafx.stage.Stage
 *  javafx.stage.Window
 *  javafx.stage.WindowEvent
 *  org.apache.pdfbox.pdmodel.PDDocument
 *  org.apache.pdfbox.rendering.ImageType
 *  org.apache.pdfbox.rendering.PDFRenderer
 *  org.apache.pdfbox.tools.imageio.ImageIOUtil
 *  org.apache.poi.hslf.usermodel.HSLFSlide
 *  org.apache.poi.hslf.usermodel.HSLFSlideShow
 *  org.apache.poi.xslf.usermodel.XMLSlideShow
 *  org.apache.poi.xslf.usermodel.XSLFSlide
 */
package net.ybroad.dispy.manager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javax.imageio.ImageIO;
import net.ybroad.dispy.lib.IsleData;
import net.ybroad.dispy.lib.Log;
import net.ybroad.dispy.lib.PlayData;
import net.ybroad.dispy.lib.Progress;
import net.ybroad.dispy.lib.UnionData;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.lib.Util;
import net.ybroad.dispy.manager.Server;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.model.HistoryData;
import net.ybroad.dispy.manager.model.IsleServerData;
import net.ybroad.dispy.manager.model.MemberData;
import net.ybroad.dispy.manager.model.PlaylistData;
import net.ybroad.dispy.manager.model.ProgressHolder;
import net.ybroad.dispy.manager.model.ScheduleData;
import net.ybroad.dispy.manager.util.Lang;
import net.ybroad.dispy.manager.view.AddDialogController;
import net.ybroad.dispy.manager.view.BgmDialogController;
import net.ybroad.dispy.manager.view.CaptureDialogController;
import net.ybroad.dispy.manager.view.CellDialogController2;
import net.ybroad.dispy.manager.view.ConvertDialogController;
import net.ybroad.dispy.manager.view.CopyDialogController;
import net.ybroad.dispy.manager.view.DimensionCheckController;
import net.ybroad.dispy.manager.view.DispyOverviewController;
import net.ybroad.dispy.manager.view.DownloadDialogController;
import net.ybroad.dispy.manager.view.HistoryDialogController;
import net.ybroad.dispy.manager.view.IndividualDialogController;
import net.ybroad.dispy.manager.view.IsleDialogController;
import net.ybroad.dispy.manager.view.LicenseDialogController;
import net.ybroad.dispy.manager.view.LogcatDialogController;
import net.ybroad.dispy.manager.view.LoginDialogController;
import net.ybroad.dispy.manager.view.MediainfoDialogController;
import net.ybroad.dispy.manager.view.MemberDialogController;
import net.ybroad.dispy.manager.view.MemberEditDialogController;
import net.ybroad.dispy.manager.view.MessageDialogController;
import net.ybroad.dispy.manager.view.OnOffDialogController;
import net.ybroad.dispy.manager.view.PlaylistDialog2Controller;
import net.ybroad.dispy.manager.view.RegisterDialogController;
import net.ybroad.dispy.manager.view.RenameDialogController;
import net.ybroad.dispy.manager.view.RootLayoutController;
import net.ybroad.dispy.manager.view.ScheduleDialog2Controller;
import net.ybroad.dispy.manager.view.ScheduleDialogController;
import net.ybroad.dispy.manager.view.ScreenDialogController;
import net.ybroad.dispy.manager.view.ShellDialogController;
import net.ybroad.dispy.manager.view.UnionDialog3Controller;
import net.ybroad.dispy.manager.view.UnionDialogController;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

public class MainApp
extends Application {
    private Stage primaryStage;
    private BorderPane rootLayout;
    private Image icon;
    private RootLayoutController rootController;
    private DispyOverviewController overviewController;
    private PlaylistDialog2Controller playController;
    private CaptureDialogController captureController;
    private MediainfoDialogController mediainfoController;
    private DownloadDialogController downloadController;
    private LogcatDialogController logcatController;
    private ShellDialogController shellController;
    private MemberDialogController memberController;
    private HistoryDialogController historyController;
    private UnionDialogController unionController;
    private UnionDialog3Controller union3Controller;
    private List<DispyData> historyRequest;
    private HashMap<String, ProgressHolder> progressMap = new HashMap();
    private ObservableList<DispyData> dispyData = FXCollections.observableArrayList();
    private ReentrantLock dispyDataLock = new ReentrantLock(true);
    private FilteredList<DispyData> filteredData = new FilteredList(this.dispyData, dispyData -> true);
    private DispyData selectedData;
    private ObservableList<MemberData> memberData = FXCollections.observableArrayList();
    private UserData userData = new UserData();
    private Server server;
    private String message = "";

    public static void main(String[] stringArray) {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        MainApp.launch((String[])stringArray);
    }

    public void start(Stage stage) {
        this.primaryStage = stage;
        this.icon = new Image(((Object)((Object)this)).getClass().getResource("/images/icon.png").toExternalForm());
        System.setProperty("prism.lcdtext", "false");
        stage.setOnCloseRequest((EventHandler)new EventHandler<WindowEvent>(){

            public void handle(WindowEvent windowEvent) {
                for (File file2 : new File(System.getProperty("java.io.tmpdir")).listFiles((file, string) -> string.startsWith("dispy"))) {
                    Log.out("delete temp: " + file2.getAbsolutePath());
                    try {
                        if (file2.isDirectory()) {
                            Util.rmdir(file2);
                            continue;
                        }
                        file2.delete();
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                    }
                }
                MainApp.this.serverLogout(false);
                Platform.exit();
                System.exit(0);
            }
        });
        this.initRootLayout();
        this.showDispyOverview();
        this.server = new Server(this);
        new Thread(){

            @Override
            public void run() {
                try {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                Platform.runLater((Runnable)new Runnable(){

                    @Override
                    public void run() {
                        UserData userData = LoginDialogController.loadUser();
                        if (userData != null) {
                            ((MainApp)MainApp.this).userData.name = userData.name;
                            ((MainApp)MainApp.this).userData.pw = userData.pw;
                        }
                        if (!((MainApp)MainApp.this).userData.pw.isEmpty()) {
                            MainApp.this.server.connect(MainApp.this.userData);
                        } else {
                            MainApp.this.showLoginDialog();
                        }
                    }
                });
            }
        }.start();
    }

    public void stop() throws Exception {
        this.serverLogout(false);
        System.exit(0);
    }

    private void initRootLayout() {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/RootLayout.fxml"));
            Lang.setResource(fXMLLoader);
            this.rootLayout = (BorderPane)fXMLLoader.load();
            Scene scene = new Scene((Parent)this.rootLayout);
            Lang.setStyleSheet(scene);
            this.primaryStage.setScene(scene);
            this.primaryStage.setTitle(Lang.getString("root.title") + " (v.1.47." + 36 + ")");
            this.primaryStage.getIcons().add((Object)this.icon);
            this.rootController = (RootLayoutController)fXMLLoader.getController();
            this.rootController.setMainApp(this);
            Rectangle2D rectangle2D = Screen.getPrimary().getVisualBounds();
            this.primaryStage.setX((rectangle2D.getWidth() - this.rootLayout.getPrefWidth()) / 2.0);
            this.primaryStage.setY((rectangle2D.getHeight() - this.rootLayout.getPrefHeight()) / 2.0);
            this.primaryStage.show();
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    private void showDispyOverview() {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/DispyOverview.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            this.rootLayout.setCenter((Node)anchorPane);
            this.overviewController = (DispyOverviewController)fXMLLoader.getController();
            this.overviewController.setMainApp(this);
            this.overviewController.setUserData(this.userData);
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showLoginDialog() {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/LoginDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("login.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            LoginDialogController loginDialogController = (LoginDialogController)fXMLLoader.getController();
            loginDialogController.setDialogStage(stage);
            loginDialogController.setUser(this.userData);
            stage.showAndWait();
            switch (loginDialogController.getClickAction()) {
                case 1: 
                case 2: {
                    this.server.connect(this.userData);
                    break;
                }
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showAddDialog() {
        if (!this.server.isConnected()) {
            return;
        }
        try {
            DispyData dispyData = new DispyData();
            if (this.memberController != null) {
                dispyData.setOwner(this.memberController.getCurrent());
            }
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/AddDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("add.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            AddDialogController addDialogController = (AddDialogController)fXMLLoader.getController();
            addDialogController.setDialogStage(stage);
            addDialogController.setClient(dispyData);
            stage.showAndWait();
            if (addDialogController.isClickOk()) {
                this.server.sendAdd(dispyData);
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showAddUnionDialog() {
        if (!this.server.isConnected()) {
            return;
        }
        try {
            DispyData dispyData = new DispyData();
            if (this.memberController != null) {
                dispyData.setOwner(this.memberController.getCurrent());
            }
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/AddDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("root.union"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            AddDialogController addDialogController = (AddDialogController)fXMLLoader.getController();
            addDialogController.setDialogStage(stage);
            addDialogController.setClient(dispyData);
            addDialogController.setUnion(true);
            stage.showAndWait();
            if (addDialogController.isClickOk()) {
                this.server.sendAdd(dispyData);
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showDropDialog() {
        if (this.selectedData != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle(Lang.getString("drop.title"));
            alert.setHeaderText(Lang.getString("drop.header"));
            alert.setContentText(Lang.getString("drop.content", this.selectedData.getId(), this.selectedData.getName()));
            Scene scene = alert.getDialogPane().getScene();
            ((Stage)scene.getWindow()).getIcons().add((Object)this.icon);
            Lang.setStyleSheet(scene);
            Optional optional = alert.showAndWait();
            if (optional.get() == ButtonType.OK) {
                this.server.sendRemove(this.selectedData);
                this.dispyDataLock.lock();
                this.dispyData.remove((Object)this.selectedData);
                this.dispyDataLock.unlock();
                this.selectedData = null;
                this.rootController.selectionChanged(this.selectedData);
                if (this.memberController != null) {
                    this.dispyDataLock.lock();
                    this.memberController.updateCount(this.dispyData, this.memberData);
                    this.dispyDataLock.unlock();
                }
            }
        }
    }

    public void showDiscardDialog() {
        if (this.selectedData != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle(Lang.getString("discard.title"));
            alert.setHeaderText(Lang.getString("discard.header"));
            alert.setContentText(Lang.getString("discard.content", this.selectedData.getId(), this.selectedData.getName()));
            Scene scene = alert.getDialogPane().getScene();
            ((Stage)scene.getWindow()).getIcons().add((Object)this.icon);
            Lang.setStyleSheet(scene);
            Optional optional = alert.showAndWait();
            if (optional.get() == ButtonType.OK) {
                this.server.sendDiscard(this.selectedData);
                this.dispyDataLock.lock();
                this.dispyData.remove((Object)this.selectedData);
                this.dispyDataLock.unlock();
                this.selectedData = null;
                if (this.memberController != null) {
                    this.dispyDataLock.lock();
                    this.memberController.updateCount(this.dispyData, this.memberData);
                    this.dispyDataLock.unlock();
                }
            }
        }
    }

    public void showRenameDialog() {
        if (this.selectedData == null) {
            return;
        }
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/RenameDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("rename.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            RenameDialogController renameDialogController = (RenameDialogController)fXMLLoader.getController();
            renameDialogController.setDialogStage(stage);
            renameDialogController.setClient(this.selectedData);
            renameDialogController.setAllowRegister(this.userData.name.startsWith("admin"));
            stage.showAndWait();
            if (renameDialogController.isClickOk()) {
                this.server.sendRename(this.selectedData);
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void setSelected(DispyData dispyData) {
        ProgressHolder progressHolder;
        this.selectedData = dispyData;
        this.rootController.selectionChanged(this.selectedData);
        if (this.selectedData != null && (progressHolder = this.progressMap.get(dispyData.getId())) != null) {
            this.overviewController.updateProgress(progressHolder.type, progressHolder.current, progressHolder.count, progressHolder.value);
        }
    }

    public void updateOverview(DispyData dispyData) {
        this.dispyDataLock.lock();
        DispyData dispyData2 = null;
        for (DispyData dispyData3 : this.dispyData) {
            if (dispyData3 == dispyData) {
                dispyData2 = dispyData3;
                break;
            }
            if (!Objects.equals(dispyData3.getId(), dispyData.getId())) continue;
            dispyData2 = dispyData3;
            dispyData3.copyFrom(dispyData);
            break;
        }
        if (dispyData2 == null) {
            this.dispyData.add((Object)dispyData);
        } else if (dispyData2 == this.selectedData) {
            this.overviewController.showDispyDetails(this.selectedData);
            this.overviewController.groupReset();
            if (!this.selectedData.getOnoffBoolean()) {
                this.progressMap.remove(this.selectedData.getId());
            }
        }
        if (this.memberController != null) {
            this.memberController.updateCount(this.dispyData, this.memberData);
        }
        if (this.unionController != null) {
            this.unionController.updateData(dispyData);
        }
        this.dispyDataLock.unlock();
    }

    public DispyData getDispyData(String string) {
        this.dispyDataLock.lock();
        for (DispyData dispyData : this.dispyData) {
            if (!Objects.equals(dispyData.getId(), string)) continue;
            this.dispyDataLock.unlock();
            return dispyData;
        }
        this.dispyDataLock.unlock();
        return null;
    }

    public void serverLogout(boolean bl) {
        this.server.disconnect();
        if (bl) {
            this.userData.pw = "";
            LoginDialogController.saveUser(this.userData);
        }
    }

    public void showRegisterDialog() {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/RegisterDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("member.register"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            RegisterDialogController registerDialogController = (RegisterDialogController)fXMLLoader.getController();
            registerDialogController.setDialogStage(stage);
            registerDialogController.setUser(new UserData());
            stage.showAndWait();
            switch (registerDialogController.getClickAction()) {
                case 2: {
                    this.server.sendRegister(registerDialogController.getUser());
                    break;
                }
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showMemberDialog(UserData userData) {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/MemberDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.setTitle(Lang.getString("member.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            this.memberController = (MemberDialogController)fXMLLoader.getController();
            this.memberController.setMainApp(this);
            this.memberController.setDialogStage(stage);
            this.memberController.setUser(userData);
            this.memberController.setMember(this.memberData);
            this.dispyDataLock.lock();
            this.memberController.updateCount(this.dispyData, this.memberData);
            this.dispyDataLock.unlock();
            stage.show();
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showMemberEditDialog(UserData userData) {
        if (!this.userData.name.startsWith("admin")) {
            return;
        }
        if (!this.userData.name.equals("admin") && userData.name.startsWith("admin") && !this.userData.name.equals(userData.name)) {
            return;
        }
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/MemberEditDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("member.edit.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            MemberEditDialogController memberEditDialogController = (MemberEditDialogController)fXMLLoader.getController();
            memberEditDialogController.setDialogStage(stage);
            memberEditDialogController.setUser(userData);
            stage.showAndWait();
            switch (memberEditDialogController.getClickAction()) {
                case 1: 
                case 2: {
                    this.memberData.clear();
                    this.dispyDataLock.lock();
                    this.dispyData.clear();
                    this.dispyDataLock.unlock();
                    this.selectedData = null;
                    this.setFilter(null);
                    this.rootController.selectionChanged(this.selectedData);
                    this.memberController.updateCount(this.dispyData, this.memberData);
                    this.server.sendRemember(userData.name, memberEditDialogController.getUser());
                    break;
                }
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showIsleDialog(IsleServerData isleServerData) {
        if (!this.userData.name.startsWith("admin")) {
            return;
        }
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/IsleDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(isleServerData == null ? Lang.getString("isle.new") : Lang.getString("isle.fix"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            IsleDialogController isleDialogController = (IsleDialogController)fXMLLoader.getController();
            isleDialogController.setDialogStage(stage);
            isleDialogController.setIsleServer(isleServerData);
            isleDialogController.setRemovable(this.userData.name.equals("admin"));
            stage.showAndWait();
            IsleData isleData = isleDialogController.getIsle();
            if (isleData != null) {
                this.memberData.clear();
                this.rootController.selectionChanged(this.selectedData);
                this.memberController.updateCount(this.dispyData, this.memberData);
                this.server.sendIsle(isleData);
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showDisconnectDialog(String string) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(Lang.getString("disconnect.title"));
        alert.setHeaderText(Lang.getString("disconnect.header"));
        alert.setContentText(Lang.getString(string));
        Scene scene = alert.getDialogPane().getScene();
        ((Stage)scene.getWindow()).getIcons().add((Object)this.icon);
        Lang.setStyleSheet(scene);
        alert.showAndWait();
    }

    public ObservableList<DispyData> getDispyData() {
        return this.filteredData;
    }

    public void connectionChanged(boolean bl, UserData userData, String string) {
        this.rootController.connectionChanged(bl, userData);
        if (!bl) {
            this.dispyDataLock.lock();
            this.dispyData.clear();
            this.dispyDataLock.unlock();
            this.selectedData = null;
            this.setFilter(null);
            if (this.playController != null) {
                this.playController.close();
                this.playController = null;
            }
            if (this.captureController != null) {
                this.captureController.close();
                this.captureController = null;
            }
            if (this.mediainfoController != null) {
                this.mediainfoController.close();
                this.mediainfoController = null;
            }
            if (this.downloadController != null) {
                this.downloadController.close();
                this.downloadController = null;
            }
            if (this.logcatController != null) {
                this.logcatController.close();
                this.logcatController = null;
            }
            if (this.shellController != null) {
                this.shellController.close();
                this.shellController = null;
            }
            if (this.memberController != null) {
                this.memberController.close();
                this.memberController = null;
                this.memberData.clear();
            }
            if (this.historyController != null) {
                this.historyController.close();
                this.historyController = null;
                this.historyRequest = null;
            }
            if (this.unionController != null) {
                this.unionController.close();
                this.unionController = null;
                this.unionController = null;
            }
            if (this.union3Controller != null) {
                this.union3Controller.close();
                this.union3Controller = null;
                this.union3Controller = null;
            }
            this.progressMap.clear();
        } else if (this.memberController == null && (userData.name.startsWith("admin") || userData.name.endsWith("#"))) {
            this.showMemberDialog(userData);
        }
        this.overviewController.showShutter(!bl);
        this.overviewController.setRenewUrl(string);
        Rectangle2D rectangle2D = Screen.getPrimary().getVisualBounds();
        if (this.memberController != null) {
            this.memberController.setPosition(rectangle2D, this.primaryStage);
        } else {
            this.primaryStage.setX((rectangle2D.getWidth() - this.rootLayout.getPrefWidth()) / 2.0);
            this.primaryStage.setY((rectangle2D.getHeight() - this.rootLayout.getPrefHeight()) / 2.0);
        }
    }

    public void showLicenseDialog() {
        if (this.selectedData == null) {
            return;
        }
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/LicenseDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("license.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            LicenseDialogController licenseDialogController = (LicenseDialogController)fXMLLoader.getController();
            licenseDialogController.setDialogStage(stage);
            stage.showAndWait();
            long l = licenseDialogController.getLimit();
            if (l > 0L) {
                this.server.sendLicenseLimit(this.selectedData, l);
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showScreenDialog() {
        if (this.selectedData == null) {
            return;
        }
        try {
            String string = this.selectedData.getVersionInt() < 47 ? null : this.selectedData.clockcolor;
            String string2 = this.selectedData.getVersionInt() < 40 ? null : this.selectedData.clockpos;
            int n = this.selectedData.getVersionInt() < 73 ? -1 : this.selectedData.rotate;
            FXMLLoader fXMLLoader = new FXMLLoader();
            if (this.selectedData.getVersionInt() >= 103 && this.userData.name.startsWith("admin")) {
                fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/ScreenDialog2.fxml"));
            } else {
                fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/ScreenDialog.fxml"));
            }
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("screen.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            ScreenDialogController screenDialogController = (ScreenDialogController)fXMLLoader.getController();
            screenDialogController.setDialogStage(stage, this);
            screenDialogController.setLogo(this.selectedData.logooriginal);
            screenDialogController.setBackground(this.selectedData.bgoriginal);
            screenDialogController.setClock(this.selectedData.clock, string, string2, n);
            screenDialogController.setNum(this.selectedData.numW, this.selectedData.numH);
            screenDialogController.setOnOffShow(this.selectedData.getVersionInt() >= 102);
            stage.showAndWait();
            if (screenDialogController.isOkClicked()) {
                File file = null;
                String string3 = screenDialogController.getLogo();
                if (!string3.equals(this.selectedData.logooriginal)) {
                    file = new File(string3);
                }
                File file2 = null;
                String string4 = screenDialogController.getBackground();
                if (!string4.equals(this.selectedData.bgoriginal)) {
                    file2 = new File(string4);
                }
                string = this.selectedData.getVersionInt() < 47 ? null : screenDialogController.getClockColor();
                string2 = this.selectedData.getVersionInt() < 40 ? null : screenDialogController.getClockPos();
                int n2 = n = this.selectedData.getVersionInt() < 73 ? -1 : screenDialogController.getRotate();
                if (this.selectedData.getOnoffBoolean()) {
                    this.server.sendScreen(this.selectedData, file, file2, screenDialogController.getClock(), string, string2, screenDialogController.getNumW(), screenDialogController.getNumH(), n);
                }
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showOnOffDialog() {
        if (this.selectedData == null) {
            return;
        }
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/OnOffDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("onoff.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            OnOffDialogController onOffDialogController = (OnOffDialogController)fXMLLoader.getController();
            onOffDialogController.setDialogStage(stage);
            onOffDialogController.setOnOff(this.selectedData.on, this.selectedData.off);
            stage.showAndWait();
            if (onOffDialogController.isOkClicked()) {
                this.server.sendOnOff(this.selectedData, onOffDialogController.getOn(), onOffDialogController.getOff());
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showUnionDialog() {
        if (this.selectedData == null || !this.selectedData.getId().startsWith("u")) {
            return;
        }
        try {
            DispyData dispyData2;
            ObservableList observableList = FXCollections.observableArrayList();
            for (DispyData dispyData2 : this.overviewController.getGroupList()) {
                if (dispyData2.getId().startsWith("w") || dispyData2.getId().startsWith("u")) continue;
                observableList.add((Object)dispyData2);
            }
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/UnionDialog.fxml"));
            Lang.setResource(fXMLLoader);
            dispyData2 = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("union.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)dispyData2);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            this.unionController = (UnionDialogController)fXMLLoader.getController();
            this.unionController.setDialogStage(stage);
            this.unionController.setData(this.selectedData, (ObservableList<DispyData>)observableList);
            stage.showAndWait();
            if (this.unionController.isClickOk()) {
                this.server.sendUnion(this.unionController.getResult());
            }
            this.unionController = null;
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showUnionDialog2() {
        if (this.selectedData == null) {
            return;
        }
        if (!this.selectedData.getId().startsWith("u")) {
            try {
                FXMLLoader fXMLLoader = new FXMLLoader();
                fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/UnionDialog3.fxml"));
                Lang.setResource(fXMLLoader);
                AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Union Client Setting");
                stage.getIcons().add((Object)this.icon);
                stage.initOwner((Window)this.primaryStage);
                stage.setResizable(false);
                stage.sizeToScene();
                Scene scene = new Scene((Parent)anchorPane);
                Lang.setStyleSheet(scene);
                stage.setScene(scene);
                this.union3Controller = (UnionDialog3Controller)fXMLLoader.getController();
                this.union3Controller.setDialogStage(stage);
                this.union3Controller.setData(this.selectedData);
                stage.showAndWait();
                if (this.union3Controller.isClickOk()) {
                    this.server.sendUnionClient(this.union3Controller.getResult());
                }
                this.union3Controller = null;
            }
            catch (IOException iOException) {
                Log.out(iOException);
            }
        }
    }

    public void showCellDialog() {
        if (this.selectedData == null) {
            return;
        }
        try {
            int n = this.selectedData.getTypeInt();
            int[] nArray = (int[])this.selectedData.getCellInt().clone();
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/CellDialog2.fxml"));
            Lang.setResource(fXMLLoader);
            BorderPane borderPane = (BorderPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("cell.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)borderPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            CellDialogController2 cellDialogController2 = (CellDialogController2)fXMLLoader.getController();
            cellDialogController2.setDialogStage(stage);
            cellDialogController2.setVersion(this.selectedData.getVersionInt());
            cellDialogController2.setScreen(this.selectedData.getWidth(), this.selectedData.getHeight());
            cellDialogController2.setCell(n, nArray);
            stage.showAndWait();
            if (cellDialogController2.isOkClicked()) {
                this.server.sendCell(this.selectedData, cellDialogController2.getType(), cellDialogController2.getSize());
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showSnapDialog(boolean bl) {
        if (this.selectedData == null || !this.selectedData.getOnoffBoolean()) {
            return;
        }
        try {
            int n;
            int n2;
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/CaptureDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("capture.title", this.selectedData.getName()));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            this.captureController = (CaptureDialogController)fXMLLoader.getController();
            this.captureController.setDialogStage(stage);
            int n3 = 2;
            if (this.selectedData.getVersionInt() < 40) {
                n3 = 4;
            }
            if (this.selectedData.numW > this.selectedData.numH) {
                n2 = this.selectedData.width / n3;
                n = this.selectedData.height * this.selectedData.numH / this.selectedData.numW / n3;
            } else if (this.selectedData.numW < this.selectedData.numH) {
                n2 = this.selectedData.width * this.selectedData.numW / this.selectedData.numH / n3;
                n = this.selectedData.height / n3;
            } else {
                n2 = this.selectedData.width / n3;
                n = this.selectedData.height / n3;
            }
            Rectangle2D rectangle2D = Screen.getPrimary().getVisualBounds();
            while ((double)n2 > rectangle2D.getWidth() || (double)n > rectangle2D.getHeight()) {
                n2 /= 2;
                n /= 2;
            }
            this.captureController.setImageSize(n2, n);
            this.captureController.setDispyId(this.selectedData.getId());
            if (bl) {
                this.server.sendScreenCap(this.selectedData);
            } else {
                this.server.sendSnapshot(this.selectedData);
            }
            stage.showAndWait();
            this.captureController = null;
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showSnapImage(String string, File file) {
        if (this.captureController != null) {
            if (string.equals(this.captureController.getDispyId())) {
                this.captureController.setSnapImage(file);
            } else {
                this.captureController.close();
                this.captureController = null;
            }
        }
    }

    public void showMediainfoDialog(String string, String string2) {
        this.server.sendMediainfo(string, string2);
    }

    public void showMediainfo(String string, String string2, String string3) {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/MediainfoDialog.fxml"));
            Lang.setResource(fXMLLoader);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("mediainfo.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)fXMLLoader.load());
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            this.mediainfoController = (MediainfoDialogController)fXMLLoader.getController();
            this.mediainfoController.setDialogStage(stage, this);
            this.mediainfoController.setInfo(string, string2, string3);
            this.mediainfoController.setShowDownload(this.userData.name.startsWith("admin"));
            stage.showAndWait();
            this.mediainfoController = null;
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showDownloadDialog(String string, String string2, File file) {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/DownloadDialog.fxml"));
            Lang.setResource(fXMLLoader);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("download.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)fXMLLoader.load());
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            this.downloadController = (DownloadDialogController)fXMLLoader.getController();
            this.downloadController.setDialogStage(stage);
            this.downloadController.setFile(file);
            this.server.sendDownload(string, string2, file);
            stage.showAndWait();
            this.downloadController = null;
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showDownload(String string, double d) {
        if (this.downloadController != null) {
            this.downloadController.setPercent(string, d);
        }
    }

    public void showLogcatDialog() {
        if (this.selectedData == null || !this.selectedData.getOnoffBoolean()) {
            return;
        }
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/LogcatDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("logcat.title", this.selectedData.getName()));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            this.logcatController = (LogcatDialogController)fXMLLoader.getController();
            this.logcatController.setDialogStage(stage);
            this.logcatController.setDispyId(this.selectedData.getId());
            this.server.sendLogcat(this.selectedData);
            stage.showAndWait();
            this.logcatController = null;
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showLanMasterDialog() {
        if (this.selectedData == null || !this.selectedData.getOnoffBoolean()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(Lang.getString("lanmaster.title"));
        alert.setHeaderText(this.selectedData.getName());
        alert.setContentText(Lang.getString("lanmaster.content"));
        ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add((Object)this.icon);
        Lang.setStyleSheet(alert.getDialogPane().getScene());
        ButtonType buttonType = new ButtonType(Lang.getString("lanmaster.set"));
        ButtonType buttonType2 = new ButtonType(Lang.getString("lanmaster.unset"));
        ButtonType buttonType3 = new ButtonType(Lang.getString("app.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll((Object[])new ButtonType[]{buttonType, buttonType2, buttonType3});
        Optional optional = alert.showAndWait();
        if (optional.get() == buttonType) {
            this.server.sendLanMaster(this.selectedData.getId(), true);
        } else if (optional.get() == buttonType2) {
            this.server.sendLanMaster(this.selectedData.getId(), false);
        }
    }

    public void showLanSlaveDialog() {
        if (this.selectedData == null || !this.selectedData.getOnoffBoolean()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(Lang.getString("lanslave.title"));
        alert.setHeaderText(this.selectedData.getName());
        alert.setContentText(Lang.getString("lanslave.content"));
        ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add((Object)this.icon);
        Lang.setStyleSheet(alert.getDialogPane().getScene());
        ButtonType buttonType = new ButtonType(Lang.getString("lanslave.set"));
        ButtonType buttonType2 = new ButtonType(Lang.getString("lanslave.unset"));
        ButtonType buttonType3 = new ButtonType(Lang.getString("app.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll((Object[])new ButtonType[]{buttonType, buttonType2, buttonType3});
        Optional optional = alert.showAndWait();
        if (optional.get() == buttonType) {
            this.server.sendLanSlave(this.selectedData.getId(), true);
        } else if (optional.get() == buttonType2) {
            this.server.sendLanSlave(this.selectedData.getId(), false);
        }
    }

    public void showLogcat(String string, String[] stringArray) {
        if (this.logcatController != null) {
            if (string.equals(this.logcatController.getDispyId())) {
                this.logcatController.setLogs(stringArray);
            } else {
                this.logcatController.close();
                this.logcatController = null;
            }
        }
    }

    public void showShellDialog(boolean bl) {
        if (!(bl || this.selectedData != null && this.selectedData.getOnoffBoolean())) {
            return;
        }
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/ShellDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("shell.title", bl ? "*** SERVER ***" : this.selectedData.getName()));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            this.shellController = (ShellDialogController)fXMLLoader.getController();
            this.shellController.setDialogStage(stage, this);
            this.shellController.setDispyId(bl ? null : this.selectedData.getId());
            stage.showAndWait();
            this.shellController = null;
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showShellResult(String string, String string2) {
        if (this.shellController != null) {
            if (string.equals(this.shellController.getDispyId())) {
                this.shellController.setResult(string2);
            } else {
                this.shellController.close();
                this.shellController = null;
            }
        }
    }

    public void sendShell(String string, String string2) {
        this.server.sendShell(string, string2);
    }

    public void showPlaylistDialog() {
        block12: {
            if (this.selectedData == null) {
                return;
            }
            try {
                Object object;
                FXMLLoader fXMLLoader = new FXMLLoader();
                fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/PlaylistDialog2.fxml"));
                Lang.setResource(fXMLLoader);
                AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle(Lang.getString("play.title"));
                stage.getIcons().add((Object)this.icon);
                stage.initOwner((Window)this.primaryStage);
                stage.setResizable(false);
                stage.sizeToScene();
                Scene scene = new Scene((Parent)anchorPane);
                Lang.setStyleSheet(scene);
                stage.setScene(scene);
                this.playController = (PlaylistDialog2Controller)fXMLLoader.getController();
                this.playController.setDialogStage(stage, this);
                this.playController.setUserData(this.userData);
                this.playController.setDispyData(this.selectedData);
                this.playController.setPlaylist(-1, (Image)null);
                stage.showAndWait();
                ArrayList<ObservableList<PlaylistData>> arrayList = this.playController.getPlaylist();
                if (arrayList == null) break block12;
                ArrayList<ArrayList<PlaylistData>> arrayList2 = new ArrayList<ArrayList<PlaylistData>>();
                if (arrayList.isEmpty()) break block12;
                boolean bl = false;
                boolean bl2 = false;
                boolean bl3 = false;
                ArrayList<PlaylistData> arrayList3 = new ArrayList<PlaylistData>();
                for (ObservableList<PlaylistData> observableList : arrayList) {
                    ArrayList<PlaylistData> arrayList4 = new ArrayList<PlaylistData>((Collection<PlaylistData>)observableList);
                    for (PlayData playData : arrayList4) {
                        if (playData.type.equals("video") && playData.changed) continue;
                        if (playData.type.equals("pdf") && playData.changed) {
                            bl2 = true;
                            arrayList3.add((PlaylistData)playData);
                            continue;
                        }
                        if (!playData.type.equals("ppt") || !playData.changed) continue;
                        bl3 = true;
                        arrayList3.add((PlaylistData)playData);
                    }
                    arrayList2.add(arrayList4);
                }
                if (!arrayList3.isEmpty()) {
                    try {
                        fXMLLoader = new FXMLLoader();
                        fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/ConvertDialog.fxml"));
                        Lang.setResource(fXMLLoader);
                        anchorPane = (AnchorPane)fXMLLoader.load();
                        stage = new Stage();
                        stage.initModality(Modality.APPLICATION_MODAL);
                        stage.setTitle(Lang.getString("convert.title"));
                        stage.getIcons().add((Object)this.icon);
                        stage.initOwner((Window)this.primaryStage);
                        stage.setResizable(false);
                        stage.sizeToScene();
                        scene = new Scene((Parent)anchorPane);
                        Lang.setStyleSheet(scene);
                        stage.setScene(scene);
                        object = (ConvertDialogController)fXMLLoader.getController();
                        ((ConvertDialogController)object).setDialogStage(stage);
                        ((ConvertDialogController)object).setTargetList(this.selectedData.getId().startsWith("w"), arrayList3);
                        stage.showAndWait();
                        if (!((ConvertDialogController)object).getResult()) {
                            return;
                        }
                        arrayList3.clear();
                        arrayList3.addAll((Collection<PlaylistData>)((ConvertDialogController)object).getCheckedList());
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                    }
                }
                try {
                    object = this.selectedData.getId();
                    final boolean bl4 = bl;
                    final boolean bl5 = bl2;
                    final boolean bl6 = bl3;
                    Task<Boolean> task = new Task<Boolean>((String)object, arrayList3, arrayList2){
                        final /* synthetic */ String val$id;
                        final /* synthetic */ ArrayList val$files;
                        final /* synthetic */ List val$sendList;
                        {
                            this.val$id = string;
                            this.val$files = arrayList;
                            this.val$sendList = list;
                        }

                        protected Boolean call() throws Exception {
                            if (bl4 || bl5 || bl6) {
                                Progress progress = new Progress(){

                                    @Override
                                    public void update(final double d) {
                                        Platform.runLater((Runnable)new Runnable(){

                                            @Override
                                            public void run() {
                                                if (MainApp.this.selectedData != null && MainApp.this.selectedData.getId().equals(val$id)) {
                                                    MainApp.this.overviewController.updateProgress(type, current, count, d);
                                                }
                                                MainApp.this.progressMap.put(val$id, new ProgressHolder(type, current, count, d));
                                            }
                                        });
                                    }
                                };
                                progress.set("progress.convert", this.val$files.size());
                                for (List list : this.val$sendList) {
                                    if (bl4) {
                                        MainApp.this._mp4h265(list, this.val$files, progress);
                                    }
                                    if (bl5) {
                                        MainApp.this._pdf2image(list, progress);
                                    }
                                    if (!bl6) continue;
                                    MainApp.this._ppt2image(list, progress);
                                }
                                progress.update(-1.0);
                            }
                            MainApp.this.server.sendPlaylistGroup(MainApp.this.selectedData, this.val$sendList, new Progress(){

                                @Override
                                public void update(final double d) {
                                    Platform.runLater((Runnable)new Runnable(){

                                        @Override
                                        public void run() {
                                            if (MainApp.this.selectedData != null && MainApp.this.selectedData.getId().equals(val$id)) {
                                                MainApp.this.overviewController.updateProgress(type, current, count, d);
                                            }
                                            MainApp.this.progressMap.put(val$id, new ProgressHolder(type, current, count, d));
                                        }
                                    });
                                }
                            });
                            return true;
                        }
                    };
                    new Thread((Runnable)task).start();
                }
                catch (Exception exception) {
                    Log.out(exception);
                }
            }
            catch (IOException iOException) {
                Log.out(iOException);
            }
        }
    }

    public void showReserveDialog(int n) {
        block12: {
            if (this.selectedData == null) {
                return;
            }
            try {
                Object object;
                FXMLLoader fXMLLoader = new FXMLLoader();
                fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/PlaylistDialog2.fxml"));
                Lang.setResource(fXMLLoader);
                AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle(Lang.getString("reserve.title", n + 1, this.selectedData.getReserve(n)));
                stage.getIcons().add((Object)this.icon);
                stage.initOwner((Window)this.primaryStage);
                stage.setResizable(false);
                stage.sizeToScene();
                Scene scene = new Scene((Parent)anchorPane);
                Lang.setStyleSheet(scene);
                stage.setScene(scene);
                this.playController = (PlaylistDialog2Controller)fXMLLoader.getController();
                this.playController.setDialogStage(stage, this);
                this.playController.setUserData(this.userData);
                this.playController.setDispyData(this.selectedData);
                this.playController.setPlaylist(n, this.icon);
                stage.showAndWait();
                ArrayList<ObservableList<PlaylistData>> arrayList = this.playController.getPlaylist();
                long l = this.playController.getReserveTime();
                if (arrayList == null || l <= 0L) break block12;
                ArrayList<ArrayList<PlaylistData>> arrayList2 = new ArrayList<ArrayList<PlaylistData>>();
                if (arrayList.isEmpty()) break block12;
                boolean bl = false;
                boolean bl2 = false;
                boolean bl3 = false;
                ArrayList<PlaylistData> arrayList3 = new ArrayList<PlaylistData>();
                for (ObservableList<PlaylistData> observableList : arrayList) {
                    ArrayList<PlaylistData> arrayList4 = new ArrayList<PlaylistData>((Collection<PlaylistData>)observableList);
                    for (PlayData playData : arrayList4) {
                        if (playData.type.equals("video") && playData.changed) continue;
                        if (playData.type.equals("pdf") && playData.changed) {
                            bl2 = true;
                            arrayList3.add((PlaylistData)playData);
                            continue;
                        }
                        if (!playData.type.equals("ppt") || !playData.changed) continue;
                        bl3 = true;
                        arrayList3.add((PlaylistData)playData);
                    }
                    arrayList2.add(arrayList4);
                }
                if (!arrayList3.isEmpty()) {
                    try {
                        fXMLLoader = new FXMLLoader();
                        fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/ConvertDialog.fxml"));
                        Lang.setResource(fXMLLoader);
                        anchorPane = (AnchorPane)fXMLLoader.load();
                        stage = new Stage();
                        stage.initModality(Modality.APPLICATION_MODAL);
                        stage.setTitle(Lang.getString("convert.title"));
                        stage.getIcons().add((Object)this.icon);
                        stage.initOwner((Window)this.primaryStage);
                        stage.setResizable(false);
                        stage.sizeToScene();
                        scene = new Scene((Parent)anchorPane);
                        Lang.setStyleSheet(scene);
                        stage.setScene(scene);
                        object = (ConvertDialogController)fXMLLoader.getController();
                        ((ConvertDialogController)object).setDialogStage(stage);
                        ((ConvertDialogController)object).setTargetList(this.selectedData.getId().startsWith("w"), arrayList3);
                        stage.showAndWait();
                        if (!((ConvertDialogController)object).getResult()) {
                            return;
                        }
                        arrayList3.clear();
                        arrayList3.addAll((Collection<PlaylistData>)((ConvertDialogController)object).getCheckedList());
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                    }
                }
                try {
                    object = this.selectedData.getId();
                    final boolean bl4 = bl;
                    final boolean bl5 = bl2;
                    final boolean bl6 = bl3;
                    Task<Boolean> task = new Task<Boolean>((String)object, arrayList3, arrayList2, n, l){
                        final /* synthetic */ String val$id;
                        final /* synthetic */ ArrayList val$files;
                        final /* synthetic */ List val$sendList;
                        final /* synthetic */ int val$index;
                        final /* synthetic */ long val$time;
                        {
                            this.val$id = string;
                            this.val$files = arrayList;
                            this.val$sendList = list;
                            this.val$index = n;
                            this.val$time = l;
                        }

                        protected Boolean call() throws Exception {
                            if (bl4 || bl5 || bl6) {
                                Progress progress = new Progress(){

                                    @Override
                                    public void update(final double d) {
                                        Platform.runLater((Runnable)new Runnable(){

                                            @Override
                                            public void run() {
                                                if (MainApp.this.selectedData != null && MainApp.this.selectedData.getId().equals(val$id)) {
                                                    MainApp.this.overviewController.updateProgress(type, current, count, d);
                                                }
                                                MainApp.this.progressMap.put(val$id, new ProgressHolder(type, current, count, d));
                                            }
                                        });
                                    }
                                };
                                progress.set("progress.convert", this.val$files.size());
                                for (List list : this.val$sendList) {
                                    if (bl4) {
                                        MainApp.this._mp4h265(list, this.val$files, progress);
                                    }
                                    if (bl5) {
                                        MainApp.this._pdf2image(list, progress);
                                    }
                                    if (!bl6) continue;
                                    MainApp.this._ppt2image(list, progress);
                                }
                                progress.update(-1.0);
                            }
                            MainApp.this.server.sendReservelistGroup(MainApp.this.selectedData, this.val$index, this.val$time, this.val$sendList, new Progress(){

                                @Override
                                public void update(final double d) {
                                    Platform.runLater((Runnable)new Runnable(){

                                        @Override
                                        public void run() {
                                            if (MainApp.this.selectedData != null && MainApp.this.selectedData.getId().equals(val$id)) {
                                                MainApp.this.overviewController.updateProgress(type, current, count, d);
                                            }
                                            MainApp.this.progressMap.put(val$id, new ProgressHolder(type, current, count, d));
                                        }
                                    });
                                }
                            });
                            return true;
                        }
                    };
                    new Thread((Runnable)task).start();
                }
                catch (Exception exception) {
                    Log.out(exception);
                }
            }
            catch (IOException iOException) {
                Log.out(iOException);
            }
        }
    }

    private double _h265check(File file) {
        try {
            String string;
            boolean bl = false;
            boolean bl2 = false;
            boolean bl3 = false;
            double d = -1.0;
            String string2 = "ffmpeg.exe -i \"" + file.getAbsolutePath() + "\"";
            Log.out("h265check: " + string2);
            Process process = Runtime.getRuntime().exec(new String[]{"ffmpeg.exe", "-i", file.getAbsolutePath()});
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((string = bufferedReader.readLine()) != null) {
                if (string.contains("Stream #")) {
                    if (string.contains("Video: hevc")) {
                        bl = true;
                        continue;
                    }
                    if (!string.contains("Audio:")) continue;
                    bl3 = true;
                    if (!string.contains("aac")) continue;
                    bl2 = true;
                    continue;
                }
                if (!string.contains("Duration:")) continue;
                int n = string.indexOf("Duration:");
                int n2 = string.indexOf(",");
                String[] stringArray = string.substring(n + "Duration: ".length(), n2).split(":");
                d = 0.0;
                d += (double)(Integer.parseInt(stringArray[0]) * 3600);
                d += (double)(Integer.parseInt(stringArray[1]) * 60);
                d += Double.parseDouble(stringArray[2]);
            }
            bufferedReader.close();
            if (bl && (!bl3 || bl2) && file.getName().toLowerCase().endsWith(".mp4")) {
                return 0.0;
            }
            return d;
        }
        catch (Exception exception) {
            Log.out(exception);
            return -2.0;
        }
    }

    private void _mp4h265(List<PlayData> list, ArrayList<PlaylistData> arrayList, Progress progress) {
        try {
            File file = Files.createTempDirectory("dispy", new FileAttribute[0]).toFile();
            for (int i = list.size() - 1; i >= 0; --i) {
                PlayData playData = list.get(i);
                if (!playData.type.equals("video") || !playData.changed || !arrayList.contains(playData) || playData.getExpandedData() != null) continue;
                double d = this._h265check(playData.file);
                Log.out("h265check: " + d);
                if (d > 0.0) {
                    String string;
                    File file2 = new File(file, playData.file.getName() + ".mp4");
                    String string2 = "ffmpeg.exe -i \"" + playData.file.getAbsolutePath() + "\" -c:v libx265 -c:a aac -b:a 128k -ac 2 -y \"" + file2.getAbsolutePath() + "\"";
                    Log.out("h265encode: " + string2);
                    Process process = Runtime.getRuntime().exec(new String[]{"ffmpeg.exe", "-i", playData.file.getAbsolutePath(), "-c:v", "libx265", "-c:a", "aac", "-b:a", "128k", "-ac", "2", "-y", file2.getAbsolutePath()});
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    while ((string = bufferedReader.readLine()) != null) {
                        if (!string.contains("time=")) continue;
                        int n = string.indexOf("time=");
                        int n2 = string.indexOf(" ", n);
                        String[] stringArray = string.substring(n + "time=".length(), n2).split(":");
                        double d2 = 0.0;
                        d2 += (double)(Integer.parseInt(stringArray[0]) * 3600);
                        d2 += (double)(Integer.parseInt(stringArray[1]) * 60);
                        progress.update(100.0 * (d2 += Double.parseDouble(stringArray[2])) / d);
                    }
                    playData.alternative = file2;
                    file2.deleteOnExit();
                } else if (d < 0.0) {
                    list.remove(i);
                }
                progress.next();
            }
            file.deleteOnExit();
        }
        catch (Exception exception) {
            Log.out(exception);
        }
    }

    private void _pdf2image(List<PlayData> list, Progress progress) {
        try {
            File file = Files.createTempDirectory("dispy", new FileAttribute[0]).toFile();
            for (int i = list.size() - 1; i >= 0; --i) {
                int n;
                File[] fileArray;
                PlayData playData = list.get(i);
                if (!playData.type.equals("pdf") || !playData.changed || playData.getExpandedData() != null) continue;
                final String string = playData.file.getName() + "_";
                String string2 = new File(file, string).getAbsolutePath();
                PDDocument pDDocument = null;
                try {
                    pDDocument = PDDocument.load((File)playData.file);
                    fileArray = new PDFRenderer(pDDocument);
                    n = pDDocument.getNumberOfPages();
                    for (int j = 0; j < n; ++j) {
                        Log.out("pdf: " + j + "/" + n);
                        try {
                            BufferedImage bufferedImage = fileArray.renderImageWithDPI(j, 240.0f, ImageType.RGB);
                            float f = (float)Math.max(bufferedImage.getWidth(), bufferedImage.getHeight()) / 1920.0f;
                            if (f > 1.0f) {
                                bufferedImage = fileArray.renderImageWithDPI(j, 240.0f / f, ImageType.RGB);
                            }
                            ImageIOUtil.writeImage((BufferedImage)bufferedImage, (String)(string2 + (j + 1) + ".jpg"), (int)240);
                        }
                        catch (Exception exception) {
                            Log.out(exception);
                            InputStream inputStream = ((Object)((Object)this)).getClass().getResourceAsStream("/images/error_pdf.png");
                            Files.copy(inputStream, Paths.get(string2 + (j + 1) + ".jpg", new String[0]), new CopyOption[0]);
                        }
                        progress.update(100.0 * (double)(j + 1) / (double)n);
                    }
                    pDDocument.close();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    if (pDDocument != null) {
                        pDDocument.close();
                    }
                    list.remove(i);
                    progress.next();
                    continue;
                }
                fileArray = file.listFiles(new FilenameFilter(){

                    @Override
                    public boolean accept(File file, String string2) {
                        return string2.startsWith(string);
                    }
                });
                for (n = fileArray.length; n > 0; --n) {
                    PlayData playData2 = new PlayData();
                    playData2.type = "image";
                    playData2.show[0] = playData.show[0];
                    playData2.show[1] = playData.show[1];
                    playData2.show[2] = playData.show[2];
                    playData2.show[3] = playData.show[3];
                    playData2.file = new File(string2 + n + ".jpg");
                    playData2.time = playData.time;
                    playData2.changed = playData.changed;
                    list.add(i + 1, playData2);
                    playData2.file.deleteOnExit();
                }
                playData.font = fileArray.length;
                progress.next();
            }
            file.deleteOnExit();
        }
        catch (Exception exception) {
            Log.out(exception);
        }
    }

    private void _ppt2image(List<PlayData> list, Progress progress) {
        try {
            File file = Files.createTempDirectory("dispy", new FileAttribute[0]).toFile();
            for (int i = list.size() - 1; i >= 0; --i) {
                Object object;
                Object object2;
                Object object3;
                Object object4;
                Object object5;
                Object object6;
                Object object7;
                PlayData playData = list.get(i);
                if (!playData.type.equals("ppt") || !playData.changed || playData.getExpandedData() != null) continue;
                String string = playData.file.getName() + "_";
                String string2 = new File(file, string).getAbsolutePath();
                int n = 2;
                int n2 = 0;
                XMLSlideShow xMLSlideShow = null;
                HSLFSlideShow hSLFSlideShow = null;
                try {
                    FileInputStream fileInputStream = new FileInputStream(playData.file);
                    xMLSlideShow = new XMLSlideShow((InputStream)fileInputStream);
                    fileInputStream.close();
                    object7 = xMLSlideShow.getPageSize();
                    ((Dimension)object7).width *= n;
                    ((Dimension)object7).height *= n;
                    object6 = xMLSlideShow.getSlides();
                    n2 = 0;
                    object5 = object6.iterator();
                    while (object5.hasNext()) {
                        object4 = (XSLFSlide)object5.next();
                        Log.out("ppt: " + n2);
                        ++n2;
                        try {
                            object3 = new BufferedImage(((Dimension)object7).width, ((Dimension)object7).height, 1);
                            object2 = ((BufferedImage)object3).createGraphics();
                            ((Graphics2D)object2).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            ((Graphics2D)object2).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                            ((Graphics2D)object2).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            ((Graphics2D)object2).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                            ((Graphics2D)object2).setPaint(Color.white);
                            ((Graphics)object2).clearRect(0, 0, ((Dimension)object7).width, ((Dimension)object7).height);
                            ((Graphics2D)object2).scale(n, n);
                            object4.draw((Graphics2D)object2);
                            object = new FileOutputStream(string2 + n2 + ".png");
                            ImageIO.write((RenderedImage)object3, "png", (OutputStream)object);
                            ((FileOutputStream)object).close();
                        }
                        catch (Exception exception) {
                            Log.out(exception);
                            object2 = ((Object)((Object)this)).getClass().getResourceAsStream("/images/error_ppt.png");
                            Files.copy((InputStream)object2, Paths.get(string2 + n2 + ".jpg", new String[0]), new CopyOption[0]);
                        }
                        progress.update(100.0 * (double)n2 / (double)object6.size());
                    }
                    xMLSlideShow.close();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    if (xMLSlideShow != null) {
                        xMLSlideShow.close();
                    }
                    try {
                        object7 = new FileInputStream(playData.file);
                        hSLFSlideShow = new HSLFSlideShow((InputStream)object7);
                        ((FileInputStream)object7).close();
                        object6 = hSLFSlideShow.getPageSize();
                        ((Dimension)object6).width *= n;
                        ((Dimension)object6).height *= n;
                        object5 = hSLFSlideShow.getSlides();
                        n2 = 0;
                        object4 = object5.iterator();
                        while (object4.hasNext()) {
                            object3 = (HSLFSlide)object4.next();
                            ++n2;
                            object2 = new BufferedImage(((Dimension)object6).width, ((Dimension)object6).height, 1);
                            object = ((BufferedImage)object2).createGraphics();
                            ((Graphics2D)object).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            ((Graphics2D)object).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                            ((Graphics2D)object).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            ((Graphics2D)object).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                            ((Graphics2D)object).setPaint(Color.white);
                            ((Graphics)object).clearRect(0, 0, ((Dimension)object6).width, ((Dimension)object6).height);
                            ((Graphics2D)object).scale(n, n);
                            object3.draw((Graphics2D)object);
                            FileOutputStream fileOutputStream = new FileOutputStream(string2 + n2 + ".png");
                            ImageIO.write((RenderedImage)object2, "png", fileOutputStream);
                            fileOutputStream.close();
                            progress.update(100.0 * (double)n2 / (double)object5.size());
                        }
                        hSLFSlideShow.close();
                    }
                    catch (Exception exception2) {
                        Log.out(exception2);
                        if (hSLFSlideShow != null) {
                            hSLFSlideShow.close();
                        }
                        list.remove(i);
                        progress.next();
                        continue;
                    }
                }
                for (int j = n2; j > 0; --j) {
                    object7 = new PlayData();
                    ((PlayData)object7).type = "image";
                    ((PlayData)object7).show[0] = playData.show[0];
                    ((PlayData)object7).show[1] = playData.show[1];
                    ((PlayData)object7).show[2] = playData.show[2];
                    ((PlayData)object7).show[3] = playData.show[3];
                    ((PlayData)object7).file = new File(string2 + j + ".png");
                    ((PlayData)object7).time = playData.time;
                    ((PlayData)object7).changed = playData.changed;
                    list.add(i + 1, (PlayData)object7);
                    ((PlayData)object7).file.deleteOnExit();
                }
                playData.font = n2;
                progress.next();
            }
            file.deleteOnExit();
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showBgmDialog() {
        if (this.selectedData == null) {
            return;
        }
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/BgmDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("bgm.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            BgmDialogController bgmDialogController = (BgmDialogController)fXMLLoader.getController();
            bgmDialogController.setDialogStage(stage);
            bgmDialogController.setUserData(this.userData);
            bgmDialogController.setDispyData(this.selectedData);
            stage.showAndWait();
            final ArrayList<PlayData> arrayList = bgmDialogController.getPlaylist();
            if (arrayList != null) {
                final String string = this.selectedData.getId();
                new Thread(){

                    @Override
                    public void run() {
                        MainApp.this.server.sendBgmlist(MainApp.this.selectedData, arrayList, new Progress(){

                            @Override
                            public void update(final double d) {
                                Platform.runLater((Runnable)new Runnable(){

                                    @Override
                                    public void run() {
                                        if (MainApp.this.selectedData != null && MainApp.this.selectedData.getId().equals(string)) {
                                            MainApp.this.overviewController.updateProgress(type, current, count, d);
                                        }
                                        MainApp.this.progressMap.put(string, new ProgressHolder(type, current, count, d));
                                    }
                                });
                            }
                        });
                    }
                }.start();
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public PlaylistData showIndivisualDialog(Stage stage, PlaylistData playlistData) {
        try {
            DispyData dispyData2;
            UnionData unionData2;
            ObservableList observableList = FXCollections.observableArrayList();
            block2: for (UnionData unionData2 : this.selectedData.union) {
                for (DispyData dispyData2 : this.overviewController.getGroupList()) {
                    if (!Objects.equals(dispyData2.getId(), unionData2.id)) continue;
                    observableList.add((Object)dispyData2);
                    continue block2;
                }
            }
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/IndividualDialog.fxml"));
            Lang.setResource(fXMLLoader);
            unionData2 = (AnchorPane)fXMLLoader.load();
            Stage stage2 = new Stage();
            stage2.initModality(Modality.APPLICATION_MODAL);
            stage2.setTitle(Lang.getString("play.type.individual"));
            stage2.getIcons().add((Object)this.icon);
            stage2.initOwner((Window)stage);
            stage2.setResizable(false);
            stage2.sizeToScene();
            dispyData2 = new Scene((Parent)unionData2);
            Lang.setStyleSheet((Scene)dispyData2);
            stage2.setScene((Scene)dispyData2);
            IndividualDialogController individualDialogController = (IndividualDialogController)fXMLLoader.getController();
            individualDialogController.setDialogStage(stage2, this);
            individualDialogController.setUserData(this.userData);
            individualDialogController.setDispyData(this.selectedData);
            individualDialogController.setUnionData((ObservableList<DispyData>)observableList);
            individualDialogController.setPlaylistData(playlistData);
            stage2.showAndWait();
            playlistData = individualDialogController.getPlaylist();
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
        return playlistData;
    }

    public boolean showDimensionCheckDialog(Stage stage, File file, int n, int n2) {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/DimensionCheckDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage2 = new Stage();
            stage2.initModality(Modality.APPLICATION_MODAL);
            stage2.setTitle(Lang.getString("dimension.title"));
            stage2.getIcons().add((Object)this.icon);
            stage2.initOwner((Window)stage);
            stage2.setResizable(false);
            stage2.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage2.setScene(scene);
            DimensionCheckController dimensionCheckController = (DimensionCheckController)fXMLLoader.getController();
            dimensionCheckController.setDialogStage(stage2);
            dimensionCheckController.setFileSize(file.getName(), n + " \u00d7 " + n2);
            stage2.showAndWait();
            return dimensionCheckController.getResult();
        }
        catch (IOException iOException) {
            Log.out(iOException);
            return false;
        }
    }

    public void showBitrateCheckDialog(Stage stage, File file, int n, int n2) {
        if (this.selectedData != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle(Lang.getString("bitrate.title"));
            if (n < 0) {
                alert.setHeaderText(Lang.getString("bitrate.fail"));
                alert.setContentText(file.getName());
            } else {
                alert.setHeaderText(Lang.getString("bitrate.over"));
                alert.setContentText(Lang.getString("bitrate.content", file.getName(), n, n2));
            }
            Scene scene = alert.getDialogPane().getScene();
            ((Stage)scene.getWindow()).getIcons().add((Object)this.icon);
            Lang.setStyleSheet(scene);
            alert.showAndWait();
        }
    }

    public void replacePlaylist(int n, int n2) {
        if (this.selectedData != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle(Lang.getString("replace.title"));
            alert.setHeaderText(Lang.getString("replace.header"));
            alert.setContentText(Lang.getString("replace.content", n, n2));
            Scene scene = alert.getDialogPane().getScene();
            ((Stage)scene.getWindow()).getIcons().add((Object)this.icon);
            Lang.setStyleSheet(scene);
            Optional optional = alert.showAndWait();
            if (optional.get() == ButtonType.OK) {
                this.server.sendReplace(this.selectedData, n, n2);
            }
        }
    }

    public void showScheduleDialog() {
        if (this.selectedData == null) {
            return;
        }
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/ScheduleDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("schedule.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            ScheduleDialogController scheduleDialogController = (ScheduleDialogController)fXMLLoader.getController();
            scheduleDialogController.setDialogStage(stage);
            scheduleDialogController.setMainApp(this);
            scheduleDialogController.setData(this.selectedData);
            stage.showAndWait();
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showScheduleDialog2(ObservableList<ScheduleData> observableList, int n) {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/ScheduleDialog2.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("schedule.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            ScheduleDialog2Controller scheduleDialog2Controller = (ScheduleDialog2Controller)fXMLLoader.getController();
            scheduleDialog2Controller.setDialogStage(stage);
            scheduleDialog2Controller.setData(observableList, n);
            stage.showAndWait();
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showHistoryDialog(List<DispyData> list) {
        if (list.isEmpty()) {
            return;
        }
        this.historyRequest = list;
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/HistoryDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("history.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            this.historyController = (HistoryDialogController)fXMLLoader.getController();
            this.historyController.setDialogStage(stage);
            this.historyController.setMainApp(this);
            stage.showAndWait();
            this.historyController = null;
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void requestHistory(String string, String string2) {
        this.server.sendHistory(string, string2, this.historyRequest);
    }

    public void updateHistory(ArrayList<HistoryData> arrayList) {
        if (this.historyController != null) {
            this.historyController.setHistoryData(arrayList);
        }
    }

    public void showCopyDialog() {
        if (this.selectedData == null) {
            return;
        }
        try {
            DispyData dispyData2;
            ObservableList<DispyData> observableList = FXCollections.observableArrayList();
            for (DispyData dispyData2 : this.overviewController.getGroupList()) {
                if (dispyData2.getId().equals(this.selectedData.getId())) continue;
                observableList.add((Object)dispyData2);
            }
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/CopyDialog.fxml"));
            Lang.setResource(fXMLLoader);
            dispyData2 = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("copy.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)dispyData2);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            CopyDialogController copyDialogController = (CopyDialogController)fXMLLoader.getController();
            copyDialogController.setDialogStage(stage);
            copyDialogController.setTargetList(observableList);
            stage.showAndWait();
            observableList = copyDialogController.getCheckedList();
            for (DispyData dispyData3 : observableList) {
                dispyData3.setChecked(false);
            }
            if (!observableList.isEmpty()) {
                this.server.sendCopy(this.selectedData, new ArrayList<DispyData>((Collection<DispyData>)observableList));
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showExportDialog() {
        if (this.selectedData == null) {
            return;
        }
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(Lang.getString("root.export.target"));
        final File file = directoryChooser.showDialog((Window)this.primaryStage);
        if (file != null) {
            Object object;
            Stage stage;
            ArrayList<List<PlaylistData>> arrayList = new ArrayList<List<PlaylistData>>();
            for (int i = 0; i < 4; ++i) {
                arrayList.add(this.selectedData.getPlaylist(i));
            }
            final ArrayList<Stage> arrayList2 = new ArrayList<Stage>();
            boolean bl = false;
            boolean bl2 = false;
            boolean bl3 = false;
            final ArrayList<PlaylistData> arrayList3 = new ArrayList<PlaylistData>();
            for (List object22 : arrayList) {
                stage = new ArrayList(object22);
                for (Object object2 : stage) {
                    ((PlayData)object2).changed = true;
                    if (((PlayData)object2).type.equals("video")) continue;
                    if (((PlayData)object2).type.equals("pdf")) {
                        bl2 = true;
                        arrayList3.add((PlaylistData)object2);
                        continue;
                    }
                    if (!((PlayData)object2).type.equals("ppt")) continue;
                    bl3 = true;
                    arrayList3.add((PlaylistData)object2);
                }
                arrayList2.add(stage);
            }
            if (!arrayList3.isEmpty()) {
                try {
                    Object object2;
                    object = new FXMLLoader();
                    object.setLocation(((Object)((Object)this)).getClass().getResource("view/ConvertDialog.fxml"));
                    Lang.setResource((FXMLLoader)object);
                    AnchorPane anchorPane = (AnchorPane)object.load();
                    stage = new Stage();
                    stage.initModality(Modality.APPLICATION_MODAL);
                    stage.setTitle(Lang.getString("convert.title"));
                    stage.getIcons().add((Object)this.icon);
                    stage.initOwner((Window)this.primaryStage);
                    stage.setResizable(false);
                    stage.sizeToScene();
                    Scene scene = new Scene((Parent)anchorPane);
                    Lang.setStyleSheet(scene);
                    stage.setScene(scene);
                    object2 = (ConvertDialogController)object.getController();
                    ((ConvertDialogController)object2).setDialogStage(stage);
                    ((ConvertDialogController)object2).setTargetList(this.selectedData.getId().startsWith("w"), arrayList3);
                    stage.showAndWait();
                    if (!((ConvertDialogController)object2).getResult()) {
                        return;
                    }
                    arrayList3.clear();
                    arrayList3.addAll((Collection<PlaylistData>)((ConvertDialogController)object2).getCheckedList());
                }
                catch (Exception exception) {
                    Log.out(exception);
                }
            }
            try {
                object = new Alert(Alert.AlertType.WARNING);
                object.initModality(Modality.APPLICATION_MODAL);
                object.setTitle(Lang.getString("export.title"));
                object.setHeaderText(file.getAbsolutePath());
                object.setContentText(Lang.getString("wait.message"));
                ((Stage)object.getDialogPane().getScene().getWindow()).getIcons().add((Object)this.icon);
                object.getDialogPane().getScene().getWindow().setOnCloseRequest(arg_0 -> MainApp.lambda$showExportDialog$1((Alert)object, arg_0));
                object.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
                object.show();
                final String string = this.selectedData.getId();
                final boolean bl4 = bl;
                final boolean bl5 = bl2;
                final boolean bl6 = bl3;
                Task<Boolean> task = new Task<Boolean>((Alert)object){
                    final /* synthetic */ Alert val$alert;
                    {
                        this.val$alert = alert;
                    }

                    protected Boolean call() throws Exception {
                        Object object;
                        if (bl4 || bl5 || bl6) {
                            object = new Progress(){

                                @Override
                                public void update(final double d) {
                                    if (MainApp.this.selectedData != null && MainApp.this.selectedData.getId().equals(string)) {
                                        Platform.runLater((Runnable)new Runnable(){

                                            @Override
                                            public void run() {
                                                MainApp.this.overviewController.updateProgress(type, current, count, d);
                                                MainApp.this.progressMap.put(string, new ProgressHolder(type, current, count, d));
                                            }
                                        });
                                    }
                                }
                            };
                            ((Progress)object).set("progress.convert", arrayList3.size());
                            for (List list : arrayList2) {
                                for (PlayData playData : list) {
                                    playData.changed = true;
                                }
                                if (bl4) {
                                    MainApp.this._mp4h265(list, arrayList3, (Progress)object);
                                }
                                if (bl5) {
                                    MainApp.this._pdf2image(list, (Progress)object);
                                }
                                if (!bl6) continue;
                                MainApp.this._ppt2image(list, (Progress)object);
                            }
                            ((Progress)object).update(-1.0);
                        }
                        object = MainApp.this._export(((MainApp)MainApp.this).selectedData.play, arrayList2, file);
                        Platform.runLater(() -> 7.lambda$call$0(this.val$alert, (String)object));
                        return true;
                    }

                    private static /* synthetic */ void lambda$call$0(Alert alert, String string2) {
                        alert.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
                        if (string2 == null) {
                            alert.setAlertType(Alert.AlertType.INFORMATION);
                            alert.setContentText(Lang.getString("export.ok"));
                        } else {
                            alert.setAlertType(Alert.AlertType.ERROR);
                            Label label = new Label(Lang.getString("export.ng") + "\n\t" + string2);
                            label.setStyle("-fx-padding: 10px;");
                            alert.getDialogPane().setContent((Node)label);
                            alert.getDialogPane().setMinHeight(Double.NEGATIVE_INFINITY);
                            alert.getDialogPane().getScene().getWindow().sizeToScene();
                        }
                    }
                };
                new Thread((Runnable)task).start();
            }
            catch (Exception exception) {
                Log.out(exception);
            }
            for (List list : arrayList) {
                for (PlayData playData : list) {
                    playData.changed = false;
                }
            }
        }
    }

    private String _export(String[] stringArray, List<List<PlayData>> list, File file) {
        String string = null;
        File file2 = null;
        try {
            Object object;
            file2 = Files.createTempDirectory("dispy", new FileAttribute[0]).toFile();
            file2.deleteOnExit();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(new File(file2, ".setting.txt")), "UTF-8"));
            bufferedWriter.write(this.selectedData.getTypeInt() + "\n");
            for (int n3 : this.selectedData.getCellInt()) {
                bufferedWriter.write(n3 + "\n");
            }
            int n = 0;
            String[] stringArray2 = stringArray;
            int n2 = stringArray2.length;
            for (int n3 = 0; n3 < n2; ++n3) {
                object = stringArray2[n3];
                if (object == null || ((String)object).isEmpty()) {
                    bufferedWriter.write(n + "\n");
                } else {
                    bufferedWriter.write((String)object + "\n");
                }
                ++n;
            }
            bufferedWriter.close();
            for (n2 = 0; n2 < list.size(); ++n2) {
                File file3 = new File(file2, String.valueOf(n2));
                file3.mkdirs();
                object = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(new File(file3, ".playinfo.txt")), "UTF-8"));
                List<PlayData> list2 = list.get(n2);
                if (list2.isEmpty()) {
                    ((Writer)object).write("\n");
                } else {
                    int n4 = Integer.MAX_VALUE;
                    for (PlayData playData : list2) {
                        int n5;
                        playData.changed = true;
                        int[] nArray = playData.getExpandsCell();
                        if (nArray != null) {
                            ((Writer)object).write("expands\u02fd" + nArray[0] + "\u02fd" + nArray[1] + "\u02fd" + nArray[2] + "\u02fd" + nArray[3] + "\n");
                            n4 = 0;
                        }
                        if ((n5 = playData.getExpandedCell()) >= 0) {
                            int n6 = list.get(n5).indexOf(playData.getExpandedData());
                            ((Writer)object).write("expanded\u02fd" + n5 + "\u02fd" + n6 + "\n");
                            ((Writer)object).write("expanded\u02fd" + n5 + "\u02fd" + (n6 + 1) + "\n");
                            playData = list.get(n5).get(n6 + 1);
                            playData.changed = false;
                        }
                        switch (playData.type) {
                            case "video": 
                            case "image": {
                                if (playData.changed) {
                                    try {
                                        if (playData.alternative != null) {
                                            Util.cp(playData.file, new File(file3, playData.alternative.getName()));
                                        } else {
                                            Util.cp(playData.file, new File(file3, playData.file.getName()));
                                        }
                                    }
                                    catch (IOException iOException) {
                                        return Lang.getString("export.err.file", playData.file.getName());
                                    }
                                }
                                if (n5 >= 0) break;
                                if (playData.alternative != null) {
                                    ((Writer)object).write(playData.type + "\u02fd" + playData.time + "\u02fd" + playData.show[0] + "\u02fd" + playData.show[1] + "\u02fd" + playData.show[2] + "\u02fd" + playData.show[3] + "\u02fd" + playData.open + "\u02fd" + playData.till + "\u02fd" + playData.alternative.getName() + "\n");
                                    break;
                                }
                                ((Writer)object).write(playData.type + "\u02fd" + playData.time + "\u02fd" + playData.show[0] + "\u02fd" + playData.show[1] + "\u02fd" + playData.show[2] + "\u02fd" + playData.show[3] + "\u02fd" + playData.open + "\u02fd" + playData.till + "\u02fd" + playData.file.getName() + "\n");
                                break;
                            }
                            case "pdf": 
                            case "ppt": {
                                if (nArray == null) break;
                                n4 = playData.font;
                                break;
                            }
                            case "text1": 
                            case "text2": 
                            case "web": 
                            case "style1": {
                                if (n5 >= 0) break;
                                ((Writer)object).write(playData.type + "\u02fd" + playData.time + "\u02fd" + playData.font + "\u02fd" + playData.show[0] + "\u02fd" + playData.show[1] + "\u02fd" + playData.show[2] + "\u02fd" + playData.show[3] + "\u02fd" + playData.open + "\u02fd" + playData.till + "\u02fd" + playData.text + "\n");
                                break;
                            }
                            default: {
                                if (!playData.type.startsWith("sync")) break;
                                ((Writer)object).write("sync\u02fd" + playData.type.substring("sync".length()) + "\n");
                            }
                        }
                        if (--n4 >= 0) continue;
                        n4 = Integer.MAX_VALUE;
                        ((Writer)object).write("expands\u02fd0\u02fd0\u02fd0\u02fd0\n");
                    }
                }
                ((BufferedWriter)object).close();
            }
            if (!Util.zipDir(file2, new File(file, "yboard.export"))) {
                string = Lang.getString("export.err.zip");
            }
        }
        catch (Exception exception) {
            Log.out(exception);
            string = exception.getMessage();
        }
        if (file2 != null) {
            try {
                Util.rmdir(file2);
            }
            catch (IOException iOException) {
                Log.out(iOException);
            }
        }
        return string;
    }

    public void showSendMessageDialog() {
        try {
            FXMLLoader fXMLLoader = new FXMLLoader();
            fXMLLoader.setLocation(((Object)((Object)this)).getClass().getResource("view/MessageDialog.fxml"));
            Lang.setResource(fXMLLoader);
            AnchorPane anchorPane = (AnchorPane)fXMLLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(Lang.getString("message.title"));
            stage.getIcons().add((Object)this.icon);
            stage.initOwner((Window)this.primaryStage);
            stage.setResizable(false);
            stage.sizeToScene();
            Scene scene = new Scene((Parent)anchorPane);
            Lang.setStyleSheet(scene);
            stage.setScene(scene);
            MessageDialogController messageDialogController = (MessageDialogController)fXMLLoader.getController();
            messageDialogController.setDialogStage(stage);
            messageDialogController.setText(this.message.replace("\\n", "\n"));
            stage.showAndWait();
            if (messageDialogController.getResult()) {
                this.server.sendMessage(messageDialogController.getLogin(), messageDialogController.getNow(), messageDialogController.getText().replace("\n", "\\n"));
            }
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
    }

    public void showReveivedMessageDialog(String string) {
        this.message = string;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setGraphic(null);
        alert.setTitle(Lang.getString("message.title"));
        alert.setHeaderText(string.replace("\\n", "\n"));
        alert.setContentText(new SimpleDateFormat(Lang.getString("message.datetime")).format(new Date()));
        Scene scene = alert.getDialogPane().getScene();
        ((Stage)scene.getWindow()).getIcons().add((Object)this.icon);
        Lang.setStyleSheet(scene);
        alert.setOnShown(dialogEvent -> {
            Rectangle2D rectangle2D = Screen.getPrimary().getVisualBounds();
            ((Stage)scene.getWindow()).setX((rectangle2D.getWidth() - alert.getDialogPane().getWidth()) / 2.0);
            ((Stage)scene.getWindow()).setY(10.0);
        });
        alert.showAndWait();
    }

    public void clientApk() {
        this.server.sendApk(this.selectedData);
    }

    public void clientCut() {
        this.server.sendCut(this.selectedData);
    }

    public void clientExit() {
        this.server.sendExit(this.selectedData);
    }

    public void clientRestart() {
        this.server.sendRestart(this.selectedData);
    }

    public void clientReboot() {
        this.server.sendReboot(this.selectedData);
    }

    public void setFilter(String string) {
        this.filteredData.setPredicate(dispyData -> {
            if (string == null) {
                return true;
            }
            return dispyData.getOwner().equals(string);
        });
    }

    public void updateMember(String string, String string2, String string3, String string4, String string5) {
        MemberData memberData = null;
        for (MemberData memberData2 : this.memberData) {
            if (!memberData2.getName().equals(string)) continue;
            memberData = memberData2;
            break;
        }
        if (memberData != null) {
            memberData.setOnoff(string2);
            memberData.setTouch(string3);
            memberData.setLimit(string4);
            memberData.memo = string5;
        } else {
            memberData = new MemberData(string, string3, string2, string4, string5);
            this.memberData.add((Object)memberData);
            if (this.memberController != null) {
                this.dispyDataLock.lock();
                this.memberController.updateCount(this.dispyData, this.memberData);
                this.dispyDataLock.unlock();
            }
        }
    }

    public void updateIsle(String string, String string2, String string3, String string4, String string5) {
        IsleServerData isleServerData = null;
        for (MemberData memberData : this.memberData) {
            if (!memberData.getName().equals(string2) || !(memberData instanceof IsleServerData)) continue;
            isleServerData = (IsleServerData)memberData;
            break;
        }
        if (isleServerData != null) {
            isleServerData.setIsle(string, string2, string3, string4, string5);
        } else {
            isleServerData = new IsleServerData(string, string2, string3, string4, string5);
            this.memberData.add((Object)isleServerData);
            if (this.memberController != null) {
                this.dispyDataLock.lock();
                this.memberController.updateCount(this.dispyData, this.memberData);
                this.dispyDataLock.unlock();
            }
        }
    }

    public void updateApp(String string) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(Lang.getString("update.title"));
        alert.setHeaderText(Lang.getString("update.header"));
        alert.setContentText(Lang.getString("update.content"));
        Scene scene = alert.getDialogPane().getScene();
        ((Stage)scene.getWindow()).getIcons().add((Object)this.icon);
        Lang.setStyleSheet(scene);
        alert.showAndWait();
        this.openUrl(string);
        System.exit(0);
    }

    public void updateProgress(String string, String string2, int n, int n2, double d) {
        if (this.selectedData != null && this.selectedData.getId().equals(string)) {
            this.overviewController.updateProgress(string2, n, n2, d);
        }
        this.progressMap.put(string, new ProgressHolder(string2, n, n2, d));
    }

    public void openUrl(String string) {
        try {
            this.getHostServices().showDocument(string);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static /* synthetic */ void lambda$showExportDialog$1(Alert alert, WindowEvent windowEvent) {
        if (alert.getAlertType() == Alert.AlertType.WARNING) {
            windowEvent.consume();
        }
    }
}

