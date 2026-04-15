package com.pharmasystem.ui;

import com.pharmasystem.database.UserDAO;
import com.pharmasystem.model.User;
import com.pharmasystem.session.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // Clear error when user starts typing
        username.textProperty().addListener((obs, o, n) -> clearError());
        password.textProperty().addListener((obs, o, n) -> clearError());

        // Allow Enter key on password field to trigger login
        password.setOnAction(this::handleLogin);
        username.setOnAction(e -> password.requestFocus());
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        clearError();

        String enteredUsername = username.getText() == null ? "" : username.getText().trim();
        String enteredPassword = password.getText() == null ? "" : password.getText();

        // Client-side validation
        if (enteredUsername.isEmpty() && enteredPassword.isEmpty()) {
            showError("Please enter your username and password.");
            username.requestFocus();
            return;
        }
        if (enteredUsername.isEmpty()) {
            showError("Username is required.");
            username.requestFocus();
            return;
        }
        if (enteredPassword.isEmpty()) {
            showError("Password is required.");
            password.requestFocus();
            return;
        }

        // Disable button during auth to prevent double-clicks
        setLoading(true);

        User user;
        try {
            user = userDAO.login(enteredUsername, enteredPassword);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            setLoading(false);
            return;
        } catch (Exception e) {
            showError("An unexpected error occurred. Please try again.");
            e.printStackTrace();
            setLoading(false);
            return;
        }

        if (user == null) {
            showError("Incorrect username or password.");
            password.clear();
            password.requestFocus();
            setLoading(false);
            return;
        }

        Session.currentUser = user;

        try {
            UIUtils.switchScene(
                (Node) event.getSource(),
                "/com/pharmasystem/ui/Dashboard.fxml",
                "PhaRx — Dashboard",
                true
            );
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to open the dashboard. Please contact support.");
            setLoading(false);
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
    }

    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    private void setLoading(boolean loading) {
        if (loginButton != null) {
            loginButton.setDisable(loading);
            loginButton.setText(loading ? "Signing in..." : "Sign In");
        }
    }
}
