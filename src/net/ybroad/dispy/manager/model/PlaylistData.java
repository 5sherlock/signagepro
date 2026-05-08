/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.property.SimpleBooleanProperty
 *  javafx.beans.property.SimpleStringProperty
 *  javafx.beans.property.StringProperty
 *  javafx.beans.value.ObservableBooleanValue
 */
package net.ybroad.dispy.manager.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import net.ybroad.dispy.lib.PlayData;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.util.Lang;

public class PlaylistData
extends PlayData {
    public static final String STATE_CODE_ORIGINAL = "original";
    public static final String STATE_CODE_NEW = "new";
    public static final String STATE_CODE_EDITED = "edited";
    private String stateCode = "original";
    private final StringProperty state = new SimpleStringProperty("");
    private final StringProperty type = new SimpleStringProperty("");
    private final StringProperty attr = new SimpleStringProperty("");
    private final StringProperty show = new SimpleStringProperty("");
    private final StringProperty open = new SimpleStringProperty("");
    private final StringProperty till = new SimpleStringProperty("");
    private final StringProperty data = new SimpleStringProperty("");
    private final SimpleBooleanProperty check = new SimpleBooleanProperty(false);
    public ArrayList<PlaylistData> individualItems;
    public DispyData individualOwner;

    public PlaylistData(PlayData playData) {
        int n = playData.getExpandedCell();
        switch (playData.type) {
            case "music": 
            case "video": 
            case "image": 
            case "pdf": 
            case "ppt": {
                if (n < 0) {
                    this.setType(playData.type);
                } else {
                    this.setTypeExpanded(playData.type, n);
                }
                this.setAttrInt(playData.time);
                this.setShowInt(playData.show[0], playData.show[1], playData.show[2], playData.show[3]);
                this.setOpen(playData.open);
                this.setTill(playData.till);
                this.setDataFile(playData.file);
                break;
            }
            case "text1": 
            case "text2": 
            case "web": 
            case "style1": {
                if (n < 0) {
                    this.setType(playData.type);
                } else {
                    this.setTypeExpanded(playData.type, n);
                }
                this.setAttrInt(playData.time, playData.font);
                this.setShowInt(playData.show[0], playData.show[1], playData.show[2], playData.show[3]);
                this.setOpen(playData.open);
                this.setTill(playData.till);
                this.setDataText(playData.text);
                break;
            }
            case "individual": {
                if (n < 0) {
                    this.setType(playData.type);
                } else {
                    this.setTypeExpanded(playData.type, n);
                }
                this.setDataText(playData.text);
                break;
            }
            case "sync": 
            case "synclan": {
                this.setTypeSync(playData.type, playData.time);
            }
        }
        this.setState(STATE_CODE_ORIGINAL);
    }

    public PlaylistData(String string) {
        this.setType(string);
    }

    public PlaylistData(String string, int n) {
        this.setTypeSync(string, n);
    }

    public String getState() {
        return (String)this.state.get();
    }

    public String getStateCode() {
        return this.stateCode;
    }

    public void setState(String string) {
        if (this.stateCode.equals(STATE_CODE_NEW)) {
            this.changed = true;
        } else {
            this.state.set((Object)Lang.getString("play.state." + string));
            this.stateCode = string;
        }
    }

    public StringProperty stateProperty() {
        return this.state;
    }

    public String getType() {
        return (String)this.type.get();
    }

    public String getTypeCode() {
        return ((PlayData)this).type;
    }

    public void setType(String string) {
        this.type.set((Object)Lang.getString("play.type." + string));
        ((PlayData)this).type = string;
    }

    public void setTypeSync(String string, int n) {
        this.type.set((Object)Lang.getString("play.type." + string + ".n", n));
        ((PlayData)this).type = string + n;
    }

    private void setTypeExpanded(String string, int n) {
        this.type.set((Object)Lang.getString("play.expanded", n));
        ((PlayData)this).type = string;
    }

    public StringProperty typeProperty() {
        return this.type;
    }

    public String getAttr() {
        return (String)this.attr.get();
    }

    public int getAttrTime() {
        return this.time;
    }

    public int getAttrFont() {
        return this.font;
    }

    public void setAttr(String string) {
        this.attr.set((Object)string);
    }

    public void setAttrInt(int n) {
        this.attr.set((Object)Lang.getString("play.unit.time", n));
        this.time = n;
    }

    public void setAttrInt(int n, int n2) {
        this.attr.set((Object)Lang.getString("play.unit.timefont", n, n2));
        this.time = n;
        this.font = n2;
    }

    public StringProperty attrProperty() {
        return this.attr;
    }

    public String getShow() {
        return (String)this.show.get();
    }

    public int getShowWeekStart() {
        return ((PlayData)this).show[0];
    }

    public int getShowWeekEnd() {
        return ((PlayData)this).show[1];
    }

    public int getShowStart() {
        return ((PlayData)this).show[2];
    }

    public int getShowEnd() {
        return ((PlayData)this).show[3];
    }

    public void setShow(String string) {
        this.show.set((Object)string);
    }

    public void setShowInt(int n, int n2, int n3, int n4) {
        if (n < 0 || n2 < 0 || n - n2 == 1 || n == 0 && n2 == 6) {
            n = -1;
            n2 = -1;
        }
        if (n3 < 0 || n4 < 0 || n3 - n4 == 1 || n3 == 0 && n4 == 23) {
            n3 = -1;
            n4 = -1;
        }
        if (n < 0 && n2 < 0 && n3 < 0 && n4 < 0) {
            ((PlayData)this).show[0] = n;
            ((PlayData)this).show[1] = n2;
            ((PlayData)this).show[2] = n3;
            ((PlayData)this).show[3] = n4;
            this.show.set((Object)Lang.getString("play.show.always"));
        } else {
            Object object;
            String string;
            if (n < 0 || n2 < 0) {
                string = Lang.getString("play.show.everyday");
            } else if (n == n2) {
                object = Lang.getString("week.short").split(",");
                string = object[n];
            } else {
                object = Lang.getString("week.short").split(",");
                string = object[n] + "~" + object[n2];
            }
            ((PlayData)this).show[0] = n;
            ((PlayData)this).show[1] = n2;
            object = n3 < 0 || n4 < 0 ? Lang.getString("play.show.allday") : (n3 == n4 ? String.valueOf(n3) : n3 + "~" + n4);
            ((PlayData)this).show[2] = n3;
            ((PlayData)this).show[3] = n4;
            this.show.set((Object)(string + " / " + (String)object));
        }
    }

    public StringProperty showProperty() {
        return this.show;
    }

    public long getOpen() {
        return ((PlayData)this).open;
    }

    public void setOpen(long l) {
        ((PlayData)this).open = l;
        if (l > 0L) {
            this.open.set((Object)new SimpleDateFormat(" / yy-MM-dd HH:mm ~").format(new Date(l)));
        } else {
            this.open.set((Object)"");
        }
    }

    public StringProperty openProperty() {
        return this.open;
    }

    public long getTill() {
        return ((PlayData)this).till;
    }

    public void setTill(long l) {
        ((PlayData)this).till = l;
        if (l > 0L) {
            if (((PlayData)this).open > 0L) {
                this.till.set((Object)new SimpleDateFormat(" yy-MM-dd HH:mm").format(new Date(l)));
            } else {
                this.till.set((Object)new SimpleDateFormat(" / ~ yy-MM-dd HH:mm").format(new Date(l)));
            }
        } else {
            this.till.set((Object)"");
        }
    }

    public StringProperty tillProperty() {
        return this.till;
    }

    public String getData() {
        return (String)this.data.get();
    }

    public File getDataFile() {
        return this.file;
    }

    public String getDataText() {
        return this.text;
    }

    public void setData(String string) {
        this.data.set((Object)string);
    }

    public void setDataFile(File file) {
        this.data.set((Object)file.getName());
        this.file = file;
    }

    public void setDataText(String string) {
        if (string != null) {
            string = string.replace("\n", "\\n");
        }
        this.data.set((Object)string);
        this.text = string;
    }

    public StringProperty dataProperty() {
        return this.data;
    }

    @Override
    public void setExpanded(int n, PlayData playData) {
        this.type.set((Object)Lang.getString("play.expanded", n));
        super.setExpanded(n, playData);
    }

    public boolean getChecked() {
        return this.check.get();
    }

    public void setChecked(boolean bl) {
        this.check.set(bl);
    }

    public ObservableBooleanValue checkProperty() {
        return this.check;
    }
}

