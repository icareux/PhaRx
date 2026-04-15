package com.pharmasystem.model;

import com.pharmasystem.util.AppValidator;
import java.time.LocalDate;
import java.util.List;

public class Prescription {
    private String prescriptionId;
    private List<Medicine> medicines;
    private LocalDate issueDate;

    public Prescription(String id, List<Medicine> meds) {
        this.prescriptionId = AppValidator.requireNonBlank(id, "Prescription ID");
        if (meds == null || meds.isEmpty()) {
            throw new IllegalArgumentException("Prescription must include at least one medicine.");
        }
        this.medicines = List.copyOf(meds);
        this.issueDate = LocalDate.now();
    }

    public boolean validatePrescription() {
        return issueDate != null && !issueDate.plusDays(30).isBefore(LocalDate.now());
    }
}
