package com.pharmasystem.ui;

import com.pharmasystem.database.UserDAO;
import com.pharmasystem.model.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PharmacistApprovalController {

    @FXML private TextField     username;
    @FXML private PasswordField password;
    @FXML private Label         errorLabel;

    private boolean approved = false;

    public boolean isApproved() { return approved; }

    @FXML
    private void handleApprove() {
        clearError();
        String u = username.getText() == null ? "" : username.getText().trim();
        String p = password.getText() == null ? "" : password.getText();

        if (u.isEmpty() || p.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        UserDAO userDAO = new UserDAO();
        User user;
        try {
            user = userDAO.login(u, p);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        if (user instanceof Pharmacist) {
            approved = true;
            closeWindow();
        } else {
            password.clear();
            showError("Invalid pharmacist credentials.");
        }
    }

    @FXML
    private void handleCancel() {
        approved = false;
        closeWindow();
    }

    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg);
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setText("");
    }

    private void closeWindow() {
        Stage stage = (Stage) username.getScene().getWindow();
        stage.close();
    }
}
