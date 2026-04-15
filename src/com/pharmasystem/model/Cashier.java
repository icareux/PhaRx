package com.pharmasystem.model;

public class Cashier extends User {

    @Override
    public void login() {
        System.out.println("Cashier logged in");
    }
}