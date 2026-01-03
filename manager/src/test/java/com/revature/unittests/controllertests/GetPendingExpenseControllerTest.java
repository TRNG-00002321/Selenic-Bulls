package com.revature.unittests.controllertests;

import com.revature.api.ExpenseController;
import com.revature.repository.Approval;
import com.revature.repository.Expense;
import com.revature.repository.ExpenseWithUser;
import com.revature.repository.User;
import com.revature.service.ExpenseService;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Expense Management System")
@Feature("Controller API Endpoints")
@Story("As a manager, I want to view a list of all pending expenses so that I can review them efficiently")
public class GetPendingExpenseControllerTest {
    
    @Mock
    private ExpenseService expenseService;
    
    @Mock
    private Context context;
    
    private ExpenseController expenseController;
    private AutoCloseable closeable;
    
    @BeforeAll
    static void setUpClass() {
        Allure.addAttachment("Test Suite Information", 
            "Unit tests for ExpenseController.getPendingExpenses() method");
        System.out.println("Starting GetPendingExpenseControllerTest suite");
    }
    
    @AfterAll
    static void tearDownClass() {
        System.out.println("Completed GetPendingExpenseControllerTest suite");
    }
    
    @BeforeEach
    void setUp() {
        //Resource management for mocks, used as a flag to ensure proper closure
        //When null, mocks are not initialized therefore cannot be closed
        //When not-null, mocks are initialized and need to be closed after test
        closeable = MockitoAnnotations.openMocks(this);

        //Initialize controller with mocked dependencies
        expenseController = new ExpenseController(expenseService);
        Allure.step("Test setup completed - ExpenseController initialized with mocked ExpenseService");
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) 
            closeable.close();
        
        reset(expenseService, context);
        Allure.step("Test cleanup completed - Mocks reset");
    }
    
    /**
     * Provides test data for parameterized tests covering different scenarios
     * Outer Skeleton: Happy Path, Sad Path, Edge Cases, Boundary Conditions
     * Inner Skeleton: Scenario Description, Mock Return Data, Exception Flag, Expected Outcome, Assertion Description
     */
    static Stream<Arguments> getPendingExpensesControllerTestData() {
        return Stream.of(
            // Happy Path - Multiple pending expenses
            Arguments.of(
                "Happy Path: Multiple Pending Expenses",
                createMultiplePendingExpenses(),
                false,
                3,
                "Should return successful JSON response with multiple pending expenses"
            ),
            // Edge Case - No pending expenses
            Arguments.of(
                "Edge Case: No Pending Expenses",
                Collections.emptyList(),
                false,
                0,
                "Should return successful JSON response with empty list when no pending expenses exist"
            ),
            // Boundary - Single pending expense
            Arguments.of(
                "Boundary Case: Single Pending Expense",
                createSinglePendingExpense(),
                false,
                1,
                "Should return successful JSON response with single pending expense"
            ),
            // Sad Path - Service throws exception
            Arguments.of(
                "Sad Path: Service Exception",
                null,
                true,
                -1,
                "Should throw InternalServerErrorResponse when service layer fails"
            )
        );
    }
    
    @ParameterizedTest(name = " ~ [{index}] {0}")
    @MethodSource("getPendingExpensesControllerTestData")
    @DisplayName("Test getPendingExpenses controller endpoint with various scenarios")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tests the getPendingExpenses method of ExpenseController with different service response scenarios including happy path, sad path, edge cases, and boundary conditions")
    void testGetPendingExpenses(String scenario, List<ExpenseWithUser> mockServiceData,
                               boolean shouldThrowException, int expectedCount, String description) {
        // Log scenario details to Allure (will be different for each parameterized run)
        Allure.step("Arrange: " + scenario);
        Allure.addAttachment("Scenario", scenario);
        Allure.addAttachment("Description", description);
        

        //********** Arrange: Stub repository behavior **********//
        
        //Sad Path - Database Exception (wrapped SQLException)
        if (shouldThrowException) 
        {
            // Simulate service layer exception
            when(expenseService.getPendingExpenses())
                .thenThrow(new RuntimeException("Database connection failed"));
            Allure.step("Configured mock service to throw RuntimeException");
        }
        //Happy Path / Edge Cases / Boundary Conditions: Return mock data
        else 
        {
            // Simulate successful service response
            when(expenseService.getPendingExpenses()).thenReturn(mockServiceData);
            Allure.step("Configured mock service to return " + expectedCount + " pending expenses");
        }
        

        //********** Act & Assert **********//

        //Sad Path - Database Exception
        if (shouldThrowException) 
        {
            Allure.step("Act & Assert: Exception handling");
            InternalServerErrorResponse exception = assertThrows(InternalServerErrorResponse.class,
                () -> expenseController.getPendingExpenses(context),
                "Should throw InternalServerErrorResponse when service fails");
            
            assertEquals("Failed to retrieve pending expenses: Database connection failed", 
                exception.getMessage());
            Allure.step("Successfully caught and validated InternalServerErrorResponse: " + exception.getMessage());
            
            // Verify no JSON response was set on context
            verify(context, never()).json(any());
        } 
        else 
        {
            Allure.step("Act: Calling getPendingExpenses() controller method");
            
            // Should not throw any exception
            assertDoesNotThrow(() -> expenseController.getPendingExpenses(context));
            
            Allure.step("Assert: Validating JSON response structure");
            
            // Capture the JSON response that was set on the context
            ArgumentCaptor<Map<String, Object>> jsonCaptor = ArgumentCaptor.forClass(Map.class);
            verify(context, times(1)).json(jsonCaptor.capture());
            
            Map<String, Object> jsonResponse = jsonCaptor.getValue();
            
            // Validate JSON response structure
            assertNotNull(jsonResponse, "JSON response should not be null");
            assertTrue((Boolean) jsonResponse.get("success"), "Response should indicate success");
            assertEquals(expectedCount, ((Number) jsonResponse.get("count")).intValue(), 
                "Response count should match expected count");
            
            // Validate data payload
            @SuppressWarnings("unchecked")
            List<ExpenseWithUser> responseData = (List<ExpenseWithUser>) jsonResponse.get("data");
            assertNotNull(responseData, "Response data should not be null");
            assertEquals(expectedCount, responseData.size(), 
                "Response data size should match expected count");
            
            if (expectedCount > 0) 
            {
                // Validate data structure for non-empty responses using lambda expression
                responseData.forEach(expenseWithUser -> {
                    assertNotNull(expenseWithUser.getExpense(), "Expense should not be null");
                    assertNotNull(expenseWithUser.getUser(), "User should not be null");
                    assertNotNull(expenseWithUser.getApproval(), "Approval should not be null");
                    assertEquals("pending", expenseWithUser.getApproval().getStatus(),
                        "All expenses should have pending status");
                });
                Allure.step("Validated data structure and pending status for all returned expenses");
            }
            
            Allure.addAttachment("Response Summary", 
                String.format("JSON response contains %d pending expenses with success=true for scenario: %s", 
                    expectedCount, scenario));
        }
        
        //Mocking: behavior interaction verifications
        verify(expenseService, times(1)).getPendingExpenses();
        Allure.step("Verified correct service method invocation");
    }
    
    // Helper methods to create ExpenseWithUser test data
    private static List<ExpenseWithUser> createMultiplePendingExpenses() {
        List<ExpenseWithUser> expenses = new ArrayList<>();
        
        // Expense 1 - Office Supplies
        Expense expense1 = new Expense(1, 101, 250.50, "Office Supplies", "2024-12-01");
        User user1 = new User(101, "john.doe", "password", "employee");
        Approval approval1 = new Approval(1, 1, "pending", null, null, null);
        expenses.add(new ExpenseWithUser(expense1, user1, approval1));
        
        // Expense 2 - Business Travel
        Expense expense2 = new Expense(2, 102, 1200.00, "Business Travel", "2024-12-02");
        User user2 = new User(102, "jane.smith", "password", "employee");
        Approval approval2 = new Approval(2, 2, "pending", null, null, null);
        expenses.add(new ExpenseWithUser(expense2, user2, approval2));
        
        // Expense 3 - Team Lunch
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
