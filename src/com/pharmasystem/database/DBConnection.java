package com.pharmasystem.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    public static Connection connect() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:pharmacy.db");
            conn.createStatement().execute("PRAGMA foreign_keys = ON");
            return conn;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to connect to pharmacy.db", e);
        }
    }
}
