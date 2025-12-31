package com.revature.repotests;

import com.revature.repository.Approval;
import com.revature.repository.DatabaseConnection;
import com.revature.repository.Expense;
import com.revature.repository.ExpenseRepository;
import com.revature.repository.ExpenseWithUser;
import com.revature.repository.User;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Expense Management System")
@Feature("Repository Data Access")
@Story("As a manager, I want to view a list of all pending expenses so that I can review them efficiently")
public class GetPendingExpenseRepoTest {
    
    @Mock
    private DatabaseConnection databaseConnection;
    
    @Mock
    private Connection connection;
    
    @Mock
    private PreparedStatement preparedStatement;
    
    @Mock
    private ResultSet resultSet;
    
    private ExpenseRepository expenseRepository;
    private AutoCloseable closeable;
    
    @BeforeAll
    static void setUpClass() {
        Allure.addAttachment("Test Suite Information", 
            "Unit tests for ExpenseRepository.findPendingExpensesWithUsers() method");
        System.out.println("Starting FindPendingExpensesRepositoryTest suite");
    }
    
    @AfterAll
    static void tearDownClass() {
        System.out.println("Completed FindPendingExpensesRepositoryTest suite");
    }
    
    @BeforeEach
    void setUp() {
        //Resource management for mocks, used as a flag to ensure proper closure
        //When null, mocks are not initialized therefore cannot be closed
        //When not-null, mocks are initialized and need to be closed after test
        closeable = MockitoAnnotations.openMocks(this);

        //Initialize repository with mocked dependencies
        expenseRepository = new ExpenseRepository(databaseConnection);
        Allure.step("Test setup completed - ExpenseRepository initialized with mocked DatabaseConnection");
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) 
            closeable.close();
        
        reset(databaseConnection, connection, preparedStatement, resultSet);
        Allure.step("Test cleanup completed - Mocks reset");
    }
    
    /**
     * Provides test data for parameterized tests covering different scenarios
     * Outer Skeleton: Happy Path, Sad Path, Edge Cases, Boundary Conditions
     * Inner Skeleton: Scenario Description, Mock Return Data, Exception Flag, Expected Outcome, Assertion Description
     */
    static Stream<Arguments> findPendingExpensesTestData() {
        return Stream.of(
            // Happy Path - Multiple pending expenses
            Arguments.of(
                "Happy Path: Multiple Pending Expenses",
                3,
                false,
                "Should return all pending expenses when database contains multiple pending records"
            ),
            // Edge Case - No pending expenses
            Arguments.of(
                "Edge Case: No Pending Expenses",
                0,
                false,
                "Should return empty list when no pending expenses exist in database"
            ),
            // Boundary - Single pending expense
            Arguments.of(
                "Boundary Case: Single Pending Expense",
                1,
                false,
                "Should return single expense when only one pending expense exists in database"
            ),
            // Sad Path - Database connection failure
            Arguments.of(
                "Sad Path: Database Connection Failure",
                -1,
                true,
                "Should throw RuntimeException when database connection fails"
            )
        );
    }
    
    @ParameterizedTest(name = " ~ [{index}] {0}")
    @MethodSource("findPendingExpensesTestData")
    @DisplayName("Test findPendingExpensesWithUsers with various database scenarios")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tests the findPendingExpensesWithUsers method of ExpenseRepository with different database scenarios including happy path, sad path, edge cases, and boundary conditions")
    void testFindPendingExpensesWithUsers(String scenario, int expectedCount, 
                                         boolean shouldThrowException, String description) throws SQLException {
        // Log scenario details to Allure (will be different for each parameterized run)
        Allure.step("Arrange: " + scenario);
        Allure.addAttachment("Scenario", scenario);
        Allure.addAttachment("Description", description);
        
        
        //********** Arrange: Stub repository behavior **********//

        //Sad Path - Database Exception (wrapped SQLException)
        if (shouldThrowException) 
        {
            // Simulate database connection failure
            when(databaseConnection.getConnection()).thenThrow(new SQLException("Connection failed", "08001", 1001));
            Allure.step("Configured mock to throw SQLException on connection attempt");
        } 
        //Happy Path / Edge Cases / Boundary Conditions: Return mock objects
        else {
            // Setup successful database interaction chain
            when(databaseConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            
            // Configure ResultSet behavior based on expected count
            setupResultSetMocking(expectedCount);
            
            Allure.step("Configured mock database chain to return " + expectedCount + " pending expenses");
        }
        

        //********** ACT & ASSERT **********//

        //Sad Path - Exception Handling
        if (shouldThrowException) {
            Allure.step("Act & Assert: Database Exception handling");
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> expenseRepository.findPendingExpensesWithUsers(),
                "Should throw RuntimeException when database connection fails");
            
            assertEquals("Error finding pending expenses", exception.getMessage());
            assertTrue(exception.getCause() instanceof SQLException);
            Allure.step("Successfully caught and validated RuntimeException with SQLException cause");
            
            // Mocking: Verify connection attempt was made
            verify(databaseConnection, times(1)).getConnection();
        }
        //Happy Path / Edge Cases / Boundary Conditions: Validate returned data
        else 
        {
            Allure.step("Act: Calling findPendingExpensesWithUsers()");
            List<ExpenseWithUser> result = expenseRepository.findPendingExpensesWithUsers();
            
            Allure.step("Assert: Validating database query results");
            assertNotNull(result, "Result should not be null");
            assertEquals(expectedCount, result.size(), 
                "Result size should match expected count for scenario: " + scenario);
            
            if (expectedCount > 0) {
                // Validate structure of returned data
                result.forEach(expenseWithUser -> {
                    assertNotNull(expenseWithUser.getExpense(), "Expense should not be null");
                    assertNotNull(expenseWithUser.getUser(), "User should not be null");
                    assertNotNull(expenseWithUser.getApproval(), "Approval should not be null");
                    assertEquals("pending", expenseWithUser.getApproval().getStatus(), 
                        "All returned expenses should have pending status");
                });
                Allure.step("Validated data structure and pending status for all returned expenses");
            }
            
            //Mocking: Verify database interaction chain
            verify(databaseConnection, times(1)).getConnection();
            verify(connection, times(1)).prepareStatement(anyString());
            verify(preparedStatement, times(1)).executeQuery();
            verify(resultSet, atLeastOnce()).next();
            
            Allure.addAttachment("Query Result Summary", 
                String.format("Retrieved %d pending expenses for scenario: %s", result.size(), scenario));
        }
        
        Allure.step("Verified correct database interaction sequence");
    }
    
    /**
     * Helper method to setup ResultSet mocking based on expected record count
     */
    private void setupResultSetMocking(int expectedCount) throws SQLException {
        if (expectedCount == 0) {
            // No records - resultSet.next() returns false immediately
            when(resultSet.next()).thenReturn(false);
        } else if (expectedCount == 1) {
            // Single record scenario
            when(resultSet.next()).thenReturn(true, false);
            setupSingleRecordData(resultSet, 1);
        } else if (expectedCount == 3) {
            // Multiple records scenario
            when(resultSet.next()).thenReturn(true, true, true, false);
            setupMultipleRecordData(resultSet);
        }
    }
    
    /**
     * Setup ResultSet data for single record scenario, boundary case
     */
    private void setupSingleRecordData(ResultSet rs, int recordIndex) throws SQLException {
        // Expense data
        when(rs.getInt("id")).thenReturn(recordIndex);
        when(rs.getInt("user_id")).thenReturn(100 + recordIndex);
        when(rs.getDouble("amount")).thenReturn(150.75);
        when(rs.getString("description")).thenReturn("Software License");
        when(rs.getString("date")).thenReturn("2024-12-01");
        
        // User data
        when(rs.getString("username")).thenReturn("test.user");
        when(rs.getString("role")).thenReturn("employee");
        
        // Approval data
        when(rs.getInt("approval_id")).thenReturn(recordIndex);
        when(rs.getString("status")).thenReturn("pending");
        when(rs.getObject("reviewer")).thenReturn(null);
        when(rs.getString("comment")).thenReturn(null);
        when(rs.getString("review_date")).thenReturn(null);
    }
    
    /**
     * Setup ResultSet data for multiple records scenario, happy path
     */
    private void setupMultipleRecordData(ResultSet rs) throws SQLException {
        // Use thenReturn with multiple values for sequential calls
        when(rs.getInt("id")).thenReturn(1, 2, 3);
        when(rs.getInt("user_id")).thenReturn(101, 102, 103);
        when(rs.getDouble("amount")).thenReturn(250.50, 1200.00, 75.25);
        when(rs.getString("description"))
            .thenReturn("Office Supplies", "Business Travel", "Team Lunch");
        when(rs.getString("date"))
            .thenReturn("2024-12-01", "2024-12-02", "2024-12-03");
        
        // User data
        when(rs.getString("username"))
            .thenReturn("john.doe", "jane.smith", "bob.wilson");
        when(rs.getString("role")).thenReturn("employee");
        
        // Approval data
        when(rs.getInt("approval_id")).thenReturn(1, 2, 3);
        when(rs.getString("status")).thenReturn("pending");
        when(rs.getObject("reviewer")).thenReturn(null);
        when(rs.getString("comment")).thenReturn(null);
        when(rs.getString("review_date")).thenReturn(null);
    }
}