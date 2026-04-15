package com.pharmasystem.database;

import com.pharmasystem.model.Medicine;
import com.pharmasystem.model.SaleRecord;
import com.pharmasystem.util.AppValidator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SaleDAO {

    public boolean saveSale(String saleId, Map<Medicine, Integer> items) {
        saleId = AppValidator.requireNonBlank(saleId, "Sale ID");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Sale must include at least one item.");
        }

        String saleSQL = "INSERT INTO sale VALUES (?, ?, ?)";
        String itemSQL = "INSERT INTO sale_item(saleId, medicine_id, quantity, subtotal) VALUES (?,?,?,?)";
        BatchDAO batchDAO = new BatchDAO();

        try (Connection conn = DBConnection.connect();
             PreparedStatement saleStmt = conn.prepareStatement(saleSQL);
             PreparedStatement itemStmt = conn.prepareStatement(itemSQL)) {

            boolean originalAutoCommit = conn.getAutoCommit();

            try {
                conn.setAutoCommit(false);

                double total = 0;

                for (Map.Entry<Medicine, Integer> entry : items.entrySet()) {
                    Medicine medicine = entry.getKey();
                    if (medicine == null) {
                        throw new IllegalArgumentException("Sale contains an invalid medicine.");
                    }
                    int quantity = AppValidator.requirePositive(entry.getValue(), "Quantity");
                    total += medicine.getPrice() * quantity;

                    if (!batchDAO.deductStockFIFO(conn, medicine.getMedicineId(), quantity)) {
                        conn.rollback();
                        return false;
                    }
                }

                saleStmt.setString(1, saleId);
                saleStmt.setString(2, java.time.LocalDate.now().toString());
                saleStmt.setDouble(3, total);
                saleStmt.executeUpdate();

                for (Map.Entry<Medicine, Integer> entry : items.entrySet()) {
                    Medicine medicine = entry.getKey();
                    int quantity = entry.getValue();

                    itemStmt.setString(1, saleId);
                    itemStmt.setInt(2, medicine.getMedicineId());
                    itemStmt.setInt(3, quantity);
                    itemStmt.setDouble(4, medicine.getPrice() * quantity);
                    itemStmt.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<SaleRecord> getSaleRecords(boolean todayOnly, boolean newestFirst) {
        List<SaleRecord> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT saleId, saleDate, totalAmount FROM sale");

        if (todayOnly) {
            sql.append(" WHERE saleDate = ?");
        }

        sql.append(" ORDER BY saleDate ");
        sql.append(newestFirst ? "DESC" : "ASC");
        sql.append(", saleId ");
        sql.append(newestFirst ? "DESC" : "ASC");

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            if (todayOnly) {
                stmt.setString(1, LocalDate.now().toString());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new SaleRecord(
                            rs.getString("saleId"),
                            LocalDate.parse(rs.getString("saleDate")),
                            rs.getDouble("totalAmount")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return records;
    }
}
