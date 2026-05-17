/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.manager.model;

public class ProgressHolder {
    public String type;
    public int current;
    public int count;
    public double value;

    public ProgressHolder(String string, int n, int n2, double d) {
        this.type = string;
        this.current = n;
        this.count = n2;
        this.value = d;
    }
}

