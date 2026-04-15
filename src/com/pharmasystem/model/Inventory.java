package com.pharmasystem.model;

import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private List<Medicine> medicineList = new ArrayList<>();

    public void addMedicine(Medicine m) {
        medicineList.add(m);
    }

    public void removeMedicine(String id) {
        medicineList.removeIf(m -> String.valueOf(m.getMedicineId()).equals(id));
    }

    public List<Medicine> checkLowStockItems() {
        List<Medicine> low = new ArrayList<>();
        for (Medicine m : medicineList)
            if (m.isLowStock()) low.add(m);
        return low;
    }
}
