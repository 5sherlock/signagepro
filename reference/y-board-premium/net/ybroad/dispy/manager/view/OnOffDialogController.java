/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.fxml.FXML
 *  javafx.scene.control.CheckBox
 *  javafx.scene.control.Spinner
 *  javafx.scene.control.SpinnerValueFactory
 *  javafx.scene.control.SpinnerValueFactory$IntegerSpinnerValueFactory
 *  javafx.stage.Stage
 */
package net.ybroad.dispy.manager.view;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

public class OnOffDialogController {
    @FXML
    private CheckBox checkOnAll;
    @FXML
    private CheckBox checkOnMon;
    @FXML
    private CheckBox checkOnTue;
    @FXML
    private CheckBox checkOnWed;
    @FXML
    private CheckBox checkOnThu;
    @FXML
    private CheckBox checkOnFri;
    @FXML
    private CheckBox checkOnSat;
    @FXML
    private CheckBox checkOnSun;
    @FXML
    private CheckBox checkOffAll;
    @FXML
    private CheckBox checkOffMon;
    @FXML
    private CheckBox checkOffTue;
    @FXML
    private CheckBox checkOffWed;
    @FXML
    private CheckBox checkOffThu;
    @FXML
    private CheckBox checkOffFri;
    @FXML
    private CheckBox checkOffSat;
    @FXML
    private CheckBox checkOffSun;
    @FXML
    private Spinner<Integer> spinnerHourOnAll;
    @FXML
    private Spinner<Integer> spinnerHourOnMon;
    @FXML
    private Spinner<Integer> spinnerHourOnTue;
    @FXML
    private Spinner<Integer> spinnerHourOnWed;
    @FXML
    private Spinner<Integer> spinnerHourOnThu;
    @FXML
    private Spinner<Integer> spinnerHourOnFri;
    @FXML
    private Spinner<Integer> spinnerHourOnSat;
    @FXML
    private Spinner<Integer> spinnerHourOnSun;
    @FXML
    private Spinner<Integer> spinnerMinOnAll;
    @FXML
    private Spinner<Integer> spinnerMinOnMon;
    @FXML
    private Spinner<Integer> spinnerMinOnTue;
    @FXML
    private Spinner<Integer> spinnerMinOnWed;
    @FXML
    private Spinner<Integer> spinnerMinOnThu;
    @FXML
    private Spinner<Integer> spinnerMinOnFri;
    @FXML
    private Spinner<Integer> spinnerMinOnSat;
    @FXML
    private Spinner<Integer> spinnerMinOnSun;
    @FXML
    private Spinner<Integer> spinnerHourOffAll;
    @FXML
    private Spinner<Integer> spinnerHourOffMon;
    @FXML
    private Spinner<Integer> spinnerHourOffTue;
    @FXML
    private Spinner<Integer> spinnerHourOffWed;
    @FXML
    private Spinner<Integer> spinnerHourOffThu;
    @FXML
    private Spinner<Integer> spinnerHourOffFri;
    @FXML
    private Spinner<Integer> spinnerHourOffSat;
    @FXML
    private Spinner<Integer> spinnerHourOffSun;
    @FXML
    private Spinner<Integer> spinnerMinOffAll;
    @FXML
    private Spinner<Integer> spinnerMinOffMon;
    @FXML
    private Spinner<Integer> spinnerMinOffTue;
    @FXML
    private Spinner<Integer> spinnerMinOffWed;
    @FXML
    private Spinner<Integer> spinnerMinOffThu;
    @FXML
    private Spinner<Integer> spinnerMinOffFri;
    @FXML
    private Spinner<Integer> spinnerMinOffSat;
    @FXML
    private Spinner<Integer> spinnerMinOffSun;
    private Stage dialogStage;
    private boolean ok = false;

    @FXML
    private void initialize() {
        this.spinnerHourOnAll.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        this.spinnerHourOnMon.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        this.spinnerHourOnTue.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        this.spinnerHourOnWed.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        this.spinnerHourOnThu.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        this.spinnerHourOnFri.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        this.spinnerHourOnSat.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        this.spinnerHourOnSun.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9, 1));
        this.spinnerMinOnAll.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOnMon.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOnTue.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOnWed.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOnThu.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOnFri.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOnSat.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOnSun.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerHourOffAll.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18, 1));
        this.spinnerHourOffMon.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18, 1));
        this.spinnerHourOffTue.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18, 1));
        this.spinnerHourOffWed.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18, 1));
        this.spinnerHourOffThu.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18, 1));
        this.spinnerHourOffFri.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18, 1));
        this.spinnerHourOffSat.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18, 1));
        this.spinnerHourOffSun.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18, 1));
        this.spinnerMinOffAll.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOffMon.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOffTue.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOffWed.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOffThu.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOffFri.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOffSat.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.spinnerMinOffSun.setValueFactory((SpinnerValueFactory)new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
        this.checkOnMon.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOnMon.setDisable(bl2 == false);
            this.spinnerMinOnMon.setDisable(bl2 == false);
        });
        this.checkOnTue.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOnTue.setDisable(bl2 == false);
            this.spinnerMinOnTue.setDisable(bl2 == false);
        });
        this.checkOnWed.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOnWed.setDisable(bl2 == false);
            this.spinnerMinOnWed.setDisable(bl2 == false);
        });
        this.checkOnThu.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOnThu.setDisable(bl2 == false);
            this.spinnerMinOnThu.setDisable(bl2 == false);
        });
        this.checkOnFri.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOnFri.setDisable(bl2 == false);
            this.spinnerMinOnFri.setDisable(bl2 == false);
        });
        this.checkOnSat.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOnSat.setDisable(bl2 == false);
            this.spinnerMinOnSat.setDisable(bl2 == false);
        });
        this.checkOnSun.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOnSun.setDisable(bl2 == false);
            this.spinnerMinOnSun.setDisable(bl2 == false);
        });
        this.checkOffMon.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOffMon.setDisable(bl2 == false);
            this.spinnerMinOffMon.setDisable(bl2 == false);
        });
        this.checkOffTue.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOffTue.setDisable(bl2 == false);
            this.spinnerMinOffTue.setDisable(bl2 == false);
        });
        this.checkOffWed.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOffWed.setDisable(bl2 == false);
            this.spinnerMinOffWed.setDisable(bl2 == false);
        });
        this.checkOffThu.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOffThu.setDisable(bl2 == false);
            this.spinnerMinOffThu.setDisable(bl2 == false);
        });
        this.checkOffFri.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOffFri.setDisable(bl2 == false);
            this.spinnerMinOffFri.setDisable(bl2 == false);
        });
        this.checkOffSat.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOffSat.setDisable(bl2 == false);
            this.spinnerMinOffSat.setDisable(bl2 == false);
        });
        this.checkOffSun.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOffSun.setDisable(bl2 == false);
            this.spinnerMinOffSun.setDisable(bl2 == false);
        });
        this.spinnerHourOnAll.valueProperty().addListener((observableValue, n, n2) -> {
            this.spinnerHourOnMon.getValueFactory().setValue(n2);
            this.spinnerHourOnTue.getValueFactory().setValue(n2);
            this.spinnerHourOnWed.getValueFactory().setValue(n2);
            this.spinnerHourOnThu.getValueFactory().setValue(n2);
            this.spinnerHourOnFri.getValueFactory().setValue(n2);
            this.spinnerHourOnSat.getValueFactory().setValue(n2);
            this.spinnerHourOnSun.getValueFactory().setValue(n2);
        });
        this.spinnerMinOnAll.valueProperty().addListener((observableValue, n, n2) -> {
            this.spinnerMinOnMon.getValueFactory().setValue(n2);
            this.spinnerMinOnTue.getValueFactory().setValue(n2);
            this.spinnerMinOnWed.getValueFactory().setValue(n2);
            this.spinnerMinOnThu.getValueFactory().setValue(n2);
            this.spinnerMinOnFri.getValueFactory().setValue(n2);
            this.spinnerMinOnSat.getValueFactory().setValue(n2);
            this.spinnerMinOnSun.getValueFactory().setValue(n2);
        });
        this.spinnerHourOffAll.valueProperty().addListener((observableValue, n, n2) -> {
            this.spinnerHourOffMon.getValueFactory().setValue(n2);
            this.spinnerHourOffTue.getValueFactory().setValue(n2);
            this.spinnerHourOffWed.getValueFactory().setValue(n2);
            this.spinnerHourOffThu.getValueFactory().setValue(n2);
            this.spinnerHourOffFri.getValueFactory().setValue(n2);
            this.spinnerHourOffSat.getValueFactory().setValue(n2);
            this.spinnerHourOffSun.getValueFactory().setValue(n2);
        });
        this.spinnerMinOffAll.valueProperty().addListener((observableValue, n, n2) -> {
            this.spinnerMinOffMon.getValueFactory().setValue(n2);
            this.spinnerMinOffTue.getValueFactory().setValue(n2);
            this.spinnerMinOffWed.getValueFactory().setValue(n2);
            this.spinnerMinOffThu.getValueFactory().setValue(n2);
            this.spinnerMinOffFri.getValueFactory().setValue(n2);
            this.spinnerMinOffSat.getValueFactory().setValue(n2);
            this.spinnerMinOffSun.getValueFactory().setValue(n2);
        });
        this.checkOnAll.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOnAll.setDisable(bl2 == false);
            this.spinnerMinOnAll.setDisable(bl2 == false);
            if (bl2.booleanValue()) {
                this.checkOnMon.setSelected(true);
                this.checkOnTue.setSelected(true);
                this.checkOnWed.setSelected(true);
                this.checkOnThu.setSelected(true);
                this.checkOnFri.setSelected(true);
                this.checkOnSat.setSelected(true);
                this.checkOnSun.setSelected(true);
                int n = (Integer)this.spinnerHourOnAll.getValue();
                this.spinnerHourOnMon.getValueFactory().setValue((Object)n);
                this.spinnerHourOnTue.getValueFactory().setValue((Object)n);
                this.spinnerHourOnWed.getValueFactory().setValue((Object)n);
                this.spinnerHourOnThu.getValueFactory().setValue((Object)n);
                this.spinnerHourOnFri.getValueFactory().setValue((Object)n);
                this.spinnerHourOnSat.getValueFactory().setValue((Object)n);
                this.spinnerHourOnSun.getValueFactory().setValue((Object)n);
                int n2 = (Integer)this.spinnerMinOnAll.getValue();
                this.spinnerMinOnMon.getValueFactory().setValue((Object)n2);
                this.spinnerMinOnTue.getValueFactory().setValue((Object)n2);
                this.spinnerMinOnWed.getValueFactory().setValue((Object)n2);
                this.spinnerMinOnThu.getValueFactory().setValue((Object)n2);
                this.spinnerMinOnFri.getValueFactory().setValue((Object)n2);
                this.spinnerMinOnSat.getValueFactory().setValue((Object)n2);
                this.spinnerMinOnSun.getValueFactory().setValue((Object)n2);
            }
        });
        this.checkOffAll.selectedProperty().addListener((observableValue, bl, bl2) -> {
            this.spinnerHourOffAll.setDisable(bl2 == false);
            this.spinnerMinOffAll.setDisable(bl2 == false);
            if (bl2.booleanValue()) {
                this.checkOffMon.setSelected(true);
                this.checkOffTue.setSelected(true);
                this.checkOffWed.setSelected(true);
                this.checkOffThu.setSelected(true);
                this.checkOffFri.setSelected(true);
                this.checkOffSat.setSelected(true);
                this.checkOffSun.setSelected(true);
                int n = (Integer)this.spinnerHourOffAll.getValue();
                this.spinnerHourOffMon.getValueFactory().setValue((Object)n);
                this.spinnerHourOffTue.getValueFactory().setValue((Object)n);
                this.spinnerHourOffWed.getValueFactory().setValue((Object)n);
                this.spinnerHourOffThu.getValueFactory().setValue((Object)n);
                this.spinnerHourOffFri.getValueFactory().setValue((Object)n);
                this.spinnerHourOffSat.getValueFactory().setValue((Object)n);
                this.spinnerHourOffSun.getValueFactory().setValue((Object)n);
                int n2 = (Integer)this.spinnerMinOffAll.getValue();
                this.spinnerMinOffMon.getValueFactory().setValue((Object)n2);
                this.spinnerMinOffTue.getValueFactory().setValue((Object)n2);
                this.spinnerMinOffWed.getValueFactory().setValue((Object)n2);
                this.spinnerMinOffThu.getValueFactory().setValue((Object)n2);
                this.spinnerMinOffFri.getValueFactory().setValue((Object)n2);
                this.spinnerMinOffSat.getValueFactory().setValue((Object)n2);
                this.spinnerMinOffSun.getValueFactory().setValue((Object)n2);
            }
        });
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setOnOff(int[] nArray, int[] nArray2) {
        if (nArray[0] < 0) {
            this.checkOnMon.setSelected(false);
        } else {
            this.checkOnMon.setSelected(true);
            this.spinnerHourOnMon.getValueFactory().setValue((Object)(nArray[0] / 100));
            this.spinnerMinOnMon.getValueFactory().setValue((Object)(nArray[0] % 100));
        }
        if (nArray[1] < 0) {
            this.checkOnTue.setSelected(false);
        } else {
            this.checkOnTue.setSelected(true);
            this.spinnerHourOnTue.getValueFactory().setValue((Object)(nArray[1] / 100));
            this.spinnerMinOnTue.getValueFactory().setValue((Object)(nArray[1] % 100));
        }
        if (nArray[2] < 0) {
            this.checkOnWed.setSelected(false);
        } else {
            this.checkOnWed.setSelected(true);
            this.spinnerHourOnWed.getValueFactory().setValue((Object)(nArray[2] / 100));
            this.spinnerMinOnWed.getValueFactory().setValue((Object)(nArray[2] % 100));
        }
        if (nArray[3] < 0) {
            this.checkOnThu.setSelected(false);
        } else {
            this.checkOnThu.setSelected(true);
            this.spinnerHourOnThu.getValueFactory().setValue((Object)(nArray[3] / 100));
            this.spinnerMinOnThu.getValueFactory().setValue((Object)(nArray[3] % 100));
        }
        if (nArray[4] < 0) {
            this.checkOnFri.setSelected(false);
        } else {
            this.checkOnFri.setSelected(true);
            this.spinnerHourOnFri.getValueFactory().setValue((Object)(nArray[4] / 100));
            this.spinnerMinOnFri.getValueFactory().setValue((Object)(nArray[4] % 100));
        }
        if (nArray[5] < 0) {
            this.checkOnSat.setSelected(false);
        } else {
            this.checkOnSat.setSelected(true);
            this.spinnerHourOnSat.getValueFactory().setValue((Object)(nArray[5] / 100));
            this.spinnerMinOnSat.getValueFactory().setValue((Object)(nArray[5] % 100));
        }
        if (nArray[6] < 0) {
            this.checkOnSun.setSelected(false);
        } else {
            this.checkOnSun.setSelected(true);
            this.spinnerHourOnSun.getValueFactory().setValue((Object)(nArray[6] / 100));
            this.spinnerMinOnSun.getValueFactory().setValue((Object)(nArray[6] % 100));
        }
        if (nArray2[0] < 0) {
            this.checkOffMon.setSelected(false);
        } else {
            this.checkOffMon.setSelected(true);
            this.spinnerHourOffMon.getValueFactory().setValue((Object)(nArray2[0] / 100));
            this.spinnerMinOffMon.getValueFactory().setValue((Object)(nArray2[0] % 100));
        }
        if (nArray2[1] < 0) {
            this.checkOffTue.setSelected(false);
        } else {
            this.checkOffTue.setSelected(true);
            this.spinnerHourOffTue.getValueFactory().setValue((Object)(nArray2[1] / 100));
            this.spinnerMinOffTue.getValueFactory().setValue((Object)(nArray2[1] % 100));
        }
        if (nArray2[2] < 0) {
            this.checkOffWed.setSelected(false);
        } else {
            this.checkOffWed.setSelected(true);
            this.spinnerHourOffWed.getValueFactory().setValue((Object)(nArray2[2] / 100));
            this.spinnerMinOffWed.getValueFactory().setValue((Object)(nArray2[2] % 100));
        }
        if (nArray2[3] < 0) {
            this.checkOffThu.setSelected(false);
        } else {
            this.checkOffThu.setSelected(true);
            this.spinnerHourOffThu.getValueFactory().setValue((Object)(nArray2[3] / 100));
            this.spinnerMinOffThu.getValueFactory().setValue((Object)(nArray2[3] % 100));
        }
        if (nArray2[4] < 0) {
            this.checkOffFri.setSelected(false);
        } else {
            this.checkOffFri.setSelected(true);
            this.spinnerHourOffFri.getValueFactory().setValue((Object)(nArray2[4] / 100));
            this.spinnerMinOffFri.getValueFactory().setValue((Object)(nArray2[4] % 100));
        }
        if (nArray2[5] < 0) {
            this.checkOffSat.setSelected(false);
        } else {
            this.checkOffSat.setSelected(true);
            this.spinnerHourOffSat.getValueFactory().setValue((Object)(nArray2[5] / 100));
            this.spinnerMinOffSat.getValueFactory().setValue((Object)(nArray2[5] % 100));
        }
        if (nArray2[6] < 0) {
            this.checkOffSun.setSelected(false);
        } else {
            this.checkOffSun.setSelected(true);
            this.spinnerHourOffSun.getValueFactory().setValue((Object)(nArray2[6] / 100));
            this.spinnerMinOffSun.getValueFactory().setValue((Object)(nArray2[6] % 100));
        }
    }

    @FXML
    private void handleOk() {
        this.ok = true;
        this.dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        this.dialogStage.close();
    }

    @FXML
    private void handleNow() {
    }

    public boolean isOkClicked() {
        return this.ok;
    }

    public int[] getOn() {
        int[] nArray = new int[]{!this.checkOnMon.isSelected() ? -1 : (Integer)this.spinnerHourOnMon.getValue() * 100 + (Integer)this.spinnerMinOnMon.getValue(), !this.checkOnTue.isSelected() ? -1 : (Integer)this.spinnerHourOnTue.getValue() * 100 + (Integer)this.spinnerMinOnTue.getValue(), !this.checkOnWed.isSelected() ? -1 : (Integer)this.spinnerHourOnWed.getValue() * 100 + (Integer)this.spinnerMinOnWed.getValue(), !this.checkOnThu.isSelected() ? -1 : (Integer)this.spinnerHourOnThu.getValue() * 100 + (Integer)this.spinnerMinOnThu.getValue(), !this.checkOnFri.isSelected() ? -1 : (Integer)this.spinnerHourOnFri.getValue() * 100 + (Integer)this.spinnerMinOnFri.getValue(), !this.checkOnSat.isSelected() ? -1 : (Integer)this.spinnerHourOnSat.getValue() * 100 + (Integer)this.spinnerMinOnSat.getValue(), !this.checkOnSun.isSelected() ? -1 : (Integer)this.spinnerHourOnSun.getValue() * 100 + (Integer)this.spinnerMinOnSun.getValue()};
        return nArray;
    }

    public int[] getOff() {
        int[] nArray = new int[]{!this.checkOffMon.isSelected() ? -1 : (Integer)this.spinnerHourOffMon.getValue() * 100 + (Integer)this.spinnerMinOffMon.getValue(), !this.checkOffTue.isSelected() ? -1 : (Integer)this.spinnerHourOffTue.getValue() * 100 + (Integer)this.spinnerMinOffTue.getValue(), !this.checkOffWed.isSelected() ? -1 : (Integer)this.spinnerHourOffWed.getValue() * 100 + (Integer)this.spinnerMinOffWed.getValue(), !this.checkOffThu.isSelected() ? -1 : (Integer)this.spinnerHourOffThu.getValue() * 100 + (Integer)this.spinnerMinOffThu.getValue(), !this.checkOffFri.isSelected() ? -1 : (Integer)this.spinnerHourOffFri.getValue() * 100 + (Integer)this.spinnerMinOffFri.getValue(), !this.checkOffSat.isSelected() ? -1 : (Integer)this.spinnerHourOffSat.getValue() * 100 + (Integer)this.spinnerMinOffSat.getValue(), !this.checkOffSun.isSelected() ? -1 : (Integer)this.spinnerHourOffSun.getValue() * 100 + (Integer)this.spinnerMinOffSun.getValue()};
        return nArray;
    }
}

