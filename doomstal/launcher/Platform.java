package doomstal.launcher;

enum Platform {
    all,
    windows,
    linux,
    macosx;

    public static Platform current = all;
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win"))
            current = windows;
        else if (osName.contains("mac"))
            current = macosx;
        else if (osName.contains("linux"))
            current = linux;
    }
};
