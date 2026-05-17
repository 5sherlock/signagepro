/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

import java.io.Serializable;
import net.ybroad.dispy.lib.MySocket;

public class UserData
implements Serializable {
    private static final long serialVersionUID = 8269210209279926536L;
    public String name = "";
    public String pw = "";
    public long touch = 0L;
    public int version = 0;
    public int limit = 0;
    public String memo = "";
    public transient boolean member = false;
    public transient boolean onoff = false;
    public transient MySocket socket = null;
    public transient Object lock = new Object();

    public String toString() {
        return "name=" + this.name + "\u02fd" + "pw=" + this.pw + "\u02fd" + "touch=" + this.touch + "\u02fd" + "onoff=" + this.onoff + "\u02fd" + "version=" + this.version + "\u02fd" + "limit=" + this.limit;
    }

    public void initLoad() {
        this.onoff = false;
        this.socket = null;
        if (this.lock == null) {
            this.lock = new Object();
        }
        if (this.name == null) {
            this.name = "";
        }
        if (this.pw == null) {
            this.pw = "";
        }
        if (this.memo == null) {
            this.memo = "";
        }
    }
}

