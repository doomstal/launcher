package doomstal.launcher;

import java.net.URL;
import java.nio.charset.Charset;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class Manifest {
    private final String version = "1.0";

    public Set<Entry> entries = new TreeSet<Entry>();
    public List<String> jvmArgs = new ArrayList<String>();
    public List<String> jarArgs = new ArrayList<String>();
    public String mainClass = "net.minecraft.client.main.Main";
    private File file;
    public int sizeTotal = 0;

    public Manifest() { }

    public static Manifest read(URL url) {
        try {
            Manifest manifest = new Manifest();
            manifest.read(url.openStream());
            return manifest;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Manifest read(File file) {
        Manifest manifest = new Manifest();
        manifest.file = file;
        try {
            manifest.read(new FileInputStream(file));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return manifest;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    private void read(InputStream is) throws Exception {
        InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
        BufferedReader br = new BufferedReader(isr);
        String line;

        boolean jarArgsSet = false;
        boolean mainClassSet = false;
        boolean entriesSet = false;
        while((line = br.readLine()) != null) {
            if(line.startsWith("#")) continue;
            if(line.trim().equals("")) continue;
            Scanner s = new Scanner(line);
            s.useDelimiter(";");
            String first = s.next().trim();
            String arg;
            switch(first) {
                case "jar":
                    arg = s.next().trim();
                    jarArgs.add(arg);
                    jarArgsSet = true;
                break;
                case "jvm":
                    arg = s.next().trim();
                    jvmArgs.add(arg);
                break;
                case "mainClass":
                    mainClass = s.next().trim();
                    mainClassSet = true;
                break;
                default:
                    int size = s.nextInt();
                    sizeTotal += size;
                    String path = s.next().trim();
                    if(!Entry.valid(first, size, path)) throw new Exception("invalid manifest entry "+first+" "+size+" "+path);
                    entries.add(new Entry(first, size, path));
                    entriesSet = true;
            }
        }
        is.close();
        if(!jarArgsSet || !mainClassSet || !entriesSet) throw new Exception("manifest didn't read properly");
    }

    public void save() {
        try {
            FileWriter fw = new FileWriter(file);
            for(Entry en: entries) {
                fw.write(en.hash+";"+en.size+";"+(en.userfile?("_userfile/"+en.path):en.path)+"\n");
            }
            for(String arg: jvmArgs) {
                fw.write("jvm;"+arg+"\n");
            }
            for(String arg: jarArgs) {
                fw.write("jar;"+arg+"\n");
            }
            fw.write("mainClass;"+mainClass+"\n");
            fw.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void scanDir(File baseDir) {
        addJars(baseDir, new File(baseDir, "libraries").listFiles());
        addJars(baseDir, new File(baseDir, "versions").listFiles());
    }

    private void addJars(File baseDir, File[] files) {
        for(File file: files) {
            if(file.isDirectory()) {
                addJars(baseDir, file.listFiles());
            } else {
                String local = file.toString().substring(baseDir.toString().length()+1).replace('\\','/');
                entries.add(new Entry("", 0, local));
                System.out.println("adding "+local);
            }
        }
    }
}
