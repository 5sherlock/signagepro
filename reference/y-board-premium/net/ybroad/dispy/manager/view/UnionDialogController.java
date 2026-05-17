/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 *  javafx.collections.ObservableList
 *  javafx.fxml.FXML
 *  javafx.geometry.Pos
 *  javafx.scene.Node
 *  javafx.scene.control.CheckBox
 *  javafx.scene.control.ComboBox
 *  javafx.scene.control.Label
 *  javafx.scene.input.MouseEvent
 *  javafx.scene.layout.GridPane
 *  javafx.scene.text.TextAlignment
 *  javafx.stage.Stage
 *  javafx.util.StringConverter
 */
package net.ybroad.dispy.manager.view;

import java.util.Iterator;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import net.ybroad.dispy.lib.UnionData;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.util.Lang;

public class UnionDialogController {
    @FXML
    private ComboBox<DispyData> devCombo;
    @FXML
    private ComboBox<String> xCombo;
    @FXML
    private ComboBox<String> yCombo;
    @FXML
    private CheckBox masterCheck;
    @FXML
    private GridPane grid;
    @FXML
    private Label summary;
    private Stage dialogStage;
    private DispyData data;
    private ObservableList<DispyData> list;
    private boolean clickOk;

    @FXML
    private void initialize() {
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setData(DispyData dispyData, ObservableList<DispyData> observableList) {
        this.data = new DispyData();
        this.data.copyFrom(dispyData);
        this.data.union.clear();
        for (UnionData unionData : dispyData.union) {
            UnionData unionData2 = new UnionData(unionData);
            DispyData dispyData2 = null;
            for (DispyData dispyData3 : observableList) {
                if (!dispyData3.getId().equals(unionData2.id)) continue;
                dispyData2 = dispyData3;
                break;
            }
            unionData2.w = dispyData2 == null ? 0 : dispyData2.width;
            unionData2.h = dispyData2 == null ? 0 : dispyData2.height;
            this.data.union.add(unionData2);
        }
        this.list = observableList;
        this.devCombo.setItems(observableList);
        this.devCombo.setConverter((StringConverter)new StringConverter<DispyData>(){

            public String toString(DispyData dispyData) {
                return dispyData.getName() + " (" + dispyData.getIpOnly() + ")";
            }

            public DispyData fromString(String string) {
                return null;
            }
        });
        this.xCombo.getItems().addAll((Object[])new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"});
        this.xCombo.getSelectionModel().select(4);
        this.yCombo.getItems().addAll((Object[])new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"});
        this.yCombo.getSelectionModel().select(4);
        Platform.runLater(() -> {
            this.refreshGrid();
            this.refreshSummary();
        });
    }

    public void updateData(DispyData dispyData) {
        if (this.list.contains((Object)dispyData)) {
            this.refreshGrid();
        }
    }

    private void refreshGrid() {
        Object object;
        Object object2;
        Object object3;
        for (UnionData unionData : this.data.union) {
            Object object42;
            object3 = null;
            for (Object object42 : this.list) {
                if (!((DispyData)object42).getId().equals(unionData.id)) continue;
                object3 = object42;
                break;
            }
            if (object3 == null) continue;
            object2 = null;
            object42 = this.grid.getChildren().iterator();
            while (object42.hasNext() && (object2 = (Label)(object = (Node)object42.next())).getUserData() != object3) {
                object2 = null;
            }
            if (object2 == null) {
                object2 = new Label();
                object2.setAlignment(Pos.CENTER);
                object2.setTextAlignment(TextAlignment.CENTER);
                object2.setPrefWidth(this.grid.getWidth() / 10.0 - 1.0);
                object2.setPrefHeight(this.grid.getHeight() / 10.0 - 1.0);
                object2.setUserData(object3);
                object42 = object3;
                object2.setOnMouseClicked(arg_0 -> this.lambda$refreshGrid$1((DispyData)object42, arg_0));
                this.grid.add((Node)object2, unionData.x, unionData.y);
            }
            object2.setText(((DispyData)object3).getName() + "\n" + ((DispyData)object3).getIpOnly() + "\n" + ((DispyData)object3).getSize());
        }
        for (UnionData unionData : this.grid.getChildren()) {
            object3 = (Label)unionData;
            object2 = (DispyData)object3.getUserData();
            boolean bl = false;
            for (UnionData unionData2 : this.data.union) {
                if (!unionData2.id.equals(((DispyData)object2).getId())) continue;
                bl = unionData2.master;
                break;
            }
            object = "";
            object = this.list.contains(object2) ? (((DispyData)object2).getOnoffBoolean() ? (bl ? "-fx-background-color: palegreen; -fx-border-width: 1px; -fx-border-color: black;" : "-fx-background-color: palegreen; -fx-border-width: 0px;") : (bl ? "-fx-background-color: lightpink; -fx-border-width: 1px; -fx-border-color: black;" : "-fx-background-color: lightpink; -fx-border-width: 0px;")) : (bl ? "-fx-background-color: tomato; -fx-border-width: 1px; -fx-border-color: black;" : "-fx-background-color: tomato; -fx-border-width: 0px;");
            object3.setStyle((String)object);
        }
    }

    private void refreshSummary() {
        int n;
        int n2;
        int n3 = 0;
        for (n2 = 0; n2 < 10; ++n2) {
            n = 0;
            for (Object object : this.data.union) {
                if (n2 != ((UnionData)object).x) continue;
                ((UnionData)object).xpos = n3;
                n = Math.max(n, ((UnionData)object).w);
            }
            n3 += n;
        }
        n2 = 0;
        for (n = 0; n < 10; ++n) {
            int n4 = 0;
            for (UnionData unionData : this.data.union) {
                if (n != unionData.y) continue;
                unionData.ypos = n2;
                n4 = Math.max(n4, unionData.h);
            }
            n2 += n4;
        }
        this.data.width = n3;
        this.data.height = n2;
        this.summary.setText(Lang.getString("union.summary", this.data.union.size(), n3, n2));
    }

    private void labelClicked(DispyData dispyData) {
        this.devCombo.getSelectionModel().select((Object)dispyData);
        for (UnionData unionData : this.data.union) {
            if (!unionData.id.equals(dispyData.getId())) continue;
            this.masterCheck.setSelected(unionData.master);
            this.xCombo.getSelectionModel().select(unionData.x);
            this.yCombo.getSelectionModel().select(unionData.y);
            break;
        }
    }

    public boolean isClickOk() {
        return this.clickOk;
    }

    public DispyData getResult() {
        return this.data;
    }

    @FXML
    private void handleAdd() {
        Object object2;
        int n;
        int n2;
        DispyData dispyData = (DispyData)this.devCombo.getValue();
        if (dispyData == null) {
            return;
        }
        try {
            n2 = Integer.parseInt((String)this.xCombo.getValue());
            n = Integer.parseInt((String)this.yCombo.getValue());
        }
        catch (NumberFormatException numberFormatException) {
            return;
        }
        Label label = null;
        Iterator iterator = this.grid.getChildren().iterator();
        while (iterator.hasNext() && (label = (Label)(object2 = (Node)iterator.next())).getUserData() != dispyData) {
            label = null;
        }
        if (label == null) {
            iterator = new UnionData();
            ((UnionData)((Object)iterator)).id = dispyData.getId();
            ((UnionData)((Object)iterator)).master = this.masterCheck.isSelected();
            ((UnionData)((Object)iterator)).x = n2;
            ((UnionData)((Object)iterator)).y = n;
            ((UnionData)((Object)iterator)).w = dispyData.width;
            ((UnionData)((Object)iterator)).h = dispyData.height;
            if (((UnionData)((Object)iterator)).master) {
                for (UnionData unionData : this.data.union) {
                    unionData.master = false;
                }
            }
            this.data.union.add(iterator);
        } else {
            for (Object object2 : this.data.union) {
                if (!((UnionData)object2).id.equals(dispyData.getId())) continue;
                ((UnionData)object2).master = this.masterCheck.isSelected();
                ((UnionData)object2).x = n2;
                ((UnionData)object2).y = n;
                if (!((UnionData)object2).master) break;
                this.data.union.remove(object2);
                for (UnionData unionData : this.data.union) {
                    unionData.master = false;
                }
                this.data.union.add(object2);
                break;
            }
            GridPane.setConstraints((Node)label, (int)n2, (int)n);
        }
        this.refreshGrid();
        this.refreshSummary();
        this.devCombo.getSelectionModel().select(-1);
        this.masterCheck.setSelected(false);
    }

    @FXML
    private void handleRemove() {
        Object object2;
        DispyData dispyData = (DispyData)this.devCombo.getValue();
        if (dispyData == null) {
            return;
        }
        Label label = null;
        Iterator iterator = this.grid.getChildren().iterator();
        while (iterator.hasNext() && (label = (Label)(object2 = (Node)iterator.next())).getUserData() != dispyData) {
            label = null;
        }
        if (label == null) {
            return;
        }
        for (Object object2 : this.data.union) {
            if (!object2.id.equals(dispyData.getId())) continue;
            this.data.union.remove(object2);
            break;
        }
        this.grid.getChildren().remove((Object)label);
        this.refreshSummary();
        this.devCombo.getSelectionModel().select(-1);
        this.masterCheck.setSelected(false);
    }

    @FXML
    private void handleOk() {
        if (this.data.union.isEmpty()) {
            this.clickOk = true;
            this.dialogStage.close();
            return;
        }
        int n = 0;
        for (UnionData unionData : this.data.union) {
            n += unionData.master ? 1 : 0;
            DispyData dispyData = null;
            for (DispyData dispyData2 : this.list) {
                if (!dispyData2.getId().equals(unionData.id)) continue;
                dispyData = dispyData2;
                break;
            }
            if (dispyData != null && dispyData.getOnoffBoolean()) continue;
            return;
        }
        if (n != 1) {
            return;
        }
        this.clickOk = true;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }

    public void close() {
        this.dialogStage.close();
    }

    private /* synthetic */ void lambda$refreshGrid$1(DispyData dispyData, MouseEvent mouseEvent) {
        this.labelClicked(dispyData);
    }
}

