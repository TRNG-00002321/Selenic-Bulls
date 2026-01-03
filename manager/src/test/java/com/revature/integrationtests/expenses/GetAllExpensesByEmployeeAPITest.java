package com.revature.integrationtests.expenses;

import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import io.javalin.http.ContentType;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import static org.hamcrest.Matchers.*;

// Allure Report Annotations
import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;

/* 
This class will test retrieving all expenses filtered by employee.
*/
//TODO: JUnit assertions for validating response data list of all expenses for a specific employee
@Epic("Expense Management System")
@Feature("Employee-Specific Expense Reports")
@Story("As a manager, I want to view a list of all approved, denied, and pending expenses for a particular employee so that I can view them efficiently")
public class GetAllExpensesByEmployeeAPITest {
    // Shared specifications for reusability
    private static RequestSpecification unAuthRequestSpec;
    private static RequestSpecification authRequestSpec;
    private static ResponseSpecification successResponseSpec;
    
    // Employee ID test data constants
    private static final int VALID_EMPLOYEE_ID = 1;
    private static final int NON_EXISTENT_EMPLOYEE_ID = 999999;
    private static final int NEGATIVE_EMPLOYEE_ID = -1;
    private static final int ZERO_EMPLOYEE_ID = 0;
    private static final int MAX_INTEGER_ID = Integer.MAX_VALUE;

    @BeforeEach
    void setup() {
        //Default Base URI for RESTAssured is http://localhost so it doesnt need to be defined

        //Default Port for RESTAssured is 8080 so we need to set it to 5001
        RestAssured.port = 5001;

        // Request Specification for uniform request settings
        unAuthRequestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();

        // Login as manager to get authentication cookie
        // A cookie is a small text file that the server/browser stores to
        // remember information about the user session, like authentication state
        // The cookie holds a session token (JWT) that the server uses to identify the user
        // Allows you to send it with each request so the server recognizes you as logged in
        // without needing your password each time
        String sessionCookie = 
            given()
                .spec(unAuthRequestSpec)
                .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
            .when()
                .post("/api/auth/login")
            .then()
                .statusCode(200)
                .extract()
                .cookie("jwt");
        
        // Authenticated Request Specification uses unAuthRequestSpec above with session cookie included (JWT)
        authRequestSpec = new RequestSpecBuilder()
                .addRequestSpecification(unAuthRequestSpec)
                .addCookie("jwt", sessionCookie)
                .build();
        
        // Response Specification for uniform successful responses
        successResponseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .expectResponseTime(lessThan(10000L))
                .build();
    }
     
    @AfterEach
    void tearDown() {
        // Logout to clean up session
        given()
            .spec(authRequestSpec)
        .when()
            .post("/api/auth/logout")
        .then()
            .statusCode(200);
            
        RestAssured.reset();
    }
    
    @Test
    @DisplayName("Happy Path: Successfully retrieve expenses for valid employee")
    @Description("Verify that authenticated managers can successfully retrieve all expenses for a specific employee ID")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpensesByEmployeeSuccess() {
        Allure.step("Arrange: Prepare authenticated request for employee expenses endpoint with valid employee ID: " + VALID_EMPLOYEE_ID);
        Allure.addAttachment("Test Scenario", "Happy Path - Successfully retrieve expenses for valid employee");
        Allure.addAttachment("Employee ID", String.valueOf(VALID_EMPLOYEE_ID));
        
        Allure.step("Act: Send authenticated GET request to /api/expenses/employee/" + VALID_EMPLOYEE_ID);
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", VALID_EMPLOYEE_ID)
        .then()
            .spec(successResponseSpec)
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("data", isA(List.class))
            .body("count", greaterThanOrEqualTo(0))
            // Fix the count to be equal to the size of the data list instead of hardcoded value
            .body("count", equalTo(3))
            .body("employeeId", equalTo(VALID_EMPLOYEE_ID));
        
        Allure.step("Assert: Validated successful response with employee-specific expenses data");
    }
    
    @Test
    @DisplayName("Sad Path: Unauthorized access without authentication token")
    @Description("Verify that requests without authentication tokens are properly rejected with appropriate error messages")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpensesByEmployeeUnauthorized() {
        Allure.step("Arrange: Prepare unauthenticated request for employee expenses endpoint");
        Allure.addAttachment("Test Scenario", "Sad Path - Unauthorized access without authentication token");
        
        Allure.step("Act: Send unauthenticated GET request to /api/expenses/employee/" + VALID_EMPLOYEE_ID);
        given()
            .spec(unAuthRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", VALID_EMPLOYEE_ID)
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403)))
            .time(lessThan(10000L))
            .body("title", containsString("Authentication required"));
        
        Allure.step("Assert: Validated unauthorized access rejection with proper error message");
    }
    
    @Test
    @DisplayName("Sad Path: Invalid employee ID format - non-numeric")
    @Description("Verify that non-numeric employee IDs are handled gracefully with appropriate error responses")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByEmployeeInvalidIdFormat() {
        Allure.step("Arrange: Prepare authenticated request with non-numeric employee ID");
        Allure.addAttachment("Test Scenario", "Sad Path - Invalid employee ID format (non-numeric)");
        Allure.addAttachment("Invalid Employee ID", "invalid_id");
        
        Allure.step("Act: Send authenticated GET request with non-numeric employee ID");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", "invalid_id")
        .then()
            .statusCode(500)
            .time(lessThan(10000L))
            .body("title", containsString("Failed to retrieve expenses for employee"));
        
        Allure.step("Assert: Validated proper error handling for non-numeric employee ID");
    }
    
    @Test
    @DisplayName("Sad Path: Non-existent employee ID")
    @Description("Verify that requests for non-existent employee IDs return empty results rather than errors")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByEmployeeNonExistentEmployee() {
        Allure.step("Arrange: Prepare authenticated request with non-existent employee ID: " + NON_EXISTENT_EMPLOYEE_ID);
        Allure.addAttachment("Test Scenario", "Sad Path - Non-existent employee ID");
        Allure.addAttachment("Non-existent Employee ID", String.valueOf(NON_EXISTENT_EMPLOYEE_ID));
        
        Allure.step("Act: Send authenticated GET request with non-existent employee ID");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", NON_EXISTENT_EMPLOYEE_ID)
        .then()
            .statusCode(200) // Should return empty list, not error
            .spec(successResponseSpec)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("data", isA(List.class))
            .body("count", equalTo(0))
            .body("employeeId", equalTo(NON_EXISTENT_EMPLOYEE_ID));
        
        Allure.step("Assert: Validated empty results for non-existent employee ID");
    }

    @Test
    @DisplayName("Sad Test: Invalid session token")
    @Description("Verify that requests with invalid session tokens are properly rejected")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByEmployeeInvalidSession() {
        Allure.step("Arrange: Prepare request with invalid session token");
        Allure.addAttachment("Test Scenario", "Sad Path - Invalid session token");
        Allure.addAttachment("Invalid Token", "invalid_session_12345");
        
        Allure.step("Act: Send request with invalid session token");
        given()
            .spec(unAuthRequestSpec)
            .cookie("jwt", "invalid_session_12345")
        .when()
            .get("/api/expenses/employee/{employeeId}", VALID_EMPLOYEE_ID)
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403)))
            .time(lessThan(10000L))
            .body("title", containsString("Authentication required"));
        
        Allure.step("Assert: Validated rejection of invalid session token");
    }
    
    @Test
    @DisplayName("Edge Case: Negative employee ID")
    @Description("Verify that negative employee IDs are handled appropriately by the API")
    @Severity(SeverityLevel.MINOR)
    void testGetExpensesByEmployeeNegativeId() {
        Allure.step("Arrange: Prepare authenticated request with negative employee ID: " + NEGATIVE_EMPLOYEE_ID);
        Allure.addAttachment("Test Scenario", "Edge Case - Negative employee ID");
        Allure.addAttachment("Negative Employee ID", String.valueOf(NEGATIVE_EMPLOYEE_ID));
        
        Allure.step("Act: Send authenticated GET request with negative employee ID");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", NEGATIVE_EMPLOYEE_ID)
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(400)))
            .spec(successResponseSpec)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("data", isA(List.class))
            .body("count", equalTo(0))
            .body("employeeId", equalTo(NEGATIVE_EMPLOYEE_ID));
        
        Allure.step("Assert: Validated handling of negative employee ID");
    }
    
    @Test
    @DisplayName("Edge Case: Zero employee ID")
    @Description("Verify that zero employee ID is handled appropriately by the API")
    @Severity(SeverityLevel.MINOR)
    void testGetExpensesByEmployeeZeroId() {
        Allure.step("Arrange: Prepare authenticated request with zero employee ID: " + ZERO_EMPLOYEE_ID);
        Allure.addAttachment("Test Scenario", "Edge Case - Zero employee ID");
        Allure.addAttachment("Zero Employee ID", String.valueOf(ZERO_EMPLOYEE_ID));
        
        Allure.step("Act: Send authenticated GET request with zero employee ID");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", ZERO_EMPLOYEE_ID)
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(400)))
            .spec(successResponseSpec)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("data", isA(List.class))
            .body("count", equalTo(0))
            .body("employeeId", equalTo(ZERO_EMPLOYEE_ID));
        
        Allure.step("Assert: Validated handling of zero employee ID");
    }

    @Test
    @DisplayName("Edge Case: Special characters in employee ID path")
    @Description("Verify that special characters in employee ID path are handled gracefully")
    @Severity(SeverityLevel.MINOR)
    void testGetExpensesByEmployeeSpecialCharacters() {
        Allure.step("Arrange: Prepare authenticated request with special characters in employee ID path");
        Allure.addAttachment("Test Scenario", "Edge Case - Special characters in employee ID path");
        Allure.addAttachment("Special Character ID", "1@#$");
        
        Allure.step("Act: Send authenticated GET request with special characters in employee ID");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", "1@#$")
        .then()
            .statusCode(500)
            .time(lessThan(10000L))
            .body("title", containsString("Failed to retrieve expenses for employee"));
        
        Allure.step("Assert: Validated proper error handling for special characters in employee ID");
    }

    @Test
    @DisplayName("Edge Case: Full response structure validation")
    @Description("Verify that the API response contains all required fields with correct data types")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByEmployeeResponseStructure() {
        Allure.step("Arrange: Prepare authenticated request for response structure validation");
        Allure.addAttachment("Test Scenario", "Edge Case - Full response structure validation");
        
        Allure.step("Act: Send authenticated GET request to validate response structure");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", VALID_EMPLOYEE_ID)
        .then()
            .spec(successResponseSpec)
            .statusCode(200)
            .body("success", notNullValue())
            .body("success", isA(Boolean.class))
            .body("data", notNullValue())
            .body("data", isA(List.class))
            .body("count", notNullValue())
            .body("count", isA(Integer.class))
            .body("employeeId", notNullValue())
            .body("employeeId", isA(Integer.class));
        
        Allure.step("Assert: Validated all required fields present with correct data types");
    }
    
    @Test
    @DisplayName("Boundary Test: Maximum integer employee ID")
    @Description("Verify that the API can handle maximum integer values for employee ID")
    @Severity(SeverityLevel.MINOR)
    void testGetExpensesByEmployeeMaxIntegerId() {
        Allure.step("Arrange: Prepare authenticated request with maximum integer employee ID: " + MAX_INTEGER_ID);
        Allure.addAttachment("Test Scenario", "Boundary Test - Maximum integer employee ID");
        Allure.addAttachment("Max Integer ID", String.valueOf(MAX_INTEGER_ID));
        
        Allure.step("Act: Send authenticated GET request with maximum integer employee ID");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", MAX_INTEGER_ID)
        .then()
            .statusCode(200)
            .spec(successResponseSpec)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("count", greaterThanOrEqualTo(0))
            .body("employeeId", equalTo(MAX_INTEGER_ID));
        
        Allure.step("Assert: Validated handling of maximum integer employee ID");
    }
     
    @Test
    @DisplayName("Boundary Test: Empty employee expenses list")
    @Description("Verify that the API handles empty expense lists for employees gracefully")
    @Severity(SeverityLevel.MINOR)
    @Flaky
    void testGetExpensesByEmployeeEmptyExpensesList() {
        Allure.step("Arrange: Prepare authenticated request for empty employee expenses scenario");
        Allure.addAttachment("Test Scenario", "Boundary Test - Empty employee expenses list");
        Allure.addAttachment("Warning", "This test is expected to fail until database is cleared of expenses for employee ID: " + VALID_EMPLOYEE_ID);
        
        //This test is expected to fail until the database is cleared of all expenses for the employee
        Allure.step("Act: Send authenticated GET request expecting empty employee expenses list");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", VALID_EMPLOYEE_ID)
        .then()
            .statusCode(200)
            .spec(successResponseSpec)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("data", hasSize(0))
            .body("count", equalTo(0))
            .body("employeeId", equalTo(VALID_EMPLOYEE_ID));
        
        Allure.step("Assert: Validated graceful handling of empty employee expenses list");
    }
}
