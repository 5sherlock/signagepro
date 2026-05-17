/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.property.SimpleStringProperty
 *  javafx.beans.property.StringProperty
 */
package net.ybroad.dispy.manager.model;

import java.util.ArrayList;
import java.util.Calendar;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.ybroad.dispy.lib.PlayData;
import net.ybroad.dispy.manager.model.PlaylistData;

public class ScheduleData
extends PlaylistData {
    private StringProperty cell = new SimpleStringProperty();
    private StringProperty date = new SimpleStringProperty();
    private StringProperty hour = new SimpleStringProperty();
    private ArrayList<Long> time = new ArrayList();
    private ArrayList<int[]> show = new ArrayList();
    private static final String[] COLORS = new String[]{"thistle", "darkseagreen", "lightsalmon", "lightsteelblue", "darkkhaki"};
    private int color;

    public ScheduleData(PlayData playData, int n, int n2, long l, long l2) {
        super(playData);
        this.cell.set((Object)("#" + n));
        this.time.add(l);
        this.time.add(l2);
        this.show.add(playData.show);
        this.color = n2;
    }

    public void addTime(long l, long l2) {
        if (this.time.get(this.time.size() - 1) == Long.MAX_VALUE) {
            this.time.remove(this.time.size() - 1);
            this.time.add(l);
        }
        this.time.add(l);
        this.time.add(l2);
    }

    public void addShow(PlayData playData) {
        if (playData == null) {
            this.show.add(null);
        } else {
            this.show.add(playData.show);
        }
    }

    public StringProperty cellProperty() {
        return this.cell;
    }

    public StringProperty dayProperty(int n) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, n);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        long l = calendar.getTimeInMillis();
        int n2 = calendar.get(7) - 2;
        if (n2 < 0) {
            n2 = 6;
        }
        calendar.add(5, 1);
        long l2 = calendar.getTimeInMillis();
        int n3 = -1;
        int n4 = -1;
        for (int i = 0; i < this.time.size() - 1; i += 2) {
            boolean bl;
            long l3;
            long l4 = this.time.get(i);
            if (l4 == (l3 = this.time.get(i + 1).longValue()) || l4 >= l2 || l >= l3) continue;
            int[] nArray = this.show.get(i / 2);
            if (nArray[0] < 0 || nArray[1] < 0 || nArray[0] - nArray[1] == 1 || nArray[0] == 0 && nArray[1] == 6) {
                bl = true;
            } else if (nArray[0] <= nArray[1]) {
                bl = nArray[0] <= n2 && n2 <= nArray[1];
            } else {
                boolean bl2 = bl = nArray[0] <= n2 || n2 <= nArray[1];
            }
            if (!bl) continue;
            if (n3 < 0) {
                n3 = i / 2;
                continue;
            }
            n4 = i / 2;
            break;
        }
        if (n3 < 0) {
            this.date.set(null);
        } else if (n4 < 0) {
            this.date.set((Object)(n + "/" + COLORS[this.color + n3]));
        } else if (n3 + 1 == n4) {
            this.date.set((Object)(n + "/linear-gradient(to right, " + COLORS[this.color + n3] + " 33%, " + COLORS[this.color + n4] + " 66%)"));
        } else {
            this.date.set((Object)(n + "/linear-gradient(to right, " + COLORS[this.color + n3] + " 33%, white 66%)"));
        }
        return this.date;
    }

    public StringProperty hourProperty(int n, int n2) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, n);
        calendar.set(11, n2);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        long l = calendar.getTimeInMillis();
        calendar.add(11, 1);
        long l2 = calendar.getTimeInMillis();
        int n3 = -1;
        for (int i = 0; i < this.time.size() - 1; i += 2) {
            if ((this.time.get(i) >= l || l >= this.time.get(i + 1)) && (this.time.get(i) >= l2 || l2 >= this.time.get(i + 1))) continue;
            n3 = i / 2;
            break;
        }
        if (n3 < 0) {
            this.hour.set(null);
        } else {
            boolean bl;
            int[] nArray = this.show.get(n3);
            int n4 = calendar.get(7) - 2;
            if (n4 < 0) {
                n4 = 6;
            }
            if (nArray[0] < 0 || nArray[1] < 0 || nArray[0] - nArray[1] == 1 || nArray[0] == 0 && nArray[1] == 6) {
                bl = true;
            } else if (nArray[0] <= nArray[1]) {
                bl = nArray[0] <= n4 && n4 <= nArray[1];
            } else {
                boolean bl2 = bl = nArray[0] <= n4 || n4 <= nArray[1];
            }
            if (bl) {
                if (nArray[2] < 0 || nArray[3] < 0 || nArray[2] - nArray[3] == 1 || nArray[2] == 0 && nArray[3] == 23) {
                    bl = true;
                } else if (nArray[2] <= nArray[3]) {
                    bl = nArray[2] <= n2 && n2 <= nArray[3];
                } else {
                    boolean bl3 = bl = nArray[2] <= n2 || n2 <= nArray[3];
                }
            }
            if (bl) {
                this.hour.set((Object)COLORS[this.color + n3]);
            } else {
                this.hour.set(null);
            }
        }
        return this.hour;
    }
}

