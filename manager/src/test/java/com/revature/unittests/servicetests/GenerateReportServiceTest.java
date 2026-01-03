package com.revature.unittests.servicetests;

import com.revature.repository.*;
import com.revature.service.ExpenseService;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
public class GenerateReportServiceTest {

    @InjectMocks
    private ExpenseService expenseService;

    @Mock
    private ExpenseRepository mockExpenseRepo;

    // =========================
    // Helper Methods
    // =========================

    private String generateCsvReportFromList(List<ExpenseWithUser> expenses) {
        return expenseService.generateCsvReport(expenses);
    }

    private void assertCsvHeader(String csv) {
        String expectedHeader =
                "Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date";
        String actualHeader = csv.split("\n")[0].trim();
        assertEquals(expectedHeader, actualHeader);
    }

    private void assertCsvDataRow(String csv, String expectedRow) {
        String[] lines = csv.split("\n");
        String actualRow = lines[lines.length - 1];
        assertEquals(expectedRow, actualRow);
    }

    // =========================
    // Delegation Tests
    // =========================

    @Test
    @DisplayName("Delegate get expenses by employee")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies service delegates employee expense lookup to repository.")
    void testGetExpensesByEmployee() {

        Allure.step("Call getExpensesByEmployee");
        expenseService.getExpensesByEmployee(1);

        Allure.step("Verify repository method is invoked");
        verify(mockExpenseRepo).findExpensesByUser(1);
        }

    @Test
    @DisplayName("Delegate get expenses by category")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies service delegates category-based expense lookup to repository.")
    void testGetExpensesByCategory() {

        Allure.step("Call getExpensesByCategory with category = Travel");
        expenseService.getExpensesByCategory("Travel");

        Allure.step("Verify repository method is invoked");
        verify(mockExpenseRepo).findExpensesByCategory("Travel");
    }

    @Test
    @DisplayName("Delegate get expenses by date range")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies service delegates date-range expense lookup to repository.")
    void testGetExpensesByDateRange() {

        Allure.step("Call getExpensesByDateRange with start and end dates");
        expenseService.getExpensesByDateRange("2025-01-01", "2025-01-31");

        Allure.step("Verify repository method is invoked");
        verify(mockExpenseRepo).findExpensesByDateRange("2025-01-01", "2025-01-31");
    }

    @Test
    @DisplayName("Delegate get all expenses")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies service delegates retrieval of all expenses to repository.")
    void testGetAllExpenses() {

        Allure.step("Call getAllExpenses");
        expenseService.getAllExpenses();

        Allure.step("Verify repository method is invoked");
        verify(mockExpenseRepo).findAllExpensesWithUsers();
    }

    // ==================== Happy Path ====================

    @ParameterizedTest(name = "Scenario: {0}")
    @MethodSource("generateCsvReportHappyData")
    @DisplayName("Format: Valid Expense Record Conversion")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies CSV generation for valid expense records.")
    void generateCsvReport_happyPath(
            String testDescription,
            ExpenseWithUser expenseWithUser,
            String expectedRow
    ) {
        Allure.step("Generate CSV report for scenario: " + testDescription);
        String csv = generateCsvReportFromList(List.of(expenseWithUser));

        Allure.step("Verify CSV header");
        assertCsvHeader(csv);

        Allure.step("Verify CSV data row matches expected output");
        assertCsvDataRow(csv, expectedRow);
    }

    static Stream<Arguments> generateCsvReportHappyData() {
        return Stream.of(
                Arguments.of(
                        "Standard approved expense with reviewer comment",
                        new ExpenseWithUser(
                                new Expense(1, 10, 100.0, "Travel", "2025-01-01"),
                                new User(10, "vu", "pass", "EMPLOYEE"),
                                new Approval(1, 1, "approved", 99, "OK", "2025-01-02")
                        ),
                        "1,vu,100.0,Travel,2025-01-01,approved,99,OK,2025-01-02"
                ),
                Arguments.of(
                        "Approved expense with decimal amount",
                        new ExpenseWithUser(
                                new Expense(2, 11, 50.5, "Lunch", "2025-01-05"),
                                new User(11, "alice", "pass", "EMPLOYEE"),
                                new Approval(2, 2, "approved", 88, "Approved", "2025-01-06")
                        ),
                        "2,alice,50.5,Lunch,2025-01-05,approved,88,Approved,2025-01-06"
                )
        );
    }

    // ==================== Sad Path ====================

    @ParameterizedTest(name = "Escaping Check: {0}")
    @MethodSource("generateCsvReportSadPathData")
    @DisplayName("Format: Special Character Escaping")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies CSV generation properly escapes special characters such as commas, quotes, and newlines.")
    void generateCsvReport_sadPath_csvEscaping(
            String testDescription,
            ExpenseWithUser expenseWithUser,
            String expectedEscapedValue
    ) {
        Allure.step("Generate CSV report for scenario: " + testDescription);
        String csv = generateCsvReportFromList(List.of(expenseWithUser));

        Allure.step("Verify CSV contains escaped value");
        assertTrue(csv.contains(expectedEscapedValue));
    }

    static Stream<Arguments> generateCsvReportSadPathData() {
        return Stream.of(
                Arguments.of(
                        "Description with comma",
                        new ExpenseWithUser(
                                new Expense(3, 12, 75.0, "Taxi, Uber", "2025-02-01"),
                                new User(12, "bob", "pass", "EMPLOYEE"),
                                new Approval(3, 3, "approved", null, null, null)
                        ),
                        "\"Taxi, Uber\""
                ),
                Arguments.of(
                        "Description with quotes",
                        new ExpenseWithUser(
                                new Expense(4, 13, 80.0, "Hotel \"Hilton\"", "2025-02-02"),
                                new User(13, "charlie", "pass", "EMPLOYEE"),
                                new Approval(4, 4, "approved", null, null, null)
                        ),
                        "\"Hotel \"\"Hilton\"\"\""
                )
        );
    }

    // ==================== Edge Cases ====================

    @ParameterizedTest(name = "Edge Case: {0}")
    @MethodSource("generateCsvReportEdgeCasesData")
    @DisplayName("Format: Handling Nulls and Zeroes")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies CSV generation handles null values and zero amounts correctly.")
    void generateCsvReport_edgeCases(
            String testDescription,
            ExpenseWithUser expenseWithUser,
            String expectedRow
    ) {
        Allure.step("Generate CSV report for edge case: " + testDescription);
        String csv = generateCsvReportFromList(List.of(expenseWithUser));

        Allure.step("Verify CSV header");
        assertCsvHeader(csv);

        Allure.step("Verify CSV data row matches expected output");
        assertCsvDataRow(csv, expectedRow);
    }

    static Stream<Arguments> generateCsvReportEdgeCasesData() {
        return Stream.of(
                Arguments.of(
                        "Zero amount, null description",
                        new ExpenseWithUser(
                                new Expense(6, 15, 0.0, null, "2025-03-01"),
                                new User(15, "emma", "pass", "EMPLOYEE"),
                                new Approval(6, 6, "pending", null, null, null)
                        ),
                        "6,emma,0.0,,2025-03-01,pending,,,"
                )
        );
    }

    // ==================== Header Validation ====================

    @Test
    @DisplayName("CSV header always present")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verifies CSV output always includes a header even when no expense data is provided.")
    void testCsvHeaderPresence() {

        Allure.step("Generate CSV report with empty expense list");
        String csv = generateCsvReportFromList(List.of());

        Allure.step("Verify CSV header is present");
        assertCsvHeader(csv);
    }

    @Test
    @DisplayName("Empty list returns header only")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies CSV generation with an empty expense list returns only the header row.")
    void testCsvEmptyList() {

        Allure.step("Generate CSV report with empty expense list");
        String csv = expenseService.generateCsvReport(List.of());

        Allure.step("Verify CSV contains only one line (header)");
        assertEquals(1, csv.split("\n").length);
    }
}
