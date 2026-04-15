package com.pharmasystem.model;

public class Pharmacist extends User {

    @Override
    public void login() {
        System.out.println("Pharmacist logged in");
    }

    public boolean validatePrescription(Prescription p) {
        return p != null && p.validatePrescription();
    }
}
