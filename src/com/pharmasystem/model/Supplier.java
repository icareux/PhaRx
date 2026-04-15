package com.pharmasystem.model;

import java.util.ArrayList;
import java.util.List;

public class Supplier {
    private String supplierId;
    private String name;
    private String contactInfo;
    private List<Medicine> suppliedMedicines = new ArrayList<>();

    public Supplier(String id, String name, String contact) {
        this.supplierId = id;
        this.name = name;
        this.contactInfo = contact;
    }

    public void addMedicine(Medicine m) {
        suppliedMedicines.add(m);
    }

    public void removeMedicine(Medicine m) {
        suppliedMedicines.remove(m);
    }
}