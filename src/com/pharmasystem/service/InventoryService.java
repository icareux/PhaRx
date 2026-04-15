package com.pharmasystem.service;

import com.pharmasystem.database.BatchDAO;
import com.pharmasystem.model.Medicine;
import com.pharmasystem.util.AppValidator;

public class InventoryService {

    public boolean isAvailable(Medicine m, int qty) {
        if (m == null) {
            return false;
        }
        AppValidator.requirePositive(qty, "Quantity");
        return new BatchDAO().getTotalStockByMedicineId(m.getMedicineId()) >= qty;
    }
}
