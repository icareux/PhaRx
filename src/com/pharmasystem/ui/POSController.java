package com.pharmasystem.ui;

import com.pharmasystem.database.BatchDAO;
import com.pharmasystem.database.MedicineDAO;
import com.pharmasystem.database.SaleDAO;
import com.pharmasystem.model.Manager;
import com.pharmasystem.model.Medicine;
import com.pharmasystem.model.Pharmacist;
import com.pharmasystem.session.Session;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class POSController {

    // Search
    @FXML private TextField          searchField;
    @FXML private VBox               searchDropdown;
    @FXML private ListView<String>   searchResultsList;

    // Selected medicine panel
    @FXML private VBox      selectedMedicineCard;
    @FXML private Label     selectedName;
    @FXML private Label     selectedStock;
    @FXML private Label     selectedPrice;
    @FXML private Label     prescriptionBadge;
    @FXML private TextField quantityField;

    // Cart
    @FXML private TableView<Map.Entry<Medicine, Integer>>           cartTable;
    @FXML private TableColumn<Map.Entry<Medicine, Integer>, String>  cartNameCol;
    @FXML private TableColumn<Map.Entry<Medicine, Integer>, Integer> cartQtyCol;
    @FXML private TableColumn<Map.Entry<Medicine, Integer>, Double>  cartTotalCol;
    @FXML private TableColumn<Map.Entry<Medicine, Integer>, Void>    cartActionCol;
    @FXML private Label totalLabel;
    @FXML private Label cartCountLabel;
    @FXML private AnchorPane searchAnchor;

    private final Map<Medicine, Integer>                        cart      = new HashMap<>();
    private final ObservableList<Map.Entry<Medicine, Integer>>  cartItems = FXCollections.observableArrayList();

    private List<Medicine> allMedicines;
    private Medicine       selectedMedicine;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final SaleDAO     saleDAO     = new SaleDAO();
    private final BatchDAO    batchDAO    = new BatchDAO();

    @FXML
    public void initialize() {
        allMedicines = medicineDAO.getAllMedicines();
        
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // FIX: ensure search field text is visible by forcing correct text colour via inline style.
        // The pharx.css search-field rule already sets -fx-text-fill: #0A1628 but a JavaFX quirk
        // on some platforms can ignore it when the field is inside a StackPane/AnchorPane.
        // Applying it directly guarantees visibility.
        searchField.setStyle(
            "-fx-text-fill: #0A1628; -fx-background-color: #FFFFFF; "
            + "-fx-background-radius: 8; -fx-border-color: #E2E8F0; "
            + "-fx-border-width: 1.5; -fx-border-radius: 8; "
            + "-fx-font-size: 13px; -fx-prompt-text-fill: #CBD5E1;");

        searchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                searchField.setStyle(
                    "-fx-text-fill: #0A1628; -fx-background-color: #FFFFFF; "
                    + "-fx-background-radius: 8; -fx-border-color: #2563EB; "
                    + "-fx-border-width: 1.5; -fx-border-radius: 8; "
                    + "-fx-font-size: 13px; -fx-prompt-text-fill: #CBD5E1; "
                    + "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.1), 6, 0, 0, 0);");
            } else {
                searchField.setStyle(
                    "-fx-text-fill: #0A1628; -fx-background-color: #FFFFFF; "
                    + "-fx-background-radius: 8; -fx-border-color: #E2E8F0; "
                    + "-fx-border-width: 1.5; -fx-border-radius: 8; "
                    + "-fx-font-size: 13px; -fx-prompt-text-fill: #CBD5E1;");
            }
        });

        setupCartTable();
        searchDropdown.setPickOnBounds(false);

        searchResultsList.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    handleSearchSelection();
                }
            });

    }

    // ── Search ───────────────────────────────────────────────

    @FXML
    private void onSearchKeyReleased(KeyEvent event) {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        if (query.isEmpty()) {
            hideDropdown();
            return;
        }

        List<Medicine> results = allMedicines.stream()
            .filter(m -> m.getName().toLowerCase().contains(query) && !m.isExpired())
            .limit(10)
            .collect(Collectors.toList());

        if (results.isEmpty()) {
            hideDropdown();
            return;
        }

        ObservableList<String> displayItems = FXCollections.observableArrayList(
            results.stream()
                .map(m -> m.getName() + "  —  ₱" + String.format("%.2f", m.getPrice())
                    + "  (Stock: " + batchDAO.getTotalStock(m.getMedicineId()) + ")")
                .collect(Collectors.toList())
        );

        searchResultsList.setItems(displayItems);
        searchResultsList.setUserData(results);
        showDropdown();
    }
    
    private void handleSearchSelection() {
        int idx = searchResultsList.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;

        @SuppressWarnings("unchecked")
        List<Medicine> results =
            (List<Medicine>) searchResultsList.getUserData();

        if (results == null || idx >= results.size()) return;

        selectMedicine(results.get(idx));
        hideDropdown();
        searchField.clear();
    }
    private void selectMedicine(Medicine m) {
        selectedMedicine = m;

        int stock = batchDAO.getTotalStock(m.getMedicineId());
        selectedName.setText(m.getName());
        selectedStock.setText("Available stock: " + stock + " units");
        selectedPrice.setText("₱" + String.format("%.2f", m.getPrice()));

        prescriptionBadge.setVisible(m.requiresPrescription());
        prescriptionBadge.setManaged(m.requiresPrescription());

        selectedMedicineCard.setVisible(true);
        selectedMedicineCard.setManaged(true);
        quantityField.requestFocus();
    }

    
    private void showDropdown() {
        searchAnchor.setPrefHeight(300); // temporarily expand bounds
        searchDropdown.setVisible(true);
        searchDropdown.setManaged(true);
    }

    private void hideDropdown() {
        searchDropdown.setVisible(false);
        searchDropdown.setManaged(false);
        searchAnchor.setPrefHeight(42); // collapse back
    }


    // ── Cart ─────────────────────────────────────────────────

    private void setupCartTable() {
        cartNameCol.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getKey().getName()));

        cartQtyCol.setCellValueFactory(d ->
            new SimpleIntegerProperty(d.getValue().getValue()).asObject());

        cartTotalCol.setCellValueFactory(d -> {
            double t = d.getValue().getKey().getPrice() * d.getValue().getValue();
            return new SimpleDoubleProperty(t).asObject();
        });
        cartTotalCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : "₱" + String.format("%.2f", v));
            }
        });

        // FIX: action column – give buttons explicit min sizes so they are never hidden
        cartActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button plus   = new Button("+");
            private final Button minus  = new Button("−");
            private final Button remove = new Button("✕");
            {
                plus.getStyleClass().add("btn-icon");
                minus.getStyleClass().add("btn-icon");
                remove.getStyleClass().add("btn-remove");

                // Explicit min sizes ensure buttons are always fully visible
                plus.setMinWidth(28);   plus.setMinHeight(28);
                minus.setMinWidth(28);  minus.setMinHeight(28);
                remove.setMinWidth(28); remove.setMinHeight(28);

                plus.setOnAction(e -> {
                    var entry = getTableView().getItems().get(getIndex());
                    int next  = entry.getValue() + 1;
                    int stock = batchDAO.getTotalStock(entry.getKey().getMedicineId());
                    if (next > stock) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Stock Limit",
                            "Only " + stock + " units available.");
                        return;
                    }
                    cart.put(entry.getKey(), next);
                    refreshCart();
                });
                minus.setOnAction(e -> {
                    var entry = getTableView().getItems().get(getIndex());
                    int qty = entry.getValue() - 1;
                    if (qty <= 0) cart.remove(entry.getKey());
                    else cart.put(entry.getKey(), qty);
                    refreshCart();
                });
                remove.setOnAction(e -> {
                    var entry = getTableView().getItems().get(getIndex());
                    cart.remove(entry.getKey());
                    refreshCart();
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(4, plus, minus, remove);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        cartTable.setItems(cartItems);
    }

    @FXML
    private void addToCart() {
        if (selectedMedicine == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "No Selection", "Search and select a medicine first.");
            return;
        }

        String qtyText = quantityField.getText().trim();
        if (qtyText.isEmpty()) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Quantity Required", "Please enter a quantity.");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyText);
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Invalid Quantity", "Enter a positive whole number.");
            return;
        }

        if (selectedMedicine.isExpired()) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Expired", "This medicine cannot be sold.");
            return;
        }

        int existing   = cart.getOrDefault(selectedMedicine, 0);
        int newQty     = existing + quantity;
        int totalStock = batchDAO.getTotalStock(selectedMedicine.getMedicineId());

        if (newQty > totalStock) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Stock Limit",
                "Only " + totalStock + " unexpired units available.");
            return;
        }

        cart.put(selectedMedicine, newQty);
        quantityField.clear();
        selectedMedicineCard.setVisible(false);
        selectedMedicineCard.setManaged(false);
        selectedMedicine = null;
        refreshCart();
    }

    private void refreshCart() {
        cartItems.setAll(cart.entrySet());
        double total = cart.entrySet().stream()
            .mapToDouble(e -> e.getKey().getPrice() * e.getValue()).sum();
        totalLabel.setText("₱" + String.format("%.2f", total));
        cartCountLabel.setText(cart.values().stream().mapToInt(i -> i).sum() + " items");
    }

    // ── Sale ─────────────────────────────────────────────────

    /**
     * FIX (permissions): Both Pharmacists and Managers can process sales, not just Cashiers.
     * The original code only checked for Cashier which locked out Pharmacists/Managers entirely.
     */
    @FXML
    private void processSale() {
        if (cart.isEmpty()) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Empty Cart", "Add at least one medicine.");
            return;
        }

        // Allow Cashier, Pharmacist, and Manager to process sales
        boolean canSell = Session.currentUser != null
            && !(Session.currentUser.getClass().getSimpleName().isEmpty()); // all logged-in roles
        // Restrict only if truly no user is logged in (defensive check)
        if (Session.currentUser == null) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Access Denied", "You must be logged in to process sales.");
            return;
        }

        // Stock validation
        for (Map.Entry<Medicine, Integer> entry : cart.entrySet()) {
            int stock = batchDAO.getTotalStock(entry.getKey().getMedicineId());
            if (stock < entry.getValue()) {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Insufficient Stock",
                    "Not enough stock for " + entry.getKey().getName() + ".");
                return;
            }
        }

        // Prescription check – requires pharmacist approval unless the current user IS a pharmacist/manager
        boolean needsApproval = cart.keySet().stream().anyMatch(Medicine::requiresPrescription);
        boolean currentUserCanApprove = Session.currentUser instanceof Pharmacist
            || Session.currentUser instanceof Manager;

        if (needsApproval && !currentUserCanApprove) {
            if (!requestPharmacistApproval()) {
                UIUtils.showAlert(Alert.AlertType.WARNING, "Approval Required",
                    "Sale cancelled — pharmacist approval not granted.");
                return;
            }
        }

        openReceiptPopup();
    }

    private void openReceiptPopup() {
        try {
            FXMLLoader loader = UIUtils.createLoader("/com/pharmasystem/ui/Receipt.fxml");
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Payment");
            stage.initOwner(cartTable.getScene().getWindow());
            stage.setResizable(false);

            ReceiptController rc = loader.getController();
            rc.setCartData(cart);
            
            UIUtils.applyAppIcon(stage);
            
            stage.showAndWait();

            if (rc.isPaymentConfirmed()) {
                String saleId = UUID.randomUUID().toString();
                boolean ok = saleDAO.saveSale(saleId, cart);
                if (!ok) {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Sale Failed", "Unable to save the sale.");
                    return;
                }
                cart.clear();
                allMedicines = medicineDAO.getAllMedicines();
                refreshCart();
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Sale Complete", "Sale saved successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Unable to open receipt.");
        }
    }

    private boolean requestPharmacistApproval() {
        try {
            FXMLLoader loader = UIUtils.createLoader("/com/pharmasystem/ui/PharmacistApproval.fxml");
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Pharmacist Approval");
            stage.initOwner(cartTable.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
            PharmacistApprovalController ctrl = loader.getController();
            return ctrl.isApproved();
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Unable to open approval window.");
            return false;
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        try {
            UIUtils.switchScene((Node) event.getSource(),
                "/com/pharmasystem/ui/Dashboard.fxml", "PhaRx — Dashboard", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
