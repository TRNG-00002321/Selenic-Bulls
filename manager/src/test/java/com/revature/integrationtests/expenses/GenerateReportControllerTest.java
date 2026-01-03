package com.revature.integrationtests.expenses;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class GenerateReportControllerTest {

    private static RequestSpecification unAuthSpec;
    private static RequestSpecification authSpec;

    @BeforeAll
    static void setup() {

        RestAssured.port = 5001;

        unAuthSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();

        // Login once to obtain JWT
        String jwtToken =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                              {
                                "username": "manager1",
                                "password": "password123"
                              }
                              """)
                        .when()
                        .post("/api/auth/login")
                        .then()
                        .statusCode(200)
                        .extract()
                        .cookie("jwt");

        authSpec = new RequestSpecBuilder()
                .setAccept(ContentType.JSON)
                .setContentType(ContentType.JSON)
                .addCookie("jwt", jwtToken)
                .build();
    }

    @AfterAll
    static void teardown() {
        given()
                .spec(authSpec)
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(anyOf(is(200), is(204)));

        RestAssured.reset();
    }

    // =========================
    // AUTHENTICATED TESTS
    // =========================

    @Test
    @DisplayName("Authenticated user can generate all expenses CSV")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies authenticated users can generate a CSV report containing all expenses.")
    void shouldGenerateAllExpensesCsvWhenAuthenticated() {

        Allure.step("Send authenticated request to generate all expenses CSV");
        given()
                .spec(authSpec)
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .contentType(containsString("text/csv"));

        Allure.step("Verify CSV content and response headers");
        given()
                .spec(authSpec)
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .body(containsString("Expense ID,Employee,Amount"))
                .header("Content-Disposition",
                        containsString("all_expenses_report.csv"));
    }

    @Test
    @DisplayName("Generate CSV empty list returns header only")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies CSV report generation returns only the header row when no expenses exist.")
    void shouldReturnHeaderOnlyWhenNoExpenses() {

        Allure.step("Request all expenses CSV when no data exists");
        given()
                .spec(authSpec)
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .body(startsWith("Expense ID,Employee,Amount"));
    }


    @Test
    @DisplayName("Authenticated user can generate employee CSV")
    void shouldGenerateEmployeeCsvWhenAuthenticated() {

        given()
                .spec(authSpec)
                .pathParam("employeeId", 1)
                .when()
                .get("/api/reports/expenses/employee/{employeeId}/csv")
                .then()
                .statusCode(200)
                .body(containsString("employee1"))
                .body(containsString("Pizza"))
                .body(containsString("Notebook"))
                .body(containsString("Hotel"))
                .contentType(containsString("text/csv"));
    }

    @Disabled("Requires mocked service to force 500 error; live DB returns 200 with empty CSV")
    @Test
    @DisplayName("Employee CSV service error - invalid employeeId")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies API returns 500 when a service-layer error occurs during employee CSV generation.")
    void shouldReturn500ForServiceError() {

        Allure.step("Send request for employee CSV with invalid employeeId");
        given()
                .spec(authSpec)
                .pathParam("employeeId", 999999)
                .when()
                .get("/api/reports/expenses/employee/{employeeId}/csv")
                .then()
                .statusCode(500);
    }

    @Test
    @DisplayName("Authenticated user can generate category CSV")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies authenticated users can generate a CSV report filtered by expense category.")
    void shouldGenerateCategoryCsvWhenAuthenticated() {

        Allure.step("Request category CSV report for category = book");
        given()
                .spec(authSpec)
                .pathParam("category", "book")
                .when()
                .get("/api/reports/expenses/category/{category}/csv")
                .then()
                .statusCode(200)
                .contentType(containsString("text/csv"))
                .body(containsString("25.01"));
    }

    @Test
    @DisplayName("Empty category returns 400")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies API returns 400 Bad Request when category parameter is empty.")
    void shouldReturn400ForEmptyCategory() {

        Allure.step("Request category CSV with empty category parameter");
        given()
                .spec(authSpec)
                .pathParam("category", " ")
                .when()
                .get("/api/reports/expenses/category/{category}/csv")
                .then()
                .statusCode(400)
                .body(containsString("parameter is required"));
    }

    @Test
    @DisplayName("Authenticated user can generate date range CSV")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies authenticated users can generate a CSV report filtered by a given date range.")
    void shouldGenerateDateRangeCsvWhenAuthenticated() {

        Allure.step("Request date-range CSV report with startDate and endDate");
        given()
                .spec(authSpec)
                .queryParam("startDate", "2025-01-01")
                .queryParam("endDate", "2025-12-31")
                .when()
                .get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(200)
                .contentType(containsString("text/csv"))
                .body(containsString("employee1"))
                .body(containsString("Pizza"))
                .body(containsString("Notebook"))
                .body(containsString("Hotel"));
    }

    @Test
    @DisplayName("Missing startDate returns 400")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies API returns 400 Bad Request when startDate query parameter is missing.")
    void shouldReturn400WhenStartDateMissing() {

        Allure.step("Request date-range CSV without startDate parameter");
        given()
                .spec(authSpec)
                .queryParam("endDate", "2025-12-31")
                .when()
                .get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(400)
                .body(containsString("startDate and endDate query parameters are required"));
    }

    @Test
    @DisplayName("Invalid date format returns 400")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies API returns 400 Bad Request when date parameters use an invalid format.")
    void shouldReturn400ForInvalidDateFormat() {

        Allure.step("Request date-range CSV with invalid startDate format");
        given()
                .spec(authSpec)
                .queryParam("startDate", "01/01/2025")
                .queryParam("endDate", "2025-12-31")
                .when()
                .get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(400)
                .body(containsString("Invalid date format"));
    }

    @Test
    @DisplayName("Authenticated user can generate pending CSV")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies authenticated users can generate a CSV report containing only pending expenses.")
    void shouldGeneratePendingCsvWhenAuthenticated() {

        Allure.step("Request pending expenses CSV report");
        given()
                .spec(authSpec)
                .when()
                .get("/api/reports/expenses/pending/csv")
                .then()
                .statusCode(200)
                .contentType(containsString("text/csv"))
                .body(containsString("pending"))
                .body(not(containsString("approved")))
                .body(not(containsString("denied")));
    }
    
    // =========================
    // UNAUTHENTICATED TESTS
    // =========================

    @Test
    @DisplayName("Unauthenticated user cannot access expenses CSV")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies API rejects unauthenticated requests when accessing the expenses CSV report.")
    void shouldRejectExpensesCsvWhenUnauthenticated() {

        Allure.step("Send request for expenses CSV without authentication");
        given()
                .spec(unAuthSpec)
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(401)
                .body(containsString("Authentication required"));
    }
}
