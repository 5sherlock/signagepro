/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

public abstract class Progress {
    public static final String TYPE_BGM_2_SERVER = "progress.bgm2server";
    public static final String TYPE_BGM_2_CLIENT = "progress.bgm2client";
    public static final String TYPE_PLAY_2_SERVER = "progress.play2server";
    public static final String[] TYPE_PLAY_2_CLIENT = new String[]{"progress.play2client0", "progress.play2client1", "progress.play2client2", "progress.play2client3"};
    public static final String TYPE_RESERVE_2_SERVER = "progress.reserve2server";
    public static final String TYPE_CONVERT_FILE_FORMAT = "progress.convert";
    protected String type;
    protected int count;
    protected int current;

    public void set(String string, int n) {
        this.type = string;
        this.count = n;
        this.current = 1;
        this.update(0.0);
    }

    public void next() {
        ++this.current;
        this.update(0.0);
    }

    public abstract void update(double var1);
}

