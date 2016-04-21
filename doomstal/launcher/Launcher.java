package doomstal.launcher;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.PrintStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Launcher {
    public static final String baseURL = "http://example.com/minecraft_files/";
    public static final String baseSubdir = ".launcher";
    public static final String authURL = "https://authserver.mojang.com/authenticate";

    private File optionsFile;
    public Options options = new Options();

    private File baseDir;

    private static boolean noUpdate = false;
    private static boolean local = false;
    private static boolean offline = false;

    private Manifest manifest;

    private MainFrame mainFrame;

    private Session session;

    private boolean showSpeed = false;

    private Process process;

    public void saveOptions() {
        options.save(optionsFile);
    }

    private Launcher() {
        mainFrame = new MainFrame(this);
        mainFrame.setVisible(true);

        System.out.println("platform is "+Platform.current.name());

        if(!local) {
            baseDir = new File(Utils.getUserHome(), baseSubdir);
            if(!baseDir.exists()) baseDir.mkdir();
        } else {
            baseDir = new File(".");
        }

        System.out.println("using "+ baseDir);

        File manifestFile = new File(baseDir, "manifest.txt");
        URL manifestURL;
        try {
            manifestURL = new URL(baseURL + "manifest.txt");
        } catch(MalformedURLException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        optionsFile = new File(baseDir, "options.txt");
        boolean optionsLoaded = options.load(optionsFile);

        mainFrame.setStatus(Strings.checkingUpdates);

        System.out.println("loading manifest");

        Manifest localManifest = Manifest.read(manifestFile);

        boolean ready = true;
        if(!noUpdate && !local) {
            Manifest remoteManifest = Manifest.read(manifestURL);

            if(remoteManifest != null) {
                if(update(localManifest, remoteManifest)) {
                    localManifest.entries = remoteManifest.entries;
                    localManifest.jarArgs = remoteManifest.jarArgs;
                    localManifest.jvmArgs = remoteManifest.jvmArgs;
                    localManifest.mainClass = remoteManifest.mainClass;
                    localManifest.save();
                } else {
                    System.err.println("update failed!");
                    mainFrame.setStatus(Strings.updateFailed);
                    JOptionPane.showMessageDialog(null, Strings.updateFailed, "warning", JOptionPane.WARNING_MESSAGE);
                    ready = false;
                    offline = true;
                }
            } else {
                JOptionPane.showMessageDialog(null, Strings.couldNotGetUpdate, "warning", JOptionPane.WARNING_MESSAGE);
                offline = true;
            }
        }

        if(offline) {
            JOptionPane.showMessageDialog(null, Strings.offlineOnly, "warning", JOptionPane.WARNING_MESSAGE);
        }
        if(local) localManifest.scanDir(baseDir);

        if(localManifest.isEmpty()) {
            System.err.println("local manifest corrupt!");
            mainFrame.setStatus(Strings.localManifestCorrupt);
            ready = false;
            JOptionPane.showMessageDialog(null, Strings.corruptLaunchImpossible, "error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        manifest = localManifest;

        if(ready) {
            System.out.println("ready");
            mainFrame.setStatus(Strings.readyToLaunch);
            if(!optionsLoaded) mainFrame.showOptions();
            mainFrame.allowStart();
        }
    }

    private final static Pattern loginError = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private final static Pattern loginErrorMessage = Pattern.compile("\"errorMessage\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private final static Pattern loginErrorCause = Pattern.compile("\"cause\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private final static Pattern loginName = Pattern.compile("\"selectedProfile\".*\"name\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private final static Pattern loginId = Pattern.compile("\"selectedProfile\".*\"id\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private final static Pattern loginAccessToken = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    public boolean login(String username, String password) {
        if(local || offline) {
            session = new Session(username);
            return true;
        }
        session = null;
        try {
            System.out.println("logging in");

            URL url = new URL(authURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Content-Type", "application/json");

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes("{\"agent\":{\"name\":\"Minecraft\",\"version\":1},\"username\":\""+username+"\",\"password\":\""+password+"\"}");
            wr.flush();
            wr.close();

            String content;
            if(con.getResponseCode() == 200) {
                content = new Scanner(con.getInputStream()).useDelimiter("\\Z").next();
            } else {
                content = new Scanner(con.getErrorStream()).useDelimiter("\\Z").next();
            }

            Matcher mError = loginError.matcher(content);
            if(mError.find()) {
                System.err.println("error=\""+mError.group(1)+"\"");

                Matcher mMessage = loginErrorMessage.matcher(content);
                if(mMessage.find()) System.err.println("errorMessage=\""+mMessage.group(1)+"\"");

                Matcher mCause = loginErrorCause.matcher(content);
                if(mCause.find()) System.err.println("cause=\""+mCause.group(1)+"\"");

                mainFrame.setStatus(Strings.invalidUsernamePassword);
                JOptionPane.showMessageDialog(null, "invalid username or password!", "authentication failed!", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            Matcher mName = loginName.matcher(content);
            Matcher mId = loginId.matcher(content);
            Matcher mAccess = loginAccessToken.matcher(content);
            if(mName.find() && mId.find() && mAccess.find()) {
                session = new Session(mName.group(1), mId.group(1), mAccess.group(1));
                System.out.println("logged in");
                mainFrame.setStatus(Strings.loggedIn);
                System.out.println("username=\""+session.username+"\"");
                System.out.println("uuid=\""+session.uuid+"\"");
                System.out.println("access=\""+session.access+"\"");
            } else {
                System.err.println(content);
                throw new Exception("response error");
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("error during authentication");
            mainFrame.setStatus(Strings.authenticationFailed);
            JOptionPane.showMessageDialog(null, Strings.errorDuringAuthentication, Strings.authenticationFailed, JOptionPane.WARNING_MESSAGE);
        }
        if(session == null) {
            if(JOptionPane.showConfirmDialog(null, Strings.playOffline, Strings.playOffline, JOptionPane.YES_NO_OPTION) == 0) {
                session = new Session(username);
                return true;
            }
            return false;
        }
        return true;
    }

    public void start() {
        if(session == null) return;

        System.out.println("launching game");
        mainFrame.setStatus(Strings.launching);
        mainFrame.disableAll();

        new Thread() {
            public void run() {
                launch(manifest);
            }
        }.start();
    }

    private void launch(Manifest manifest) {
        List<String> command = new ArrayList<>();

        try {
            File fj = new File(System.getProperty("java.home"));
            fj = new File(fj, "bin");
            fj = new File(fj, "java");
            command.add(fj.getCanonicalPath());
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        File nativesDir = new File(baseDir, "natives" + '/' + Platform.current.name());
        try {
            command.add("-Djava.library.path="+nativesDir.getCanonicalPath());
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if(options.memory.length() > 0) {
            command.add("-Xms"+options.memory);
            command.add("-Xmx"+options.memory);
        }

        String[] javaVersionElements = System.getProperty("java.version").split("\\.");
        int major = Integer.parseInt(javaVersionElements[1]);
        System.out.println("java version is "+major);
        if(options.gc.length() > 0) {
            if(major >= 8) {
                command.add("-XX:MaxMetaspaceSize="+options.gc);
            } else {
                command.add("-XX:MaxPermSize="+options.gc);
            }
        }

        for(String arg: manifest.jvmArgs) {
            command.add(arg);
        }

        command.add("-cp");

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Entry ent : manifest.entries) {
            if( (ent.path.startsWith("libraries/") && (ent.platform == Platform.current || ent.platform == Platform.all))
            || ent.path.startsWith("versions/")) {
                if (first) {
                    first = false;
                } else {
                    builder.append(File.pathSeparator);
                }
                try {
                    builder.append((new File(baseDir, ent.path)).getCanonicalPath());
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
        command.add(builder.toString());

        command.add(manifest.mainClass);

        command.add("--username");
        command.add(session.username);
        command.add("--uuid");
        command.add(session.uuid);
        command.add("--accessToken");
        command.add(session.access);
        command.add("--userProperties");
        command.add("{}");
        command.add("--userType");
        command.add("mojang");

        command.add("--gameDir");
        File minecraftDir = null;
        try {
            minecraftDir = new File(baseDir, "minecraft");
            if(!minecraftDir.exists()) minecraftDir.mkdir();
            command.add(minecraftDir.getCanonicalPath());
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        command.add("--assetsDir");
        try {
            File assetsDir = new File(baseDir, "assets");
            command.add(assetsDir.getCanonicalPath());
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        command.add("--width");
        command.add(""+options.width);
        command.add("--height");
        command.add(""+options.height);

        for(String arg: manifest.jarArgs) {
            command.add(arg);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        processBuilder.directory(minecraftDir);

        try {
            process = processBuilder.start();
            new ThreadOutputReader(process.getInputStream(), System.out).start();
            new ThreadOutputReader(process.getErrorStream(), System.err).start();
            int code = process.waitFor();
            System.out.println("process terminated with code " + code);
            System.exit(code);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private class ThreadOutputReader extends Thread {
        InputStream is;
        PrintStream ps;

        ThreadOutputReader(InputStream is, PrintStream ps) {
            this.is = is;
            this.ps = ps;
        }

        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                boolean visible = (ps == System.out);
                while((line = br.readLine()) != null) {
                    if(visible && line.toLowerCase().contains("lwjgl")) {
                        mainFrame.setVisible(false);
                        visible = false;
                    }
                    ps.println(line);
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    boolean update(Manifest local, Manifest remote) {
        if(remote == null) return false;

        System.out.println("checking files");
        mainFrame.setStatus(Strings.checkingFiles);
        mainFrame.setProgress(0, 0, 1);

        Set<Entry> toDownload = new HashSet<Entry>();
        Set<Entry> toDownloadAssets = new HashSet<Entry>();
        int downloadTotal = 0;
        int downloadTotalAssets = 0;
        IUpdate progressCallback = new CountCallback(remote.sizeTotal);
        int assetsSizeTotal = 0;
        Entry entryAssetsZip = null;

        for(Entry entry: remote.entries) {
            if(entry.path.equals("assets.zip")) {
                entryAssetsZip = entry;
                progressCallback.update(entry.size);
                continue;
            }
            if(entry.asset) {
                if(!entry.checkHash(baseDir, progressCallback)) {
                    toDownloadAssets.add(entry);
                    downloadTotalAssets += entry.size;
                    File file = new File(baseDir, entry.path);
                    if(file.exists()) file.delete();
                }
                assetsSizeTotal += entry.size;
            } else {
                if(!entry.checkHash(baseDir, progressCallback)) {
                    toDownload.add(entry);
                    downloadTotal += entry.size;
                }
            }
        }

        boolean extract = false;
        if(downloadTotalAssets > 0 && entryAssetsZip != null) {
            extract = true;

            System.out.println("checking assets.zip");
            mainFrame.setStatus(Strings.checkingAssets);
            mainFrame.setProgress(0, 0, 1);

            if(!entryAssetsZip.checkHash(baseDir, new CountCallback(entryAssetsZip.size))
            && downloadTotalAssets > assetsSizeTotal/2) {

                System.out.println("downloading assets.zip");
                mainFrame.setStatus(Strings.downloadingAssets);
                mainFrame.setProgress(0, 0, 1);
                progressCallback = new CountCallback(entryAssetsZip.size * 2);

                showSpeed = true;
                boolean downloaded = Utils.downloadFile(baseURL, baseDir, entryAssetsZip, progressCallback);
                showSpeed = false;
                if(!downloaded || !entryAssetsZip.checkHash(baseDir, progressCallback)) {
                    System.err.println("failed to download assets.zip");
                    extract = false;
                }
            }
        }
        if(extract) {
            System.out.println("extracting assets.zip");
            mainFrame.setStatus(Strings.extractingAssets);
            mainFrame.setProgress(0, 0, 1);

            try {
                Utils.extractZip(baseDir, entryAssetsZip.path, new CountCallback(assetsSizeTotal));
            } catch(IOException e) {
                e.printStackTrace();
                extract = false;
            }
        }
        if(!extract) {
            toDownload.addAll(toDownloadAssets);
            downloadTotal += downloadTotalAssets;
        }

        int count = 0;
        int countMax = toDownload.size();
        if(countMax > 0) {
            System.out.println("downloading");
            mainFrame.setStatus(Strings.downloading);
            mainFrame.setProgress(0, 0, 1);
            progressCallback = new CountCallback(downloadTotal * 2);

            showSpeed = true;
            for(Entry entry: toDownload) {
                System.out.println("downloading "+entry.path);
                mainFrame.setStatus(Strings.downloading+" ("+count+"/"+countMax+")");
                String downloadUrl = entry.path;
                if(!Utils.downloadFile(baseURL, baseDir, entry, progressCallback)
                || !entry.checkHash(baseDir, progressCallback)) {
                    System.err.println("download failed!");
                    mainFrame.setStatus(Strings.downloadFailed);
                    return false;
                }
                ++count;
            }
            showSpeed = false;
        }

        Set<String> toDelete = new HashSet<String>();
        for(Entry entry: local.entries) {
            if(!remote.entries.contains(entry) && !entry.userfile) {
                System.out.println("to delete "+entry.path);
                toDelete.add(entry.path);
            }
        }

        count = 0;
        countMax = toDelete.size();
        if(countMax > 0) {
            System.out.println("deleting outdated files");
            mainFrame.setStatus(Strings.deletingOutdated);
            mainFrame.setProgress(0, 0, countMax);
            for(String path: toDelete) {
                File file = new File(baseDir, path);
                if(file.exists())
                    file.delete();
                ++count;
                mainFrame.setProgress(count, 0, countMax);
            }
        }

        mainFrame.setProgress(1, 0, 1);
        return true;
    }

    private class CountCallback implements IUpdate {
        private int count = 0;
        private int countMax;
        private long prevTime;

        CountCallback(int m) {
            countMax = m;
            prevTime = System.nanoTime();
        }

        @Override
        public void update(int inc) {
            count += inc;
            mainFrame.setProgress(count, 0, countMax);

            if(showSpeed) {
                float speed = count / ((System.nanoTime() - prevTime) / 1000000000.0F);
                if(speed > 1048576) {
                    mainFrame.setProgressString(String.format("%.2f", speed / 1048576) + Strings.speedmbs);
                } else if (speed > 1024) {
                    mainFrame.setProgressString((int)(speed / 1024) + Strings.speedkbs);
                } else {
                    mainFrame.setProgressString((int)speed + Strings.speedbs);
                }
            }
        }
    }

    public static void main(String[] args) {
        for(String s: args) {
            switch(s) {
                case "noupdate":
                    noUpdate = true;
                    offline = true;
                break;
                case "local":
                    local = true;
                break;
                default:
                    System.err.println("unknown argument "+s);
            }
        }

        new Launcher();
    }
}
