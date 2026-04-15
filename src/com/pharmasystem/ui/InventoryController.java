package com.pharmasystem.ui;

import javafx.scene.input.KeyEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.pharmasystem.database.MedicineDAO;
import com.pharmasystem.database.BatchDAO;
import com.pharmasystem.session.Session;
import com.pharmasystem.model.*;

import javafx.event.ActionEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryController {

    @FXML private TableView<Medicine>              table;
    @FXML private TableColumn<Medicine, String>    nameCol;
    @FXML private TableColumn<Medicine, Integer>   stockCol;
    @FXML private TableColumn<Medicine, Double>    priceCol;
    @FXML private TableColumn<Medicine, String>    expiryCol;   // NEW – was missing cell factory
    @FXML private TableColumn<Medicine, String>    rxCol;       // NEW – was missing cell factory

    @FXML private Button    updateBtn;
    @FXML private Button    addMedicineBtn;
    @FXML private Button    adjustPriceBtn;
    @FXML private Button    deleteMedicineBtn;    // NEW
    @FXML private Button    removeExpiredBtn;     // NEW
    @FXML private TextField inventorySearch;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final BatchDAO    batchDAO    = new BatchDAO();

    @FXML
    public void initialize() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // ── Column cell factories ──────────────────────────────────
        nameCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));

        stockCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(
                batchDAO.getTotalStockByMedicineId(data.getValue().getMedicineId())
            ).asObject());

        priceCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleDoubleProperty(
                data.getValue().getPrice()
            ).asObject());
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("₱%.2f", v));
            }
        });

        // Expiry date – shows earliest expiry across all batches for quick overview
        expiryCol.setCellValueFactory(data -> {
            List<Batch> batches = batchDAO.getBatchesByMedicine(data.getValue().getMedicineId());
            if (batches == null || batches.isEmpty()) {
                return new javafx.beans.property.SimpleStringProperty("—");
            }
            // Find the soonest expiry date that is not null
            String earliest = batches.stream()
                .filter(b -> b.getExpiryDate() != null)
                .map(b -> b.getExpiryDate().toString())
                .sorted()
                .findFirst()
                .orElse("—");
            return new javafx.beans.property.SimpleStringProperty(earliest);
        });
        expiryCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                // Highlight expired dates in red
                try {
                    if (!"—".equals(v) && LocalDate.parse(v).isBefore(LocalDate.now())) {
                        setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                } catch (Exception e) {
                    setStyle("");
                }
            }
        });

        // Prescription requirement
        rxCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().requiresPrescription() ? "Yes" : "No"
            ));
        rxCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                if ("Yes".equals(v)) {
                    setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #64748B;");
                }
            }
        });

        // ── Role-based button visibility ───────────────────────────
        boolean isManager = Session.currentUser instanceof Manager;
        updateBtn.setDisable(!isManager);
        addMedicineBtn.setDisable(!isManager);
        adjustPriceBtn.setDisable(!isManager);
        if (deleteMedicineBtn != null) deleteMedicineBtn.setDisable(!isManager);
        if (removeExpiredBtn  != null) removeExpiredBtn.setDisable(!isManager);

        loadData();
    }

    // ── Data loading ───────────────────────────────────────────────

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(medicineDAO.getAllMedicines()));
    }

    @FXML
    private void onInventorySearch(KeyEvent event) {
        String keyword = inventorySearch.getText();
        if (keyword == null || keyword.trim().isEmpty()) {
            loadData();
            return;
        }
        String lower = keyword.toLowerCase();
        table.setItems(FXCollections.observableArrayList(
            medicineDAO.getAllMedicines().stream()
                .filter(m -> m.getName().toLowerCase().contains(lower))
                .toList()
        ));
    }

    // ── Stock management ───────────────────────────────────────────

    @FXML
    private void handleAddButton() {
        if (!(Session.currentUser instanceof Manager)) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Access Denied", "Only managers can manage stock.");
            return;
        }
        Medicine selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "No Selection", "Select a medicine first.");
            return;
        }
        openAddStockPopup(selected);
    }

    private void openAddStockPopup(Medicine selected) {
        Stage popup = new Stage();
        popup.setTitle("Update Stock – " + selected.getName());

        TextField qtyField = new TextField();
        qtyField.setPromptText("Quantity (+ add / − reduce)");

        DatePicker expiryPicker = new DatePicker();
        expiryPicker.setPromptText("Expiry Date (required for new stock)");

        Button confirm = new Button("Confirm");

        confirm.setOnAction(e -> {
            int qty;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
            } catch (Exception ex) {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Invalid", "Quantity must be a number.");
                return;
            }
            if (qty == 0) {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Invalid", "Quantity must not be zero.");
                return;
            }
            if (qty > 0 && expiryPicker.getValue() == null) {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Missing Data", "Expiry date required for new stock.");
                return;
            }
            try {
                batchDAO.updateStock(selected.getMedicineId(), qty, expiryPicker.getValue(), LocalDate.now());
                popup.close();
                loadData();
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showAlert(Alert.AlertType.ERROR, "Error",
                    ex.getMessage() == null ? "Stock update failed." : ex.getMessage());
            }
        });

        VBox layout = new VBox(10, qtyField, expiryPicker, confirm);
        layout.setPadding(new javafx.geometry.Insets(14));

        popup.setScene(new javafx.scene.Scene(layout, 320, 200));
        popup.initOwner(table.getScene().getWindow());
        popup.showAndWait();
    }

    // ── Add / delete medicine ──────────────────────────────────────

    @FXML
    private void openAddMedicine() {
        if (!(Session.currentUser instanceof Manager)) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Access Denied", "Only managers can add medicines.");
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = UIUtils.createLoader("/com/pharmasystem/ui/AddMedicine.fxml");
            Stage stage = new Stage();
            stage.setScene(new javafx.scene.Scene(loader.load()));
            stage.setTitle("Add Medicine");
            stage.initOwner(table.getScene().getWindow());
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Open Failed", "Unable to open Add Medicine.");
        }
    }

    /** Delete a medicine and all its batches – manager only. */
    @FXML
    private void handleDeleteMedicine() {
        if (!(Session.currentUser instanceof Manager)) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Access Denied", "Only managers can delete medicines.");
            return;
        }
        Medicine selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "No Selection", "Select a medicine to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete \"" + selected.getName() + "\"?");
        confirm.setContentText(
            "This will permanently remove the medicine and ALL its batches from inventory.\n\nThis action cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted = medicineDAO.deleteMedicine(selected.getMedicineId());
                if (deleted) {
                    loadData();
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Deleted",
                        "\"" + selected.getName() + "\" has been removed.");
                } else {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Delete Failed",
                        "Unable to delete the medicine. Please try again.");
                }
            }
        });
    }

    // ── Expired medicine management ────────────────────────────────

    /** Show expired medicines and let the manager confirm removal. */
    @FXML
    private void handleRemoveExpired() {
        if (!(Session.currentUser instanceof Manager)) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Access Denied", "Only managers can remove expired medicines.");
            return;
        }

        LocalDate today = LocalDate.now();
        List<Medicine> allMeds = medicineDAO.getAllMedicines();

        // Collect medicines that have at least one expired batch
        List<Medicine> expired = allMeds.stream()
            .filter(m -> {
                List<Batch> batches = batchDAO.getBatchesByMedicine(m.getMedicineId());
                return batches != null && batches.stream()
                    .anyMatch(b -> b.getExpiryDate() != null && b.getExpiryDate().isBefore(today));
            })
            .collect(Collectors.toList());

        if (expired.isEmpty()) {
            UIUtils.showAlert(Alert.AlertType.INFORMATION, "No Expired Medicines",
                "There are no expired medicines in inventory.");
            return;
        }

        // Build a summary for the confirmation dialog
        StringBuilder sb = new StringBuilder("The following medicines have expired batches:\n\n");
        for (Medicine m : expired) {
            sb.append("  •  ").append(m.getName()).append("\n");
        }
        sb.append("\nConfirm to remove all expired batches.\n(Medicines with no remaining stock will remain listed.)");

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Expired Batches");
        confirm.setHeaderText("Remove expired inventory?");
        confirm.setContentText(sb.toString());
        confirm.getDialogPane().setPrefWidth(460);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int removed = batchDAO.removeExpiredBatches();
                loadData();
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Done",
                    removed + " expired batch(es) removed from inventory.");
            }
        });
    }

    // ── Price adjustment ───────────────────────────────────────────

    @FXML
    private void openAdjustPrices() {
        if (!(Session.currentUser instanceof Manager)) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Access Denied", "Only managers can adjust prices.");
            return;
        }
        Medicine selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "No Selection", "Select a medicine first.");
            return;
        }

        Stage popup = new Stage();
        popup.setTitle("Adjust Price");

        Label nameLabel         = new Label("Medicine: " + selected.getName());
        Label currentPriceLabel = new Label(String.format("Current Price: ₱%.2f", selected.getPrice()));
        TextField priceField    = new TextField();
        priceField.setPromptText("New Price");
        Button saveButton = new Button("Save Price");

        saveButton.setOnAction(e -> {
            try {
                double newPrice = Double.parseDouble(priceField.getText().trim());
                if (newPrice < 0) {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Invalid Price", "Price cannot be negative.");
                    return;
                }
                boolean updated = medicineDAO.updatePrice(selected.getMedicineId(), newPrice);
                if (!updated) {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Update Failed", "Unable to update the price.");
                    return;
                }
                popup.close();
                loadData();
            } catch (NumberFormatException ex) {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Invalid Price", "Enter a valid numeric price.");
            }
        });

        VBox layout = new VBox(10, nameLabel, currentPriceLabel, priceField, saveButton);
        layout.setPadding(new javafx.geometry.Insets(14));

        popup.setScene(new javafx.scene.Scene(layout, 320, 190));
        popup.initOwner(table.getScene().getWindow());
        popup.showAndWait();
    }

    // ── Batch viewer ───────────────────────────────────────────────

    @FXML
    private void openViewBatches() {
        Medicine selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "No Selection", "Select a medicine first.");
            return;
        }

        TableView<Batch> batchTable = new TableView<>();
        TableColumn<Batch, Integer> batchIdCol  = new TableColumn<>("Batch ID");
        TableColumn<Batch, Integer> qtyCol      = new TableColumn<>("Quantity");
        TableColumn<Batch, String>  expiryCol   = new TableColumn<>("Expiry Date");
        TableColumn<Batch, String>  purchaseCol = new TableColumn<>("Purchase Date");

        batchIdCol.setCellValueFactory(data  -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getBatchId()).asObject());
        qtyCol.setCellValueFactory(data      -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        expiryCol.setCellValueFactory(data   -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getExpiryDate() != null ? data.getValue().getExpiryDate().toString() : "—"));
        purchaseCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getPurchaseDate() != null ? data.getValue().getPurchaseDate().toString() : "—"));

        // Highlight expired rows
        expiryCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                try {
                    if (!"—".equals(v) && LocalDate.parse(v).isBefore(LocalDate.now())) {
                        setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                    } else { setStyle(""); }
                } catch (Exception e) { setStyle(""); }
            }
        });

        batchTable.getColumns().addAll(batchIdCol, qtyCol, expiryCol, purchaseCol);
        batchTable.setItems(FXCollections.observableArrayList(
            batchDAO.getBatchesByMedicine(selected.getMedicineId())));

        Label title = new Label("Batches for " + selected.getName());
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());

        HBox actions = new HBox(closeButton);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox layout = new VBox(10, title, batchTable, actions);
        layout.setPadding(new javafx.geometry.Insets(12));

        Stage popup = new Stage();
        popup.setTitle("View Batches – " + selected.getName());
        popup.setScene(new javafx.scene.Scene(layout, 580, 380));
        popup.initOwner(table.getScene().getWindow());
        popup.showAndWait();
    }

    // ── Navigation ─────────────────────────────────────────────────

    @FXML
    private void goBack(ActionEvent event) {
        try {
            UIUtils.switchScene((Node) event.getSource(),
                "/com/pharmasystem/ui/Dashboard.fxml", "PhaRx", true);
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to return to the dashboard.");
        }
    }
}
