package com.pharmasystem.service;

import com.pharmasystem.database.BatchDAO;
import com.pharmasystem.model.Medicine;
import com.pharmasystem.model.Prescription;

public class SaleService {

    public boolean processSale(Medicine m, int qty, Prescription p) {
        if (m == null) return false;
        if (qty <= 0) return false;

        if (m.isExpired()) return false;
        if (new BatchDAO().getTotalStockByMedicineId(m.getMedicineId()) < qty) return false;

        if (m.requiresPrescription()) {
            if (p == null || !p.validatePrescription()) return false;
        }

        return true;
    }
}
