/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import net.ybroad.dispy.lib.MySocket;
import net.ybroad.dispy.lib.UnionData;

public class ClientData
implements Serializable {
    private static final long serialVersionUID = -3776342331246219473L;
    public static final int TYPE_VERTICAL = 1;
    public static final int TYPE_HORIZONTAL = 2;
    public static final int TYPE_BORDER = 3;
    public static final int TYPE_BOOKSHELF_1 = 4;
    public static final int TYPE_BOOKSHELF_2 = 5;
    public static final int TYPE_VERTICAL_2 = 6;
    public static final int TYPE_HORIZONTAL_2 = 7;
    public static final int TYPE_THREE_AND_ONE = 8;
    public static final int TYPE_ONE_AND_THREE = 9;
    public static final int TYPE_BG_VERTICAL = 10;
    public static final int TYPE_BG_HORIZONTAL = 11;
    public static final int TYPE_BG_BORDER_1 = 12;
    public static final int TYPE_BG_BORDER_2 = 13;
    public static final int TYPE_BG_BORDER_3 = 14;
    public static final int TYPE_BG_BORDER_4 = 15;
    public static final int TYPE_SIDEWING = 16;
    public static final int CELL_COUNT = 4;
    public static final int RESERVE_COUNT = 4;
    public String id = "";
    public String name = "";
    public long first = -1L;
    public String info = "";
    public String spec = "";
    public long free = -1L;
    public String ip = "";
    public int width = 0;
    public int height = 0;
    public int rotate = 0;
    public int numW = 1;
    public int numH = 1;
    public boolean clock = true;
    public String clockcolor = "ffffffff";
    public String clockpos = "0rc";
    public String logo = null;
    public String logooriginal = null;
    public String background = null;
    public String bgoriginal = null;
    public int type = 1;
    public int[] cell = new int[]{100, 0, 0, 0};
    public String[] play = new String[]{null, null, null, null};
    public String[] playitem = new String[]{null, null, null, null};
    public String[] playlast = new String[]{null, null, null, null};
    public String[][] reserve = new String[][]{{null, null, null, null}, {null, null, null, null}, {null, null, null, null}, {null, null, null, null}};
    public String[][] reserveitem = new String[][]{{null, null, null, null}, {null, null, null, null}, {null, null, null, null}, {null, null, null, null}};
    public long[][] reservetime = new long[][]{{0L, 0L, 0L, 0L}, {0L, 0L, 0L, 0L}, {0L, 0L, 0L, 0L}, {0L, 0L, 0L, 0L}};
    public String bgm = null;
    public String bgmitem = null;
    public String bgmlast = null;
    public String owner = "";
    public String group = "";
    public int version = 0;
    public long license = 0L;
    public long touch = 0L;
    public long connect = 0L;
    public long disconnect = 0L;
    public String reason = "";
    public HashMap<String, Integer> history = new HashMap();
    public ArrayList<UnionData> union = new ArrayList();
    public int unionx = 0;
    public int uniony = 0;
    public int unionw = 0;
    public int unionh = 0;
    public double unionf = 1.0;
    public int[] on = new int[]{-1, -1, -1, -1, -1, -1, -1};
    public int[] off = new int[]{-1, -1, -1, -1, -1, -1, -1};
    public int mbps = 0;
    public transient boolean onoff = false;
    public transient MySocket socket = null;
    public transient Object lock = new Object();
    public transient ReentrantLock groupLock = new ReentrantLock();
    public transient boolean select = false;
    public transient long txtime = 0L;
    public transient long txbyte = 0L;

    public String toString() {
        return "id=" + this.id + "\u02fd" + "name=" + this.name + "\u02fd" + "width=" + this.width + "\u02fd" + "height=" + this.height + "\u02fd" + "type=" + this.type + "\u02fd" + "cell=" + Arrays.toString(this.cell).replace(", ", ",") + "\u02fd" + "play=" + Arrays.toString(this.play).replace(", ", ",") + "\u02fd" + "playitem=" + Arrays.toString(this.playitem).replace(", ", ",") + "\u02fd" + "playlast=" + Arrays.toString(this.playlast).replace(", ", ",") + "\u02fd" + "reserve0=" + Arrays.toString(this.reserve[0]).replace(", ", ",") + "\u02fd" + "reserve0item=" + Arrays.toString(this.reserveitem[0]).replace(", ", ",") + "\u02fd" + "reserve1=" + Arrays.toString(this.reserve[1]).replace(", ", ",") + "\u02fd" + "reserve1item=" + Arrays.toString(this.reserveitem[1]).replace(", ", ",") + "\u02fd" + "reserve2=" + Arrays.toString(this.reserve[2]).replace(", ", ",") + "\u02fd" + "reserve2item=" + Arrays.toString(this.reserveitem[2]).replace(", ", ",") + "\u02fd" + "reserve3=" + Arrays.toString(this.reserve[3]).replace(", ", ",") + "\u02fd" + "reserve3item=" + Arrays.toString(this.reserveitem[3]).replace(", ", ",") + "\u02fd" + "bgm=" + this.bgm + "\u02fd" + "bgmitem=" + this.bgmitem + "\u02fd" + "bgmlast=" + this.bgmlast + "\u02fd" + "owner=" + this.owner + "\u02fd" + "group=" + this.group + "\u02fd" + "version=" + this.version + "\u02fd" + "license=" + this.license + "\u02fd" + "touch=" + this.touch + "\u02fd" + "onoff=" + this.onoff;
    }

    public boolean equals(Object object) {
        if (object instanceof ClientData) {
            return this.id.equals(((ClientData)object).id);
        }
        return false;
    }

    public void initLoad() {
        this.onoff = false;
        this.socket = null;
        if (this.lock == null) {
            this.lock = new Object();
        }
        if (this.groupLock == null) {
            this.groupLock = new ReentrantLock();
        }
        this.select = false;
        if (this.id == null) {
            this.id = "";
        }
        if (this.name == null) {
            this.name = "";
        }
        if (this.owner == null) {
            this.owner = "";
        }
        if (this.group == null) {
            this.group = "";
        }
        if (this.info == null) {
            this.info = "";
            this.free = -1L;
        }
        if (this.spec == null) {
            this.spec = "";
            this.free = -1L;
        }
        if (this.ip == null) {
            this.ip = "";
        }
        if (this.numW < 1 || this.numH < 1) {
            this.numW = 1;
            this.numH = 1;
            this.clock = true;
        }
        if (this.clockcolor == null) {
            this.clockcolor = "ffffffff";
        }
        if (this.clockpos == null) {
            this.clockpos = "0rc";
        }
        if (this.cell == null) {
            this.cell = new int[]{100, 0, 0, 0};
        }
        if (this.play == null) {
            this.play = new String[]{null, null, null, null};
        }
        if (this.playitem == null) {
            this.playitem = new String[]{null, null, null, null};
        }
        if (this.playlast == null) {
            this.playlast = new String[]{this.play[0], this.play[1], this.play[2], this.play[3]};
        }
        if (this.reserve == null) {
            this.reserve = new String[][]{{null, null, null, null}, {null, null, null, null}, {null, null, null, null}, {null, null, null, null}};
        }
        if (this.reserveitem == null) {
            this.reserveitem = new String[][]{{null, null, null, null}, {null, null, null, null}, {null, null, null, null}, {null, null, null, null}};
        }
        if (this.reservetime == null) {
            this.reservetime = new long[][]{{0L, 0L, 0L, 0L}, {0L, 0L, 0L, 0L}, {0L, 0L, 0L, 0L}, {0L, 0L, 0L, 0L}};
        }
        if (this.reason == null) {
            this.reason = "";
        }
        if (this.history == null) {
            this.history = new HashMap();
        }
        if (this.union == null) {
            this.union = new ArrayList();
        }
        if (this.unionf == 0.0) {
            this.unionf = 1.0;
        }
    }
}

