/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.binding.Bindings
 *  javafx.beans.property.ReadOnlyStringWrapper
 *  javafx.beans.property.SimpleStringProperty
 *  javafx.beans.property.StringProperty
 *  javafx.beans.value.ObservableValue
 */
package net.ybroad.dispy.manager.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.manager.util.Lang;

public class MemberData
extends UserData {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty device = new SimpleStringProperty();
    protected final StringProperty limit = new SimpleStringProperty();
    protected final ReadOnlyStringWrapper devLimit = new ReadOnlyStringWrapper();
    private final StringProperty touch = new SimpleStringProperty();
    protected final StringProperty onoff = new SimpleStringProperty();

    public MemberData() {
        this.devLimit.bind((ObservableValue)Bindings.concat((Object[])new Object[]{this.device, "/", this.limit}));
    }

    public MemberData(String string, String string2, String string3, String string4, String string5) {
        this();
        this.setName(string);
        this.setDevice("0/0");
        this.setTouch(string2);
        this.setOnoff(string3);
        this.setLimit(string4);
        this.memo = string5;
    }

    public String getName() {
        return (String)this.name.get();
    }

    public void setName(String string) {
        ((UserData)this).name = string;
        this.name.set((Object)string);
    }

    public StringProperty nameProperty() {
        return this.name;
    }

    public String getDevice() {
        return (String)this.device.get();
    }

    public void setDevice(String string) {
        this.device.set((Object)string);
    }

    public StringProperty deviceProperty() {
        return this.devLimit;
    }

    public String getLimit() {
        return (String)this.limit.get();
    }

    public int getLimitInt() {
        return ((UserData)this).limit;
    }

    public void setLimit(String string) {
        ((UserData)this).limit = Integer.parseInt(string);
        this.limit.set((Object)(((UserData)this).limit == 0 ? "\u221e" : string));
    }

    public String getTouch() {
        return (String)this.touch.get();
    }

    public void setTouch(String string) {
        if (string.isEmpty()) {
            ((UserData)this).touch = 0L;
            this.touch.set((Object)"");
        } else {
            try {
                ((UserData)this).touch = Long.parseLong(string);
                this.touch.set((Object)sdf.format(new Date(((UserData)this).touch)));
            }
            catch (NumberFormatException numberFormatException) {
                ((UserData)this).touch = 0L;
                this.touch.set((Object)string);
            }
        }
    }

    public StringProperty touchProperty() {
        return this.touch;
    }

    public String getOnoff() {
        return (String)this.onoff.get();
    }

    public void setOnoff(String string) {
        ((UserData)this).onoff = string.equals("1");
        this.onoff.set((Object)(((UserData)this).onoff ? Lang.getString("member.connect") : Lang.getString("member.disconnect")));
    }

    public StringProperty onoffProperty() {
        return this.onoff;
    }
}

