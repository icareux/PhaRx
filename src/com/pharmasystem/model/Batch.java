package com.pharmasystem.model;

import java.time.LocalDate;

public class Batch {

    private int batchId;
    private int medicine_id;
    private int quantity;
    private LocalDate expiryDate;
    private LocalDate purchaseDate;

    public Batch(int batchId, int medicine_id, int quantity,
                 LocalDate expiryDate, LocalDate purchaseDate) {

        this.batchId = batchId;
        this.medicine_id = medicine_id;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.purchaseDate = purchaseDate;
    }

    public int getBatchId() {
        return batchId;
    }

    public int getMedicineId() {
        return medicine_id;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }
}
