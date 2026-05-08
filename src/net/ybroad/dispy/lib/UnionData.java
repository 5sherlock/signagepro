/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

import java.io.Serializable;

public class UnionData
implements Serializable {
    private static final long serialVersionUID = 4398319203163542695L;
    public String id;
    public boolean master;
    public int x;
    public int y;
    public int w;
    public int h;
    public int xpos;
    public int ypos;
    public double f;

    public UnionData() {
        this.id = "";
    }

    public UnionData(UnionData unionData) {
        this.id = unionData.id;
        this.master = unionData.master;
        this.x = unionData.x;
        this.y = unionData.y;
        this.w = unionData.w;
        this.h = unionData.h;
        this.xpos = unionData.xpos;
        this.ypos = unionData.ypos;
        this.f = unionData.f;
    }
}

