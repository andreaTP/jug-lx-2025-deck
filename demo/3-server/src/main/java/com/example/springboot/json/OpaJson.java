package com.example.springboot.json;

public final class OpaJson {

    private OpaJson() {};

    public static class Allow {
        public boolean allow;
    }
    public static class Authz {
        public Allow authz;
    }
    public static class Result {
        public Authz result;
    }
    public static class User {
        public String user;
        public User(String name) {
            this.user = name;
        }
    }

}
