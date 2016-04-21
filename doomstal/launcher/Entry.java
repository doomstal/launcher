package doomstal.launcher;

import java.io.File;

public class Entry implements Comparable {
    public String path;
    public int size;
    public String hash;
    public Platform platform;
    public boolean userfile;
    public boolean asset;

    public static boolean valid(String hash, int size, String path) {
        if(size < 0) return false;
        if(!hash.toLowerCase().matches("^[0-9abcdef]{40}$")) return false;
        if(path == null || path.length() == 0) return false;
        try {
            File test = new File(path);
            if(test.getCanonicalPath() == null) return false;
        } catch(Exception e) { return false; }
        return true;
    }

    public Entry(String hash, int size, String path) {
        this.hash = hash;
        this.size = size;
        this.path = path;
        if(path.indexOf("/windows/") != -1) this.platform = Platform.windows;
        else if(path.indexOf("/linux/") != -1) this.platform = Platform.linux;
        else if(path.indexOf("/osx/") != -1) this.platform = Platform.osx;
        else this.platform = Platform.all;
        if(path.startsWith("_userfile/")) {
            this.path = path.substring(10);
            this.userfile = true;
        } else {
            this.userfile = false;
        }
        if(path.startsWith("assets/")) this.asset = true;
    }

    public boolean checkHash(File baseDir, IUpdate progressCallback) {
        if(platform != Platform.all && platform != Platform.current) {
            if(progressCallback != null) progressCallback.update(size);
            return true;
        }
        File file = new File(baseDir, path);
        if(!file.exists() || file.length() != size) {
            if(progressCallback != null) progressCallback.update(size);
            return false;
        }
        if(userfile) {
            if(progressCallback != null) progressCallback.update(size);
            return true;
        }

        return hash.equalsIgnoreCase(Utils.fileHash(file, progressCallback));
    }

    @Override
    public boolean equals(Object o) {
        return path.equals(((Entry)o).path);
    }

    @Override
    public int compareTo(Object o) {
        return path.compareTo(((Entry)o).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
