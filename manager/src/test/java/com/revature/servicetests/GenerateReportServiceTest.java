package com.revature.servicetests;

import com.revature.repository.Approval;
import com.revature.repository.Expense;
import com.revature.repository.ExpenseWithUser;
import com.revature.repository.User;
import com.revature.service.ExpenseService;
import com.revature.repository.ExpenseRepository;
import com.revature.repository.ApprovalRepository;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Expense Management System")
@Feature("CSV Report Generation")
@Story("As a manager, I want to generate CSV reports of expenses for analysis and record-keeping")
public class GenerateReportServiceTest {

    @InjectMocks
    private ExpenseService expenseService;

    @Mock
    private ExpenseRepository mockExpenseRepo;

    @Mock
    private ApprovalRepository mockApprovalRepo;

    // ==================== Helper Steps ====================
    @Step("Given the repository is prepared to return {data.size} records")
    private void mockRepositoryWithData(List<ExpenseWithUser> data) {
        when(mockExpenseRepo.findAllExpensesWithUsers()).thenReturn(data);
    }

    @Step("When the CSV report is generated for the expense list")
    private String generateCsvReportFromList(List<ExpenseWithUser> expenses) {
        return expenseService.generateCsvReport(expenses);
    }

    @Step("Then the CSV header should match the standard format")
    private void assertCsvHeader(String csv) {
        String expectedHeader = "Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date";
        String actualHeader = csv.split("\n")[0].trim();
        assertEquals(expectedHeader, actualHeader, 
            "CSV header must match expected format exactly");
    }

    @Step("Then the last row of the CSV should match the expected data: {expectedRow}")
    private void assertCsvDataRow(String csv, String expectedRow) {
        String[] lines = csv.split("\n");
        String actualDataRow = lines[lines.length - 1];
        assertEquals(expectedRow, actualDataRow, 
            "CSV data row format must match expected output");
    }

    // ==================== Repository Method Tests ====================
    @Test
    @DisplayName("Delegate: Get Expenses by Employee")
    @Severity(SeverityLevel.NORMAL)
    @Description("This test ensures that the service correctly delegates the call to the repository's 'findExpensesByUser' method when an employee ID is provided.")
    void testGetExpensesByEmployee() {
        int employeeId = 1;
        
        expenseService.getExpensesByEmployee(employeeId);
        
        verify(mockExpenseRepo, times(1)).findExpensesByUser(employeeId);
    }

    @Test
    @DisplayName("Delegate: Get Expenses by Category")
    @Severity(SeverityLevel.NORMAL)
    @Description("This test verifies that the service layer correctly triggers the repository search for expenses matching a specific category string.")
    void testGetExpensesByCategory() {
        String category = "Travel";
        
        expenseService.getExpensesByCategory(category);
        
        verify(mockExpenseRepo, times(1)).findExpensesByCategory(category);
    }

    @Test
    @DisplayName("Delegate: Get Expenses by Date Range")
    @Severity(SeverityLevel.NORMAL)
    @Description("This test validates that the service passes the start and end date strings directly to the repository's date-filtering query logic.")
    void testGetExpensesByDateRange() {
        String start = "2025-01-01";
        String end = "2025-01-31";
        
        expenseService.getExpensesByDateRange(start, end);
        
        verify(mockExpenseRepo, times(1)).findExpensesByDateRange(start, end);
    }

    @Test
    @DisplayName("Delegate: Get All Expenses")
    @Severity(SeverityLevel.NORMAL)
    @Description("This test confirms that when fetching all expenses, the service directly calls the repository's method without any intermediate processing.")
    void testGetAllExpenses() {
        expenseService.getAllExpenses();
        
        verify(mockExpenseRepo, times(1)).findAllExpensesWithUsers();
    }

    // ==================== Happy Path ====================
    @ParameterizedTest(name = "Scenario: {0}")
    @MethodSource("generateCsvReportHappyData")
    @DisplayName("Format: Valid Expense Record Conversion")
    @Severity(SeverityLevel.CRITICAL)
    @Description("This test checks the conversion of a fully populated ExpenseWithUser object into a single CSV row, ensuring correct ordering of fields and decimal precision.")
    void generateCsvReport_happyPath(
            String testDescription,
            ExpenseWithUser expenseWithUser,
            String expectedRow
    ) {
        // Given: An expense with valid data
        int expenseId = expenseWithUser.getExpense().getId();
        String employeeName = expenseWithUser.getUser().getUsername();
        double amount = expenseWithUser.getExpense().getAmount();
        
        // When: Generating CSV report
        String csv = generateCsvReportFromList(List.of(expenseWithUser));
        
        // Then: Verify CSV structure and content
        assertCsvHeader(csv);
        assertCsvDataRow(csv, expectedRow);
    }

    static Stream<Object[]> generateCsvReportHappyData() {
        return Stream.of(
                new Object[]{
                        "Standard approved expense with reviewer comment",
                        new ExpenseWithUser(
                                new Expense(1, 10, 100.0, "Travel", "2025-01-01"),
                                new User(10, "vu", "pass", "EMPLOYEE"),
                                new Approval(1, 1, "approved", 99, "OK", "2025-01-02")
                        ),
                        "1,vu,100.0,Travel,2025-01-01,approved,99,OK,2025-01-02"
                },
                new Object[]{
                        "Approved expense with decimal amount and approval details",
                        new ExpenseWithUser(
                                new Expense(2, 11, 50.5, "Lunch", "2025-01-05"),
                                new User(11, "alice", "pass", "EMPLOYEE"),
                                new Approval(2, 2, "approved", 88, "Approved", "2025-01-06")
                        ),
                        "2,alice,50.5,Lunch,2025-01-05,approved,88,Approved,2025-01-06"
                }
        );
    }

    // ==================== Sad Path - CSV Escaping ====================
    @ParameterizedTest(name = "Escaping Check: {0}")
    @MethodSource("generateCsvReportSadPathData")
    @DisplayName("Format: Special Character Escaping")
    @Severity(SeverityLevel.CRITICAL)
    @Description("This test ensures that the CSV generator properly handles 'problematic' characters like commas (which break columns), quotes (which break boundaries), and newlines (which break rows).")
    void generateCsvReport_sadPath_csvEscaping(
            String testDescription,
            ExpenseWithUser expenseWithUser,
            String expectedEscapedValue
    ) {
        // Given: An expense with special characters
        String description = expenseWithUser.getExpense().getDescription();
        
        // When: Generating CSV report
        String csv = generateCsvReportFromList(List.of(expenseWithUser));
        
        // Then: Verify special characters are properly escaped
        assertTrue(csv.contains(expectedEscapedValue), 
            "CSV should properly escape special characters in: " + description);
    }

    static Stream<Object[]> generateCsvReportSadPathData() {
        return Stream.of(
                new Object[]{
                        "Description with comma separator - must be quoted",
                        new ExpenseWithUser(
                                new Expense(3, 12, 75.0, "Taxi, Uber", "2025-02-01"),
                                new User(12, "bob", "pass", "EMPLOYEE"),
                                new Approval(3, 3, "approved", null, null, null)
                        ),
                        "\"Taxi, Uber\""
                },
                new Object[]{
                        "Description with embedded quotes - must be escaped",
                        new ExpenseWithUser(
                                new Expense(4, 13, 80.0, "Hotel \"Hilton\"", "2025-02-02"),
                                new User(13, "charlie", "pass", "EMPLOYEE"),
                                new Approval(4, 4, "approved", null, null, null)
                        ),
                        "\"Hotel \"\"Hilton\"\"\""
                },
                new Object[]{
                        "Description with newline - must be quoted",
                        new ExpenseWithUser(
                                new Expense(5, 14, 90.0, "Line\nBreak", "2025-02-03"),
                                new User(14, "david", "pass", "EMPLOYEE"),
                                new Approval(5, 5, "approved", null, null, null)
                        ),
                        "\"Line\nBreak\""
                }
        );
    }

    // ==================== Edge Cases - Null & Zero ====================
    @ParameterizedTest(name = "Edge Case: {0}")
    @MethodSource("generateCsvReportEdgeCasesData")
    @DisplayName("Format: Handling Nulls and Zeroes")
    @Severity(SeverityLevel.NORMAL)
    @Description("This test verifies that the service handles 'empty' data gracefully—converting null descriptions to empty strings and preserving zero values without crashing.")
    void generateCsvReport_edgeCases(
            String testDescription,
            ExpenseWithUser expenseWithUser,
            String expectedRow
    ) {
        // Given: An expense with null or zero values
        Double amount = expenseWithUser.getExpense().getAmount();
        String description = expenseWithUser.getExpense().getDescription();
        
        // When: Generating CSV report
        String csv = generateCsvReportFromList(List.of(expenseWithUser));
        
        // Then: Verify null/zero values are properly formatted
        assertCsvHeader(csv);
        assertCsvDataRow(csv, expectedRow);
    }

    static Stream<Object[]> generateCsvReportEdgeCasesData() {
        return Stream.of(
                new Object[]{
                        "Pending expense with zero amount and null description",
                        new ExpenseWithUser(
                                new Expense(6, 15, 0.0, null, "2025-03-01"),
                                new User(15, "emma", "pass", "EMPLOYEE"),
                                new Approval(6, 6, "pending", null, null, null)
                        ),
                        "6,emma,0.0,,2025-03-01,pending,,,"
                },
                new Object[]{
                        "Pending expense with zero amount and empty description",
                        new ExpenseWithUser(
                                new Expense(7, 16, 0.0, "", "2025-03-02"),
                                new User(16, "frank", "pass", "EMPLOYEE"),
                                new Approval(7, 7, "pending", null, null, null)
                        ),
                        "7,frank,0.0,,2025-03-02,pending,,,"
                }
        );
    }

    // ==================== Boundary Cases - Extreme Values ====================
    @ParameterizedTest(name = "Boundary: {0}")
    @MethodSource("generateCsvReportBoundaryData")
    @DisplayName("Format: Extreme Monetary Values")
    @Severity(SeverityLevel.NORMAL)
    @Description("This test confirms that the CSV generation maintains accuracy for very large numbers (millions) and the smallest possible positive currency increments.")
    void generateCsvReport_boundaryCases(
            String testDescription,
            ExpenseWithUser expenseWithUser,
            String expectedRow
    ) {
        // Given: An expense with boundary amount values
        double amount = expenseWithUser.getExpense().getAmount();
        
        // When: Generating CSV report
        String csv = generateCsvReportFromList(List.of(expenseWithUser));
        
        // Then: Verify boundary values are properly handled
        assertCsvHeader(csv);
        assertCsvDataRow(csv, expectedRow);
    }

    static Stream<Object[]> generateCsvReportBoundaryData() {
        return Stream.of(
                new Object[]{
                        "Maximum amount value (9,999,999.99)",
                        new ExpenseWithUser(
                                new Expense(8, 17, 9_999_999.99, "Max Amount", "2025-04-01"),
                                new User(17, "grace", "pass", "EMPLOYEE"),
                                new Approval(8, 8, "approved", 101, "Approved", "2025-04-02")
                        ),
                        "8,grace,9999999.99,Max Amount,2025-04-01,approved,101,Approved,2025-04-02"
                },
                new Object[]{
                        "Minimum amount value (0.01)",
                        new ExpenseWithUser(
                                new Expense(9, 18, 0.01, "Min Amount", "2025-04-03"),
                                new User(18, "henry", "pass", "EMPLOYEE"),
                                new Approval(9, 9, "approved", 102, "Approved", "2025-04-04")
                        ),
                        "9,henry,0.01,Min Amount,2025-04-03,approved,102,Approved,2025-04-04"
                }
        );
    }

    // ==================== CSV Header & Format Verification ====================
    @Test
    @DisplayName("Header: Verify CSV Column Titles")
    @Severity(SeverityLevel.BLOCKER)
    @Description("This test validates that the very first line of any generated report contains the exact column names required by the business specification.")
    void testCsvHeaderPresence() {
        // When: Generating CSV report with empty list
        String csv = generateCsvReportFromList(List.of());
        
        // Then: Verify header is present
        assertCsvHeader(csv);
    }

    @Test
    @DisplayName("Header: Empty Data Set Result")
    @Severity(SeverityLevel.NORMAL)
    @Description("This test ensures that when no data is found, the system still returns a valid CSV file containing only the header row, rather than an empty string or null.")
    void testCsvEmptyList() {
        // When: Generating CSV report with empty list
        String csv = expenseService.generateCsvReport(List.of());
        String[] lines = csv.split("\n");

        // Then: Verify only header is present
        assertEquals(1, lines.length, "Empty list should produce only header line");
        assertTrue(lines[0].contains("Expense ID"), "Header must contain 'Expense ID'");
    }
}
