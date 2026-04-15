package com.pharmasystem.util;

import java.time.LocalDate;

public final class AppValidator {

    private AppValidator() {
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    public static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        return value;
    }

    public static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        return value;
    }

    public static double requireNonNegative(double value, String fieldName) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(fieldName + " must be a valid non-negative number.");
        }
        return value;
    }

    public static LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    public static void requireExpiryAfterPurchase(LocalDate purchaseDate, LocalDate expiryDate) {
        requireDate(purchaseDate, "Purchase date");
        requireDate(expiryDate, "Expiry date");

        if (expiryDate.isBefore(purchaseDate)) {
            throw new IllegalArgumentException("Expiry date cannot be earlier than purchase date.");
        }
    }
}
