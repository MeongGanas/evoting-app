package com.example.evotingapp.role;

public abstract class User {
    public static final String ROLE_VOTER = "VOTER";
    public static final String ROLE_PANWAS = "PANWAS";
    public static final String ROLE_KPU = "KPU";

    protected final String userId;
    protected final String username;

    protected User(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public abstract String getRoleLabel();
}
