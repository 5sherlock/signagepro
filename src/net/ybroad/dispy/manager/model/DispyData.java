/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.property.SimpleBooleanProperty
 *  javafx.beans.property.SimpleStringProperty
 *  javafx.beans.property.StringProperty
 *  javafx.beans.value.ObservableBooleanValue
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 */
package net.ybroad.dispy.manager.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.ybroad.dispy.lib.ClientData;
import net.ybroad.dispy.lib.PlayData;
import net.ybroad.dispy.lib.Util;
import net.ybroad.dispy.manager.model.PlaylistData;
import net.ybroad.dispy.manager.util.Lang;

public class DispyData
extends ClientData {
    private static final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("yy-MM-dd HH:mm");
    private static final SimpleDateFormat sdf3 = new SimpleDateFormat("yy-MM-dd HH:mm");
    private final StringProperty version = new SimpleStringProperty();
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty cell0 = new SimpleStringProperty();
    private final StringProperty cell1 = new SimpleStringProperty();
    private final StringProperty cell2 = new SimpleStringProperty();
    private final StringProperty cell3 = new SimpleStringProperty();
    private final StringProperty play0 = new SimpleStringProperty();
    private final StringProperty play1 = new SimpleStringProperty();
    private final StringProperty play2 = new SimpleStringProperty();
    private final StringProperty play3 = new SimpleStringProperty();
    private final StringProperty owner = new SimpleStringProperty();
    private final StringProperty license = new SimpleStringProperty();
    private final StringProperty touch = new SimpleStringProperty();
    private final StringProperty onoff = new SimpleStringProperty();
    private final SimpleBooleanProperty check = new SimpleBooleanProperty();
    private List<PlaylistData>[] tempParsedPlaylist = new List[4];
    private List<PlaylistData>[][] tempParsedReservelist = new List[4][4];

    public DispyData() {
        this(null);
    }

    public DispyData(ClientData clientData) {
        if (clientData == null) {
            clientData = new ClientData();
            clientData.initLoad();
        }
        this.initLoad();
        this.setVersion(clientData.version);
        this.setId(clientData.id);
        this.setName(clientData.name);
        this.setSize(clientData.width, clientData.height, clientData.rotate);
        this.setType(clientData.type);
        this.setCell0(clientData.cell[0]);
        this.setCell1(clientData.cell[1]);
        this.setCell2(clientData.cell[2]);
        this.setCell3(clientData.cell[3]);
        this.setPlay0(clientData.play[0]);
        this.setPlay1(clientData.play[1]);
        this.setPlay2(clientData.play[2]);
        this.setPlay3(clientData.play[3]);
        this.setOwner(clientData.owner);
        this.setLicense(clientData.license);
        this.setTouch(clientData.touch);
        this.setOnoff(clientData.onoff);
    }

    public void copyFrom(DispyData dispyData) {
        DispyData dispyData2 = dispyData;
        ((ClientData)this).id = ((ClientData)dispyData2).id;
        ((ClientData)this).name = ((ClientData)dispyData2).name;
        this.info = dispyData2.info;
        this.spec = dispyData2.spec;
        this.free = dispyData2.free;
        this.width = dispyData2.width;
        this.height = dispyData2.height;
        this.rotate = dispyData2.rotate;
        this.numW = dispyData2.numW;
        this.numH = dispyData2.numH;
        this.clock = dispyData2.clock;
        this.clockpos = dispyData2.clockpos;
        this.background = dispyData2.background;
        this.bgoriginal = dispyData2.bgoriginal;
        ((ClientData)this).type = ((ClientData)dispyData2).type;
        this.cell = dispyData2.cell;
        this.play = dispyData2.play;
        this.playitem = dispyData2.playitem;
        this.playlast = dispyData2.playlast;
        this.reserve = dispyData2.reserve;
        this.reserveitem = dispyData2.reserveitem;
        this.reservetime = dispyData2.reservetime;
        ((ClientData)this).owner = ((ClientData)dispyData2).owner;
        this.group = dispyData2.group;
        ((ClientData)this).version = ((ClientData)dispyData2).version;
        ((ClientData)this).license = ((ClientData)dispyData2).license;
        ((ClientData)this).touch = ((ClientData)dispyData2).touch;
        this.history = dispyData2.history;
        ((ClientData)this).onoff = ((ClientData)dispyData2).onoff;
        this.select = dispyData2.select;
        this.version.set(dispyData.version.get());
        this.id.set(dispyData.id.get());
        this.name.set(dispyData.name.get());
        this.size.set(dispyData.size.get());
        this.type.set(dispyData.type.get());
        this.cell0.set(dispyData.cell0.get());
        this.cell1.set(dispyData.cell1.get());
        this.cell2.set(dispyData.cell2.get());
        this.cell3.set(dispyData.cell3.get());
        this.play0.set(dispyData.play0.get());
        this.play1.set(dispyData.play1.get());
        this.play2.set(dispyData.play2.get());
        this.play3.set(dispyData.play3.get());
        this.owner.set(dispyData.owner.get());
        this.touch.set(dispyData.touch.get());
        this.onoff.set(dispyData.onoff.get());
        this.check.set(dispyData.check.get());
        this.tempParsedPlaylist = dispyData.tempParsedPlaylist;
        this.tempParsedReservelist = dispyData.tempParsedReservelist;
    }

    public String getVersion() {
        return (String)this.version.get();
    }

    public int getVersionInt() {
        return ((ClientData)this).version;
    }

    public void setVersion(String string) {
        this.version.set((Object)string);
        ((ClientData)this).version = Integer.parseInt(string);
    }

    public void setVersion(int n) {
        this.version.set((Object)String.valueOf(n));
        ((ClientData)this).version = n;
    }

    public String getId() {
        return (String)this.id.get();
    }

    public void setId(String string) {
        this.id.set((Object)string);
        ((ClientData)this).id = string;
    }

    public StringProperty idProperty() {
        return this.id;
    }

    public String getName() {
        return (String)this.name.get();
    }

    public void setName(String string) {
        this.name.set((Object)(string.isEmpty() ? Lang.getString("overview.noname") : string));
        ((ClientData)this).name = string;
    }

    public StringProperty nameProperty() {
        return this.name;
    }

    public String getSize() {
        return (String)this.size.get();
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setSize(int n, int n2, int n3) {
        if (this.numW != this.numH) {
            if (n3 == 90) {
                this.size.set((Object)(n + " \u00d7 " + n2 + "  (" + this.numW + ":" + this.numH + " / 90\u00ba)"));
            } else if (n3 == 180) {
                this.size.set((Object)(n + " \u00d7 " + n2 + "  (" + this.numW + ":" + this.numH + " / 180\u00ba)"));
            } else if (n3 == 270) {
                this.size.set((Object)(n + " \u00d7 " + n2 + "  (" + this.numW + ":" + this.numH + " / -90\u00ba)"));
            } else {
                this.size.set((Object)(n + " \u00d7 " + n2 + "  (" + this.numW + ":" + this.numH + ")"));
            }
        } else if (n3 == 90) {
            this.size.set((Object)(n + " \u00d7 " + n2 + "  (90\u00ba)"));
        } else if (n3 == 180) {
            this.size.set((Object)(n + " \u00d7 " + n2 + "  (180\u00ba)"));
        } else if (n3 == 270) {
            this.size.set((Object)(n + " \u00d7 " + n2 + "  (-90\u00ba)"));
        } else {
            this.size.set((Object)(n + " \u00d7 " + n2));
        }
        this.width = n;
        this.height = n2;
    }

    public StringProperty sizeProperty() {
        return this.size;
    }

    public String getType() {
        return (String)this.type.get();
    }

    public int getTypeInt() {
        return ((ClientData)this).type;
    }

    public String getTypeString() {
        switch (((ClientData)this).type) {
            case 1: {
                return Lang.getString("type.vertical");
            }
            case 2: {
                return Lang.getString("type.horizontal");
            }
            case 3: {
                return Lang.getString("type.border");
            }
            case 4: {
                return Lang.getString("type.bookshelf1");
            }
            case 5: {
                return Lang.getString("type.bookshelf2");
            }
            case 6: {
                return Lang.getString("type.vertical2");
            }
            case 7: {
                return Lang.getString("type.horizontal2");
            }
            case 8: {
                return Lang.getString("type.threeandone");
            }
            case 9: {
                return Lang.getString("type.oneandthree");
            }
            case 10: {
                return Lang.getString("type.bgvertical");
            }
            case 11: {
                return Lang.getString("type.gbhorizontal");
            }
            case 12: {
                return Lang.getString("type.bgborder1");
            }
            case 13: {
                return Lang.getString("type.bgborder2");
            }
            case 14: {
                return Lang.getString("type.bgborder3");
            }
            case 15: {
                return Lang.getString("type.bgborder4");
            }
            case 16: {
                return Lang.getString("type.sidewing");
            }
        }
        return "???";
    }

    public void setType(String string) {
        this.type.set((Object)string);
        switch (string) {
            case "Vertical": {
                ((ClientData)this).type = 1;
                break;
            }
            case "Horizontal": {
                ((ClientData)this).type = 2;
                break;
            }
            case "Border": {
                ((ClientData)this).type = 3;
            }
        }
    }

    public void setType(int n) {
        switch (n) {
            case 1: {
                this.type.set((Object)"Vertical");
                break;
            }
            case 2: {
                this.type.set((Object)"Horizontal");
                break;
            }
            case 3: {
                this.type.set((Object)"Border");
            }
        }
        ((ClientData)this).type = n;
    }

    public StringProperty typeProperty() {
        return this.type;
    }

    public String getCell0() {
        return (String)this.cell0.get();
    }

    public int getCell0Int() {
        return this.cell[0];
    }

    public void setCell0(String string) {
        this.setCell0(Integer.parseInt(string));
    }

    public void setCell0(int n) {
        if (n < 0) {
            this.cell0.set((Object)Lang.getString("size.full"));
        } else {
            this.cell0.set((Object)String.valueOf(n));
        }
        this.cell[0] = n;
    }

    public StringProperty cell0Property() {
        return this.cell0;
    }

    public String getCell1() {
        return (String)this.cell1.get();
    }

    public int getCell1Int() {
        return this.cell[1];
    }

    public void setCell1(String string) {
        this.cell1.set((Object)string);
        this.cell[1] = Integer.parseInt(string);
    }

    public void setCell1(int n) {
        this.cell1.set((Object)String.valueOf(n));
        this.cell[1] = n;
    }

    public StringProperty cell1Property() {
        return this.cell1;
    }

    public String getCell2() {
        return (String)this.cell2.get();
    }

    public int getCell2Int() {
        return this.cell[2];
    }

    public void setCell2(String string) {
        this.cell2.set((Object)string);
        this.cell[2] = Integer.parseInt(string);
    }

    public void setCell2(int n) {
        this.cell2.set((Object)String.valueOf(n));
        this.cell[2] = n;
    }

    public StringProperty cell2Property() {
        return this.cell2;
    }

    public String getCell3() {
        return (String)this.cell3.get();
    }

    public int getCell3Int() {
        return this.cell[3];
    }

    public void setCell3(String string) {
        this.cell3.set((Object)string);
        this.cell[3] = Integer.parseInt(string);
    }

    public void setCell3(int n) {
        this.cell3.set((Object)String.valueOf(n));
        this.cell[3] = n;
    }

    public StringProperty cell3Property() {
        return this.cell3;
    }

    public int getCellInt(int n) {
        return this.cell[n];
    }

    public int[] getCellInt() {
        return this.cell;
    }

    public String getPlay0() {
        return (String)this.play0.get();
    }

    public String getPlayItem0() {
        return this.parseItem(this.playitem[0]);
    }

    public void setPlay0(String string) {
        this.setPlay(0, string);
    }

    public StringProperty play0Property() {
        return this.play0;
    }

    public String getPlay1() {
        return (String)this.play1.get();
    }

    public String getPlayItem1() {
        return this.parseItem(this.playitem[1]);
    }

    public void setPlay1(String string) {
        this.setPlay(1, string);
    }

    public StringProperty play1Property() {
        return this.play1;
    }

    public String getPlay2() {
        return (String)this.play2.get();
    }

    public String getPlayItem2() {
        return this.parseItem(this.playitem[2]);
    }

    public void setPlay2(String string) {
        this.setPlay(2, string);
    }

    public StringProperty play2Property() {
        return this.play2;
    }

    public String getPlay3() {
        return (String)this.play3.get();
    }

    public String getPlayItem3() {
        return this.parseItem(this.playitem[3]);
    }

    public void setPlay3(String string) {
        this.setPlay(3, string);
    }

    public StringProperty play3Property() {
        return this.play3;
    }

    public String getPlayTime() {
        for (int i = 0; i < this.play.length; ++i) {
            String string = this.play[i];
            if (string == null || string.isEmpty()) continue;
            return sdf2.format(new Date(Long.parseLong(string)));
        }
        return Lang.getString("overview.noplay");
    }

    public void setPlay(int n, String string) {
        StringProperty stringProperty;
        switch (n) {
            case 0: {
                stringProperty = this.play0;
                break;
            }
            case 1: {
                stringProperty = this.play1;
                break;
            }
            case 2: {
                stringProperty = this.play2;
                break;
            }
            case 3: {
                stringProperty = this.play3;
                break;
            }
            default: {
                return;
            }
        }
        if (string == null || string.isEmpty() || string.equals("null")) {
            stringProperty.set((Object)Lang.getString("overview.noplay"));
            this.play[n] = "";
            this.playitem[n] = null;
            return;
        }
        int n2 = string.indexOf("=");
        if (n2 < 0) {
            stringProperty.set((Object)string);
            this.play[n] = "";
            this.playitem[n] = null;
        } else {
            String string2 = string.substring(0, n2);
            if (string2 == null || string2.isEmpty() || string2.equals("null")) {
                stringProperty.set((Object)Lang.getString("overview.noplay"));
                this.play[n] = "";
                this.playitem[n] = null;
            } else {
                String string3 = string.substring(n2 + 1);
                StringBuilder stringBuilder = new StringBuilder();
                int n3 = 0;
                block35: for (String string4 : string3.split("\u02fe")) {
                    if (n3 > 0) {
                        --n3;
                        continue;
                    }
                    String[] stringArray = string4.split("\u02ff");
                    switch (stringArray[0]) {
                        case "video": {
                            stringBuilder.append(", v").append(stringArray[1]);
                            continue block35;
                        }
                        case "image": {
                            stringBuilder.append(", i").append(stringArray[1]);
                            continue block35;
                        }
                        case "pdf": {
                            stringBuilder.append(", f").append(stringArray[1]);
                            continue block35;
                        }
                        case "ppt": {
                            stringBuilder.append(", p").append(stringArray[1]);
                            continue block35;
                        }
                        case "text1": 
                        case "text2": {
                            stringBuilder.append(", t").append(stringArray[1]);
                            continue block35;
                        }
                        case "web": {
                            stringBuilder.append(", w").append(stringArray[1]);
                            continue block35;
                        }
                        case "style1": {
                            stringBuilder.append(", r").append(stringArray[1]);
                            continue block35;
                        }
                        case "individual": {
                            n3 = stringArray[7].split("_").length;
                            stringBuilder.append(", e").append(n3);
                            continue block35;
                        }
                        case "sync": {
                            stringBuilder.append(", s").append(stringArray[1]);
                            continue block35;
                        }
                        case "synclan": {
                            stringBuilder.append(", l").append(stringArray[1]);
                            continue block35;
                        }
                        case "expands": {
                            continue block35;
                        }
                        case "expanded": {
                            continue block35;
                        }
                        default: {
                            stringBuilder.append(", ?").append(stringArray[0]);
                        }
                    }
                }
                stringProperty.set((Object)stringBuilder.substring(2));
                this.play[n] = string2;
                this.playitem[n] = string3;
            }
        }
    }

    public List<PlaylistData> getPlaylist(int n) {
        ObservableList observableList = FXCollections.observableArrayList();
        if (this.playitem[n] != null) {
            boolean bl = false;
            int n2 = -1;
            int n3 = -1;
            for (String string : this.playitem[n].split("\u02fe")) {
                PlaylistData playlistData;
                PlaylistData playlistData2;
                String[] stringArray = string.split("\u02ff");
                if (stringArray.length < 10) continue;
                PlayData playData = new PlayData();
                playData.type = stringArray[0];
                playData.time = Integer.parseInt(stringArray[1]);
                playData.font = Integer.parseInt(stringArray[2]);
                playData.show[0] = Integer.parseInt(stringArray[3]);
                playData.show[1] = Integer.parseInt(stringArray[4]);
                playData.show[2] = Integer.parseInt(stringArray[5]);
                playData.show[3] = Integer.parseInt(stringArray[6]);
                playData.open = Long.parseLong(stringArray[7]);
                playData.till = Long.parseLong(stringArray[8]);
                if (playData.type.equals("video") || playData.type.equals("image") || playData.type.equals("pdf") || playData.type.equals("ppt")) {
                    playData.file = new File(stringArray[9]);
                    playlistData2 = new PlaylistData(playData);
                    if (bl) {
                        bl = false;
                        playlistData = this.tempParsedPlaylist[n2].get(n3);
                        playlistData.addExpands(n, playlistData2);
                        playlistData2.setExpanded(n2, playlistData);
                    }
                    observableList.add((Object)playlistData2);
                    continue;
                }
                if (playData.type.equals("text1") || playData.type.equals("text2") || playData.type.equals("web") || playData.type.equals("style1")) {
                    playData.text = stringArray[9];
                    playlistData2 = new PlaylistData(playData);
                    if (bl) {
                        bl = false;
                        playlistData = this.tempParsedPlaylist[n2].get(n3);
                        playlistData.addExpands(n, playlistData2);
                        playlistData2.setExpanded(n2, playlistData);
                    }
                    observableList.add((Object)playlistData2);
                    continue;
                }
                if (playData.type.equals("individual")) {
                    playData.text = stringArray[9];
                    playlistData2 = new PlaylistData(playData);
                    observableList.add((Object)playlistData2);
                    continue;
                }
                if (playData.type.equals("sync") || playData.type.equals("synclan")) {
                    observableList.add((Object)new PlaylistData(playData));
                    continue;
                }
                if (playData.type.equals("expands") || !playData.type.equals("expanded")) continue;
                bl = true;
                n2 = Integer.parseInt(stringArray[1]);
                n3 = Integer.parseInt(stringArray[2]);
                if (n2 >= 0 && n3 >= 0) continue;
                bl = false;
            }
        }
        this.tempParsedPlaylist[n] = observableList;
        return observableList;
    }

    public String getReserve(int n) {
        for (int i = 0; i < 4; ++i) {
            long l = this.reservetime[i][n];
            if (l <= 0L) continue;
            return sdf3.format(new Date(l));
        }
        return Lang.getString("reserve.none");
    }

    public String getReserve(int n, int n2) {
        long l = this.reservetime[n][n2];
        if (l > 0L) {
            return sdf3.format(new Date(l));
        }
        return Lang.getString("reserve.none");
    }

    public String getReserveItem(int n, int n2) {
        if (this.reservetime[n][n2] > 0L) {
            String string = this.reserveitem[n][n2];
            StringBuilder stringBuilder = new StringBuilder();
            int n3 = 0;
            block32: for (String string2 : string.split("\u02fe")) {
                if (n3 > 0) {
                    --n3;
                    continue;
                }
                String[] stringArray = string2.split("\u02ff");
                switch (stringArray[0]) {
                    case "": 
                    case "null": {
                        return Lang.getString("reserve.none");
                    }
                    case "video": {
                        stringBuilder.append(", v").append(stringArray[1]);
                        continue block32;
                    }
                    case "image": {
                        stringBuilder.append(", i").append(stringArray[1]);
                        continue block32;
                    }
                    case "pdf": {
                        stringBuilder.append(", f").append(stringArray[1]);
                        continue block32;
                    }
                    case "ppt": {
                        stringBuilder.append(", p").append(stringArray[1]);
                        continue block32;
                    }
                    case "text1": 
                    case "text2": {
                        stringBuilder.append(", t").append(stringArray[1]);
                        continue block32;
                    }
                    case "web": {
                        stringBuilder.append(", w").append(stringArray[1]);
                        continue block32;
                    }
                    case "style1": {
                        stringBuilder.append(", r").append(stringArray[1]);
                        continue block32;
                    }
                    case "individual": {
                        n3 = stringArray[7].split("_").length;
                        stringBuilder.append(", e").append(n3);
                        continue block32;
                    }
                    case "sync": {
                        stringBuilder.append(", s").append(stringArray[1]);
                        continue block32;
                    }
                    case "synclan": {
                        stringBuilder.append(", l").append(stringArray[1]);
                        continue block32;
                    }
                    case "expands": {
                        continue block32;
                    }
                    case "expanded": {
                        continue block32;
                    }
                    default: {
                        stringBuilder.append(", ?").append(stringArray[0]);
                    }
                }
            }
            return stringBuilder.substring(2);
        }
        return Lang.getString("reserve.none");
    }

    public String getReserveItemItem(int n, int n2) {
        return this.parseItem(this.reserveitem[n][n2]);
    }

    public long getReserveTime(int n, int n2) {
        return this.reservetime[n][n2];
    }

    public List<PlaylistData> getReservelist(int n, int n2) {
        ObservableList observableList = FXCollections.observableArrayList();
        if (this.reserveitem[n][n2] != null) {
            boolean bl = false;
            int n3 = -1;
            int n4 = -1;
            for (String string : this.reserveitem[n][n2].split("\u02fe")) {
                PlaylistData playlistData;
                PlaylistData playlistData2;
                String[] stringArray = string.split("\u02ff");
                if (stringArray.length < 10) continue;
                PlayData playData = new PlayData();
                playData.type = stringArray[0];
                playData.time = Integer.parseInt(stringArray[1]);
                playData.font = Integer.parseInt(stringArray[2]);
                playData.show[0] = Integer.parseInt(stringArray[3]);
                playData.show[1] = Integer.parseInt(stringArray[4]);
                playData.show[2] = Integer.parseInt(stringArray[5]);
                playData.show[3] = Integer.parseInt(stringArray[6]);
                if (playData.type.equals("video") || playData.type.equals("image") || playData.type.equals("pdf") || playData.type.equals("ppt")) {
                    playData.file = new File(stringArray[9]);
                    playlistData2 = new PlaylistData(playData);
                    if (bl) {
                        bl = false;
                        playlistData = this.tempParsedReservelist[n3][n2].get(n4);
                        playlistData.addExpands(n, playlistData2);
                        playlistData2.setExpanded(n3, playlistData);
                    }
                    observableList.add((Object)playlistData2);
                    continue;
                }
                if (playData.type.equals("text1") || playData.type.equals("text2") || playData.type.equals("web") || playData.type.equals("style1")) {
                    playData.text = stringArray[9];
                    playlistData2 = new PlaylistData(playData);
                    if (bl) {
                        bl = false;
                        playlistData = this.tempParsedReservelist[n3][n2].get(n4);
                        playlistData.addExpands(n, playlistData2);
                        playlistData2.setExpanded(n3, playlistData);
                    }
                    observableList.add((Object)playlistData2);
                    continue;
                }
                if (playData.type.equals("individual")) {
                    playData.text = stringArray[9];
                    playlistData2 = new PlaylistData(playData);
                    observableList.add((Object)playlistData2);
                    continue;
                }
                if (playData.type.equals("sync") || playData.type.equals("synclan")) {
                    observableList.add((Object)new PlaylistData(playData));
                    continue;
                }
                if (playData.type.equals("expands") || !playData.type.equals("expanded")) continue;
                bl = true;
                n3 = Integer.parseInt(stringArray[1]);
                n4 = Integer.parseInt(stringArray[2]);
                if (n3 >= 0 && n4 >= 0) continue;
                bl = false;
            }
        }
        this.tempParsedReservelist[n][n2] = observableList;
        return observableList;
    }

    public void setReserve(int n, int n2, String string) {
        int n3 = string.indexOf("=");
        int n4 = string.indexOf("=", n3 + 1);
        if (n3 < 0 || n4 < 0) {
            this.reserve[n][n2] = "";
            this.reservetime[n][n2] = 0L;
            this.reserveitem[n][n2] = null;
        } else {
            this.reserve[n][n2] = string.substring(0, n3);
            this.reservetime[n][n2] = Long.parseLong(string.substring(n3 + 1, n4));
            this.reserveitem[n][n2] = string.substring(n4 + 1);
        }
    }

    public void setBgm(String string) {
        if (string == null || string.isEmpty() || string.startsWith("null") || !string.contains("=")) {
            this.bgm = "";
            this.bgmitem = null;
            return;
        }
        String[] stringArray = string.split("=");
        this.bgm = stringArray[0];
        this.bgmitem = stringArray[1];
    }

    public String getBgm() {
        if (this.bgm == null || this.bgm.isEmpty() || this.bgm.equals("null")) {
            return Lang.getString("overview.noplay");
        }
        return this.bgmitem.split("\u02fe").length + " (" + sdf2.format(new Date(Long.parseLong(this.bgm))) + ")";
    }

    public String getBgmItem() {
        if (this.bgmitem == null || this.bgmitem.isEmpty() || this.bgmitem.equals("null")) {
            return Lang.getString("overview.noplay");
        }
        return this.parseItem(this.bgmitem);
    }

    public List<PlaylistData> getBgmlist() {
        ObservableList observableList = FXCollections.observableArrayList();
        if (this.bgmitem != null) {
            for (String string : this.bgmitem.split("\u02fe")) {
                String[] stringArray = string.split("\u02ff");
                if (stringArray.length < 10 || !stringArray[0].equals("music")) continue;
                PlayData playData = new PlayData();
                playData.type = "music";
                playData.show[0] = Integer.parseInt(stringArray[3]);
                playData.show[1] = Integer.parseInt(stringArray[4]);
                playData.show[2] = Integer.parseInt(stringArray[5]);
                playData.show[3] = Integer.parseInt(stringArray[6]);
                playData.file = new File(stringArray[9]);
                PlaylistData playlistData = new PlaylistData(playData);
                observableList.add((Object)playlistData);
            }
        }
        return observableList;
    }

    public String getOwner() {
        return (String)this.owner.get();
    }

    public void setOwner(String string) {
        this.owner.set((Object)string);
        ((ClientData)this).owner = string;
    }

    public StringProperty ownerProperty() {
        return this.owner;
    }

    public String getLicense() {
        return (String)this.license.get();
    }

    public long getLicenseLong() {
        return ((ClientData)this).license;
    }

    public void setLicense(String string) {
        this.license.set((Object)string);
        ((ClientData)this).license = Long.parseLong(string);
    }

    public void setLicense(long l) {
        if (l == 0L) {
            this.license.set((Object)"");
        } else {
            this.license.set((Object)sdf1.format(new Date(l)));
        }
        ((ClientData)this).license = l;
    }

    public StringProperty licenseProperty() {
        return this.license;
    }

    public String getTouch() {
        return (String)this.touch.get();
    }

    public void setTouch(String string) {
        this.touch.set((Object)string);
        ((ClientData)this).touch = Long.parseLong(string);
    }

    public void setTouch(long l) {
        if (l == 0L) {
            this.touch.set((Object)Lang.getString("app.noinfo"));
        } else {
            this.touch.set((Object)sdf1.format(new Date(l)));
        }
        ((ClientData)this).touch = l;
    }

    public StringProperty touchProperty() {
        return this.touch;
    }

    public String getOnoff() {
        return (String)this.onoff.get();
    }

    public boolean getOnoffBoolean() {
        return ((ClientData)this).onoff;
    }

    public void setOnoff(String string) {
        this.onoff.set((Object)string);
        ((ClientData)this).onoff = string.equals(Lang.getString("overview.connect"));
    }

    public void setOnoff(boolean bl) {
        this.onoff.set((Object)(bl ? Lang.getString("overview.connect") : Lang.getString("overview.disconnect")));
        ((ClientData)this).onoff = bl;
    }

    public void setOnoff(int n) {
        this.onoff.set((Object)(n == 1 ? Lang.getString("overview.connect") : Lang.getString("overview.disconnect")));
        ((ClientData)this).onoff = n == 1;
    }

    public StringProperty onoffProperty() {
        return this.onoff;
    }

    public String getIp() {
        if (this.ip.isEmpty()) {
            return "";
        }
        return "(" + this.ip + ")";
    }

    public String getIpOnly() {
        if (this.ip.isEmpty()) {
            return "";
        }
        int n = this.ip.indexOf(" ");
        if (n < 0) {
            return this.ip;
        }
        return this.ip.substring(0, n);
    }

    public boolean getChecked() {
        return this.check.get();
    }

    public void setChecked(boolean bl) {
        this.check.set(bl);
        this.select = bl;
    }

    public ObservableBooleanValue checkProperty() {
        return this.check;
    }

    private String parseItem(String string) {
        if (string == null) {
            return "";
        }
        String[] stringArray = string.split("\u02fe");
        if (stringArray == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string2 : stringArray) {
            if (string2 == null || string2.equals("null")) continue;
            String[] stringArray2 = string2.split("\u02ff");
            if (stringArray2[0].equals("sync") || stringArray2[0].equals("synclan")) {
                stringBuilder.append(Lang.getString("play.type." + stringArray2[0] + ".n", stringArray2[1])).append("\n");
                continue;
            }
            if (stringArray2[0].equals("expands") || stringArray2[0].equals("expanded")) {
                stringBuilder.append(Lang.getString("play.expanded", stringArray2[1])).append(" ");
                continue;
            }
            if (stringArray2[0].equals("web") || stringArray2[0].equals("text1") || stringArray2[0].equals("text2") || stringArray2[0].equals("style1")) {
                stringBuilder.append(Lang.getString("play.type." + stringArray2[0])).append(" [").append(Lang.getString("play.unit.time", stringArray2[1])).append("] " + stringArray2[9]).append("\n");
                continue;
            }
            if (stringArray2[0].equals("music")) {
                stringBuilder.append(Util.getFileName(stringArray2[9])).append("\n");
                continue;
            }
            if (stringArray2[0].equals("individual")) {
                arrayList.clear();
                arrayList.addAll(Arrays.asList(stringArray2[9].split("_")));
                stringBuilder.append(Lang.getString("play.type." + stringArray2[0])).append(" (").append(arrayList.size()).append(")").append("\n");
                continue;
            }
            if (!arrayList.isEmpty()) {
                stringBuilder.append(" (").append((String)arrayList.remove(0)).append(") ");
            }
            stringBuilder.append(Lang.getString("play.type." + stringArray2[0])).append(" [").append(Lang.getString("play.unit.time", stringArray2[1])).append("] " + Util.getFileName(stringArray2[9])).append("\n");
        }
        return stringBuilder.toString().trim();
    }
}

