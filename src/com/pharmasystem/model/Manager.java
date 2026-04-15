package com.pharmasystem.model;

public class Manager extends User {

    @Override
    public void login() {
        System.out.println("Manager logged in");
    }
}