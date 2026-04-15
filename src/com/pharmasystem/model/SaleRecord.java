package com.pharmasystem.model;

import java.time.LocalDate;

public class SaleRecord {
    private final String saleId;
    private final LocalDate saleDate;
    private final double totalAmount;

    public SaleRecord(String saleId, LocalDate saleDate, double totalAmount) {
        this.saleId = saleId;
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;
    }

    public String getSaleId() {
        return saleId;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}
