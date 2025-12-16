package com.revature.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection utility for SQLite database.
 * Handles connection management for the shared expense manager database.
 */
public class DatabaseConnection {
    private final String databasePath;
    
    public DatabaseConnection() {
        // Use environment variable or default path
        System.setProperty("databasePath", "C:/Users/Stephenie/Documents/Revature/Project1QAsuals/employee/expense_manager.db");
        this.databasePath = System.getenv("DATABASE_PATH") != null
            ? System.getenv("DATABASE_PATH")
            : System.getProperty("databasePath");
    }
    
    public DatabaseConnection(String databasePath) {
        this.databasePath = databasePath;
    }
    
    /**
     * Get a database connection.
     * @return SQLite database connection
     * @throws SQLException if connection fails
     */
    public Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + databasePath;
        return DriverManager.getConnection(url);
    }
}