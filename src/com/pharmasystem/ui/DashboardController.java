package com.pharmasystem.ui;

import com.pharmasystem.database.BatchDAO;
import com.pharmasystem.database.MedicineDAO;
import com.pharmasystem.model.Batch;
import com.pharmasystem.model.Medicine;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import com.pharmasystem.session.Session;
import com.pharmasystem.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class DashboardController {
    
    @FXML private HBox root;

    @FXML private ListView<String> notificationList;
    @FXML private Label welcomeLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label userAvatarLabel;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final BatchDAO    batchDAO    = new BatchDAO();

    @FXML
    public void initialize() {
        User user = Session.currentUser;

        if (user == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Session Expired", "Please log in again.");
            return;
        }

        String name = user.getUsername();
        welcomeLabel.setText(name);

        // Avatar initials (first letter of username)
        if (userAvatarLabel != null && name != null && !name.isEmpty()) {
            userAvatarLabel.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        if (user instanceof Cashier) {
            if (userRoleLabel != null) userRoleLabel.setText("Cashier");
        } else if (user instanceof Pharmacist) {
            if (userRoleLabel != null) userRoleLabel.setText("Pharmacist");
        } else if (user instanceof Manager) {
            if (userRoleLabel != null) userRoleLabel.setText("Manager");
        }

        loadNotifications();
        
        Platform.runLater(() -> {
                Stage stage = (Stage) root.getScene().getWindow();
                stage.setMaximized(true);
            });

    }

    @FXML
    private void openPOS(ActionEvent event) {
        try {
            UIUtils.switchScene((Node) event.getSource(),
                "/com/pharmasystem/ui/POS.fxml", "PhaRx — POS", true);
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open POS.");
        }
    }

    @FXML
    private void openInventory(ActionEvent event) {
        try {
            UIUtils.switchScene((Node) event.getSource(),
                "/com/pharmasystem/ui/Inventory.fxml", "PhaRx — Inventory", true);
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open Inventory.");
        }
    }

    @FXML
    private void openRecords(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = UIUtils.createLoader("/com/pharmasystem/ui/Records.fxml");
            javafx.stage.Stage stage = UIUtils.openPopup(
                ((Node) event.getSource()).getScene().getWindow(),
                "Sales Records", loader.load(), true);
            stage.showAndWait();
            loadNotifications();
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Open Failed", "Unable to open sales records.");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Session.currentUser = null;
        try {
            UIUtils.switchScene((Node) event.getSource(),
                "/com/pharmasystem/ui/Login.fxml", "PhaRx", true);
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to return to login.");
        }
    }

    private void loadNotifications() {
        if (notificationList == null) return;

        List<String> notifications = new ArrayList<>();

        for (Medicine medicine : medicineDAO.getLowStockMedicines(10)) {
            notifications.add("⚠  Low stock: " + medicine.getName()
                + " — only " + medicine.getQuantityInStock() + " units left.");
        }

        for (Map.Entry<Medicine, List<Batch>> entry : batchDAO.getExpiringBatchNotifications(30).entrySet()) {
            Medicine medicine = entry.getKey();
            for (Batch batch : entry.getValue()) {
                notifications.add("📅  Expiring: " + medicine.getName()
                    + " batch #" + batch.getBatchId()
                    + " on " + batch.getExpiryDate() + ".");
            }
        }

        if (notifications.isEmpty()) {
            notifications.add("✓  No urgent notifications.");
        }

        notificationList.getItems().setAll(notifications);
    }
}
