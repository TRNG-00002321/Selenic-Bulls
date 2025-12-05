package com.revature.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for expense data access operations.
 * Handles database interactions for expense management and reporting.
 */
public class ExpenseRepository {
    private final DatabaseConnection databaseConnection;
    
    public ExpenseRepository(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }
    
    /**
     * Find an expense by its ID.
     * @param expenseId the expense ID
     * @return Optional containing the expense if found, empty otherwise
     */
    public Optional<Expense> findById(int expenseId) {
        String sql = "SELECT id, user_id, amount, description, date FROM expenses WHERE id = ?";
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, expenseId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapRowToExpense(rs));
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding expense by ID: " + expenseId, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all expenses with pending approval status along with user information.
     * @return List of ExpenseWithUser objects for pending expenses
     */
    public List<ExpenseWithUser> findPendingExpensesWithUsers() {
        String sql = """
            SELECT e.id, e.user_id, e.amount, e.description, e.date,
                   u.username, u.role,
                   a.id as approval_id, a.status, a.reviewer, a.comment, a.review_date
            FROM expenses e
            JOIN users u ON e.user_id = u.id
            JOIN approvals a ON e.id = a.expense_id
            WHERE a.status = 'pending'
            ORDER BY e.date DESC
            """;
        
        List<ExpenseWithUser> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapRowToExpenseWithUser(rs));
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding pending expenses", e);
        }
        
        return results;
    }
    
    /**
     * Get all expenses for a specific user.
     * @param userId the user ID
     * @return List of ExpenseWithUser objects
     */
    public List<ExpenseWithUser> findExpensesByUser(int userId) {
        String sql = """
            SELECT e.id, e.user_id, e.amount, e.description, e.date,
                   u.username, u.role,
                   a.id as approval_id, a.status, a.reviewer, a.comment, a.review_date
            FROM expenses e
            JOIN users u ON e.user_id = u.id
            JOIN approvals a ON e.id = a.expense_id
            WHERE e.user_id = ?
            ORDER BY e.date DESC
            """;
        
        List<ExpenseWithUser> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapRowToExpenseWithUser(rs));
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding expenses for user: " + userId, e);
        }
        
        return results;
    }
    
    /**
     * Get expenses by date range.
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return List of ExpenseWithUser objects
     */
    public List<ExpenseWithUser> findExpensesByDateRange(String startDate, String endDate) {
        String sql = """
            SELECT e.id, e.user_id, e.amount, e.description, e.date,
                   u.username, u.role,
                   a.id as approval_id, a.status, a.reviewer, a.comment, a.review_date
            FROM expenses e
            JOIN users u ON e.user_id = u.id
            JOIN approvals a ON e.id = a.expense_id
            WHERE e.date >= ? AND e.date <= ?
            ORDER BY e.date DESC
            """;
        
        List<ExpenseWithUser> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapRowToExpenseWithUser(rs));
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding expenses by date range: " + startDate + " to " + endDate, e);
        }
        
        return results;
    }
    
    /**
     * Get expenses grouped by description (category).
     * @param category the description/category to filter by
     * @return List of ExpenseWithUser objects
     */
    public List<ExpenseWithUser> findExpensesByCategory(String category) {
        String sql = """
            SELECT e.id, e.user_id, e.amount, e.description, e.date,
                   u.username, u.role,
                   a.id as approval_id, a.status, a.reviewer, a.comment, a.review_date
            FROM expenses e
            JOIN users u ON e.user_id = u.id
            JOIN approvals a ON e.id = a.expense_id
            WHERE e.description LIKE ?
            ORDER BY e.date DESC
            """;
        
        List<ExpenseWithUser> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + category + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapRowToExpenseWithUser(rs));
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding expenses by category: " + category, e);
        }
        
        return results;
    }
    
    /**
     * Get all expenses with their user and approval information.
     * @return List of all ExpenseWithUser objects
     */
    public List<ExpenseWithUser> findAllExpensesWithUsers() {
        String sql = """
            SELECT e.id, e.user_id, e.amount, e.description, e.date,
                   u.username, u.role,
                   a.id as approval_id, a.status, a.reviewer, a.comment, a.review_date
            FROM expenses e
            JOIN users u ON e.user_id = u.id
            JOIN approvals a ON e.id = a.expense_id
            ORDER BY e.date DESC
            """;
        
        List<ExpenseWithUser> results = new ArrayList<>();
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapRowToExpenseWithUser(rs));
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all expenses", e);
        }
        
        return results;
    }
    
    private Expense mapRowToExpense(ResultSet rs) throws SQLException {
        Expense expense = new Expense();
        expense.setId(rs.getInt("id"));
        expense.setUserId(rs.getInt("user_id"));
        expense.setAmount(rs.getDouble("amount"));
        expense.setDescription(rs.getString("description"));
        expense.setDate(rs.getString("date"));
        return expense;
    }
    
    private ExpenseWithUser mapRowToExpenseWithUser(ResultSet rs) throws SQLException {
        // Map expense
        Expense expense = new Expense();
        expense.setId(rs.getInt("id"));
        expense.setUserId(rs.getInt("user_id"));
        expense.setAmount(rs.getDouble("amount"));
        expense.setDescription(rs.getString("description"));
        expense.setDate(rs.getString("date"));
        
        // Map user
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setRole(rs.getString("role"));
        
        // Map approval
        Approval approval = new Approval();
        approval.setId(rs.getInt("approval_id"));
        approval.setExpenseId(rs.getInt("id"));
        approval.setStatus(rs.getString("status"));
        approval.setReviewer((Integer) rs.getObject("reviewer"));
        approval.setComment(rs.getString("comment"));
        approval.setReviewDate(rs.getString("review_date"));
        
        return new ExpenseWithUser(expense, user, approval);
    }
}