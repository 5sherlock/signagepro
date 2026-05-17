/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.manager.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import net.ybroad.dispy.lib.ClientData;
import net.ybroad.dispy.lib.UnionData;

public class UnionSnap {
    private static HashMap<ClientData, UnionSnap> instances = new HashMap();
    private ArrayList<UnionData> waits;
    private HashMap<UnionData, File> files;
    private double scaleX;
    private double scaleY;
    private double x;
    private double y;
    private int w;
    private int h;

    public static void init(ClientData clientData) {
        instances.put(clientData, new UnionSnap(clientData));
    }

    public static String put(String string, File file, String string2) {
        ClientData clientData = null;
        UnionSnap unionSnap = null;
        block2: for (ClientData clientData2 : instances.keySet()) {
            for (UnionData unionData : clientData2.union) {
                if (!unionData.id.equals(string)) continue;
                clientData = clientData2;
                unionSnap = instances.get(clientData);
                break block2;
            }
        }
        if (unionSnap != null) {
            try {
                super._put(string, file, string2);
                if (unionSnap.waits.isEmpty()) {
                    instances.remove(clientData);
                }
                return clientData.id;
            }
            catch (IOException iOException) {
                iOException.printStackTrace();
                instances.remove(clientData);
                return null;
            }
        }
        return null;
    }

    private UnionSnap(ClientData clientData) {
        this.waits = new ArrayList<UnionData>(clientData.union);
        this.files = new HashMap();
        if (clientData.numW > clientData.numH) {
            this.scaleX = 2.0;
            this.scaleY = 2 * clientData.numW / clientData.numH;
        } else if (clientData.numW < clientData.numH) {
            this.scaleX = 2 * clientData.numH / clientData.numW;
            this.scaleY = 2.0;
        } else {
            this.scaleX = 2.0;
            this.scaleY = 2.0;
        }
        this.w = (int)((double)clientData.width / this.scaleX);
        this.h = (int)((double)clientData.height / this.scaleY);
        for (UnionData unionData : clientData.union) {
            if (this.x > (double)unionData.xpos / unionData.f / this.scaleX) {
                this.x = (double)unionData.xpos / unionData.f / this.scaleX;
            }
            if (!(this.y > (double)unionData.ypos / unionData.f / this.scaleX)) continue;
            this.y = (double)unionData.ypos / unionData.f / this.scaleX;
        }
    }

    private void _put(String string, File file, String string2) throws IOException {
        for (UnionData object2 : this.waits) {
            if (!object2.id.equals(string)) continue;
            this.waits.remove(object2);
            this.files.put(object2, file);
            break;
        }
        File file2 = new File(string2);
        BufferedImage bufferedImage = new BufferedImage(this.w, this.h, 1);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        for (UnionData unionData : this.files.keySet()) {
            BufferedImage bufferedImage2 = ImageIO.read(this.files.get(unionData));
            graphics2D.drawImage(bufferedImage2, (int)((double)unionData.xpos / unionData.f / this.scaleX - this.x + 0.5), (int)((double)unionData.ypos / unionData.f / this.scaleY - this.y + 0.5), (int)((double)unionData.w / unionData.f / this.scaleX + 0.5), (int)((double)unionData.h / unionData.f / this.scaleY + 0.5), null);
        }
        graphics2D.setColor(Color.RED);
        for (UnionData unionData : this.files.keySet()) {
            graphics2D.draw(new Rectangle((int)((double)unionData.xpos / unionData.f / this.scaleX - this.x + 0.5), (int)((double)unionData.ypos / unionData.f / this.scaleY - this.y + 0.5), (int)((double)unionData.w / unionData.f / this.scaleX + 0.5), (int)((double)unionData.h / unionData.f / this.scaleY + 0.5)));
        }
        graphics2D.dispose();
        ImageIO.write((RenderedImage)bufferedImage, "jpg", file2);
    }
}

