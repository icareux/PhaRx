package com.pharmasystem.database;

import com.pharmasystem.model.Medicine;
import com.pharmasystem.util.AppValidator;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MedicineDAO {
    private static final Logger LOGGER =
        Logger.getLogger(MedicineDAO.class.getName());
    
    public boolean deleteMedicine(int medicineId) {
        AppValidator.requirePositive(medicineId, "Medicine ID");

        String deleteBatches  = "DELETE FROM batches WHERE medicine_id = ?";
        String deleteMedicine = "DELETE FROM medicine WHERE medicine_id = ?";

        try (Connection conn = DBConnection.connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(deleteBatches);
                 PreparedStatement ps2 = conn.prepareStatement(deleteMedicine)) {

                ps1.setInt(1, medicineId);
                ps1.executeUpdate(); // delete child records first

                ps2.setInt(1, medicineId);
                int affected = ps2.executeUpdate();

                conn.commit();
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                "Failed to delete medicine and its batches: " + medicineId, e);
            return false;
        }
    }

    public List<Medicine> getAllMedicines() {
        List<Medicine> list = new ArrayList<>();
        String sql = "SELECT * FROM medicine";
        BatchDAO batchDAO = new BatchDAO();

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                try {
                    int medicineId = rs.getInt("medicine_id");
                    int totalStock = batchDAO.getTotalStock(medicineId);
                    String expirationText = rs.getString("expirationDate");
                    java.time.LocalDate expirationDate = null;

                    if (expirationText != null && !expirationText.isBlank()) {
                        expirationDate = java.time.LocalDate.parse(expirationText);
                    }

                    Medicine m = new Medicine(
                            medicineId,
                            rs.getString("name"),
                            "",
                            rs.getDouble("price"),
                            totalStock,
                            expirationDate,
                            0,
                            rs.getInt("requiresPrescription") == 1
                    );
                    list.add(m);
                } catch (Exception rowError) {
                    LOGGER.log(Level.SEVERE, "Failed to map medicine row", rowError);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve medicines", e);
        }

        return list;
    }

    public boolean existsById(int medicineId) {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        try (Connection conn = DBConnection.connect()) {
            return existsById(conn, medicineId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                        "Failed to check existence of medicine ID: " + medicineId, e);
            return false;
        }
    }

    public boolean existsById(Connection conn, int medicineId) throws SQLException {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        String sql = "SELECT 1 FROM medicine WHERE medicine_id=?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, medicineId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean updatePrice(int medicineId, double newPrice) {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        AppValidator.requireNonNegative(newPrice, "Price");
        String sql = "UPDATE medicine SET price=? WHERE medicine_id=?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newPrice);
            stmt.setInt(2, medicineId);
            return stmt.executeUpdate() == 1;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to update price for medicine ID: " + medicineId, e);
            return false;
        }
    }

    public List<Medicine> getLowStockMedicines(int threshold) {
        AppValidator.requirePositive(threshold, "Threshold");
        List<Medicine> medicines = new ArrayList<>();

        for (Medicine medicine : getAllMedicines()) {
            if (medicine.getQuantityInStock() <= threshold) {
                medicines.add(medicine);
            }
        }

        return medicines;
    }
}
