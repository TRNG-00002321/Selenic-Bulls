package com.revature.unittests.repotests;

import com.revature.repository.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
    @DisplayName("[Happy Path]: Retrieve all expenses for reporting")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies repository retrieves all expenses with associated user data.")
    void testFindAllExpenses() throws SQLException {

        Allure.step("Arrange: Setup mocked ResultSet for expenses and users");
        setupMockResultSet();

        Allure.step("Act: Execute findAllExpensesWithUsers query");
        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

        Allure.step("Assert: Verify results are returned");
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());

        Allure.step("Assert: Verify SQL query targets expenses table");
        verify(mockConnection).prepareStatement(contains("FROM expenses"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    @DisplayName("[Happy Path]: Retrieve expenses for a specific employee")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies repository filters expenses correctly based on different user IDs.")
    void testFindExpensesByUserParameterized(int userId) throws SQLException {

        Allure.step("Arrange: Setup mocked ResultSet for userId = " + userId);
        setupMockResultSet();

        Allure.step("Act: Execute findExpensesByUser query");
        List<ExpenseWithUser> results = expenseRepository.findExpensesByUser(userId);

        Allure.step("Assert: Verify results are returned");
        assertNotNull(results);

        Allure.step("Assert: Verify query parameter and SQL filter are applied");
        verify(mockStatement).setInt(1, userId);
        verify(mockConnection).prepareStatement(contains("WHERE e.user_id = ?"));
    }


    @Test
    @DisplayName("[Happy Path]: Retrieve expenses within a selected date range")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies repository passes start and end date parameters correctly to the SQL query.")
    void testFindExpensesByDateRange() throws SQLException {

        Allure.step("Arrange: Setup mocked ResultSet and date range parameters");
        setupMockResultSet();
        String start = "2025-01-01";
        String end = "2025-12-31";

        Allure.step("Act: Execute findExpensesByDateRange query");
        List<ExpenseWithUser> results = expenseRepository.findExpensesByDateRange(start, end);

        Allure.step("Assert: Verify results are returned");
        assertNotNull(results);

        Allure.step("Assert: Verify date parameters and SQL filter are applied");
        verify(mockStatement).setString(1, start);
        verify(mockStatement).setString(2, end);
        verify(mockConnection).prepareStatement(
                contains("e.date >= ? AND e.date <= ?")
        );
    }


    @ParameterizedTest
    @ValueSource(strings = {"Travel", "Hotel"})
    @DisplayName("[Happy Path]: Retrieve expenses matching a selected category")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies repository applies SQL LIKE filtering with wildcards for different category inputs.")
    void testFindExpensesByCategoryParameterized(String category) throws SQLException {

        Allure.step("Arrange: Setup mocked ResultSet for category = " + category);
        setupMockResultSet();

        Allure.step("Act: Execute findExpensesByCategory query");
        expenseRepository.findExpensesByCategory(category);

        Allure.step("Assert: Verify wildcard parameter and SQL LIKE clause are applied");
        verify(mockStatement).setString(1, "%" + category + "%");
        verify(mockConnection).prepareStatement(
                contains("WHERE e.description LIKE ?")
        );
    }


    @Test
    @DisplayName("[Happy Path]: Retrieve all pending expenses for review")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies repository filters expenses using the hardcoded 'pending' approval status.")
    void testFindPendingExpenses() throws SQLException {

        Allure.step("Arrange: Setup mocked ResultSet for pending expenses");
        setupMockResultSet();

        Allure.step("Act: Execute findPendingExpensesWithUsers query");
        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        Allure.step("Assert: Verify results are returned");
        assertNotNull(results);

        Allure.step("Assert: Verify SQL filters by pending status");
        verify(mockConnection).prepareStatement(
                contains("WHERE a.status = 'pending'")
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 999})
    @DisplayName("[Sad Path]: Returns empty list for non-existing users")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies repository returns an empty result when querying expenses for non-existing user IDs.")
    void testFindExpensesByUserNoResultsParameterized(int userId) throws SQLException {

        Allure.step("Arrange: Configure ResultSet to return no rows for userId = " + userId);
        when(mockResultSet.next()).thenReturn(false);

        Allure.step("Act: Execute findExpensesByUser query");
        List<ExpenseWithUser> results = expenseRepository.findExpensesByUser(userId);

        Allure.step("Assert: Verify empty result is returned");
        assertTrue(results.isEmpty());

        Allure.step("Assert: Verify userId parameter is applied to query");
        verify(mockStatement).setInt(1, userId);
    }

    @Test
    @DisplayName("[Sad Path]: Return no pending expenses when none exist")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies repository returns an empty list when no pending expenses exist.")
    void testFindPendingExpensesEmpty() throws SQLException {

        Allure.step("Arrange: Configure ResultSet to return no pending expense records");
        when(mockResultSet.next()).thenReturn(false);

        Allure.step("Act: Execute findPendingExpensesWithUsers query");
        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        Allure.step("Assert: Verify empty result is returned");
        assertTrue(results.isEmpty());
    }

    // Don’t fail the test if some stubbings were never used.
    @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
    @DisplayName("[Sad Path]: Fail when retrieving pending expenses due to database error")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies repository wraps SQLExceptions and throws a RuntimeException when a database error occurs.")
    void testFindPendingExpensesSQLException() throws SQLException {

        Allure.step("Arrange: Configure database connection to throw SQLException");
        when(mockDbConnection.getConnection()).thenThrow(new SQLException("DB failure"));

        Allure.step("Act & Assert: Verify RuntimeException is thrown");
        assertThrows(RuntimeException.class, () ->
                expenseRepository.findPendingExpensesWithUsers()
        );
    }

    @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
    @DisplayName("[Sad Path]: Fail when retrieving employee expenses due to database error")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies repository wraps SQLException and throws RuntimeException when a database error occurs during user-based expense lookup.")
    void testFindExpensesByUserSQLException() throws SQLException {

        Allure.step("Arrange: Configure database connection to throw SQLException");
        when(mockDbConnection.getConnection()).thenThrow(new SQLException("DB failure"));

        Allure.step("Act & Assert: Verify RuntimeException is thrown");
        assertThrows(RuntimeException.class, () ->
                expenseRepository.findExpensesByUser(1)
        );
    }

    @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
    @DisplayName("[Sad Path]: Fail when retrieving expenses by date range due to database error")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies repository wraps SQLException and throws RuntimeException when a database error occurs during date-range expense lookup.")
    void testFindExpensesByDateRangeSQLException() throws SQLException {

        Allure.step("Arrange: Configure database connection to throw SQLException");
        when(mockDbConnection.getConnection()).thenThrow(new SQLException("DB failure"));

        Allure.step("Act & Assert: Verify RuntimeException is thrown");
        assertThrows(RuntimeException.class, () ->
                expenseRepository.findExpensesByDateRange("2025-01-01", "2025-01-31")
        );
    }

    @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
    @DisplayName("[Sad Path]: Fail when retrieving expenses by category due to database error")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies repository wraps SQLException and throws RuntimeException when a database error occurs during category-based expense lookup.")
    void testFindExpensesByCategorySQLException() throws SQLException {

        Allure.step("Arrange: Configure database connection to throw SQLException");
        when(mockDbConnection.getConnection()).thenThrow(new SQLException("DB failure"));

        Allure.step("Act & Assert: Verify RuntimeException is thrown");
        assertThrows(RuntimeException.class, () ->
                expenseRepository.findExpensesByCategory("Travel")
        );
    }

}
