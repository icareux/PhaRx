package com.pharmasystem.model;

import java.time.LocalDate;
import com.pharmasystem.util.AppValidator;

public class Medicine {
    private int medicine_id;
    private String name;
    private String category;
    private double price;
    private int quantityInStock;
    private LocalDate expirationDate;
    private int reorderLevel;
    private boolean requiresPrescription;

    public Medicine(int id, String name, String category, double price,
                    int qty, LocalDate exp, int reorder, boolean req) {
        if (id <= 0) {
            throw new IllegalArgumentException("Medicine ID must be greater than zero.");
        }
        this.medicine_id = id;
        this.name = AppValidator.requireNonBlank(name, "Medicine name");
        this.category = category == null ? "" : category.trim();
        this.price = AppValidator.requireNonNegative(price, "Price");
        this.quantityInStock = AppValidator.requireNonNegative(qty, "Quantity in stock");
        this.expirationDate = exp;
        this.reorderLevel = AppValidator.requireNonNegative(reorder, "Reorder level");
        this.requiresPrescription = req;
    }

    public void updateStock(int amount) {
        int updated = this.quantityInStock + amount;
        if (updated < 0) {
            throw new IllegalArgumentException("Stock cannot be reduced below zero.");
        }
        this.quantityInStock = updated;
    }

    public boolean isLowStock() {
        return quantityInStock <= reorderLevel;
    }

    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    public boolean requiresPrescription() {
        return requiresPrescription;
    }

    public int getQuantityInStock() { 
        return quantityInStock; 
    }
    public double getPrice() { 
        return price; 
    }
    public String getName() {
        return name;
    }
    public int getMedicineId() {
        return medicine_id;
    }
}
