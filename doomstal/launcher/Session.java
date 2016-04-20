package doomstal.launcher;

public class Session {
    public String username;
    public String uuid;
    public String access;

    public Session(String n, String i, String a) {
        username = n;
        uuid = i;
        access = a;
    }
    public Session() {
        this("");
    }
    public Session(String n) {
        if(n.length() == 0) n = "Player";
        username = n;
        uuid = "00000000-0000-0000-0000-000000000000";
        access = "0";
    }
}
