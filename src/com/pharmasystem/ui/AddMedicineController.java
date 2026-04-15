package com.pharmasystem.ui;

import com.pharmasystem.database.BatchDAO;
import com.pharmasystem.database.DBConnection;
import com.pharmasystem.util.AppValidator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddMedicineController {

    @FXML private TextField  nameField;
    @FXML private TextField  priceField;
    @FXML private CheckBox   prescriptionCheck;
    @FXML private TextField  quantityField;
    @FXML private TextField  expiryField;
    @FXML private DatePicker expiryPicker;
    @FXML private Label      errorLabel;

    @FXML
    private void handleSave() {
        clearError();

        String name         = nameField.getText()     == null ? "" : nameField.getText().trim();
        String priceText    = priceField.getText()    == null ? "" : priceField.getText().trim();
        String quantityText = quantityField.getText() == null ? "" : quantityField.getText().trim();
        String expiryText   = expiryField.getText()   == null ? "" : expiryField.getText().trim();

        double price;
        int quantity = 0;
        try {
            AppValidator.requireNonBlank(name, "Medicine name");
            AppValidator.requireNonBlank(priceText, "Price");
            price    = AppValidator.requireNonNegative(Double.parseDouble(priceText), "Price");
            if (!quantityText.isEmpty()) {
                quantity = AppValidator.requireNonNegative(Integer.parseInt(quantityText), "Initial quantity");
            }
        } catch (Exception e) {
            showError(e.getMessage());
            return;
        }

        LocalDate expiryDate = null;
        if (quantity > 0) {
            try {
                expiryDate = resolveExpiryDate(expiryText);
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
                return;
            }
        }

        try (Connection conn = DBConnection.connect()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            String sql = "INSERT INTO medicine(name, price, quantityInStock, expirationDate, requiresPrescription) VALUES (?, ?, 0, ?, ?)";

            try {
                conn.setAutoCommit(false);
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, name);
                    stmt.setDouble(2, price);
                    if (expiryDate != null) stmt.setString(3, expiryDate.toString());
                    else stmt.setNull(3, java.sql.Types.VARCHAR);
                    stmt.setInt(4, prescriptionCheck.isSelected() ? 1 : 0);
                    stmt.executeUpdate();

                    Integer medicineId = null;
                    try (var rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) medicineId = rs.getInt(1);
                    }

                    if (medicineId == null) {
                        conn.rollback();
                        showError("Medicine could not be created.");
                        return;
                    }

                    if (quantity > 0) {
                        new BatchDAO().addBatch(conn, medicineId, quantity, expiryDate, LocalDate.now());
                    }
                }
                conn.commit();
                close();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to save medicine. Please try again.");
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private LocalDate resolveExpiryDate(String expiryText) {
        LocalDate pickedDate = expiryPicker == null ? null : expiryPicker.getValue();
        LocalDate expiryDate;

        if (pickedDate != null) {
            expiryDate = pickedDate;
        } else if (!expiryText.isBlank()) {
            try {
                expiryDate = LocalDate.parse(expiryText);
            } catch (Exception e) {
                throw new IllegalArgumentException("Expiry date must use YYYY-MM-DD or the date picker.");
            }
        } else {
            throw new IllegalArgumentException("Expiry date is required when adding initial stock.");
        }

        if (expiryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiry date cannot be in the past.");
        }
        return expiryDate;
    }

    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg);
        else UIUtils.showAlert(Alert.AlertType.ERROR, "Invalid Input", msg);
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setText("");
    }

    private void close() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
