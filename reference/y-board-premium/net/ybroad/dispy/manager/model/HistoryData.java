/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.property.SimpleStringProperty
 *  javafx.beans.property.StringProperty
 */
package net.ybroad.dispy.manager.model;

import java.util.HashMap;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class HistoryData
implements Comparable<HistoryData> {
    public final StringProperty data = new SimpleStringProperty();
    public final StringProperty sum = new SimpleStringProperty();
    public final HashMap<String, StringProperty> values = new HashMap();

    public HistoryData(String string, String string2, String string3) {
        this.data.set((Object)string2);
        this.putValue(string, string3);
    }

    public void putValue(String string, String string2) {
        int n;
        this.getValue(string).set((Object)string2);
        int n2 = Integer.parseInt(string2);
        try {
            n = Integer.parseInt((String)this.sum.get());
        }
        catch (NumberFormatException numberFormatException) {
            n = 0;
        }
        this.sum.set((Object)String.valueOf(n2 + n));
    }

    public StringProperty getValue(String string) {
        StringProperty stringProperty = this.values.get(string);
        if (stringProperty == null) {
            stringProperty = new SimpleStringProperty("");
            this.values.put(string, stringProperty);
        }
        return stringProperty;
    }

    @Override
    public int compareTo(HistoryData historyData) {
        return ((String)this.data.get()).compareTo((String)historyData.data.get());
    }
}

