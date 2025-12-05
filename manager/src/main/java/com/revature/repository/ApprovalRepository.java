package com.revature.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Repository for approval data access operations.
 * Handles database interactions for expense approval management.
 */
public class ApprovalRepository {
    private final DatabaseConnection databaseConnection;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public ApprovalRepository(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Find an approval by expense ID.
     * @param expenseId the expense ID
     * @return Optional containing the approval if found, empty otherwise
     */
    public Optional<Approval> findByExpenseId(int expenseId) {
        String sql = "SELECT id, expense_id, status, reviewer, comment, review_date FROM approvals WHERE expense_id = ?";
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, expenseId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapRowToApproval(rs));
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding approval for expense: " + expenseId, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Update approval status for an expense.
     * @param expenseId the expense ID
     * @param status the new approval status ("approved" or "denied")
     * @param reviewerId the manager's user ID
     * @param comment optional comment from the manager
     * @return true if update was successful
     */
    public boolean updateApprovalStatus(int expenseId, String status, int reviewerId, String comment) {
        String sql = """
            UPDATE approvals 
            SET status = ?, reviewer = ?, comment = ?, review_date = ?
            WHERE expense_id = ?
            """;
        
        String reviewDate = LocalDateTime.now().format(DATE_FORMATTER);
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setInt(2, reviewerId);
            stmt.setString(3, comment);
            stmt.setString(4, reviewDate);
            stmt.setInt(5, expenseId);
            
            int updatedRows = stmt.executeUpdate();
            return updatedRows > 0;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error updating approval for expense: " + expenseId, e);
        }
    }
    
    /**
     * Create a new approval record for an expense.
     * This should typically be called when an expense is first submitted.
     * @param expenseId the expense ID
     * @param status initial status (usually "pending")
     * @return the created approval
     */
    public Approval createApproval(int expenseId, String status) {
        String sql = "INSERT INTO approvals (expense_id, status) VALUES (?, ?)";
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, expenseId);
            stmt.setString(2, status);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Creating approval failed, no rows affected.");
            }
            
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int approvalId = generatedKeys.getInt(1);
                Approval approval = new Approval();
                approval.setId(approvalId);
                approval.setExpenseId(expenseId);
                approval.setStatus(status);
                return approval;
            } else {
                throw new RuntimeException("Creating approval failed, no ID obtained.");
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error creating approval for expense: " + expenseId, e);
        }
    }
    
    private Approval mapRowToApproval(ResultSet rs) throws SQLException {
        Approval approval = new Approval();
        approval.setId(rs.getInt("id"));
        approval.setExpenseId(rs.getInt("expense_id"));
        approval.setStatus(rs.getString("status"));
        approval.setReviewer((Integer) rs.getObject("reviewer"));
        approval.setComment(rs.getString("comment"));
        approval.setReviewDate(rs.getString("review_date"));
        return approval;
    }
}