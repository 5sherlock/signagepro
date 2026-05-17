/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

import java.io.Serializable;
import net.ybroad.dispy.lib.MySocket;

public class SubData
implements Serializable {
    private static final long serialVersionUID = 8268192358055763125L;
    public String name = "";
    public String addr = "";
    public int copy = 0;
    public int copylimit = 0;
    public long touch = 0L;
    public transient boolean onoff = false;
    public transient MySocket socket = null;
    public transient Object lock = new Object();

    public String toString() {
        return "addr=" + this.addr + "\u02fd" + "name=" + this.name + "\u02fd" + "limit=" + this.copylimit + "\u02fd" + "copy=" + this.copy + "\u02fd" + "touch=" + this.touch + "\u02fd" + "onoff=" + this.onoff;
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
        if (this.addr == null) {
            this.addr = "";
        }
    }
}

