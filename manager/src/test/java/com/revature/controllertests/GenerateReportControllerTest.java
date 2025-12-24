package com.revature.controllertests;

import com.revature.api.ReportController;
import com.revature.service.ExpenseService;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;

public class GenerateReportControllerTest {

    private static Javalin app;
    private static AutoCloseable closeable;

    private static ExpenseService mockService;

    private static ReportController controller;

    @BeforeAll
    public static void setup() {
        // Manually initialize to ensure static fields are not null
        mockService = mock(ExpenseService.class);
        controller = new ReportController(mockService);
        
        // Open mocks for any other potential Mockito features
        closeable = MockitoAnnotations.openMocks(GenerateReportControllerTest.class);

        app = Javalin.create();

        // Routing for all report types using the initialized controller
        app.get("/api/reports/expenses/csv", controller::generateAllExpensesReport);
        app.get("/api/reports/expenses/employee/{employeeId}/csv", controller::generateEmployeeExpensesReport);
        app.get("/api/reports/expenses/category/{category}/csv", controller::generateCategoryExpensesReport);
        app.get("/api/reports/expenses/daterange/csv", controller::generateDateRangeExpensesReport);
        app.get("/api/reports/expenses/pending/csv", controller::generatePendingExpensesReport);

        app.start(8081);
        RestAssured.baseURI = "http://localhost:8081";
    }

    @BeforeEach
    public void resetMocks() {
        // Clear stubs between tests to prevent "DB Down" leakage
        reset(mockService);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        app.stop();
        if (closeable != null) {
            closeable.close();
        }
    }

    // 1. All Expenses Report
    @Test
    void testAllExpensesHappyPath() {
        when(mockService.getAllExpenses()).thenReturn(new ArrayList<>());
        when(mockService.generateCsvReport(any())).thenReturn("id,name\n1,test");

        given().when().get("/api/reports/expenses/csv")
                .then().statusCode(200)
                .contentType("text/csv")
                .header("Content-Disposition", containsString("all_expenses_report.csv"));
    }

    // 2. Employee Report (Happy & Sad)
    @Test
    void testEmployeeReportHappyPath() {
        when(mockService.getExpensesByEmployee(1)).thenReturn(new ArrayList<>());
        when(mockService.generateCsvReport(any())).thenReturn("csv");

        given().pathParam("employeeId", 1)
                .when().get("/api/reports/expenses/employee/{employeeId}/csv")
                .then().statusCode(200);
    }

    @Test
    void testEmployeeReportInvalidId() {
        // Controller wraps NumberFormatException in InternalServerErrorResponse (500)
        given().pathParam("employeeId", "not-an-id")
                .when().get("/api/reports/expenses/employee/{employeeId}/csv")
                .then().statusCode(500);
    }

    // 3. Category Report (Happy & Sad)
    @Test
    void testCategoryReportHappyPath() {
        when(mockService.getExpensesByCategory("Travel")).thenReturn(new ArrayList<>());
        when(mockService.generateCsvReport(any())).thenReturn("csv");

        given().pathParam("category", "Travel")
                .when().get("/api/reports/expenses/category/{category}/csv")
                .then().statusCode(200)
                .header("Content-Disposition", containsString("category_Travel"));
    }

    @Test
    void testCategoryReportMissingParam() {
        // Tests the if(category == null || category.trim().isEmpty()) block
        given().pathParam("category", " ")
                .when().get("/api/reports/expenses/category/{category}/csv")
                .then().statusCode(400);
    }

    // 4. Date Range Report (Happy & Sad)
    @Test
    void testDateRangeHappyPath() {
        String start = "2025-01-01";
        String end = "2025-01-31";
        when(mockService.getExpensesByDateRange(start, end)).thenReturn(new ArrayList<>());
        when(mockService.generateCsvReport(any())).thenReturn("csv");

        given().queryParam("startDate", start).queryParam("endDate", end)
                .when().get("/api/reports/expenses/daterange/csv")
                .then().statusCode(200);
    }

    @Test
    void testDateRangeInvalidFormat() {
        // Tests the internal try-catch(DateTimeParseException)
        given().queryParam("startDate", "01-01-2025").queryParam("endDate", "2025-01-31")
                .when().get("/api/reports/expenses/daterange/csv")
                .then().statusCode(400)
                .body(containsString("Invalid date format"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"01-01-2025", "2025/01/01", "tomorrow", "2025-13-01"})
    @DisplayName("Date Range - Invalid date formats should return 400")
    void testInvalidDateFormats(String badDate) {
        given()
            .queryParam("startDate", badDate)
            .queryParam("endDate", "2025-01-01")
        .when()
            .get("/api/reports/expenses/daterange/csv")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Category filename sanitation - should replace special chars with underscores")
    void testCategoryFilenameSanitation() {
        // Edge Case: Category with spaces and special symbols
        String category = "Travel & Food!";
        when(mockService.getExpensesByCategory(anyString())).thenReturn(new ArrayList<>());
        when(mockService.generateCsvReport(any())).thenReturn("csv");

        given()
            .pathParam("category", category)
        .when()
            .get("/api/reports/expenses/category/{category}/csv")
        .then()
            .statusCode(200)
            // Expecting: category_Travel___Food__expenses_report.csv
            .header("Content-Disposition", containsString("category_Travel___Food_"));
    }

    @Test
    @DisplayName("Date Range - Boundary Dates")
    void testDateRangeBoundaryDates() {
        // Boundary Case: Leap year date or year transition
        String start = "2024-02-29"; 
        String end = "2024-03-01";
        when(mockService.getExpensesByDateRange(start, end)).thenReturn(new ArrayList<>());
        when(mockService.generateCsvReport(any())).thenReturn("csv");

        given()
            .queryParam("startDate", start)
            .queryParam("endDate", end)
        .when()
            .get("/api/reports/expenses/daterange/csv")
        .then()
            .statusCode(200);
    }

    // 5. Pending Report Happy Path
    @Test
    void testPendingReportHappyPath() {
        when(mockService.getPendingExpenses()).thenReturn(new ArrayList<>());
        when(mockService.generateCsvReport(any())).thenReturn("csv");

        given().when().get("/api/reports/expenses/pending/csv")
                .then().statusCode(200);
    }

    // 6. Generic Error Handling (Internal Server Error)
    @Test
    void testInternalServerError() {
        // Tests the catch(Exception e) block in generateAllExpensesReport
        when(mockService.getAllExpenses()).thenThrow(new RuntimeException("DB Down"));

        given().when().get("/api/reports/expenses/csv")
                .then().statusCode(500)
                .body(containsString("Failed to generate expenses report"));
    }
}
