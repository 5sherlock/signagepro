/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.LinkOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.ybroad.dispy.lib.ClientData;
import net.ybroad.dispy.lib.IsleData;
import net.ybroad.dispy.lib.Log;
import net.ybroad.dispy.lib.PlayData;
import net.ybroad.dispy.lib.Progress;
import net.ybroad.dispy.lib.SubData;
import net.ybroad.dispy.lib.UnionData;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.lib.Util;

public abstract class MySocket
extends Thread {
    public static final String NULL = "\u02fc";
    public static final String DIVIDER = "\u02fd";
    public static final String SUBDIV1 = "\u02fe";
    public static final String SUBDIV2 = "\u02ff";
    protected Socket socket = null;
    protected DataInputStream in = null;
    protected DataOutputStream out = null;
    protected int version = 0;
    private boolean closed = false;

    public MySocket() {
    }

    public MySocket(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream, int n) {
        this.socket = socket;
        this.in = dataInputStream;
        this.out = dataOutputStream;
        this.version = n;
    }

    @Override
    public void run() {
        try {
            Log.out("(MySocket start) " + this.toString());
            this.onLoop();
        }
        catch (Exception exception) {
            Log.out("(MySocket error) " + this.toString());
            Log.out(exception);
            this.onLoopError(exception);
        }
        finally {
            this.close();
            Log.out("(MySocket close) " + this.toString());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void close() {
        try {
            if (this.in != null) {
                this.in.close();
            }
        }
        catch (Exception exception) {
            Log.out(exception);
        }
        finally {
            this.in = null;
        }
        try {
            if (this.out != null) {
                this.out.close();
            }
        }
        catch (Exception exception) {
            Log.out(exception);
        }
        finally {
            this.out = null;
        }
        try {
            if (this.socket != null) {
                this.socket.close();
            }
        }
        catch (Exception exception) {
            Log.out(exception);
        }
        finally {
            this.socket = null;
        }
        try {
            if (!this.closed) {
                this.onClose();
                this.closed = true;
            }
        }
        catch (Exception exception) {
            Log.out(exception);
        }
    }

    @Override
    public String toString() {
        return "MySocket";
    }

    protected abstract void onLoop() throws Exception;

    protected abstract void onClose() throws Exception;

    protected void onLoopError(Throwable throwable) {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendClientData2Server(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "client:" + clientData.socket.version;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "id:" + clientData.id;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "info:" + clientData.info;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "spec:" + clientData.spec;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "free:" + clientData.free;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "ip:" + clientData.ip;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "screen:" + clientData.width + DIVIDER + clientData.height;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "bgm:" + clientData.bgm;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "cell:" + clientData.type + DIVIDER + clientData.cell[0] + DIVIDER + clientData.cell[1] + DIVIDER + clientData.cell[2] + DIVIDER + clientData.cell[3];
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "play:" + clientData.play[0] + DIVIDER + clientData.play[1] + DIVIDER + clientData.play[2] + DIVIDER + clientData.play[3];
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendClientPlay2Server(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "screen:" + clientData.width + DIVIDER + clientData.height;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "bgm:" + clientData.bgm;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = "play:" + clientData.play[0] + DIVIDER + clientData.play[1] + DIVIDER + clientData.play[2] + DIVIDER + clientData.play[3];
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendUserData2Server(UserData userData) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string = "user:" + userData.socket.version;
                    userData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    string = (userData.member ? "member:" : "login:") + userData.name + DIVIDER + Util.getSha256(userData.pw);
                    userData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendRegister2Server(UserData userData, UserData userData2) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string = "member:" + userData2.name + DIVIDER + Util.getSha256(userData2.pw);
                    userData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendAdd2Server(UserData userData, ClientData clientData) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string = "add:" + clientData.id + DIVIDER + clientData.name + DIVIDER + clientData.owner + DIVIDER + (clientData.group.isEmpty() ? NULL : clientData.group);
                    userData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendRemove2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "remove:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendRename2Server(UserData userData, ClientData clientData) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string = "rename:" + clientData.id + DIVIDER + clientData.name + DIVIDER + clientData.owner + DIVIDER + (clientData.group.isEmpty() ? NULL : clientData.group) + DIVIDER + clientData.mbps;
                    userData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLicense2Server(UserData userData, String string, long l) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "license:" + string + DIVIDER + l;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendScreen2Server(UserData userData, String string, File file, File file2, boolean bl, String string2, String string3, int n, int n2, int n3) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    long l = file == null ? 0L : (file.isFile() ? file.length() : -1L);
                    String string4 = "logo:" + string + DIVIDER + (file == null ? "" : file.getAbsolutePath()) + DIVIDER + l;
                    userData.socket.out.writeUTF(string4);
                    Log.out("(TX) " + string4);
                    if (l > 0L) {
                        int n4;
                        FileInputStream fileInputStream = new FileInputStream(file);
                        byte[] byArray = new byte[102400];
                        int n5 = 0;
                        while ((n4 = fileInputStream.read(byArray)) > 0) {
                            userData.socket.out.write(byArray, 0, n4);
                            if ((n5 += n4) < 0x100000) continue;
                            Log.out("(TX) ~bytes: " + n5);
                            userData.socket.out.flush();
                            n5 = 0;
                        }
                        if (n5 > 0) {
                            Log.out("(TX) ~bytes: " + n5);
                        }
                        fileInputStream.close();
                    }
                    long l2 = file2 == null ? 0L : (file2.isFile() ? file2.length() : -1L);
                    string4 = "background:" + string + DIVIDER + (file2 == null ? "" : file2.getAbsolutePath()) + DIVIDER + l2;
                    userData.socket.out.writeUTF(string4);
                    Log.out("(TX) " + string4);
                    if (l2 > 0L) {
                        int n6;
                        FileInputStream fileInputStream = new FileInputStream(file2);
                        byte[] byArray = new byte[102400];
                        int n7 = 0;
                        while ((n6 = fileInputStream.read(byArray)) > 0) {
                            userData.socket.out.write(byArray, 0, n6);
                            if ((n7 += n6) < 0x100000) continue;
                            Log.out("(TX) ~bytes: " + n7);
                            userData.socket.out.flush();
                            n7 = 0;
                        }
                        if (n7 > 0) {
                            Log.out("(TX) ~bytes: " + n7);
                        }
                        fileInputStream.close();
                    }
                    string4 = "clock:" + string + DIVIDER + (bl ? 1 : 0);
                    userData.socket.out.writeUTF(string4);
                    Log.out("(TX) " + string4);
                    if (string2 != null) {
                        string4 = "clockcolor:" + string + DIVIDER + string2;
                        userData.socket.out.writeUTF(string4);
                        Log.out("(TX) " + string4);
                    }
                    if (string3 != null) {
                        string4 = "clockpos:" + string + DIVIDER + string3;
                        userData.socket.out.writeUTF(string4);
                        Log.out("(TX) " + string4);
                    }
                    if (n3 >= 0) {
                        string4 = "rotate:" + string + DIVIDER + n3;
                        userData.socket.out.writeUTF(string4);
                        Log.out("(TX) " + string4);
                    }
                    string4 = "num:" + string + DIVIDER + n + DIVIDER + n2;
                    userData.socket.out.writeUTF(string4);
                    Log.out("(TX) " + string4);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendTouch2Server(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "touch:" + clientData.free;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendTouch2Server(UserData userData) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string = "touch";
                    userData.socket.out.writeUTF("touch");
                    Log.out("(TX) " + string);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendCell2Server(UserData userData, String string, int n, int[] nArray) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "cell:" + string + DIVIDER + n + DIVIDER + nArray[0] + DIVIDER + nArray[1] + DIVIDER + nArray[2] + DIVIDER + nArray[3];
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendUnion2Server(UserData userData, String string, int n, int n2, ArrayList<UnionData> arrayList) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "union:" + string + DIVIDER + n + DIVIDER + n2 + DIVIDER + arrayList.size();
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    for (UnionData unionData : arrayList) {
                        string2 = unionData.id + DIVIDER + (unionData.master ? "1" : "0") + DIVIDER + unionData.x + DIVIDER + unionData.y + DIVIDER + unionData.w + DIVIDER + unionData.h + DIVIDER + unionData.xpos + DIVIDER + unionData.ypos;
                        userData.socket.out.writeUTF(string2);
                        Log.out("(TX) " + string2);
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendUnionClient2Server(UserData userData, String string, int n, int n2, int n3, int n4, double d) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "unionclient:" + string + DIVIDER + n + DIVIDER + n2 + DIVIDER + n3 + DIVIDER + n4 + DIVIDER + d;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendOnOff2Server(UserData userData, String string, int[] nArray, int[] nArray2) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "onoff:" + string + DIVIDER + nArray[0] + DIVIDER + nArray[1] + DIVIDER + nArray[2] + DIVIDER + nArray[3] + DIVIDER + nArray[4] + DIVIDER + nArray[5] + DIVIDER + nArray[6] + DIVIDER + nArray2[0] + DIVIDER + nArray2[1] + DIVIDER + nArray2[2] + DIVIDER + nArray2[3] + DIVIDER + nArray2[4] + DIVIDER + nArray2[5] + DIVIDER + nArray2[6];
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendPlayGroup2Server(UserData userData, String string, List<List<PlayData>> list, Progress progress) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            int n = 0;
            for (List<PlayData> object2 : list) {
                n += object2.size();
            }
            progress.set("progress.play2server", n);
            if (userData.socket != null) {
                byte[] byArray = new byte[102400];
                try {
                    Object object3 = "group:" + list.size();
                    userData.socket.out.writeUTF((String)object3);
                    Log.out("(TX) " + (String)object3);
                    for (int i = 0; i < list.size(); ++i) {
                        List<PlayData> list2 = list.get(i);
                        int n2 = 0;
                        for (PlayData playData : list2) {
                            if (playData.getExpandsCell() != null) {
                                n2 += 2;
                                continue;
                            }
                            if (playData.getExpandedCell() < 0) continue;
                            ++n2;
                        }
                        object3 = "play:" + i + DIVIDER + string + DIVIDER + (list2.size() + n2);
                        userData.socket.out.writeUTF((String)object3);
                        Log.out("(TX) " + (String)object3);
                        n2 = 0;
                        Object object2 = null;
                        for (PlayData playData : list2) {
                            int n3;
                            if (object2 == null && (object2 = (Object)playData.getExpandsCell()) != null) {
                                object3 = "expands:" + (int)object2[0] + DIVIDER + (int)object2[1] + DIVIDER + (int)object2[2] + DIVIDER + (int)object2[3];
                                userData.socket.out.writeUTF((String)object3);
                                Log.out("(TX) " + (String)object3);
                            }
                            if ((n3 = playData.getExpandedCell()) >= 0) {
                                object3 = "expanded:" + n3 + DIVIDER + list.get(n3).indexOf(playData.getExpandedData());
                                userData.socket.out.writeUTF((String)object3);
                                Log.out("(TX) " + (String)object3);
                                playData = playData.getExpandedData();
                                playData.changed = false;
                            }
                            switch (playData.type) {
                                case "video": 
                                case "image": {
                                    int n4;
                                    String string2 = playData.file.getAbsolutePath();
                                    long l = playData.changed ? (playData.alternative != null ? playData.alternative.length() : playData.file.length()) : -1L;
                                    object3 = playData.type + ":" + playData.time + DIVIDER + playData.show[0] + DIVIDER + playData.show[1] + DIVIDER + playData.show[2] + DIVIDER + playData.show[3] + DIVIDER + playData.open + DIVIDER + playData.till + DIVIDER + string2 + DIVIDER + l;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    if (l <= 0L) break;
                                    FileInputStream fileInputStream = playData.alternative != null ? new FileInputStream(playData.alternative) : new FileInputStream(playData.file);
                                    long l2 = 0L;
                                    int n5 = 0;
                                    while ((n4 = fileInputStream.read(byArray)) > 0) {
                                        userData.socket.out.write(byArray, 0, n4);
                                        l2 += (long)n4;
                                        if ((n5 += n4) < 0x100000) continue;
                                        Log.out("(TX) ~bytes: " + n5);
                                        userData.socket.out.flush();
                                        n5 = 0;
                                        progress.update(100.0 * (double)l2 / (double)l);
                                    }
                                    if (n5 > 0) {
                                        Log.out("(TX) ~bytes: " + n5);
                                        progress.update(100.0);
                                    }
                                    fileInputStream.close();
                                    break;
                                }
                                case "pdf": 
                                case "ppt": {
                                    if (object2 != null) {
                                        n2 = playData.font;
                                    }
                                    String string3 = playData.file.getAbsolutePath();
                                    long l = playData.changed ? (long)playData.font : -1L;
                                    object3 = playData.type + ":" + playData.time + DIVIDER + playData.show[0] + DIVIDER + playData.show[1] + DIVIDER + playData.show[2] + DIVIDER + playData.show[3] + DIVIDER + playData.open + DIVIDER + playData.till + DIVIDER + string3 + DIVIDER + l;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    break;
                                }
                                case "text1": 
                                case "text2": 
                                case "web": 
                                case "style1": {
                                    object3 = playData.type + ":" + playData.time + DIVIDER + playData.font + DIVIDER + playData.show[0] + DIVIDER + playData.show[1] + DIVIDER + playData.show[2] + DIVIDER + playData.show[3] + DIVIDER + playData.open + DIVIDER + playData.till;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    object3 = playData.text;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    break;
                                }
                                case "individual": {
                                    object3 = playData.type + ":" + playData.text;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    break;
                                }
                                default: {
                                    if (playData.type.startsWith("synclan")) {
                                        object3 = "synclan:" + playData.type.substring("synclan".length());
                                        userData.socket.out.writeUTF((String)object3);
                                        Log.out("(TX) " + (String)object3);
                                        break;
                                    }
                                    if (!playData.type.startsWith("sync")) break;
                                    object3 = "sync:" + playData.type.substring("sync".length());
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                }
                            }
                            if (object2 != null && --n2 < 0) {
                                object2 = null;
                                object3 = "expands:0\u02fd0\u02fd0\u02fd0";
                                userData.socket.out.writeUTF("expands:0\u02fd0\u02fd0\u02fd0");
                                Log.out("(TX) " + (String)object3);
                            }
                            progress.next();
                        }
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
            progress.update(-1.0);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendReserveGroup2Server(UserData userData, String string, int n, long l, List<List<PlayData>> list, Progress progress) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            int n2 = 0;
            for (List<PlayData> object2 : list) {
                n2 += object2.size();
            }
            progress.set("progress.reserve2server", n2);
            if (userData.socket != null) {
                byte[] byArray = new byte[102400];
                try {
                    Object object3 = "group:" + list.size();
                    userData.socket.out.writeUTF((String)object3);
                    Log.out("(TX) " + (String)object3);
                    for (int i = 0; i < list.size(); ++i) {
                        List<PlayData> list2 = list.get(i);
                        int n3 = 0;
                        for (PlayData playData : list2) {
                            if (playData.getExpandsCell() != null) {
                                n3 += 2;
                                continue;
                            }
                            if (playData.getExpandedCell() < 0) continue;
                            ++n3;
                        }
                        object3 = "reserve:" + i + DIVIDER + string + DIVIDER + n + DIVIDER + l + DIVIDER + (list2.size() + n3);
                        userData.socket.out.writeUTF((String)object3);
                        Log.out("(TX) " + (String)object3);
                        n3 = 0;
                        Object object2 = null;
                        for (PlayData playData : list2) {
                            int n4;
                            object2 = playData.getExpandsCell();
                            if (object2 != null) {
                                object3 = "expands:" + (int)object2[0] + DIVIDER + (int)object2[1] + DIVIDER + (int)object2[2] + DIVIDER + (int)object2[3];
                                userData.socket.out.writeUTF((String)object3);
                                Log.out("(TX) " + (String)object3);
                            }
                            if ((n4 = playData.getExpandedCell()) >= 0) {
                                object3 = "expanded:" + n4 + DIVIDER + list.get(n4).indexOf(playData.getExpandedData());
                                userData.socket.out.writeUTF((String)object3);
                                Log.out("(TX) " + (String)object3);
                                playData = playData.getExpandedData();
                                playData.changed = false;
                            }
                            switch (playData.type) {
                                case "video": 
                                case "image": {
                                    int n5;
                                    String string2 = playData.file.getAbsolutePath();
                                    long l2 = playData.changed ? playData.file.length() : -1L;
                                    object3 = playData.type + ":" + playData.time + DIVIDER + playData.show[0] + DIVIDER + playData.show[1] + DIVIDER + playData.show[2] + DIVIDER + playData.show[3] + DIVIDER + playData.open + DIVIDER + playData.till + DIVIDER + string2 + DIVIDER + l2;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    if (l2 <= 0L) break;
                                    FileInputStream fileInputStream = new FileInputStream(playData.file);
                                    long l3 = 0L;
                                    int n6 = 0;
                                    while ((n5 = fileInputStream.read(byArray)) > 0) {
                                        userData.socket.out.write(byArray, 0, n5);
                                        l3 += (long)n5;
                                        if ((n6 += n5) < 0x100000) continue;
                                        Log.out("(TX) ~bytes: " + n6);
                                        userData.socket.out.flush();
                                        n6 = 0;
                                        progress.update(100.0 * (double)l3 / (double)l2);
                                    }
                                    if (n6 > 0) {
                                        Log.out("(TX) ~bytes: " + n6);
                                        progress.update(100.0);
                                    }
                                    fileInputStream.close();
                                    break;
                                }
                                case "pdf": 
                                case "ppt": {
                                    if (object2 != null) {
                                        n3 = playData.font;
                                    }
                                    String string3 = playData.file.getAbsolutePath();
                                    long l2 = playData.changed ? (long)playData.font : -1L;
                                    object3 = playData.type + ":" + playData.time + DIVIDER + playData.show[0] + DIVIDER + playData.show[1] + DIVIDER + playData.show[2] + DIVIDER + playData.show[3] + DIVIDER + playData.open + DIVIDER + playData.till + DIVIDER + string3 + DIVIDER + l2;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    break;
                                }
                                case "text1": 
                                case "text2": 
                                case "web": 
                                case "style1": {
                                    object3 = playData.type + ":" + playData.time + DIVIDER + playData.font + DIVIDER + playData.show[0] + DIVIDER + playData.show[1] + DIVIDER + playData.show[2] + DIVIDER + playData.show[3] + DIVIDER + playData.open + DIVIDER + playData.till;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    object3 = playData.text;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    break;
                                }
                                case "individual": {
                                    object3 = playData.type + ":" + playData.text;
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                    break;
                                }
                                default: {
                                    if (playData.type.startsWith("synclan")) {
                                        object3 = "synclan:" + playData.type.substring("synclan".length());
                                        userData.socket.out.writeUTF((String)object3);
                                        Log.out("(TX) " + (String)object3);
                                        break;
                                    }
                                    if (!playData.type.startsWith("sync")) break;
                                    object3 = "sync:" + playData.type.substring("sync".length());
                                    userData.socket.out.writeUTF((String)object3);
                                    Log.out("(TX) " + (String)object3);
                                }
                            }
                            if (object2 != null && --n3 < 0) {
                                object2 = null;
                                object3 = "expands:0\u02fd0\u02fd0\u02fd0";
                                userData.socket.out.writeUTF("expands:0\u02fd0\u02fd0\u02fd0");
                                Log.out("(TX) " + (String)object3);
                            }
                            progress.next();
                        }
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
            progress.update(-1.0);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendBgm2Server(UserData userData, String string, List<PlayData> list, Progress progress) {
        if (userData == null || userData.socket == null || list == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            for (int i = list.size() - 1; i >= 0; --i) {
                if (Objects.equals(list.get((int)i).type, "music")) continue;
                list.remove(i);
            }
            progress.set("progress.bgm2server", list.size());
            if (userData.socket != null) {
                byte[] byArray = new byte[102400];
                try {
                    String string2 = "bgm:" + string + DIVIDER + list.size();
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    for (PlayData playData : list) {
                        int n;
                        String string3 = playData.file.getAbsolutePath();
                        long l = playData.changed ? playData.file.length() : -1L;
                        string2 = playData.type + ":0" + DIVIDER + playData.show[0] + DIVIDER + playData.show[1] + DIVIDER + playData.show[2] + DIVIDER + playData.show[3] + DIVIDER + "-1" + DIVIDER + "-1" + DIVIDER + string3 + DIVIDER + l;
                        userData.socket.out.writeUTF(string2);
                        Log.out("(TX) " + string2);
                        if (l <= 0L) continue;
                        FileInputStream fileInputStream = new FileInputStream(playData.file);
                        long l2 = 0L;
                        int n2 = 0;
                        while ((n = fileInputStream.read(byArray)) > 0) {
                            userData.socket.out.write(byArray, 0, n);
                            l2 += (long)n;
                            if ((n2 += n) < 0x100000) continue;
                            Log.out("(TX) ~bytes: " + n2);
                            userData.socket.out.flush();
                            n2 = 0;
                            progress.update(100.0 * (double)l2 / (double)l);
                        }
                        if (n2 > 0) {
                            Log.out("(TX) ~bytes: " + n2);
                            progress.update(100.0);
                        }
                        fileInputStream.close();
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
            progress.update(-1.0);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendReplace2Server(UserData userData, String string, int n, int n2) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "replace:" + string + DIVIDER + n + DIVIDER + n2;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendCopy2Server(UserData userData, String string, List<ClientData> list) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    for (ClientData clientData : list) {
                        String string2 = "copy:" + string + DIVIDER + clientData.id;
                        userData.socket.out.writeUTF(string2);
                        Log.out("(TX) " + string2);
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendDiscard2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "discard:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendExit2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "exit:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendRestart2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "restart:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendReboot2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "reboot:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendRemember2Server(UserData userData, String string, UserData userData2) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = NULL;
                    String string3 = NULL;
                    if (userData2.member) {
                        if (!userData2.name.isEmpty()) {
                            string2 = userData2.name;
                        }
                        if (!userData2.pw.isEmpty()) {
                            string3 = Util.getSha256(userData2.pw);
                        }
                    }
                    String string4 = NULL;
                    if (!userData2.memo.isEmpty()) {
                        string4 = userData2.memo;
                    }
                    String string5 = "remember:" + string + DIVIDER + string2 + DIVIDER + string3 + DIVIDER + userData2.limit + DIVIDER + string4;
                    userData.socket.out.writeUTF(string5);
                    Log.out("(TX) " + string5);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendIsle2Server(UserData userData, IsleData isleData) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string = "isle:" + isleData.id + DIVIDER + isleData.name + DIVIDER + isleData.limit;
                    userData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendSnapshot2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "snapshot:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendScreenCapture2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "screencap:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendSnapshot2Server(ClientData clientData, String string, File file) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                byte[] byArray = new byte[102400];
                try {
                    int n;
                    String string2 = "snapshot:" + string + DIVIDER + file.length();
                    clientData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    int n2 = 0;
                    while ((n = fileInputStream.read(byArray)) > 0) {
                        clientData.socket.out.write(byArray, 0, n);
                        if ((n2 += n) < 0x100000) continue;
                        Log.out("(TX) ~bytes: " + n2);
                        clientData.socket.out.flush();
                        n2 = 0;
                    }
                    if (n2 > 0) {
                        Log.out("(TX) ~bytes: " + n2);
                    }
                    fileInputStream.close();
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendMediainfo2Server(UserData userData, String string, String string2) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string3 = "mediainfo:" + string + DIVIDER + string2;
                    userData.socket.out.writeUTF(string3);
                    Log.out("(TX) " + string3);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendDownload2Server(UserData userData, String string, String string2, String string3) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string4 = "download:" + string + DIVIDER + string2 + DIVIDER + string3;
                    userData.socket.out.writeUTF(string4);
                    Log.out("(TX) " + string4);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendName2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        if (clientData.name.isEmpty()) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "name:" + clientData.name + DIVIDER + clientData.owner;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendScreen2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        if (clientData.version < 33) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    int n;
                    int n2;
                    byte[] byArray;
                    FileInputStream fileInputStream;
                    long l;
                    long l2;
                    String string;
                    File file;
                    if (clientData.version >= 103) {
                        file = null;
                        if (clientData.logo != null) {
                            file = new File("logo", clientData.logo);
                        }
                        if (file != null && file.isFile()) {
                            string = "logo:" + file.getName() + DIVIDER + file.length();
                            clientData.socket.out.writeUTF(string);
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                            l2 = file.length();
                            l = 0L;
                            fileInputStream = new FileInputStream(file);
                            byArray = new byte[102400];
                            n2 = 0;
                            while ((n = fileInputStream.read(byArray)) > 0) {
                                clientData.socket.out.write(byArray, 0, n);
                                l += (long)n;
                                if ((n2 += n) < 0x100000) continue;
                                Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n2 + " (" + 100L * l / l2 + "%)");
                                clientData.socket.out.flush();
                                n2 = 0;
                            }
                            if (n2 > 0) {
                                Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n2 + " (" + 100L * l / l2 + "%)");
                            }
                            fileInputStream.close();
                        } else {
                            string = "logo:\u02fd-1";
                            clientData.socket.out.writeUTF("logo:\u02fd-1");
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                        }
                    }
                    file = null;
                    if (clientData.background != null) {
                        file = new File("background", clientData.background);
                    }
                    if (file != null && file.isFile()) {
                        string = "background:" + file.getName() + DIVIDER + file.length();
                        clientData.socket.out.writeUTF(string);
                        Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                        l2 = file.length();
                        l = 0L;
                        fileInputStream = new FileInputStream(file);
                        byArray = new byte[102400];
                        n2 = 0;
                        while ((n = fileInputStream.read(byArray)) > 0) {
                            clientData.socket.out.write(byArray, 0, n);
                            l += (long)n;
                            if ((n2 += n) < 0x100000) continue;
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n2 + " (" + 100L * l / l2 + "%)");
                            clientData.socket.out.flush();
                            n2 = 0;
                        }
                        if (n2 > 0) {
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n2 + " (" + 100L * l / l2 + "%)");
                        }
                        fileInputStream.close();
                    } else {
                        string = "background:\u02fd-1";
                        clientData.socket.out.writeUTF("background:\u02fd-1");
                        Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    }
                    string = "clock:" + (clientData.clock ? "1" : "0");
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    if (clientData.version >= 47) {
                        string = "clockcolor:" + clientData.clockcolor;
                        clientData.socket.out.writeUTF(string);
                        Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    }
                    if (clientData.version >= 40) {
                        string = "clockpos:" + clientData.clockpos;
                        clientData.socket.out.writeUTF(string);
                        Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    }
                    string = "num:" + clientData.numW + DIVIDER + clientData.numH;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    if (clientData.version >= 75) {
                        string = "union:" + clientData.unionx + DIVIDER + clientData.uniony + DIVIDER + clientData.unionw + DIVIDER + clientData.unionh;
                        clientData.socket.out.writeUTF(string);
                        Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    }
                    if (clientData.version >= 73) {
                        string = "rotate:" + clientData.rotate;
                        clientData.socket.out.writeUTF(string);
                        Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    }
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendOnOff2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        if (clientData.version < 102) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string;
                    if (clientData.on == null || clientData.off == null) {
                        string = "onoff:-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1";
                        clientData.socket.out.writeUTF("onoff:-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1");
                    } else {
                        string = "onoff:" + clientData.on[0] + DIVIDER + clientData.on[1] + DIVIDER + clientData.on[2] + DIVIDER + clientData.on[3] + DIVIDER + clientData.on[4] + DIVIDER + clientData.on[5] + DIVIDER + clientData.on[6] + DIVIDER + clientData.off[0] + DIVIDER + clientData.off[1] + DIVIDER + clientData.off[2] + DIVIDER + clientData.off[3] + DIVIDER + clientData.off[4] + DIVIDER + clientData.off[5] + DIVIDER + clientData.off[6];
                        clientData.socket.out.writeUTF(string);
                    }
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendCell2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "cell:" + clientData.type + DIVIDER + clientData.cell[0] + DIVIDER + clientData.cell[1] + DIVIDER + clientData.cell[2] + DIVIDER + clientData.cell[3];
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendBgm2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null || clientData.version < 77) {
            return;
        }
        try {
            Object object = clientData.lock;
            synchronized (object) {
                Object object2;
                Object object3;
                ArrayList<PlayData> arrayList = new ArrayList<PlayData>();
                long l = 0L;
                byte[] byArray = new byte[102400];
                if (clientData.bgm != null && !clientData.bgm.isEmpty()) {
                    object3 = new File("playlist", clientData.bgm);
                    File file = new File((File)object3, ".playinfo.txt");
                    try {
                        Object object4;
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(file), "UTF-8"));
                        while ((object4 = bufferedReader.readLine()) != null) {
                            PlayData playData = new PlayData();
                            object2 = ((String)object4).split(DIVIDER);
                            switch (playData.type = object2[0]) {
                                case "music": {
                                    playData.time = Integer.parseInt(object2[1]);
                                    playData.show[0] = Integer.parseInt(object2[2]);
                                    playData.show[1] = Integer.parseInt((String)object2[3]);
                                    playData.show[2] = Integer.parseInt((String)object2[4]);
                                    playData.show[3] = Integer.parseInt((String)object2[5]);
                                    playData.file = new File((File)object3, (String)object2[8]);
                                    break;
                                }
                                default: {
                                    return;
                                }
                            }
                            arrayList.add(playData);
                            try {
                                l += playData.file.toPath().toRealPath(new LinkOption[0]).toFile().length();
                            }
                            catch (Exception exception) {
                                Log.out(exception);
                            }
                        }
                        bufferedReader.close();
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                        return;
                    }
                }
                if (clientData.free < l + 100000L) {
                    clientData.socket.close();
                    return;
                }
                if (clientData.socket != null) {
                    try {
                        object3 = "bgm:" + clientData.bgm + DIVIDER + arrayList.size();
                        clientData.socket.out.writeUTF((String)object3);
                        Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + (String)object3);
                        for (PlayData playData : arrayList) {
                            int n;
                            object2 = playData.file.toPath().toRealPath(new LinkOption[0]).toFile();
                            long l2 = ((File)object2).length();
                            if (clientData.bgmlast != null && new File("playlist" + File.separator + clientData.bgmlast + File.separator + playData.file.getName()).exists()) {
                                object3 = playData.type + ":" + playData.show[0] + DIVIDER + playData.show[1] + DIVIDER + playData.show[2] + DIVIDER + playData.show[3] + DIVIDER + playData.file.getName() + DIVIDER + -1;
                                clientData.socket.out.writeUTF((String)object3);
                                Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + (String)object3);
                                continue;
                            }
                            object3 = playData.type + ":" + playData.show[0] + DIVIDER + playData.show[1] + DIVIDER + playData.show[2] + DIVIDER + playData.show[3] + DIVIDER + playData.file.getName() + DIVIDER + l2;
                            clientData.socket.out.writeUTF((String)object3);
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + (String)object3);
                            FileInputStream fileInputStream = new FileInputStream((File)object2);
                            long l3 = 0L;
                            int n2 = 0;
                            while ((n = fileInputStream.read(byArray)) > 0) {
                                clientData.socket.out.write(byArray, 0, n);
                                if ((clientData.txbyte += (long)n) > 0x280000000L) {
                                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") transmit over 10GB");
                                    fileInputStream.close();
                                    clientData.socket.close();
                                    return;
                                }
                                l3 += (long)n;
                                if ((n2 += n) < 0x100000) continue;
                                Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n2 + " (" + 100L * l3 / l2 + "%)");
                                clientData.socket.out.flush();
                                n2 = 0;
                            }
                            if (n2 > 0) {
                                Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n2 + " (" + 100L * l3 / l2 + "%)");
                            }
                            fileInputStream.close();
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") transmit bytes: " + clientData.txbyte);
                        }
                        clientData.socket.out.flush();
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                        return;
                    }
                }
            }
        }
        catch (Exception exception) {
            Log.out(exception);
            clientData.socket.close();
        }
    }

    /*
     * Exception decompiling
     */
    public static void sendPlay2Client(ClientData var0, int var1_1, String var2_2, Progress var3_3) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK], 2[TRYBLOCK], 0[TRYBLOCK]], but top level block is 75[WHILELOOP]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendReplace2Client(ClientData clientData, int n, int n2) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "replace:" + n + DIVIDER + n2;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendRefresh2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "refresh";
                    clientData.socket.out.writeUTF("refresh");
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendApk2Client(ClientData clientData, int n) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    File file = new File("apk", n + ".apk");
                    if (file.exists()) {
                        int n2;
                        String string = "update:" + n + DIVIDER + file.length();
                        clientData.socket.out.writeUTF(string);
                        Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                        byte[] byArray = new byte[102400];
                        FileInputStream fileInputStream = new FileInputStream(file);
                        int n3 = 0;
                        while ((n2 = fileInputStream.read(byArray)) > 0) {
                            clientData.socket.out.write(byArray, 0, n2);
                            if ((n3 += n2) < 0x100000) continue;
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n3);
                            clientData.socket.out.flush();
                            n3 = 0;
                        }
                        if (n3 > 0) {
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n3);
                        }
                        fileInputStream.close();
                        clientData.socket.out.flush();
                    }
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendExec2Client(ClientData clientData, int n) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    File file = new File("exec", n + ".exe");
                    if (file.exists()) {
                        int n2;
                        String string = "update:" + n + DIVIDER + file.length();
                        clientData.socket.out.writeUTF(string);
                        Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                        byte[] byArray = new byte[102400];
                        long l = file.length();
                        long l2 = 0L;
                        FileInputStream fileInputStream = new FileInputStream(file);
                        int n3 = 0;
                        while ((n2 = fileInputStream.read(byArray)) > 0) {
                            clientData.socket.out.write(byArray, 0, n2);
                            l2 += (long)n2;
                            if ((n3 += n2) < 0x100000) continue;
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n3 + " (" + 100L * l2 / l + "%)");
                            clientData.socket.out.flush();
                            n3 = 0;
                        }
                        if (n3 > 0) {
                            Log.out("(TX " + clientData.name + "=" + clientData.id + ") ~bytes: " + n3 + " (" + 100L * l2 / l + "%)");
                        }
                        fileInputStream.close();
                        clientData.socket.out.flush();
                    }
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendSnapshot2Client(ClientData clientData, String string) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string2 = "snapshot:" + string;
                    clientData.socket.out.writeUTF(string2);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string2);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendScreenCap2Client(ClientData clientData, String string) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string2 = "screencap:" + string;
                    clientData.socket.out.writeUTF(string2);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string2);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendDiscard2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "discard";
                    clientData.socket.out.writeUTF("discard");
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendExit2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "exit";
                    clientData.socket.out.writeUTF("exit");
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendRestart2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "restart";
                    clientData.socket.out.writeUTF("restart");
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendReboot2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "reboot";
                    clientData.socket.out.writeUTF("reboot");
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendUnion2Client(ClientData clientData) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        if (clientData.version < 75) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "union:" + clientData.unionx + DIVIDER + clientData.uniony + DIVIDER + clientData.unionw + DIVIDER + clientData.unionh;
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    string = "restart";
                    clientData.socket.out.writeUTF("restart");
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendClientData2User(UserData userData, ArrayList<ClientData> arrayList) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string;
                    if (userData.version >= 33) {
                        string = "ok:https://smartstore.naver.com/ybroad/products/8040236354";
                        userData.socket.out.writeUTF("ok:https://smartstore.naver.com/ybroad/products/8040236354");
                    } else {
                        string = "ok";
                        userData.socket.out.writeUTF("ok");
                    }
                    Log.out("(TX " + userData.name + ") " + string);
                    for (ClientData clientData : arrayList) {
                        MySocket._sendClientData2User(userData, clientData);
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendClientData2User(ArrayList<UserData> arrayList, ClientData clientData) {
        if (arrayList == null || arrayList.isEmpty() || clientData == null) {
            return;
        }
        for (UserData userData : arrayList) {
            if (userData == null || userData.socket == null) continue;
            Object object = userData.lock;
            synchronized (object) {
                if (userData.socket != null) {
                    try {
                        MySocket._sendClientData2User(userData, clientData);
                        userData.socket.out.flush();
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                        userData.socket.close();
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendClientData2User(UserData userData, ClientData clientData) {
        if (userData == null || userData.socket == null || clientData == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    MySocket._sendClientData2User(userData, clientData);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    private static void _sendClientData2User(UserData userData, ClientData clientData) throws Exception {
        String string = "status:" + clientData.id;
        userData.socket.out.writeUTF(string);
        Log.out("(TX " + userData.name + ") " + string);
        if (userData.version >= 11) {
            string = "name:" + clientData.name + DIVIDER + clientData.owner + DIVIDER + clientData.group + DIVIDER + clientData.version;
            userData.socket.out.writeUTF(string);
        } else {
            string = "name:" + clientData.name + DIVIDER + clientData.owner + DIVIDER + clientData.version;
            userData.socket.out.writeUTF(string);
        }
        Log.out("(TX " + userData.name + ") " + string);
        string = "license:" + clientData.license;
        userData.socket.out.writeUTF(string);
        Log.out("(TX " + userData.name + ") " + string);
        if (userData.version >= 11) {
            if (userData.name.startsWith("admin")) {
                string = "info:" + clientData.info + "\\nFirst: " + (clientData.first > 0L ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(clientData.first)) : "unknown");
                userData.socket.out.writeUTF(string);
            } else {
                string = "info:" + clientData.info;
                userData.socket.out.writeUTF(string);
            }
            Log.out("(TX " + userData.name + ") " + string);
            string = "spec:" + clientData.spec;
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
            string = "free:" + clientData.free;
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
        }
        if (userData.version >= 17) {
            string = "ip:" + clientData.ip;
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
        }
        if (userData.version >= 10) {
            if (userData.version >= 34) {
                string = "logo:" + clientData.logooriginal;
                userData.socket.out.writeUTF(string);
                Log.out("(TX " + userData.name + ") " + string);
            }
            string = "background:" + clientData.bgoriginal;
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
            string = "clock:" + (clientData.clock ? "1" : "0");
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
            if (userData.version >= 14) {
                string = "clockcolor:" + clientData.clockcolor;
                userData.socket.out.writeUTF(string);
                Log.out("(TX " + userData.name + ") " + string);
            }
            if (userData.version >= 13) {
                string = "clockpos:" + clientData.clockpos;
                userData.socket.out.writeUTF(string);
                Log.out("(TX " + userData.name + ") " + string);
            }
            if (userData.version >= 21) {
                string = "rotate:" + clientData.rotate;
                userData.socket.out.writeUTF(string);
                Log.out("(TX " + userData.name + ") " + string);
            }
            string = "num:" + clientData.numW + DIVIDER + clientData.numH;
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
        }
        if (userData.version >= 21) {
            string = "connect:" + (clientData.socket != null ? "1" : "0") + DIVIDER + clientData.touch + DIVIDER + clientData.connect + DIVIDER + clientData.disconnect + DIVIDER + (clientData.reason.isEmpty() ? NULL : clientData.reason);
            userData.socket.out.writeUTF(string);
        } else if (userData.version >= 14) {
            string = "connect:" + (clientData.socket != null ? "1" : "0") + DIVIDER + clientData.touch + DIVIDER + clientData.connect + DIVIDER + clientData.disconnect;
            userData.socket.out.writeUTF(string);
        } else {
            string = "connect:" + (clientData.socket != null ? "1" : "0") + DIVIDER + clientData.touch;
            userData.socket.out.writeUTF(string);
        }
        Log.out("(TX " + userData.name + ") " + string);
        string = "screen:" + clientData.width + DIVIDER + clientData.height;
        userData.socket.out.writeUTF(string);
        Log.out("(TX " + userData.name + ") " + string);
        if (userData.version >= 24) {
            string = "union:" + clientData.union.size() + DIVIDER + clientData.unionx + DIVIDER + clientData.uniony + DIVIDER + clientData.unionw + DIVIDER + clientData.unionh + DIVIDER + clientData.unionf;
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
            for (UnionData unionData : clientData.union) {
                string = unionData.id + DIVIDER + (unionData.master ? "1" : "0") + DIVIDER + unionData.x + DIVIDER + unionData.y + DIVIDER + unionData.w + DIVIDER + unionData.h + DIVIDER + unionData.xpos + DIVIDER + unionData.ypos + DIVIDER + unionData.f;
                userData.socket.out.writeUTF(string);
                Log.out("(TX " + userData.name + ") " + string);
            }
        } else if (userData.version >= 22) {
            string = "union:" + clientData.union.size();
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
            for (UnionData unionData : clientData.union) {
                string = unionData.id + DIVIDER + (unionData.master ? "1" : "0") + DIVIDER + unionData.x + DIVIDER + unionData.y + DIVIDER + unionData.w + DIVIDER + unionData.h + DIVIDER + unionData.xpos + DIVIDER + unionData.ypos;
                userData.socket.out.writeUTF(string);
                Log.out("(TX " + userData.name + ") " + string);
            }
        }
        if (userData.version >= 32) {
            if (clientData.on == null) {
                string = "on:-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1";
                userData.socket.out.writeUTF("on:-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1");
            } else {
                string = "on:" + clientData.on[0] + DIVIDER + clientData.on[1] + DIVIDER + clientData.on[2] + DIVIDER + clientData.on[3] + DIVIDER + clientData.on[4] + DIVIDER + clientData.on[5] + DIVIDER + clientData.on[6];
                userData.socket.out.writeUTF(string);
            }
            Log.out("(TX " + userData.name + ") " + string);
            if (clientData.off == null) {
                string = "off:-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1";
                userData.socket.out.writeUTF("off:-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1\u02fd-1");
            } else {
                string = "off:" + clientData.off[0] + DIVIDER + clientData.off[1] + DIVIDER + clientData.off[2] + DIVIDER + clientData.off[3] + DIVIDER + clientData.off[4] + DIVIDER + clientData.off[5] + DIVIDER + clientData.off[6];
                userData.socket.out.writeUTF(string);
            }
            Log.out("(TX " + userData.name + ") " + string);
        }
        if (userData.version >= 35) {
            string = "mbps:" + clientData.mbps;
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
        }
        if (userData.version >= 24) {
            string = "bgm:" + clientData.bgm + "=" + clientData.bgmitem;
            userData.socket.out.writeUTF(string);
            Log.out("(TX " + userData.name + ") " + string);
        }
        string = "cell:" + clientData.type + DIVIDER + clientData.cell[0] + DIVIDER + clientData.cell[1] + DIVIDER + clientData.cell[2] + DIVIDER + clientData.cell[3];
        userData.socket.out.writeUTF(string);
        Log.out("(TX " + userData.name + ") " + string);
        if (userData.version > 6) {
            string = "play:" + clientData.play[0] + "=" + clientData.playitem[0] + DIVIDER + clientData.play[1] + "=" + clientData.playitem[1] + DIVIDER + clientData.play[2] + "=" + clientData.playitem[2] + DIVIDER + clientData.play[3] + "=" + clientData.playitem[3];
            userData.socket.out.writeUTF(string);
        } else {
            String[] stringArray = new String[]{null, null, null, null};
            for (int i = 0; i < 4; ++i) {
                if (clientData.playitem[i] == null) continue;
                String[] stringArray2 = clientData.playitem[i].split(SUBDIV2);
                stringArray[i] = stringArray2[0];
                for (int j = 1; j < stringArray2.length; ++j) {
                    if (j % 7 >= 3) continue;
                    int n = i;
                    stringArray[n] = stringArray[n] + SUBDIV2 + stringArray2[j];
                }
            }
            string = "play:" + clientData.play[0] + "=" + stringArray[0] + DIVIDER + clientData.play[1] + "=" + stringArray[1] + DIVIDER + clientData.play[2] + "=" + stringArray[2] + DIVIDER + clientData.play[3] + "=" + stringArray[3];
            userData.socket.out.writeUTF(string);
        }
        Log.out("(TX " + userData.name + ") " + string);
        if (userData.version > 6) {
            for (int i = 0; i < 4; ++i) {
                string = "reserve:" + i + ":" + clientData.reserve[i][0] + "=" + clientData.reservetime[i][0] + "=" + clientData.reserveitem[i][0] + DIVIDER + clientData.reserve[i][1] + "=" + clientData.reservetime[i][1] + "=" + clientData.reserveitem[i][1] + DIVIDER + clientData.reserve[i][2] + "=" + clientData.reservetime[i][2] + "=" + clientData.reserveitem[i][2] + DIVIDER + clientData.reserve[i][3] + "=" + clientData.reservetime[i][3] + "=" + clientData.reserveitem[i][3];
                userData.socket.out.writeUTF(string);
                Log.out("(TX " + userData.name + ") " + string);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendClientTouch2User(ArrayList<UserData> arrayList, ClientData clientData) {
        if (arrayList == null || arrayList.isEmpty()) {
            return;
        }
        for (UserData userData : arrayList) {
            if (userData == null || userData.socket == null) continue;
            Object object = userData.lock;
            synchronized (object) {
                if (userData.socket != null) {
                    try {
                        String string;
                        if (userData.version >= 21) {
                            string = "touch:" + clientData.id + DIVIDER + (clientData.socket != null ? "1" : "0") + DIVIDER + clientData.touch + DIVIDER + clientData.connect + DIVIDER + clientData.disconnect + DIVIDER + clientData.reason + DIVIDER + clientData.free;
                            userData.socket.out.writeUTF(string);
                        } else if (userData.version >= 14) {
                            string = "touch:" + clientData.id + DIVIDER + (clientData.socket != null ? "1" : "0") + DIVIDER + clientData.touch + DIVIDER + clientData.connect + DIVIDER + clientData.disconnect + DIVIDER + clientData.free;
                            userData.socket.out.writeUTF(string);
                        } else {
                            if (userData.version >= 11) {
                                string = "free:" + clientData.id + DIVIDER + clientData.free;
                                userData.socket.out.writeUTF(string);
                            }
                            string = "touch:" + clientData.id + DIVIDER + (clientData.socket != null ? "1" : "0") + DIVIDER + clientData.touch;
                            userData.socket.out.writeUTF(string);
                        }
                        userData.socket.out.flush();
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                        userData.socket.close();
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendClientTouch2User(UserData userData, ClientData clientData) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string;
                    if (userData.version >= 11) {
                        string = "free:" + clientData.id + DIVIDER + clientData.free;
                        userData.socket.out.writeUTF(string);
                    }
                    string = "touch:" + clientData.id + DIVIDER + (clientData.socket != null ? "1" : "0") + DIVIDER + clientData.touch;
                    userData.socket.out.writeUTF(string);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendUserData2Admin(ArrayList<UserData> arrayList, ArrayList<UserData> arrayList2) {
        if (arrayList == null || arrayList.isEmpty()) {
            return;
        }
        for (UserData userData : arrayList) {
            if (userData == null || userData.socket == null) continue;
            Object object = userData.lock;
            synchronized (object) {
                if (userData.socket != null) {
                    try {
                        for (UserData userData2 : arrayList2) {
                            String string;
                            if (userData.version >= 33) {
                                string = "member:" + userData2.name + DIVIDER + (userData2.onoff ? 1 : 0) + DIVIDER + userData2.touch + DIVIDER + userData2.limit + DIVIDER + (userData2.memo.isEmpty() ? NULL : userData2.memo);
                                userData.socket.out.writeUTF(string);
                            } else if (userData.version >= 16) {
                                string = "member:" + userData2.name + DIVIDER + (userData2.onoff ? 1 : 0) + DIVIDER + userData2.touch + DIVIDER + userData2.limit;
                                userData.socket.out.writeUTF(string);
                            } else {
                                string = "member:" + userData2.name + DIVIDER + (userData2.onoff ? 1 : 0) + DIVIDER + userData2.touch;
                                userData.socket.out.writeUTF(string);
                            }
                            Log.out("(TX " + userData.name + ") " + string);
                        }
                        userData.socket.out.flush();
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                        userData.socket.close();
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendUserData2Admin(ArrayList<UserData> arrayList, UserData userData) {
        if (arrayList == null || arrayList.isEmpty()) {
            return;
        }
        for (UserData userData2 : arrayList) {
            if (userData2 == null || userData2.socket == null) continue;
            Object object = userData2.lock;
            synchronized (object) {
                if (userData2.socket != null) {
                    try {
                        String string;
                        if (userData2.version >= 33) {
                            string = "member:" + userData.name + DIVIDER + (userData.onoff ? 1 : 0) + DIVIDER + userData.touch + DIVIDER + userData.limit + DIVIDER + (userData.memo.isEmpty() ? NULL : userData.memo);
                            userData2.socket.out.writeUTF(string);
                        } else if (userData2.version >= 16) {
                            string = "member:" + userData.name + DIVIDER + (userData.onoff ? 1 : 0) + DIVIDER + userData.touch + DIVIDER + userData.limit;
                            userData2.socket.out.writeUTF(string);
                        } else {
                            string = "member:" + userData.name + DIVIDER + (userData.onoff ? 1 : 0) + DIVIDER + userData.touch;
                            userData2.socket.out.writeUTF(string);
                        }
                        Log.out("(TX " + userData2.name + ") " + string);
                        userData2.socket.out.flush();
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                        userData2.socket.close();
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendUserData2Admin(UserData userData, ArrayList<UserData> arrayList) {
        if (userData == null || !userData.onoff || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    for (UserData userData2 : arrayList) {
                        String string;
                        if (userData.version >= 33) {
                            string = "member:" + userData2.name + DIVIDER + (userData2.onoff ? 1 : 0) + DIVIDER + userData2.touch + DIVIDER + userData2.limit + DIVIDER + (userData2.memo.isEmpty() ? NULL : userData2.memo);
                            userData.socket.out.writeUTF(string);
                        } else if (userData.version >= 16) {
                            string = "member:" + userData2.name + DIVIDER + (userData2.onoff ? 1 : 0) + DIVIDER + userData2.touch + DIVIDER + userData2.limit;
                            userData.socket.out.writeUTF(string);
                        } else {
                            string = "member:" + userData2.name + DIVIDER + (userData2.onoff ? 1 : 0) + DIVIDER + userData2.touch;
                            userData.socket.out.writeUTF(string);
                        }
                        Log.out("(TX " + userData.name + ") " + string);
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendIsleData2Admin(UserData userData, ArrayList<IsleData> arrayList) {
        if (userData == null || !userData.onoff || userData.socket == null || userData.version < 20) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    for (IsleData isleData : arrayList) {
                        String string = "isle:" + isleData.id + DIVIDER + isleData.name + DIVIDER + isleData.key + DIVIDER + isleData.limit + DIVIDER + isleData.touch;
                        userData.socket.out.writeUTF(string);
                        Log.out("(TX " + userData.name + ") " + string);
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendUpdate2User(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "update:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendSnapshot2User(UserData userData, String string, File file) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                byte[] byArray = new byte[102400];
                try {
                    int n;
                    String string2 = "snapshot:" + string + DIVIDER + file.length();
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    int n2 = 0;
                    while ((n = fileInputStream.read(byArray)) > 0) {
                        userData.socket.out.write(byArray, 0, n);
                        if ((n2 += n) < 0x100000) continue;
                        Log.out("(TX " + userData.name + ") ~bytes: " + n2);
                        userData.socket.out.flush();
                        n2 = 0;
                    }
                    if (n2 > 0) {
                        Log.out("(TX " + userData.name + ") ~bytes: " + n2);
                    }
                    fileInputStream.close();
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendMediainfo2User(UserData userData, String string, String string2, String string3) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string4 = "mediainfo:" + string + DIVIDER + string2 + DIVIDER + string3;
                    userData.socket.out.writeUTF(string4);
                    Log.out("(TX " + userData.name + ") " + string4);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendDownload2User(UserData userData, File file, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                byte[] byArray = new byte[102400];
                try {
                    int n;
                    String string2 = "download:" + string + DIVIDER + file.length();
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    userData.socket.out.flush();
                    FileInputStream fileInputStream = new FileInputStream(file);
                    int n2 = 0;
                    while ((n = fileInputStream.read(byArray)) > 0) {
                        userData.socket.out.write(byArray, 0, n);
                        if ((n2 += n) < 0x100000) continue;
                        Log.out("(TX " + userData.name + ") ~bytes: " + n2);
                        userData.socket.out.flush();
                        n2 = 0;
                    }
                    if (n2 > 0) {
                        Log.out("(TX " + userData.name + ") ~bytes: " + n2);
                    }
                    fileInputStream.close();
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendProgress2User(UserData userData, String string, String string2) {
        if (userData == null || userData.socket == null || userData.version < 12) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string3 = "progress:" + string + DIVIDER + string2;
                    userData.socket.out.writeUTF(string3);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendProgress2User(ArrayList<UserData> arrayList, String string, String string2) {
        if (arrayList == null || arrayList.isEmpty()) {
            return;
        }
        for (UserData userData : arrayList) {
            if (userData == null || userData.socket == null || userData.version < 12) continue;
            Object object = userData.lock;
            synchronized (object) {
                if (userData.socket != null) {
                    try {
                        String string3 = "progress:" + string + DIVIDER + string2;
                        userData.socket.out.writeUTF(string3);
                        userData.socket.out.flush();
                    }
                    catch (Exception exception) {
                        Log.out(exception);
                        userData.socket.close();
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendProgress2Server(ClientData clientData, String string, int n, int n2, double d) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string2 = "progress:" + string + DIVIDER + n + DIVIDER + n2 + DIVIDER + d;
                    clientData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLog2Server(ClientData clientData, File file) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    int n;
                    String string = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date(file.lastModified()));
                    String string2 = "log:" + string + DIVIDER + file.getName() + DIVIDER + file.length();
                    clientData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    byte[] byArray = new byte[102400];
                    FileInputStream fileInputStream = new FileInputStream(file);
                    int n2 = 0;
                    while ((n = fileInputStream.read(byArray)) > 0) {
                        clientData.socket.out.write(byArray, 0, n);
                        if ((n2 += n) < 0x100000) continue;
                        Log.out("(TX) ~bytes: " + n2);
                        clientData.socket.out.flush();
                        n2 = 0;
                    }
                    if (n2 > 0) {
                        Log.out("(TX) ~bytes: " + n2);
                    }
                    fileInputStream.close();
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean sendHistory2Server(ClientData clientData, List<String> list) {
        if (clientData == null || clientData.socket == null) {
            return false;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string = "history:" + list.size();
                    clientData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    Iterator<String> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        String string2;
                        string = string2 = iterator.next();
                        clientData.socket.out.writeUTF(string);
                        Log.out("(TX) " + string);
                    }
                    clientData.socket.out.flush();
                    return true;
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendHistory2Server(UserData userData, String string, String string2, List<ClientData> list) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string3 = "history:" + string + DIVIDER + string2 + DIVIDER + list.size();
                    userData.socket.out.writeUTF(string3);
                    Log.out("(TX) " + string3);
                    for (ClientData clientData : list) {
                        string3 = clientData.id;
                        userData.socket.out.writeUTF(string3);
                        Log.out("(TX) " + string3);
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendHistory2User(UserData userData, Map<String, Integer> map) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string = "history:" + map.size();
                    userData.socket.out.writeUTF(string);
                    Log.out("(TX " + userData.name + ") " + string);
                    for (String string2 : map.keySet()) {
                        string = string2 + DIVIDER + map.get(string2);
                        userData.socket.out.writeUTF(string);
                        Log.out("(TX " + userData.name + ") " + string);
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendSubData2Server(SubData subData) {
        if (subData == null || subData.socket == null) {
            return;
        }
        Object object = subData.lock;
        synchronized (object) {
            if (subData.socket != null) {
                try {
                    String string = "sub:0";
                    subData.socket.out.writeUTF("sub:0");
                    Log.out("(TX) " + string);
                    string = "copy:" + subData.copy;
                    subData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    subData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    subData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendSubBackup2Server(SubData subData, File file) {
        if (subData == null || subData.socket == null) {
            return;
        }
        Object object = subData.lock;
        synchronized (object) {
            if (subData.socket != null) {
                try {
                    int n;
                    String string = "backup:" + file.getName() + DIVIDER + file.length();
                    subData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    byte[] byArray = new byte[102400];
                    FileInputStream fileInputStream = new FileInputStream(file);
                    int n2 = 0;
                    while ((n = fileInputStream.read(byArray)) > 0) {
                        subData.socket.out.write(byArray, 0, n);
                        if ((n2 += n) < 0x100000) continue;
                        Log.out("(TX) ~bytes: " + n2);
                        subData.socket.out.flush();
                        n2 = 0;
                    }
                    if (n2 > 0) {
                        Log.out("(TX) ~bytes: " + n2);
                    }
                    fileInputStream.close();
                    subData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    subData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendTouch2Server(SubData subData) {
        if (subData == null || subData.socket == null) {
            return;
        }
        Object object = subData.lock;
        synchronized (object) {
            if (subData.socket != null) {
                try {
                    String string = "touch:" + subData.copy;
                    subData.socket.out.writeUTF(string);
                    Log.out("(TX) " + string);
                    subData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    subData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendCopyLimit2Sub(SubData subData) {
        if (subData == null || subData.socket == null) {
            return;
        }
        Object object = subData.lock;
        synchronized (object) {
            if (subData.socket != null) {
                try {
                    String string = "limit:" + subData.copylimit;
                    subData.socket.out.writeUTF(string);
                    Log.out("(TX " + subData.name + "=" + subData.addr + ") " + string);
                    subData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    subData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLanMaster2Server(UserData userData, String string, boolean bl) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "lanmaster:" + string + DIVIDER + (bl ? 1 : 0);
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLanMaster2Client(ClientData clientData, String string) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string2 = "lanmaster:" + string;
                    clientData.socket.out.writeUTF(string2);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string2);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLanSlave2Server(UserData userData, String string, boolean bl) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "lanslave:" + string + DIVIDER + (bl ? 1 : 0);
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLanSlave2Client(ClientData clientData, String string) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        if (clientData.id.startsWith("w") && clientData.version < 56) {
            return;
        }
        if (clientData.version < 110) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string2 = "lanslave:" + string;
                    clientData.socket.out.writeUTF(string2);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string2);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendApk2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "apk:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendCut2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "cut:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendShell2Server(UserData userData, String string, String string2) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string3;
                    if (string == null) {
                        string3 = "shell:" + string2;
                        userData.socket.out.writeUTF(string3);
                    } else {
                        string3 = "shell:" + string + DIVIDER + string2;
                        userData.socket.out.writeUTF(string3);
                    }
                    Log.out("(TX " + userData.name + ") " + string3);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendShell2Client(ClientData clientData, String string, String string2) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string3 = "shell:" + string + DIVIDER + string2;
                    clientData.socket.out.writeUTF(string3);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string3);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendShell2Server(ClientData clientData, String string, String string2) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string3 = "shell:" + string + DIVIDER + (string2 == null || string2.isEmpty() ? NULL : string2);
                    clientData.socket.out.writeUTF(string3);
                    Log.out("(TX) " + string3);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendShell2User(UserData userData, String string, String string2) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string3 = "shell:" + string + DIVIDER + string2;
                    userData.socket.out.writeUTF(string3);
                    Log.out("(TX " + userData.name + ") " + string3);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLogcat2Server(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "logcat:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLogcat2Client(ClientData clientData, String string) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string2 = "logcat:" + string;
                    clientData.socket.out.writeUTF(string2);
                    Log.out("(TX " + clientData.name + "=" + clientData.id + ") " + string2);
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLogcat2Server(ClientData clientData, String string, String[] stringArray) {
        if (clientData == null || clientData.socket == null) {
            return;
        }
        Object object = clientData.lock;
        synchronized (object) {
            if (clientData.socket != null) {
                try {
                    String string2 = "logcat:" + string + DIVIDER + stringArray.length;
                    clientData.socket.out.writeUTF(string2);
                    Log.out("(TX) " + string2);
                    String[] stringArray2 = stringArray;
                    int n = stringArray2.length;
                    for (int i = 0; i < n; ++i) {
                        String string3;
                        string2 = string3 = stringArray2[i];
                        clientData.socket.out.writeUTF(string2);
                        Log.out("(TX) " + string2);
                    }
                    clientData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    clientData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendLogcat2User(UserData userData, String string, String[] stringArray) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "logcat:" + string + DIVIDER + stringArray.length;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    String[] stringArray2 = stringArray;
                    int n = stringArray2.length;
                    for (int i = 0; i < n; ++i) {
                        String string3;
                        string2 = string3 = stringArray2[i];
                        userData.socket.out.writeUTF(string2);
                        Log.out("(TX " + userData.name + ") " + string2);
                    }
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendMessage2Server(UserData userData, boolean bl, boolean bl2, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "message:" + (bl ? 1 : 0) + DIVIDER + (bl2 ? 1 : 0) + DIVIDER + (string.isEmpty() ? NULL : string);
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void sendMessage2User(UserData userData, String string) {
        if (userData == null || userData.socket == null) {
            return;
        }
        Object object = userData.lock;
        synchronized (object) {
            if (userData.socket != null) {
                try {
                    String string2 = "message:" + string;
                    userData.socket.out.writeUTF(string2);
                    Log.out("(TX " + userData.name + ") " + string2);
                    userData.socket.out.flush();
                }
                catch (Exception exception) {
                    Log.out(exception);
                    userData.socket.close();
                }
            }
        }
    }
}

