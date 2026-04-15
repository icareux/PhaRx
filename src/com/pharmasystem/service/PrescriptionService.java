package com.pharmasystem.service;

import com.pharmasystem.model.Prescription;

public class PrescriptionService {

    public boolean validate(Prescription p) {
        return p != null && p.validatePrescription();
    }
}
