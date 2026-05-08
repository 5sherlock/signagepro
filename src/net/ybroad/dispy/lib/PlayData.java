/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

import java.io.File;
import java.util.Arrays;

public class PlayData {
    public static final String TYPE_SYNC = "sync";
    public static final String TYPE_SYNC_LAN = "synclan";
    public static final String TYPE_EXPANDS = "expands";
    public static final String TYPE_EXPANDED = "expanded";
    public static final String TYPE_INDIVIDUAL = "individual";
    public static final String TYPE_MUSIC = "music";
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_PDF = "pdf";
    public static final String TYPE_PPT = "ppt";
    public static final String TYPE_TEXT1 = "text1";
    public static final String TYPE_TEXT2 = "text2";
    public static final String TYPE_WEB = "web";
    public static final String TYPE_STYLE1 = "style1";
    public String type = null;
    public File file = null;
    public File alternative = null;
    public String text = null;
    public int time = 0;
    public int font = 0;
    public int[] show = new int[]{-1, -1, -1, -1};
    public long open = 0L;
    public long till = 0L;
    public boolean changed = false;
    private int[] expandsCell = new int[]{0, 0, 0, 0};
    private PlayData[] expandsData = new PlayData[]{null, null, null, null};
    private int expandedCell = -1;
    private PlayData expandedData = null;

    public String toString() {
        return "type=" + this.type + "," + "file=" + this.file + "," + "text=" + this.text + "," + "time=" + this.time + "," + "font=" + this.font + "," + "changed=" + this.changed + "," + "expands=" + Arrays.toString(this.expandsCell).replace(", ", "").replace("[", "").replace("]", "") + "," + "expanded=" + this.expandedCell;
    }

    public int[] getExpandsCell() {
        boolean bl = true;
        for (int n : this.expandsCell) {
            if (n == 0) continue;
            bl = false;
            break;
        }
        if (bl) {
            return null;
        }
        return this.expandsCell;
    }

    public void addExpands(int n, PlayData playData) {
        this.expandsCell[n] = 1;
        this.expandsData[n] = playData;
    }

    public int getExpandedCell() {
        return this.expandedCell;
    }

    public PlayData getExpandedData() {
        return this.expandedData;
    }

    public void setExpanded(int n, PlayData playData) {
        this.expandedCell = n;
        this.expandedData = playData;
    }
}

