/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Log {
    private static ArrayList<String> logs = new ArrayList();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Object lock = new Object();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void out(String string) {
        Object object = lock;
        synchronized (object) {
            String string2 = String.format("%s %-12s %s", sdf.format(new Date()), Thread.currentThread().getName(), string);
            System.out.println(string2);
            System.out.flush();
            logs.add(string2);
            if (logs.size() > 1000) {
                logs.remove(0);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void out(Throwable throwable) {
        Object object = lock;
        synchronized (object) {
            String string = String.format("%s %-12s (ERROR) %s", sdf.format(new Date()), Thread.currentThread().getName(), throwable.getMessage());
            System.out.print(string);
            for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
                string = "\t" + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + " " + stackTraceElement.getFileName() + ":" + stackTraceElement.getLineNumber();
                System.out.println(string);
                logs.add(string);
            }
            System.out.flush();
            while (logs.size() > 1000) {
                logs.remove(0);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String[] get() {
        Object object = lock;
        synchronized (object) {
            return logs.toArray(new String[logs.size()]);
        }
    }
}

