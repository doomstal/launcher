package doomstal.launcher;

import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
    public static String getUserHome() {
        if (Platform.current == Platform.windows) {
            return System.getenv("USERPROFILE");
        } else {
            return System.getProperty("user.home");
	    }
    }

    private static final byte[] buffer = new byte[65536];

    public static String fileHash(File file, IUpdate progressCallback) {
        try {
            InputStream fis = new FileInputStream(file);

            MessageDigest complete = MessageDigest.getInstance("SHA1");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                    if(progressCallback != null) progressCallback.update(numRead);
                }
            } while (numRead != -1);
            fis.close();
            byte[] b = complete.digest();
            String result = "";
            for (int i=0; i < b.length; i++) {
                result +=
                Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
            }
            return result;
        } catch(Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean downloadFile(String baseURL, File baseDir, Entry entry, IUpdate progressCallback) {
        if(entry == null) return false;
        String path = entry.path;
        String downloadPath = entry.path;
        if(entry.userfile) downloadPath = "_userfile/" + entry.path;
        try {
            URL url = new URL(baseURL + downloadPath);

            if(progressCallback == null) {
                ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                File file = new File(baseDir, path);
                if(!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(file);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } else {
                InputStream in = url.openStream();

                File file = new File(baseDir, path);
                if(!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(file);

                int r;
                while((r=in.read(buffer)) != -1) {
                    fos.write(buffer, 0, r);
                    progressCallback.update(r);
                }
                in.close();
                fos.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // http://www.codejava.net/java-se/file-io/programmatically-extract-a-zip-file-using-java
    public static void extractZip(File baseDir, String path, IUpdate progressCallback) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(new File(baseDir, path)));
        ZipEntry entry = zipIn.getNextEntry();
        while(entry != null) {
            if(entry.isDirectory()) {
                File dir = new File(baseDir, entry.getName());
                if(!dir.exists()) dir.mkdir();
            } else {
                File file = new File(baseDir, entry.getName());
                if(!file.exists()) {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    int read = 0;
                    while((read = zipIn.read(buffer)) != -1) {
                        bos.write(buffer, 0, read);
                        if(progressCallback != null) progressCallback.update(read);
                    }
                    bos.close();
                } else {
                    if(progressCallback != null) progressCallback.update((int)entry.getSize());
                }
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }
}
