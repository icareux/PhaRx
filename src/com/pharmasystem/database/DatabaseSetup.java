package com.pharmasystem.database;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseSetup {

    private static final String CASHIER_HASH = PasswordUtil.hash("123");
    private static final String PHARMACIST_HASH = PasswordUtil.hash("456");
    private static final String MANAGER_HASH = PasswordUtil.hash("789");

    public static void initialize() {
        String sql1 = "CREATE TABLE IF NOT EXISTS medicine (" +
                "medicine_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "price REAL NOT NULL CHECK(price >= 0)," +
                "quantityInStock INTEGER NOT NULL DEFAULT 0 CHECK(quantityInStock >= 0)," +
                "expirationDate TEXT NOT NULL," +
                "requiresPrescription INTEGER NOT NULL DEFAULT 0 CHECK(requiresPrescription IN (0,1)))";

        String sql2 = "CREATE TABLE IF NOT EXISTS sale (" +
                "saleId TEXT PRIMARY KEY," +
                "saleDate TEXT NOT NULL," +
                "totalAmount REAL NOT NULL CHECK(totalAmount >= 0))";

        String sql3 = "CREATE TABLE IF NOT EXISTS sale_item (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "saleId TEXT NOT NULL," +
                "medicine_id INTEGER NOT NULL," +
                "quantity INTEGER NOT NULL CHECK(quantity > 0)," +
                "subtotal REAL NOT NULL CHECK(subtotal >= 0)," +
                "FOREIGN KEY (saleId) REFERENCES sale(saleId) ON DELETE CASCADE," +
                "FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id))";

        String sql4 = "CREATE TABLE IF NOT EXISTS users (" + 
                "userId TEXT PRIMARY KEY," + 
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," + 
                "role TEXT NOT NULL CHECK(role IN ('CASHIER','PHARMACIST','MANAGER')))";

        String sql5 = "CREATE TABLE IF NOT EXISTS batches (" +
                "batch_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "medicine_id INTEGER NOT NULL," +
                "quantity INTEGER NOT NULL CHECK(quantity >= 0)," +
                "expiry_date TEXT NOT NULL," +
                "purchase_date TEXT NOT NULL," +
                "FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id))";
        
        String pragma = "PRAGMA foreign_keys = ON";
        String index1 = "CREATE INDEX IF NOT EXISTS idx_batches_medicine_expiry ON batches(medicine_id, expiry_date)";
        String index2 = "CREATE INDEX IF NOT EXISTS idx_sale_item_sale ON sale_item(saleId)";
        
        String insert1 = "INSERT OR IGNORE INTO medicine VALUES " +
                "(1,'Paracetamol',10.0,100,'2026-12-31',0)," +
                "(2,'Amoxicillin',25.0,50,'2026-10-01',1)," +
                "(3,'Ibuprofen',15.0,80,'2026-09-15',0)," +
                "(4,'Cetirizine',12.0,60,'2026-11-20',0)";
        
        String insert2 = "INSERT OR IGNORE INTO users VALUES " +
                "('U1','cashier','" + CASHIER_HASH + "','CASHIER')," +
                "('U2','pharma','" + PHARMACIST_HASH + "','PHARMACIST')," +
                "('U3','manager','" + MANAGER_HASH + "','MANAGER')";

        String seedBatches = "INSERT INTO batches (medicine_id, quantity, expiry_date, purchase_date) "
                + "SELECT m.medicine_id, m.quantityInStock, "
                + "COALESCE(NULLIF(m.expirationDate, ''), date('now', '+365 day')), "
                + "date('now') "
                + "FROM medicine m "
                + "WHERE COALESCE(m.quantityInStock, 0) > 0 "
                + "AND NOT EXISTS (SELECT 1 FROM batches b WHERE b.medicine_id = m.medicine_id)";

        String migrate1 = "UPDATE users SET password='" + CASHIER_HASH + "' WHERE userId='U1' AND password='123'";
        String migrate2 = "UPDATE users SET password='" + PHARMACIST_HASH + "' WHERE userId='U2' AND password='456'";
        String migrate3 = "UPDATE users SET password='" + MANAGER_HASH + "' WHERE userId='U3' AND password='789'";

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(pragma);
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
            stmt.execute(sql4);
            stmt.execute(sql5);
            stmt.execute(index1);
            stmt.execute(index2);
            stmt.execute(insert1);
            stmt.execute(insert2);
            stmt.execute(seedBatches);
            stmt.execute(migrate1);
            stmt.execute(migrate2);
            stmt.execute(migrate3);

            System.out.println("Database tables created!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
