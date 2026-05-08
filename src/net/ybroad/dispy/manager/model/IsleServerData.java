/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.binding.Bindings
 *  javafx.beans.value.ObservableValue
 */
package net.ybroad.dispy.manager.model;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import net.ybroad.dispy.manager.model.MemberData;
import net.ybroad.dispy.manager.util.Lang;

public class IsleServerData
extends MemberData {
    private static final long serialVersionUID = 3284682801821733853L;
    private String id;
    private String key;

    public IsleServerData(String string, String string2, String string3, String string4, String string5) {
        this.setIsle(string, string2, string3, string4, string5);
        this.devLimit.bind((ObservableValue)Bindings.concat((Object[])new Object[]{this.limit}));
    }

    public void setIsle(String string, String string2, String string3, String string4, String string5) {
        this.id = string;
        this.key = string3;
        this.setName(string2);
        this.setTouch(string5);
        this.onoff.set((Object)Lang.getString("isle.server"));
        this.setLimit(string4);
    }

    public String getId() {
        return this.id;
    }

    public String getKey() {
        return this.key;
    }
}

