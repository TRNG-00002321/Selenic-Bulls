package com.revature.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Repository for user data access operations.
 * Handles database interactions for user authentication and information retrieval.
 */
public class UserRepository {
    private final DatabaseConnection databaseConnection;
    
    public UserRepository(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Find a user by their ID.
     * @param userId the user ID
     * @return Optional containing the user if found, empty otherwise
     */
    public Optional<User> findById(int userId) {
        String sql = "SELECT id, username, password, role FROM users WHERE id = ?";
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                return Optional.of(user);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by ID: " + userId, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Find a user by their username.
     * @param username the username
     * @return Optional containing the user if found, empty otherwise
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password, role FROM users WHERE username = ?";
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                return Optional.of(user);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username: " + username, e);
        }
        
        return Optional.empty();
    }
}