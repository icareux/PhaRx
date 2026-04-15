package com.pharmasystem.database;

import com.pharmasystem.model.Cashier;
import com.pharmasystem.model.Manager;
import com.pharmasystem.model.Pharmacist;
import com.pharmasystem.model.User;
import com.pharmasystem.util.AppValidator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    public User login(String username, String password) {
        username = AppValidator.requireNonBlank(username, "Username");
        password = AppValidator.requireNonBlank(password, "Password");
        String sql = "SELECT * FROM users WHERE username=?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String storedPassword = rs.getString("password");
                if (!PasswordUtil.matches(password, storedPassword)) {
                    return null;
                }

                String role = rs.getString("role");

                switch (role) {
                    case "CASHIER":
                        return new Cashier();
                    case "PHARMACIST":
                        return new Pharmacist();
                    case "MANAGER":
                        return new Manager();
                    default:
                        return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
