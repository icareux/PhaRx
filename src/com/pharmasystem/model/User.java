package com.pharmasystem.model;

public abstract class User {
    private String userId;
    protected String role;

    public abstract void login();
    
    public String getUsername() {
        return userId;
    }


    public String getRole() {
        return role;
    }
}

    
