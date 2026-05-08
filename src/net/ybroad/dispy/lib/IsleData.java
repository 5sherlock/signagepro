/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

import java.io.Serializable;

public class IsleData
implements Serializable {
    private static final long serialVersionUID = 7222250523636176735L;
    public String id = "";
    public String name = "";
    public String key = "";
    public int limit = 0;
    public long touch = 0L;

    public String toString() {
        return "id=" + this.id + "\u02fd" + "name=" + this.name + "\u02fd" + "key=" + this.key + "\u02fd" + "limit=" + this.limit + "\u02fd" + "touch=" + this.touch;
    }

    public void initLoad() {
        if (this.id == null) {
            this.id = "";
        }
        if (this.name == null) {
            this.name = "";
        }
        if (this.key == null) {
            this.key = "";
        }
    }
}

