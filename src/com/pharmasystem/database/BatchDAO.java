package com.pharmasystem.database;

import com.pharmasystem.model.Batch;
import com.pharmasystem.model.Medicine;
import com.pharmasystem.util.AppValidator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;



public class BatchDAO {
    private static final Logger LOGGER = Logger.getLogger(BatchDAO.class.getName());
    
    public int removeExpiredBatches() {
        String sql = "DELETE FROM batches WHERE expiry_date < CURRENT_DATE";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            return ps.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to remove expired batches", e);
            return 0;
        }
    }
    
    public List<Batch> getBatchesByMedicine(int medicineId) {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        try (Connection conn = DBConnection.connect()) {
            return getBatchesByMedicine(conn, medicineId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get batches for medicine ID: " + medicineId, e);
            return new ArrayList<>();
        }
    }

    public List<Batch> getBatchesByMedicine(Connection conn, int medicineId) throws SQLException {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        List<Batch> list = new ArrayList<>();
        String sql = "SELECT * FROM batches "
                + "WHERE medicine_id=? AND quantity > 0 AND expiry_date >= date('now') "
                + "ORDER BY expiry_date ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, medicineId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Batch(
                            rs.getInt("batch_id"),
                            rs.getInt("medicine_id"),
                            rs.getInt("quantity"),
                            LocalDate.parse(rs.getString("expiry_date")),
                            LocalDate.parse(rs.getString("purchase_date"))
                    ));
                }
            }
        }

        return list;
    }

    public boolean deductStockFIFO(int medicineId, int qtyNeeded) {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        AppValidator.requirePositive(qtyNeeded, "Quantity");
        try (Connection conn = DBConnection.connect()) {
            boolean originalAutoCommit = conn.getAutoCommit();

            try {
                conn.setAutoCommit(false);
                boolean success = deductStockFIFO(conn, medicineId, qtyNeeded);

                if (success) {
                    conn.commit();
                } else {
                    conn.rollback();
                }

                return success;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed during FIFO stock deduction", e);
            return false;
        }
    }

    public boolean deductStockFIFO(Connection conn, int medicineId, int qtyNeeded) throws SQLException {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        AppValidator.requirePositive(qtyNeeded, "Quantity");
        List<Batch> batches = getBatchesByMedicine(conn, medicineId);

        for (Batch batch : batches) {
            if (qtyNeeded <= 0) {
                break;
            }

            int available = batch.getQuantity();

            if (available <= qtyNeeded) {
                qtyNeeded -= available;
                deleteBatch(conn, batch.getBatchId());
            } else {
                updateBatchQty(conn, batch.getBatchId(), available - qtyNeeded);
                qtyNeeded = 0;
            }
        }

        return qtyNeeded == 0;
    }

    public void addBatch(int medicineId, int quantity, LocalDate expiryDate, LocalDate purchaseDate) {
        try (Connection conn = DBConnection.connect()) {
            addBatch(conn, medicineId, quantity, expiryDate, purchaseDate);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to add batch", e);
        }
    }

    public void addBatch(Connection conn, int medicineId, int quantity, LocalDate expiryDate, LocalDate purchaseDate) throws SQLException {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        AppValidator.requirePositive(quantity, "Quantity");
        purchaseDate = purchaseDate == null ? LocalDate.now() : purchaseDate;
        AppValidator.requireExpiryAfterPurchase(purchaseDate, expiryDate);

        if (!new MedicineDAO().existsById(conn, medicineId)) {
            throw new IllegalArgumentException("Medicine does not exist.");
        }

        String sql = "INSERT INTO batches (medicine_id, quantity, expiry_date, purchase_date) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, medicineId);
            ps.setInt(2, quantity);
            ps.setString(3, expiryDate.toString());
            ps.setString(4, purchaseDate.toString());
            ps.executeUpdate();
        }
    }

    public int getTotalStockByMedicineId(int medicineId) {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM batches "
                + "WHERE medicine_id = ? AND quantity > 0 AND expiry_date >= date('now')";

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, medicineId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {    
LOGGER.log(Level.SEVERE,
        "Failed to get total stock for medicine ID: " + medicineId, e);
        }

        return 0;
    }

    public int getTotalStock(int medicineId) {
        return getTotalStockByMedicineId(medicineId);
    }

    public void updateStock(int medicineId, int quantity, LocalDate expiryDate, LocalDate purchaseDate) {
        AppValidator.requirePositive(medicineId, "Medicine ID");
        if (quantity > 0) {
            addBatch(medicineId, quantity, expiryDate, purchaseDate != null ? purchaseDate : LocalDate.now());
        } else if (quantity < 0) {
            boolean success = deductStockFIFO(medicineId, Math.abs(quantity));

            if (!success) {
                throw new RuntimeException("Not enough stock!");
            }
        }
    }

    public List<Batch> getBatchesExpiringWithin(int days) {
        AppValidator.requirePositive(days, "Days");
        List<Batch> list = new ArrayList<>();
        String sql = "SELECT * FROM batches "
                + "WHERE quantity > 0 AND expiry_date >= date('now') "
                + "AND expiry_date <= date('now', '+' || ? || ' day') "
                + "ORDER BY expiry_date ASC";

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Batch(
                            rs.getInt("batch_id"),
                            rs.getInt("medicine_id"),
                            rs.getInt("quantity"),
                            LocalDate.parse(rs.getString("expiry_date")),
                            LocalDate.parse(rs.getString("purchase_date"))
                    ));
                }
            }
        } catch (Exception e) {
LOGGER.log(Level.SEVERE,
        "Failed to retrieve batches expiring within " + days + " days", e);
        }

        return list;
    }

    public Map<Medicine, List<Batch>> getExpiringBatchNotifications(int days) {
        Map<Medicine, List<Batch>> notifications = new LinkedHashMap<>();
        MedicineDAO medicineDAO = new MedicineDAO();
        Map<Integer, Medicine> medicineMap = new LinkedHashMap<>();

        for (Medicine medicine : medicineDAO.getAllMedicines()) {
            medicineMap.put(medicine.getMedicineId(), medicine);
        }

        for (Batch batch : getBatchesExpiringWithin(days)) {
            Medicine medicine = medicineMap.get(batch.getMedicineId());
            if (medicine == null) {
                continue;
            }
            notifications.computeIfAbsent(medicine, key -> new ArrayList<>()).add(batch);
        }

        return notifications;
    }

    private void updateBatchQty(Connection conn, int batchId, int newQty) throws SQLException {
        String sql = "UPDATE batches SET quantity=? WHERE batch_id=?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newQty);
            stmt.setInt(2, batchId);
            stmt.executeUpdate();
        }
    }

    private void deleteBatch(Connection conn, int batchId) throws SQLException {
        String sql = "DELETE FROM batches WHERE batch_id=?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, batchId);
            stmt.executeUpdate();
        }
    }
}
