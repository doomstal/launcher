package doomstal.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Scanner;

public class Options {
    public String memory = "1g";
    public String gc = "512m";
    public int width = 800;
    public int height = 600;

    public Options() {}

    public boolean setMemory(String m) {
        if(!validMemoryValue(m)) return false;
        memory = m;
        return true;
    }

    public boolean setGc(String g) {
        if(!validMemoryValue(g)) return false;
        gc = g;
        return true;
    }

    public boolean validMemoryValue(String s) {
        if(s.length() == 0) return true;
        int memory;
        char suffix = Character.toLowerCase(s.charAt(s.length()-1));
        try {
            if(Character.isAlphabetic(suffix)) {
                if(suffix != 'k' && suffix != 'm' && suffix != 'g') return false;
                memory = Integer.parseInt(s.substring(0, s.length()-1));
            } else {
                memory = Integer.parseInt(s);
            }
        } catch(NumberFormatException e) { return false; }
        if(memory < 0) return false;

        return true;
    }

    public boolean setWidth(String w) {
        if(w.length() == 0) return false;
        if(!validSizeValue(w)) return false;
        width = Integer.parseInt(w);
        return true;
    }
 
    public boolean setHeight(String h) {
        if(h.length() == 0) return false;
        if(!validSizeValue(h)) return false;
        height = Integer.parseInt(h);
        return true;
    }

    public boolean validSizeValue(String s) {
        if(s.length() == 0) return true;
        int size;
        try {
            size = Integer.parseInt(s);
        } catch(NumberFormatException e) { return false; }
        if(size < 0) return false;
        return true;
    }

    public boolean load(File file) {
        boolean result = true;
        try {
            InputStream is = new FileInputStream(file);
            try {
                InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
                String line;
                while((line = br.readLine()) != null) {
                    if(line.startsWith("#")) continue;
                    if(line.trim().equals("")) continue;
                    Scanner s = new Scanner(line);
                    s.useDelimiter("=");
                    String key = s.next().trim();
                    String value = "";
                    if(s.hasNext()) value = s.next().trim();
                    switch(key) {
                        case "memory": result = result && setMemory(value); break;
                        case "gc": result = result && setGc(value); break;
                        case "width": result = result && setWidth(value); break;
                        case "height": result = result && setHeight(value); break;
                        default:
                            System.err.println("unknown option: "+key);
                    }
                }
                is.close();
            } catch(IOException e) {
                e.printStackTrace();
                System.err.println("could not read options.txt");
                return false;
            }
        } catch(FileNotFoundException e) { return false; }
        return result;
    }

    public void save(File file) {
        try {
            FileWriter fw = new FileWriter(file);
            fw.write("memory="+memory+"\n");
            fw.write("gc="+gc+"\n");
            fw.write("width="+width+"\n");
            fw.write("height="+height+"\n");
            fw.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
