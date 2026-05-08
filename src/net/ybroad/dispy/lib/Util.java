/*
 * Decompiled with CFR 0.152.
 */
package net.ybroad.dispy.lib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.ybroad.dispy.lib.Log;
import net.ybroad.dispy.lib.Progress;

public class Util {
    public static String getSha256(String string) {
        return Util.getDigest(string, "SHA-256");
    }

    public static String getSha256Div4(String string) {
        return Util.skip(Util.getSha256(string), 4);
    }

    public static String getMd5(String string) {
        return Util.getDigest(string, "MD5");
    }

    public static String getMd5Div2(String string) {
        return Util.skip(Util.getDigest(string, "MD5"), 2);
    }

    private static String getDigest(String string, String string2) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(string2);
            byte[] byArray = messageDigest.digest(string.getBytes("UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            for (byte by : byArray) {
                String string3 = Integer.toHexString(0xFF & by);
                if (string3.length() == 1) {
                    stringBuilder.append("0");
                }
                stringBuilder.append(string3);
            }
            return stringBuilder.toString();
        }
        catch (Exception exception) {
            Log.out(exception);
            return null;
        }
    }

    private static String skip(String string, int n) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < string.length(); i += n) {
            stringBuilder.append(string.charAt(i));
        }
        return stringBuilder.toString();
    }

    public static String getFileName(String string) {
        String string2 = string.replace("\\", "/");
        int n = string2.lastIndexOf("/");
        if (n >= 0) {
            return string2.substring(n + 1);
        }
        return string;
    }

    public static void rmdir(File file) throws IOException {
        File[] fileArray;
        if (file == null) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        try {
            fileArray = file.toPath();
            if (Files.isSymbolicLink((Path)fileArray)) {
                Files.delete((Path)fileArray);
                return;
            }
        }
        catch (Error | Exception throwable) {
            Log.out(throwable);
        }
        fileArray = file.listFiles();
        if (fileArray != null) {
            for (File file2 : fileArray) {
                try {
                    Path path = file2.toPath();
                    if (Files.isSymbolicLink(path)) {
                        Files.delete(path);
                        continue;
                    }
                }
                catch (Error | Exception throwable) {
                    // empty catch block
                }
                if (file2.isFile()) {
                    file2.delete();
                    continue;
                }
                Util.rmdir(file2);
            }
        }
        file.delete();
    }

    public static void cp(File file, File file2) throws IOException {
        if (file.isDirectory()) {
            file2.mkdirs();
            for (File file3 : file.listFiles()) {
                Util.cp(file3, new File(file2, file3.getName()));
            }
        } else {
            boolean bl = false;
            try {
                bl = Files.isSymbolicLink(file.toPath());
            }
            catch (Error | Exception throwable) {
                Log.out(throwable);
            }
            if (bl) {
                Files.createSymbolicLink(file2.toPath(), file.toPath().toRealPath(new LinkOption[0]), new FileAttribute[0]);
            } else {
                int n;
                FileInputStream fileInputStream = new FileInputStream(file);
                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                byte[] byArray = new byte[102400];
                while ((n = fileInputStream.read(byArray)) > 0) {
                    fileOutputStream.write(byArray, 0, n);
                }
                fileOutputStream.close();
                fileInputStream.close();
            }
        }
    }

    public static void cp(File file, File file2, Progress progress) throws IOException {
        if (!file.isFile()) {
            return;
        }
        if (file2.exists()) {
            file2.delete();
        }
        try {
            int n;
            FileInputStream fileInputStream = new FileInputStream(file);
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            byte[] byArray = new byte[102400];
            int n2 = 0;
            long l = 0L;
            long l2 = file.length();
            while ((n = fileInputStream.read(byArray)) > 0) {
                fileOutputStream.write(byArray, 0, n);
                l += (long)n;
                if ((n2 += n) <= 0x100000) continue;
                n2 = 0;
                progress.update(100.0 * (double)l / (double)l2);
            }
            fileOutputStream.close();
            fileInputStream.close();
        }
        catch (IOException iOException) {
            Log.out(iOException);
        }
        progress.update(-1.0);
    }

    public static void mv(File file, File file2) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file2.exists()) {
            file2.delete();
        }
        try {
            Files.move(Paths.get(file.getPath(), new String[0]), Paths.get(file2.getPath(), new String[0]), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Error | Exception throwable) {
            file.renameTo(file2);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String createPidFile(String string) {
        String string2 = ManagementFactory.getRuntimeMXBean().getName();
        String string3 = string2.substring(0, string2.indexOf("@"));
        try (PrintStream printStream = null;){
            printStream = new PrintStream(string);
            printStream.println(string3);
        }
        return string3;
    }

    public static boolean zipDir(File file, File file2) {
        try {
            final Path path = file.toPath();
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            final ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            Files.walkFileTree(path, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult visitFile(Path path2, BasicFileAttributes basicFileAttributes) throws IOException {
                    zipOutputStream.putNextEntry(new ZipEntry(path.relativize(path2).toString().replace("\\", "/")));
                    Files.copy(path2, zipOutputStream);
                    zipOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path path2, BasicFileAttributes basicFileAttributes) throws IOException {
                    zipOutputStream.putNextEntry(new ZipEntry(path.relativize(path2).toString() + "/"));
                    zipOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
            zipOutputStream.close();
            fileOutputStream.close();
            return true;
        }
        catch (Exception exception) {
            Log.out(exception);
            return false;
        }
    }

    public static boolean unzip(File file, File file2) {
        try {
            ZipEntry zipEntry;
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String string = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    Util.mkdirs(file2, string);
                    continue;
                }
                String string2 = Util.dirpart(string);
                if (string2 != null) {
                    Util.mkdirs(file2, string2);
                }
                Util.extractFile(zipInputStream, file2, string);
            }
            zipInputStream.close();
            return true;
        }
        catch (IOException iOException) {
            Log.out(iOException);
            return false;
        }
    }

    private static void extractFile(ZipInputStream zipInputStream, File file, String string) throws IOException {
        byte[] byArray = new byte[102400];
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(file, string)));
        int n = -1;
        while ((n = zipInputStream.read(byArray)) > 0) {
            bufferedOutputStream.write(byArray, 0, n);
        }
        bufferedOutputStream.close();
    }

    private static void mkdirs(File file, String string) {
        File file2 = new File(file, string);
        if (!file2.exists()) {
            file2.mkdirs();
        }
    }

    private static String dirpart(String string) {
        int n = string.lastIndexOf(File.separatorChar);
        return n == -1 ? null : string.substring(0, n);
    }

    public static String getMacAddress() throws Exception {
        String string = System.getProperty("os.name");
        if (string.startsWith("Windows")) {
            return Util.parseMacAddress(Util.runIfConfigCommand("ipconfig /all"), "-");
        }
        if (string.startsWith("Linux")) {
            return Util.parseMacAddress(Util.runIfConfigCommand("ifconfig"), ":");
        }
        throw new Exception("unknown operating system: " + string);
    }

    private static String runIfConfigCommand(String string) throws Exception {
        int n;
        Process process = Runtime.getRuntime().exec(string);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
        StringBuffer stringBuffer = new StringBuffer();
        while ((n = ((InputStream)bufferedInputStream).read()) != -1) {
            stringBuffer.append((char)n);
        }
        ((InputStream)bufferedInputStream).close();
        return stringBuffer.toString();
    }

    private static String parseMacAddress(String string, String string2) {
        String string3 = null;
        String[] stringArray = string.split("\\p{XDigit}{2}(" + string2 + "\\p{XDigit}{2}){5}");
        int n = 0;
        for (String string4 : stringArray) {
            if (string4.length() >= string.length()) continue;
            n = string4.length();
            string3 = string.substring(n, n + 17);
            if (!string3.equals("00" + string2 + "00" + string2 + "00" + string2 + "00" + string2 + "00" + string2 + "00")) break;
            string = string.substring(n + 17);
        }
        return string3;
    }
}

