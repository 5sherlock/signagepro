/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 */
package net.ybroad.dispy.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.application.Platform;
import net.ybroad.dispy.lib.ClientData;
import net.ybroad.dispy.lib.IsleData;
import net.ybroad.dispy.lib.Log;
import net.ybroad.dispy.lib.MySocket;
import net.ybroad.dispy.lib.PlayData;
import net.ybroad.dispy.lib.Progress;
import net.ybroad.dispy.lib.UnionData;
import net.ybroad.dispy.lib.UserData;
import net.ybroad.dispy.manager.MainApp;
import net.ybroad.dispy.manager.model.DispyData;
import net.ybroad.dispy.manager.model.HistoryData;
import net.ybroad.dispy.manager.util.UnionSnap;

public class Server {
    public static final int VERSION_CURRENT = 36;
    public static boolean USE_SERVER_TEXT = false;
    private MainApp mainApp;
    private UserData data = new UserData();

    public Server(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void connect(UserData userData) {
        Object object;
        Closeable closeable;
        Object object2;
        this.data = userData;
        if (userData.socket != null) {
            return;
        }
        InetSocketAddress inetSocketAddress = null;
        try {
            object2 = new File("server.txt");
            closeable = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream((File)object2), "UTF-8"));
            object = closeable.readLine();
            closeable.close();
            String[] stringArray = ((String)object).split(":");
            if (stringArray.length != 2 || stringArray[0].isEmpty() || stringArray[1].isEmpty()) {
                throw new Exception("server.txt: " + stringArray);
            }
            String string = stringArray[0];
            int n = Integer.parseInt(stringArray[1]);
            inetSocketAddress = new InetSocketAddress(string, n);
        }
        catch (Exception exception) {
            Log.out(exception);
            inetSocketAddress = null;
        }
        try {
            object2 = new Socket();
            if (inetSocketAddress != null) {
                Log.out("(Server) try to connect : text - " + inetSocketAddress.toString());
                ((Socket)object2).connect(inetSocketAddress);
                USE_SERVER_TEXT = true;
            } else {
                Log.out("(Server) try to connect : pro");
                ((Socket)object2).connect(new InetSocketAddress("pro.yboard.io", 10080));
            }
            closeable = new DataInputStream(new BufferedInputStream(((Socket)object2).getInputStream(), 102400));
            object = new DataOutputStream(new BufferedOutputStream(((Socket)object2).getOutputStream(), 102400));
            userData.socket = new MyServer((Socket)object2, (DataInputStream)closeable, (DataOutputStream)object);
            userData.socket.start();
        }
        catch (IOException iOException) {
            Log.out(iOException);
            this.mainApp.showDisconnectDialog("disconnect.content.reset");
            this.mainApp.connectionChanged(false, userData, null);
        }
    }

    public void disconnect() {
        if (this.data.socket != null) {
            MySocket.sendTouch2Server(this.data);
            this.data.socket.close();
        }
    }

    public boolean isConnected() {
        return this.data.socket != null && this.data.onoff;
    }

    public void sendRegister(UserData userData) {
        if (this.data.socket != null) {
            MySocket.sendRegister2Server(this.data, userData);
            MySocket.sendRemember2Server(this.data, userData.name, userData);
        }
    }

    public void sendAdd(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendAdd2Server(this.data, dispyData);
        }
    }

    public void sendRemove(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendRemove2Server(this.data, dispyData.getId());
        }
    }

    public void sendRename(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendRename2Server(this.data, dispyData);
        }
    }

    public void sendLicenseLimit(DispyData dispyData, long l) {
        if (this.data.socket != null) {
            MySocket.sendLicense2Server(this.data, dispyData.getId(), l);
        }
    }

    public void sendScreen(DispyData dispyData, File file, File file2, boolean bl, String string, String string2, int n, int n2, int n3) {
        if (this.data.socket != null) {
            MySocket.sendScreen2Server(this.data, dispyData.getId(), file, file2, bl, string, string2, n, n2, n3);
        }
    }

    public void sendOnOff(DispyData dispyData, int[] nArray, int[] nArray2) {
        if (this.data.socket != null) {
            MySocket.sendOnOff2Server(this.data, dispyData.getId(), nArray, nArray2);
        }
    }

    public void sendCell(DispyData dispyData, int n, int[] nArray) {
        if (this.data.socket != null) {
            MySocket.sendCell2Server(this.data, dispyData.getId(), n, nArray);
        }
    }

    public void sendUnion(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendUnion2Server(this.data, dispyData.getId(), dispyData.width, dispyData.height, dispyData.union);
        }
    }

    public void sendUnionClient(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendUnionClient2Server(this.data, dispyData.getId(), dispyData.unionx, dispyData.uniony, dispyData.unionw, dispyData.unionh, dispyData.unionf);
        }
    }

    public void sendPlaylistGroup(DispyData dispyData, List<List<PlayData>> list, Progress progress) {
        if (this.data.socket != null) {
            MySocket.sendPlayGroup2Server(this.data, dispyData.getId(), list, progress);
        }
    }

    public void sendReservelistGroup(DispyData dispyData, int n, long l, List<List<PlayData>> list, Progress progress) {
        if (this.data.socket != null) {
            MySocket.sendReserveGroup2Server(this.data, dispyData.getId(), n, l, list, progress);
        }
    }

    public void sendBgmlist(DispyData dispyData, List<PlayData> list, Progress progress) {
        if (this.data.socket != null) {
            MySocket.sendBgm2Server(this.data, dispyData.getId(), list, progress);
        }
    }

    public void sendReplace(DispyData dispyData, int n, int n2) {
        if (this.data.socket != null) {
            MySocket.sendReplace2Server(this.data, dispyData.getId(), n, n2);
        }
    }

    public void sendCopy(DispyData dispyData, List<DispyData> list) {
        if (this.data.socket != null) {
            MySocket.sendCopy2Server(this.data, dispyData.getId(), new ArrayList<ClientData>(list));
        }
    }

    public void sendDiscard(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendDiscard2Server(this.data, dispyData.getId());
        }
    }

    public void sendApk(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendApk2Server(this.data, dispyData.getId());
        }
    }

    public void sendCut(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendCut2Server(this.data, dispyData.getId());
        }
    }

    public void sendExit(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendExit2Server(this.data, dispyData.getId());
        }
    }

    public void sendRestart(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendRestart2Server(this.data, dispyData.getId());
        }
    }

    public void sendReboot(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendReboot2Server(this.data, dispyData.getId());
        }
    }

    public void sendRemember(String string, UserData userData) {
        if (this.data.socket != null) {
            MySocket.sendRemember2Server(this.data, string, userData);
        }
    }

    public void sendIsle(IsleData isleData) {
        if (this.data.socket != null) {
            MySocket.sendIsle2Server(this.data, isleData);
        }
    }

    public void sendSnapshot(DispyData dispyData) {
        if (this.data.socket != null) {
            UnionSnap.init(dispyData);
            MySocket.sendSnapshot2Server(this.data, dispyData.getId());
        }
    }

    public void sendScreenCap(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendScreenCapture2Server(this.data, dispyData.getId());
        }
    }

    public void sendMediainfo(String string, String string2) {
        if (this.data.socket != null) {
            MySocket.sendMediainfo2Server(this.data, string, string2);
        }
    }

    public void sendDownload(String string, String string2, File file) {
        if (this.data.socket != null) {
            MySocket.sendDownload2Server(this.data, string, string2, file.getAbsolutePath());
        }
    }

    public void sendLogcat(DispyData dispyData) {
        if (this.data.socket != null) {
            MySocket.sendLogcat2Server(this.data, dispyData.getId());
        }
    }

    public void sendShell(String string, String string2) {
        if (this.data.socket != null) {
            MySocket.sendShell2Server(this.data, string, string2);
        }
    }

    public void sendLanMaster(String string, boolean bl) {
        if (this.data.socket != null) {
            MySocket.sendLanMaster2Server(this.data, string, bl);
        }
    }

    public void sendLanSlave(String string, boolean bl) {
        if (this.data.socket != null) {
            MySocket.sendLanSlave2Server(this.data, string, bl);
        }
    }

    public void sendHistory(String string, String string2, List<DispyData> list) {
        if (this.data.socket != null) {
            MySocket.sendHistory2Server(this.data, string, string2, new ArrayList<ClientData>(list));
        }
    }

    public void sendMessage(boolean bl, boolean bl2, String string) {
        if (this.data.socket != null) {
            MySocket.sendMessage2Server(this.data, bl, bl2, string);
        }
    }

    private class MyServer
    extends MySocket {
        private TouchThread touchThread;

        MyServer(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
            super(socket, dataInputStream, dataOutputStream, 36);
        }

        @Override
        protected void onLoop() throws Exception {
            Log.out("(Server) started");
            MyServer.sendUserData2Server(Server.this.data);
            if (this.touchThread != null) {
                this.touchThread.stop = true;
                this.touchThread = null;
            }
            this.touchThread = new TouchThread();
            this.touchThread.start();
            block37: while (true) {
                String string;
                if (!(string = this.in.readUTF()).startsWith("touch:")) {
                    Log.out("(RX) " + string);
                }
                if (string.length() == 0) continue;
                int n = string.indexOf(58);
                String string2 = n > 0 ? string.substring(0, n) : string;
                String string3 = n > 0 ? string.substring(n + 1) : "";
                switch (string2) {
                    case "ok": {
                        Object object;
                        if (string3.isEmpty()) {
                            return;
                        }
                        if (((Server)Server.this).data.name.equals("admin") && new File(System.getProperty("user.dir")).getName().equals("app")) {
                            try {
                                object = new PrintStream(new File("log.txt"), "UTF-8");
                                System.setOut((PrintStream)object);
                                System.setErr((PrintStream)object);
                            }
                            catch (Exception exception) {
                                Log.out(exception);
                            }
                        }
                        ((Server)Server.this).data.onoff = true;
                        object = "\u02fc".equals(string3) ? null : string3;
                        Platform.runLater(() -> this.lambda$onLoop$0((String)object));
                        break;
                    }
                    case "status": {
                        Object object;
                        int n2;
                        if (string3.isEmpty()) {
                            return;
                        }
                        DispyData dispyData = Server.this.mainApp.getDispyData(string3);
                        if (dispyData == null) {
                            dispyData = new DispyData();
                            dispyData.setId(string3);
                        }
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string4 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("name")) {
                            return;
                        }
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length < 4) {
                            return;
                        }
                        dispyData.setName(stringArray[0]);
                        dispyData.setOwner(stringArray[1]);
                        dispyData.group = stringArray[2];
                        dispyData.setVersion(stringArray[3]);
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string5 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("license")) {
                            return;
                        }
                        dispyData.setLicense(Long.parseLong(string3));
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string6 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("info")) {
                            return;
                        }
                        dispyData.info = string3;
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string7 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("spec")) {
                            return;
                        }
                        dispyData.spec = string3;
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string8 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("free")) {
                            return;
                        }
                        dispyData.free = Long.parseLong(string3);
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string9 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("ip")) {
                            return;
                        }
                        dispyData.ip = string3;
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string10 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("logo")) {
                            return;
                        }
                        if (string3.equals("null")) {
                            string3 = "";
                        }
                        dispyData.logooriginal = string3;
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string11 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("background")) {
                            return;
                        }
                        if (string3.equals("null")) {
                            string3 = "";
                        }
                        dispyData.bgoriginal = string3;
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string12 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("clock")) {
                            return;
                        }
                        dispyData.clock = string3.equals("1");
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string13 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("clockcolor")) {
                            return;
                        }
                        dispyData.clockcolor = string3;
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string14 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("clockpos")) {
                            return;
                        }
                        dispyData.clockpos = string3;
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string15 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("rotate")) {
                            return;
                        }
                        dispyData.rotate = Integer.parseInt(string3);
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string16 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("num")) {
                            return;
                        }
                        stringArray = string3.split("\u02fd");
                        if (stringArray.length != 2) {
                            return;
                        }
                        dispyData.numW = Integer.parseInt(stringArray[0]);
                        dispyData.numH = Integer.parseInt(stringArray[1]);
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string17 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("connect")) {
                            return;
                        }
                        stringArray = string3.split("\u02fd");
                        if (stringArray.length != 5) {
                            return;
                        }
                        dispyData.setOnoff(Integer.parseInt(stringArray[0]));
                        dispyData.setTouch(Long.parseLong(stringArray[1]));
                        dispyData.connect = Long.parseLong(stringArray[2]);
                        dispyData.disconnect = Long.parseLong(stringArray[3]);
                        dispyData.reason = stringArray[4].equals("\u02fc") ? "" : stringArray[4];
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string18 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("screen")) {
                            return;
                        }
                        stringArray = string3.split("\u02fd");
                        if (stringArray.length < 2) {
                            return;
                        }
                        dispyData.setSize(Integer.parseInt(stringArray[0]), Integer.parseInt(stringArray[1]), dispyData.rotate);
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string19 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("union")) {
                            return;
                        }
                        stringArray = string3.split("\u02fd");
                        if (stringArray.length < 6) {
                            return;
                        }
                        ArrayList<Object> arrayList = new ArrayList<Object>();
                        int n3 = Integer.parseInt(stringArray[0]);
                        dispyData.unionx = Integer.parseInt(stringArray[1]);
                        dispyData.uniony = Integer.parseInt(stringArray[2]);
                        dispyData.unionw = Integer.parseInt(stringArray[3]);
                        dispyData.unionh = Integer.parseInt(stringArray[4]);
                        dispyData.unionf = Double.parseDouble(stringArray[5]);
                        for (n2 = 0; n2 < n3; ++n2) {
                            string = this.in.readUTF();
                            Log.out("(RX) " + string);
                            stringArray = string.split("\u02fd");
                            if (stringArray.length < 9) {
                                return;
                            }
                            object = new UnionData();
                            ((UnionData)object).id = stringArray[0];
                            ((UnionData)object).master = stringArray[1].equals("1");
                            ((UnionData)object).x = Integer.parseInt(stringArray[2]);
                            ((UnionData)object).y = Integer.parseInt(stringArray[3]);
                            ((UnionData)object).w = Integer.parseInt(stringArray[4]);
                            ((UnionData)object).h = Integer.parseInt(stringArray[5]);
                            ((UnionData)object).xpos = Integer.parseInt(stringArray[6]);
                            ((UnionData)object).ypos = Integer.parseInt(stringArray[7]);
                            ((UnionData)object).f = Double.parseDouble(stringArray[8]);
                            arrayList.add(object);
                        }
                        dispyData.union.clear();
                        dispyData.union.addAll(arrayList);
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string20 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("on")) {
                            return;
                        }
                        stringArray = string3.split("\u02fd");
                        if (stringArray.length < 7) {
                            return;
                        }
                        dispyData.on = new int[]{Integer.parseInt(stringArray[0]), Integer.parseInt(stringArray[1]), Integer.parseInt(stringArray[2]), Integer.parseInt(stringArray[3]), Integer.parseInt(stringArray[4]), Integer.parseInt(stringArray[5]), Integer.parseInt(stringArray[6])};
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string21 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("off")) {
                            return;
                        }
                        stringArray = string3.split("\u02fd");
                        if (stringArray.length < 7) {
                            return;
                        }
                        dispyData.off = new int[]{Integer.parseInt(stringArray[0]), Integer.parseInt(stringArray[1]), Integer.parseInt(stringArray[2]), Integer.parseInt(stringArray[3]), Integer.parseInt(stringArray[4]), Integer.parseInt(stringArray[5]), Integer.parseInt(stringArray[6])};
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string22 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("mbps")) {
                            return;
                        }
                        dispyData.mbps = Integer.parseInt(string3);
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string23 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("bgm")) {
                            return;
                        }
                        dispyData.setBgm(string3);
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string24 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("cell")) {
                            return;
                        }
                        stringArray = string3.split("\u02fd");
                        if (stringArray.length < 5) {
                            return;
                        }
                        dispyData.setType(Integer.parseInt(stringArray[0]));
                        dispyData.setCell0(Integer.parseInt(stringArray[1]));
                        dispyData.setCell1(Integer.parseInt(stringArray[2]));
                        dispyData.setCell2(Integer.parseInt(stringArray[3]));
                        dispyData.setCell3(Integer.parseInt(stringArray[4]));
                        string = this.in.readUTF();
                        Log.out("(RX) " + string);
                        n = string.indexOf(58);
                        string2 = n > 0 ? string.substring(0, n) : string;
                        String string25 = string3 = n > 0 ? string.substring(n + 1) : "";
                        if (!string2.equals("play")) {
                            return;
                        }
                        stringArray = string3.split("\u02fd");
                        if (stringArray.length < 4) {
                            return;
                        }
                        dispyData.setPlay0(stringArray[0]);
                        dispyData.setPlay1(stringArray[1]);
                        dispyData.setPlay2(stringArray[2]);
                        dispyData.setPlay3(stringArray[3]);
                        for (n2 = 0; n2 < 4; ++n2) {
                            string = this.in.readUTF();
                            Log.out("(RX) " + string);
                            n = string.indexOf(58);
                            string2 = n > 0 ? string.substring(0, n) : string;
                            String string26 = string3 = n > 0 ? string.substring(n + 1) : "";
                            if (!string2.equals("reserve")) {
                                return;
                            }
                            n = string3.indexOf(58);
                            string2 = n > 0 ? string3.substring(0, n) : string3;
                            String string27 = string3 = n > 0 ? string3.substring(n + 1) : "";
                            if (n2 != Integer.parseInt(string2)) {
                                return;
                            }
                            stringArray = string3.split("\u02fd");
                            if (stringArray.length < 4) {
                                return;
                            }
                            dispyData.setReserve(n2, 0, stringArray[0]);
                            dispyData.setReserve(n2, 1, stringArray[1]);
                            dispyData.setReserve(n2, 2, stringArray[2]);
                            dispyData.setReserve(n2, 3, stringArray[3]);
                        }
                        Object object2 = dispyData;
                        Platform.runLater(() -> this.lambda$onLoop$1((DispyData)object2));
                        break;
                    }
                    case "free": {
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length < 2) {
                            return;
                        }
                        Object object2 = Server.this.mainApp.getDispyData(stringArray[0]);
                        if (object2 == null) continue block37;
                        Object object = stringArray[1];
                        Platform.runLater(() -> this.lambda$onLoop$2((DispyData)object2, (String)object));
                        break;
                    }
                    case "touch": {
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length < 7) {
                            return;
                        }
                        Object object2 = Server.this.mainApp.getDispyData(stringArray[0]);
                        if (object2 == null) continue block37;
                        Object object = stringArray[1];
                        Object object3 = stringArray[2];
                        String string28 = stringArray[3];
                        Object object4 = stringArray[4];
                        Object object5 = stringArray[5];
                        String string29 = stringArray[6];
                        Platform.runLater(() -> this.lambda$onLoop$3((DispyData)object2, (String)object, (String)object3, string28, (String)object4, (String)object5, string29));
                        break;
                    }
                    case "member": {
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length < 5) {
                            return;
                        }
                        Object object2 = stringArray[0];
                        Object object = stringArray[1];
                        Object object3 = stringArray[2];
                        String string30 = stringArray[3];
                        Object object4 = stringArray[4];
                        Platform.runLater(() -> this.lambda$onLoop$4((String)object2, (String)object, (String)object3, string30, (String)object4));
                        break;
                    }
                    case "isle": {
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length < 5) {
                            return;
                        }
                        Object object2 = stringArray[0];
                        Object object = stringArray[1];
                        Object object3 = stringArray[2];
                        String string31 = stringArray[3];
                        Object object4 = stringArray[4];
                        Platform.runLater(() -> this.lambda$onLoop$5((String)object2, (String)object, (String)object3, string31, (String)object4));
                        break;
                    }
                    case "update": {
                        if (string3.isEmpty()) {
                            return;
                        }
                        Object object2 = string3 + "exe";
                        Platform.runLater(() -> this.lambda$onLoop$6((String)object2));
                        break;
                    }
                    case "snapshot": 
                    case "screencap": {
                        int n4;
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length != 2) {
                            return;
                        }
                        Object object = stringArray[0];
                        Object object3 = File.createTempFile("dispy", ".jpg");
                        ((File)object3).deleteOnExit();
                        Object object4 = new byte[102400];
                        Object object5 = new DataOutputStream(new FileOutputStream((File)object3));
                        int n5 = 0;
                        for (int i = Integer.parseInt(stringArray[1]); i > 0; i -= n4) {
                            n4 = this.in.read((byte[])object4, 0, Math.min(((byte[])object4).length, i));
                            ((DataOutputStream)object5).write((byte[])object4, 0, n4);
                            if ((n5 += n4) < 0x100000) continue;
                            Log.out("(RX) ~bytes: " + n5);
                            n5 = 0;
                        }
                        if (n5 > 0) {
                            Log.out("(RX) ~bytes: " + n5);
                        }
                        ((FilterOutputStream)object5).close();
                        Log.out("(Server) snapshot file: " + ((File)object3).getAbsolutePath());
                        File file = File.createTempFile("dispy", ".jpg");
                        file.deleteOnExit();
                        String string32 = UnionSnap.put((String)object, (File)object3, file.getAbsolutePath());
                        Platform.runLater(() -> this.lambda$onLoop$7(string32, file, (String)object, (File)object3));
                        break;
                    }
                    case "mediainfo": {
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length != 3) {
                            return;
                        }
                        String string33 = stringArray[0];
                        String string34 = stringArray[1];
                        String string35 = stringArray[2].replace("\\n", "\n");
                        Platform.runLater(() -> Server.this.mainApp.showMediainfo(string33, string34, string35));
                        break;
                    }
                    case "download": {
                        int n6;
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length != 2) {
                            return;
                        }
                        String string36 = stringArray[0];
                        File file = new File(string36);
                        int n7 = n6 = Integer.parseInt(stringArray[1]);
                        Object object4 = new byte[102400];
                        Object object5 = new DataOutputStream(new FileOutputStream(file));
                        int n8 = 0;
                        while (n7 > 0) {
                            int n9 = this.in.read((byte[])object4, 0, Math.min(((byte[])object4).length, n7));
                            ((DataOutputStream)object5).write((byte[])object4, 0, n9);
                            n7 -= n9;
                            if ((n8 += n9) >= 0x100000) {
                                Log.out("(RX) ~bytes: " + n8);
                                n8 = 0;
                            }
                            double d = 100.0 * (double)(n6 - n7) / (double)n6;
                            Platform.runLater(() -> Server.this.mainApp.showDownload(string36, d));
                        }
                        if (n8 > 0) {
                            Log.out("(RX) ~bytes: " + n8);
                        }
                        ((FilterOutputStream)object5).close();
                        Platform.runLater(() -> Server.this.mainApp.showDownload(string36, Double.MAX_VALUE));
                        Log.out("(Server) download file: " + string36);
                        break;
                    }
                    case "logcat": {
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length != 2) {
                            return;
                        }
                        String string37 = stringArray[0];
                        int n10 = Integer.parseInt(stringArray[1]);
                        String[] stringArray2 = new String[n10];
                        for (int i = 0; i < n10; ++i) {
                            string = this.in.readUTF();
                            Log.out("(RX) " + string);
                            stringArray2[i] = string;
                        }
                        Platform.runLater(() -> Server.this.mainApp.showLogcat(string37, stringArray2));
                        break;
                    }
                    case "shell": {
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length != 2) continue block37;
                        String string38 = stringArray[0];
                        String string39 = stringArray[1];
                        Platform.runLater(() -> Server.this.mainApp.showShellResult(string38, string39.equals("\u02fc") ? "" : string39));
                        break;
                    }
                    case "progress": {
                        String[] stringArray = string3.split("\u02fd");
                        if (stringArray.length < 5) {
                            return;
                        }
                        String string40 = stringArray[0];
                        String string41 = stringArray[1];
                        int n11 = Integer.parseInt(stringArray[2]);
                        int n12 = Integer.parseInt(stringArray[3]);
                        double d = Double.parseDouble(stringArray[4]);
                        Platform.runLater(() -> Server.this.mainApp.updateProgress(string40, string41, n11, n12, d));
                        break;
                    }
                    case "history": {
                        String[] stringArray;
                        if (string3.isEmpty()) {
                            return;
                        }
                        final ArrayList<HistoryData> arrayList = new ArrayList<HistoryData>();
                        int n3 = Integer.parseInt(string3);
                        for (int i = 0; i < n3; ++i) {
                            string = this.in.readUTF();
                            Log.out("(RX) " + string);
                            stringArray = string.split("\u02fd");
                            if (stringArray.length < 3) {
                                return;
                            }
                            String string42 = stringArray[0];
                            String string43 = stringArray[1];
                            String string44 = stringArray[2];
                            boolean bl = false;
                            for (HistoryData historyData : arrayList) {
                                if (!((String)historyData.data.get()).equals(string43)) continue;
                                historyData.putValue(string42, string44);
                                bl = true;
                                break;
                            }
                            if (bl) continue;
                            arrayList.add(new HistoryData(string42, string43, string44));
                        }
                        Collections.sort(arrayList);
                        Platform.runLater((Runnable)new Runnable(){

                            @Override
                            public void run() {
                                Server.this.mainApp.updateHistory(arrayList);
                            }
                        });
                        break;
                    }
                    case "message": {
                        if (string3.isEmpty()) {
                            return;
                        }
                        final String string45 = string3;
                        Platform.runLater((Runnable)new Runnable(){

                            @Override
                            public void run() {
                                Server.this.mainApp.showReveivedMessageDialog(string45);
                            }
                        });
                        break;
                    }
                    default: {
                        return;
                    }
                }
            }
        }

        @Override
        protected void onClose() throws Exception {
            if (this.touchThread != null) {
                this.touchThread.stop = true;
                this.touchThread = null;
            }
            if (((Server)Server.this).data.socket == null) {
                return;
            }
            ((Server)Server.this).data.socket = null;
            Log.out("(Server) closed");
            Platform.runLater(() -> {
                Server.this.mainApp.showDisconnectDialog(((Server)Server.this).data.onoff ? "disconnect.content.reset" : (((Server)Server.this).data.member ? "disconnect.content.register" : "disconnect.content.login"));
                Server.this.mainApp.connectionChanged(false, Server.this.data, null);
            });
        }

        private /* synthetic */ void lambda$onLoop$7(String string, File file, String string2, File file2) {
            if (string != null) {
                Server.this.mainApp.showSnapImage(string, file);
            } else {
                Server.this.mainApp.showSnapImage(string2, file2);
            }
        }

        private /* synthetic */ void lambda$onLoop$6(String string) {
            Server.this.mainApp.updateApp(string);
        }

        private /* synthetic */ void lambda$onLoop$5(String string, String string2, String string3, String string4, String string5) {
            Server.this.mainApp.updateIsle(string, string2, string3, string4, string5);
        }

        private /* synthetic */ void lambda$onLoop$4(String string, String string2, String string3, String string4, String string5) {
            Server.this.mainApp.updateMember(string, string2, string3, string4, "\u02fc".equals(string5) ? "" : string5);
        }

        private /* synthetic */ void lambda$onLoop$3(DispyData dispyData, String string, String string2, String string3, String string4, String string5, String string6) {
            dispyData.setOnoff(Integer.parseInt(string) == 1);
            dispyData.setTouch(Long.parseLong(string2));
            dispyData.connect = Long.parseLong(string3);
            dispyData.disconnect = Long.parseLong(string4);
            dispyData.reason = string5;
            dispyData.free = Long.parseLong(string6);
            Server.this.mainApp.updateOverview(dispyData);
        }

        private /* synthetic */ void lambda$onLoop$2(DispyData dispyData, String string) {
            dispyData.free = Long.parseLong(string);
            Server.this.mainApp.updateOverview(dispyData);
        }

        private /* synthetic */ void lambda$onLoop$1(DispyData dispyData) {
            Server.this.mainApp.updateOverview(dispyData);
        }

        private /* synthetic */ void lambda$onLoop$0(String string) {
            Server.this.mainApp.connectionChanged(true, Server.this.data, string);
        }

        private class TouchThread
        extends Thread {
            private boolean stop = false;

            private TouchThread() {
            }

            @Override
            public void run() {
                while (!this.stop) {
                    for (int i = 0; i < 60; ++i) {
                        if (this.stop) {
                            return;
                        }
                        try {
                            Thread.sleep(1000L);
                            continue;
                        }
                        catch (InterruptedException interruptedException) {
                            Log.out(interruptedException);
                        }
                    }
                    MySocket.sendTouch2Server(Server.this.data);
                }
            }
        }
    }
}

