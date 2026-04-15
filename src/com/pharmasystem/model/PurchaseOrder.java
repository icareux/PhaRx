package com.pharmasystem.model;

import java.util.Map;

public class PurchaseOrder {
    private String orderId;
    private Supplier supplier;
    private Map<Medicine, Integer> orderedMedicines;
    private String orderStatus;

    public void receiveOrder() {
        orderStatus = "RECEIVED";
    }
}