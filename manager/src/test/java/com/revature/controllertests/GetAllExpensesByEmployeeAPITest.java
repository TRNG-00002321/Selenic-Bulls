package com.revature.controllertests;

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
    }
    
    @Test
    @DisplayName("Sad Path: Unauthorized access without authentication token")
    @Description("Verify that requests without authentication tokens are properly rejected with appropriate error messages")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpensesByEmployeeUnauthorized() {
        given()
            .spec(unAuthRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", VALID_EMPLOYEE_ID)
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403)))
            .time(lessThan(10000L))
            .body("title", containsString("Authentication required"));
    }
    
    @Test
    @DisplayName("Sad Path: Invalid employee ID format - non-numeric")
    @Description("Verify that non-numeric employee IDs are handled gracefully with appropriate error responses")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByEmployeeInvalidIdFormat() {
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", "invalid_id")
        .then()
            .statusCode(500)
            .time(lessThan(10000L))
            .body("title", containsString("Failed to retrieve expenses for employee"));
    }
    
    @Test
    @DisplayName("Sad Path: Non-existent employee ID")
    @Description("Verify that requests for non-existent employee IDs return empty results rather than errors")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByEmployeeNonExistentEmployee() {
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
    }

    @Test
    @DisplayName("Sad Test: Invalid session token")
    @Description("Verify that requests with invalid session tokens are properly rejected")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByEmployeeInvalidSession() {
        given()
            .spec(unAuthRequestSpec)
            .cookie("jwt", "invalid_session_12345")
        .when()
            .get("/api/expenses/employee/{employeeId}", VALID_EMPLOYEE_ID)
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403)))
            .time(lessThan(10000L))
            .body("title", containsString("Authentication required"));
    }
    
    @Test
    @DisplayName("Edge Case: Negative employee ID")
    @Description("Verify that negative employee IDs are handled appropriately by the API")
    @Severity(SeverityLevel.MINOR)
    void testGetExpensesByEmployeeNegativeId() {
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
    }
    
    @Test
    @DisplayName("Edge Case: Zero employee ID")
    @Description("Verify that zero employee ID is handled appropriately by the API")
    @Severity(SeverityLevel.MINOR)
    void testGetExpensesByEmployeeZeroId() {
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
    }

    @Test
    @DisplayName("Edge Case: Special characters in employee ID path")
    @Description("Verify that special characters in employee ID path are handled gracefully")
    @Severity(SeverityLevel.MINOR)
    void testGetExpensesByEmployeeSpecialCharacters() {
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/employee/{employeeId}", "1@#$")
        .then()
            .statusCode(500)
            .time(lessThan(10000L))
            .body("title", containsString("Failed to retrieve expenses for employee"));
    }

    @Test
    @DisplayName("Edge Case: Full response structure validation")
    @Description("Verify that the API response contains all required fields with correct data types")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByEmployeeResponseStructure() {
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
    }
    
    @Test
    @DisplayName("Boundary Test: Maximum integer employee ID")
    @Description("Verify that the API can handle maximum integer values for employee ID")
    @Severity(SeverityLevel.MINOR)
    void testGetExpensesByEmployeeMaxIntegerId() {
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
    }
     
    @Test
    @DisplayName("Boundary Test: Empty employee expenses list")
    @Description("Verify that the API handles empty expense lists for employees gracefully")
    @Severity(SeverityLevel.MINOR)
    @Flaky
    void testGetExpensesByEmployeeEmptyExpensesList() {
        //This test is expected to fail until the database is cleared of all expenses for the employee
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
    }
}
