package com.revature.repotests;

import com.revature.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GenerateReportRepoTest {

    @InjectMocks
    private ExpenseRepository expenseRepository;

    @Mock
    private DatabaseConnection mockDbConnection;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    public void setup() throws SQLException {
        when(mockDbConnection.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
    }

    private void setupMockResultSet() throws SQLException {
        // Simulating one row of data returned from a JOIN query
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getInt("user_id")).thenReturn(10);
        when(mockResultSet.getDouble("amount")).thenReturn(100.0);
        when(mockResultSet.getString("description")).thenReturn("Travel");
        when(mockResultSet.getString("date")).thenReturn("2025-01-01");
        when(mockResultSet.getString("username")).thenReturn("testuser");
        when(mockResultSet.getString("role")).thenReturn("EMPLOYEE");
        when(mockResultSet.getInt("approval_id")).thenReturn(100);
        when(mockResultSet.getString("status")).thenReturn("pending");
    }

    @Test
    @DisplayName("findAllExpensesWithUsers - should return list of all expenses")
    void testFindAllExpenses() throws SQLException {
        setupMockResultSet();

        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        verify(mockConnection).prepareStatement(contains("FROM expenses"));
    }

    @Test
    @DisplayName("findExpensesByUser - should filter by specific user ID")
    void testFindExpensesByUser() throws SQLException {
        setupMockResultSet();
        int userId = 10;

        List<ExpenseWithUser> results = expenseRepository.findExpensesByUser(userId);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(mockStatement).setInt(1, userId);
        verify(mockConnection).prepareStatement(contains("WHERE e.user_id = ?"));
    }

    @Test
    @DisplayName("findExpensesByDateRange - should pass start and end dates to query")
    void testFindExpensesByDateRange() throws SQLException {
        setupMockResultSet();
        String start = "2025-01-01";
        String end = "2025-01-31";

        List<ExpenseWithUser> results = expenseRepository.findExpensesByDateRange(start, end);

        assertNotNull(results);
        verify(mockStatement).setString(1, start);
        verify(mockStatement).setString(2, end);
        verify(mockConnection).prepareStatement(contains("e.date >= ? AND e.date <= ?"));
    }

    @Test
    @DisplayName("findExpensesByCategory - should use LIKE operator with wildcards")
    void testFindExpensesByCategory() throws SQLException {
        setupMockResultSet();
        String category = "Travel";

        expenseRepository.findExpensesByCategory(category);

        verify(mockStatement).setString(1, "%Travel%");
        verify(mockConnection).prepareStatement(contains("WHERE e.description LIKE ?"));
    }

    @Test
    @DisplayName("findPendingExpensesWithUsers - should filter by hardcoded 'pending' status")
    void testFindPendingExpenses() throws SQLException {
        setupMockResultSet();

        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        assertNotNull(results);
        verify(mockConnection).prepareStatement(contains("WHERE a.status = 'pending'"));
    }

    @Test
    @DisplayName("Sad Path: findExpensesByUser - should return empty list when no records found")
    void testNoResultsReturnsEmptyList() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);

        List<ExpenseWithUser> results = expenseRepository.findExpensesByUser(999);

        assertTrue(results.isEmpty());
        verify(mockStatement).setInt(1, 999);
    }

//    @Test
//    @DisplayName("Edge Case: SQL Exception should be wrapped in RuntimeException")
//    void testDatabaseError() throws SQLException {
//        when(mockDbConnection.getConnection()).thenThrow(new SQLException("Database connection failed"));
//
//        assertThrows(RuntimeException.class, () -> {
//            expenseRepository.findAllExpensesWithUsers();
//        });
//    }
}
