package com.pharmasystem.ui;

import com.pharmasystem.model.Medicine;
import com.pharmasystem.session.Session;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class ReceiptController {

    @FXML private Label receiptDateLabel;
    @FXML private Label receiptCashierLabel;
    @FXML private Label receiptIdLabel;
    @FXML private ListView<String> receiptItemsList;
    @FXML private Label receiptTotalLabel;
    @FXML private TextField paymentField;
    @FXML private Label changeLabel;
    @FXML private Label receiptErrorLabel;

    private double totalAmount = 0;
    private boolean paymentConfirmed = false;

    public void setCartData(Map<Medicine, Integer> cart) {
        totalAmount = cart.entrySet().stream()
            .mapToDouble(e -> e.getKey().getPrice() * e.getValue()).sum();

        // Populate items list
        ObservableList<String> lines = FXCollections.observableArrayList();
        for (Map.Entry<Medicine, Integer> entry : cart.entrySet()) {
            double lineTotal = entry.getKey().getPrice() * entry.getValue();
            lines.add(String.format("%-28s x%-3d  ₱%.2f",
                entry.getKey().getName(), entry.getValue(), lineTotal));
        }
        receiptItemsList.setItems(lines);

        // Meta
        receiptTotalLabel.setText("₱" + String.format("%.2f", totalAmount));
        receiptDateLabel.setText("Date: " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a")));

        String cashierName = Session.currentUser != null
            ? Session.currentUser.getUsername() : "—";
        receiptCashierLabel.setText("Cashier: " + cashierName);

        receiptIdLabel.setText("#" + String.format("%06d", (int)(Math.random() * 999999)));

        changeLabel.setText("₱0.00");
    }

    @FXML
    private void onPaymentKeyReleased(KeyEvent event) {
        receiptErrorLabel.setText("");
        String text = paymentField.getText().trim();
        if (text.isEmpty()) {
            changeLabel.setText("₱0.00");
            changeLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 16px; -fx-font-weight: bold;");
            return;
        }
        try {
            double paid   = Double.parseDouble(text);
            double change = paid - totalAmount;
            if (change < 0) {
                changeLabel.setText("−₱" + String.format("%.2f", Math.abs(change)));
                changeLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else {
                changeLabel.setText("₱" + String.format("%.2f", change));
                changeLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 16px; -fx-font-weight: bold;");
            }
        } catch (NumberFormatException e) {
            changeLabel.setText("₱0.00");
        }
    }

    @FXML
    private void handleConfirmPayment() {
        String text = paymentField.getText().trim();
        if (text.isEmpty()) {
            receiptErrorLabel.setText("Please enter the amount tendered.");
            return;
        }

        double paid;
        try {
            paid = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            receiptErrorLabel.setText("Please enter a valid amount.");
            return;
        }

        if (paid < totalAmount) {
            receiptErrorLabel.setText("Amount tendered is less than the total.");
            return;
        }

        paymentConfirmed = true;
        closeStage();
    }

    @FXML
    private void handleCancel() {
        paymentConfirmed = false;
        closeStage();
    }

    public boolean isPaymentConfirmed() { return paymentConfirmed; }

    private void closeStage() {
        Stage stage = (Stage) paymentField.getScene().getWindow();
        stage.close();
    }
}
