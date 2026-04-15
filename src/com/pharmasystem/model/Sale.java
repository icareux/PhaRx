package com.pharmasystem.model;

import com.pharmasystem.util.AppValidator;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Sale {
    private String saleId;
    private LocalDate saleDate;
    private Map<Medicine, Integer> soldItems = new HashMap<>();

    public Sale(String id) {
        this.saleId = AppValidator.requireNonBlank(id, "Sale ID");
        this.saleDate = LocalDate.now();
    }

    public void addItem(Medicine m, int qty) {
        if (m == null) {
            throw new IllegalArgumentException("Medicine is required.");
        }
        AppValidator.requirePositive(qty, "Quantity");
        soldItems.merge(m, qty, Integer::sum);
    }

    public double calculateTotal() {
        return soldItems.entrySet().stream()
                .mapToDouble(e -> e.getKey().getPrice() * e.getValue())
                .sum();
    }
}
