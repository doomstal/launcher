package doomstal.launcher;

enum Platform {
    all,
    windows,
    linux,
    osx;

    public static Platform current = all;
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win"))
            current = windows;
        else if (osName.contains("mac"))
            current = osx;
        else if (osName.contains("linux"))
            current = linux;
    }
};
