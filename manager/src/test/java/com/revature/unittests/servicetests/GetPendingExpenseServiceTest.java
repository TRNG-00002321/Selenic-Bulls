package com.revature.unittests.servicetests;

import com.revature.repository.Approval;
import com.revature.repository.ApprovalRepository;
import com.revature.repository.Expense;
import com.revature.repository.ExpenseRepository;
import com.revature.repository.ExpenseWithUser;
import com.revature.repository.User;
import com.revature.service.ExpenseService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Expense Management System")
@Feature("Manager Expense Review")
@Story("As a manager, I want to view a list of all pending expenses so that I can review them efficiently")
public class GetPendingExpenseServiceTest {
    
    @Mock
    private ExpenseRepository expenseRepository;
    
    @Mock
    private ApprovalRepository approvalRepository;
    
    private ExpenseService expenseService;
    private AutoCloseable closeable;
    
    @BeforeAll
    static void setUpClass() {
        Allure.addAttachment("Test Suite Information", 
            "Unit tests for ExpenseService.getPendingExpenses() method");
        System.out.println("Starting GetPendingExpenseServiceTest suite");
    }
    
    @AfterAll
    static void tearDownClass() {
        System.out.println("Completed GetPendingExpenseServiceTest suite");
    }
    
    @BeforeEach
    void setUp() {
        //Resource management for mocks, used as a flag to ensure proper closure
        //When null, mocks are not initialized therefore cannot be closed
        //When not-null, mocks are initialized and need to be closed after test
        closeable = MockitoAnnotations.openMocks(this);

        //Initialize service with mocked dependencies
        expenseService = new ExpenseService(expenseRepository, approvalRepository);
        Allure.step("Test setup completed - ExpenseService initialized with mocked dependencies");
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) 
            closeable.close();
        
        reset(expenseRepository, approvalRepository);
        Allure.step("Test cleanup completed - Mocks reset");
    }
    
    /**
     * Provides test data for parameterized tests covering different scenarios
     * Outer Skeleton: Happy Path, Sad Path, Edge Cases, Boundary Conditions
     * Inner Skeleton: Scenario Description, Mock Return Data, Exception Flag, Expected Outcome, Assertion Description
     */
    static Stream<Arguments> getPendingExpensesTestData() {
        return Stream.of(
            // Happy Path - Multiple pending expenses
            Arguments.of(
                "Happy Path: Multiple Pending Expenses",
                createMultiplePendingExpenses(),
                false,
                3,
                "Should return all pending expenses when repository contains multiple pending expenses"
            ),
            // Edge Case - Empty list
            Arguments.of(
                "Edge Case: No Pending Expenses",
                Collections.emptyList(),
                false,
                0,
                "Should return empty list when no pending expenses exist"
            ),
            // Boundary - Single pending expense
            Arguments.of(
                "Boundary Case: Single Pending Expense",
                createSinglePendingExpense(),
                false,
                1,
                "Should return single expense when only one pending expense exists"
            ),
            // Sad Path - Repository throws exception (wraps SQLException in RuntimeException)
            Arguments.of(
                "Sad Path: Repository Database Exception",
                null,
                true,
                -1,
                "Should propagate RuntimeException when repository encounters database issues (wrapped SQLException)"
            )
        );
    }
    
    @ParameterizedTest(name = " ~ [{index}] {0}")
    @MethodSource("getPendingExpensesTestData")
    @DisplayName("Test getPendingExpenses with various scenarios")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tests the getPendingExpenses method of ExpenseService with different data scenarios including happy path, sad path, edge cases, and boundary conditions")
    void testGetPendingExpenses(String scenario, List<ExpenseWithUser> mockReturnData, 
                               boolean shouldThrowException, int expectedSize, String description) {
        // Log scenario details to Allure (will be different for each parameterized run)
        Allure.step("Arrange: " + scenario);
        Allure.addAttachment("Scenario", scenario);
        Allure.addAttachment("Description", description);
        

        //********** Arrange: Stub repository behavior **********//

        //Sad Path - Database Exception (wrapped SQLException)
        if (shouldThrowException) 
        {
            //Simulate repository database exception (realistic: SQLException wrapped in RuntimeException)
            when(expenseRepository.findPendingExpensesWithUsers())
                .thenThrow(new RuntimeException("Error finding pending expenses"));
            Allure.step("Configured mock to throw RuntimeException (simulating wrapped SQLException)");
        } 
        //Happy Path / Edge Cases / Boundary Conditions: Return mock data
        else 
        {
            //Simulate repository returning controlled test data
            when(expenseRepository.findPendingExpensesWithUsers()).thenReturn(mockReturnData);
            Allure.step("Configured mock to return " + (mockReturnData == null ? "null" : mockReturnData.size()) + " expenses");
        }
        

        //********** Act & Assert **********//

        //Sad Path - Database Exception
        if (shouldThrowException) 
        {   
            //Verify RuntimeException is thrown (represents wrapped SQLException)
            Allure.step("Act & Assert: Database Exception handling");
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> expenseService.getPendingExpenses(),
                "Should throw RuntimeException when repository encounters database issues");
            
            //Validate exception message matches repository's error wrapping
            assertEquals("Error finding pending expenses", exception.getMessage());
            Allure.step("Successfully caught and validated RuntimeException: " + exception.getMessage());
        }
        //Happy Path / Edge Cases / Boundary Conditions: Verify returned data
        else 
        {
            // Call service method (stubbed repo depenedency to return controlled data)
            Allure.step("Act: Calling getPendingExpenses()");
            List<ExpenseWithUser> result = expenseService.getPendingExpenses();
            
            // Validate results
            Allure.step("Assert: Validating results");
            assertNotNull(result, "Result should not be null");
            assertEquals(expectedSize, result.size(), 
                "Result size should match expected size for scenario: " + scenario);
            
            // Attribute validations for non-empty results
            if (!result.isEmpty()) 
            {
                // Validate each expense has pending status using lambda expression
                result.forEach(expenseWithUser -> {
                    //Approval object should not be null and status should be 'pending'
                    assertNotNull(expenseWithUser.getApproval(), "Approval should not be null");
                    assertEquals("pending", expenseWithUser.getApproval().getStatus(), 
                        "All expenses should have pending status");
                });
                Allure.step("Validated that all returned expenses have pending status");
            }
            
            Allure.addAttachment("Result Summary", 
                String.format("Returned %d pending expenses for scenario: %s", result.size(), scenario));
        }
        
        //Mocking: behavior interaction verifications
        Allure.step("Verifying repository interactions");
        verify(expenseRepository, times(1)).findPendingExpensesWithUsers();
        verifyNoInteractions(approvalRepository);
        Allure.step("Verified correct repository method calls");
    }
    
    // Helper methods to create ExpenseWithUser test data
    private static List<ExpenseWithUser> createMultiplePendingExpenses() {
        List<ExpenseWithUser> expenses = new ArrayList<>();
        
        // Expense 1
        Expense expense1 = new Expense(1, 101, 250.50, "Office Supplies", "2024-12-01");
        User user1 = new User(101, "john.doe", "password", "employee");
        Approval approval1 = new Approval(1, 1, "pending", null, null, null);
        expenses.add(new ExpenseWithUser(expense1, user1, approval1));
        
        // Expense 2
        Expense expense2 = new Expense(2, 102, 1200.00, "Business Travel", "2024-12-02");
        User user2 = new User(102, "jane.smith", "password", "employee");
        Approval approval2 = new Approval(2, 2, "pending", null, null, null);
        expenses.add(new ExpenseWithUser(expense2, user2, approval2));
        
        // Expense 3
        Expense expense3 = new Expense(3, 103, 75.25, "Team Lunch", "2024-12-03");
        User user3 = new User(103, "bob.wilson", "password", "employee");
        Approval approval3 = new Approval(3, 3, "pending", null, null, null);
        expenses.add(new ExpenseWithUser(expense3, user3, approval3));
        
        return expenses;
    }
    
    // Helper methods to create a single ExpenseWithUser for the boundry test data
    // Minimal valid pending expense
    private static List<ExpenseWithUser> createSinglePendingExpense() {
        List<ExpenseWithUser> expenses = new ArrayList<>();
        
        Expense expense = new Expense(1, 101, 150.75, "Software License", "2024-12-01");
        User user = new User(101, "test.user", "password", "employee");
        Approval approval = new Approval(1, 1, "pending", null, null, null);
        expenses.add(new ExpenseWithUser(expense, user, approval));
        
        return expenses;
    }
}
