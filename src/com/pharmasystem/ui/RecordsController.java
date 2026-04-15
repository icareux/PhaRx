package com.pharmasystem.ui;

import com.pharmasystem.database.SaleDAO;
import com.pharmasystem.model.SaleRecord;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class RecordsController {

    @FXML private TableView<SaleRecord> recordsTable;
    @FXML private TableColumn<SaleRecord, String> saleIdCol;
    @FXML private TableColumn<SaleRecord, String> saleDateCol;
    @FXML private TableColumn<SaleRecord, Double> totalAmountCol;
    @FXML private ChoiceBox<String> scopeChoice;
    @FXML private ChoiceBox<String> sortChoice;

    private final SaleDAO saleDAO = new SaleDAO();

    @FXML
    public void initialize() {
        saleIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSaleId()));
        saleDateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSaleDate().toString()));
        totalAmountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getTotalAmount()).asObject());

        scopeChoice.setItems(FXCollections.observableArrayList("Today's Sales", "All Sales"));
        sortChoice.setItems(FXCollections.observableArrayList("Newest First", "Oldest First"));
        scopeChoice.setValue("Today's Sales");
        sortChoice.setValue("Newest First");

        scopeChoice.setOnAction(e -> loadRecords());
        sortChoice.setOnAction(e -> loadRecords());

        loadRecords();
    }

    @FXML
    private void refreshRecords() {
        loadRecords();
    }

    private void loadRecords() {
        boolean todayOnly = !"All Sales".equals(scopeChoice.getValue());
        boolean newestFirst = !"Oldest First".equals(sortChoice.getValue());
        recordsTable.setItems(FXCollections.observableArrayList(
                saleDAO.getSaleRecords(todayOnly, newestFirst)
        ));
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) recordsTable.getScene().getWindow();
        stage.close();
    }
}
