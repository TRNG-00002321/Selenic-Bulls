package com.revature.integrationtests.expenses;

import static org.hamcrest.Matchers.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import io.javalin.http.ContentType;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.*;

// Allure Report Annotations
import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;

/*
This class will test retrieving all expenses.
*/
//TODO: JUnit assertions for validating response data list of all expenses
@Epic("Expense Management System")
@Feature("Manager Expense Overview")
@Story("As a manager, I want to view a list of all approved, denied, and pending expenses so that I can view them efficiently")
public class GetAllExpensesAPITest {
    // Shared specifications for reusability
    private static RequestSpecification unAuthRequestSpec;
    private static RequestSpecification authRequestSpec;
    private static ResponseSpecification successResponseSpec;

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
    @DisplayName("Happy Path: Successfully retrieve all expenses")
    @Description("Verify that authenticated managers can successfully retrieve all expenses regardless of status")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesSuccess() {
        Allure.step("Arrange: Prepare authenticated request for all expenses endpoint");
        Allure.addAttachment("Test Scenario", "Happy Path - Successfully retrieve all expenses");
        
        Allure.step("Act: Send authenticated GET request to /api/expenses");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses")
        .then()
            .spec(successResponseSpec)
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("data", isA(List.class))
            .body("count", greaterThanOrEqualTo(0))
            .body("count", equalTo(3));
        
        Allure.step("Assert: Validated successful response with all expenses data");
    }
    
    @Test
    @DisplayName("Sad Path: Unauthorized access without authentication token")
    @Description("Verify that requests without authentication tokens are properly rejected. NOTE: This endpoint currently has a security vulnerability bug")
    @Severity(SeverityLevel.BLOCKER)
    void testGetAllExpensesUnauthorized() {
        Allure.step("Arrange: Document security vulnerability in /api/expenses endpoint");
        Allure.addAttachment("Security Issue", "This endpoint is publicly accessible without authentication - CRITICAL VULNERABILITY");
        Allure.addAttachment("Risk Assessment", "Anyone can retrieve all company expenses (pending/approved/denied) without authentication");
        
        // There is a bug in the  project.
        // This endpoint is currently publicly accessible and does not 
        // require authentication, even though it should. Users can successfully 
        // call it without being logged in, meaning authorization checks are not enforced. 
        // As a result, anyone with the URL can retrieve all company expenses (pending, approved, or denied).
        // This is a security risk as sensitive financial data is exposed to unauthorized users.
        // This test is expected to fail until the bug is fixed.
        Allure.step("Act: Send unauthenticated request to demonstrate security vulnerability");
        given()
            .spec(unAuthRequestSpec)
        .when()
            .get("/api/expenses")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403)))
            .time(lessThan(10000L))
            .body("title", containsString("Authentication required"));
        
        Allure.step("Assert: Expected failure - endpoint should require authentication but doesn't");
    }
    
    @Test
    @DisplayName("Sad Path: Invalid authentication token")
    @Description("Verify that requests with invalid authentication tokens are properly rejected. NOTE: This endpoint currently has a security vulnerability bug")
    @Severity(SeverityLevel.BLOCKER)
    void testGetAllExpensesInvalidAuthToken() {
        Allure.step("Arrange: Document security vulnerability with invalid tokens");
        Allure.addAttachment("Security Issue", "Endpoint accepts invalid tokens - part of authentication bypass vulnerability");
        
        // There is a bug in the project.
        // This endpoint is currently publicly accessible and does not 
        // require authentication, even though it should. Users can successfully 
        // call it without being logged in, meaning authorization checks are not enforced. 
        // As a result, anyone with the URL can retrieve all company expenses (pending, approved, or denied).
        // This is a security risk as sensitive financial data is exposed to unauthorized users.
        // This test is expected to fail until the bug is fixed.
        Allure.step("Act: Send request with invalid JWT token");
        given()
            .spec(unAuthRequestSpec)
            .cookie("jwt", "expired_or_invalid_session_token")
        .when()
            .get("/api/expenses")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403)))
            .time(lessThan(10000L))
            .body("title", containsString("Authentication required"));
        
        Allure.step("Assert: Expected failure - invalid tokens should be rejected but aren't");
    }
    
    @Test
    @DisplayName("Edge Case: Full response structure validation")
    @Description("Verify that the API response contains all required fields with correct data types")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllExpensesResponseStructure() {
        Allure.step("Arrange: Prepare authenticated request for response structure validation");
        Allure.addAttachment("Test Scenario", "Edge Case - Full response structure validation");
        
        Allure.step("Act: Send authenticated GET request to validate response structure");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses")
        .then()
            .spec(successResponseSpec)
            .statusCode(200)
            .body("success", notNullValue())
            .body("success", isA(Boolean.class))
            .body("data", notNullValue())
            .body("data", isA(List.class))
            .body("count", notNullValue())
            .body("count", isA(Integer.class));
        
        Allure.step("Assert: Validated all required fields present with correct data types");
    }

    @Test
    @DisplayName("Edge Case: Wrong HTTP method validation")
    @Description("Verify that incorrect HTTP methods are properly rejected with appropriate error codes")
    @Severity(SeverityLevel.MINOR)
    void testGetAllExpensesWrongHttpMethod() {
        Allure.step("Arrange: Prepare request with wrong HTTP method (POST instead of GET)");
        Allure.addAttachment("Test Scenario", "Edge Case - Wrong HTTP method validation");
        
        Allure.step("Act: Send POST request to GET-only endpoint");
        given()
            .spec(authRequestSpec)
        .when()
            .post("/api/expenses") // Using POST instead of GET
        .then()
            .statusCode(anyOf(equalTo(405), equalTo(404))) // Method not allowed or not found
            .time(lessThan(10000L))
            .body("title", containsString("not found"));
        
        Allure.step("Assert: Validated proper rejection of wrong HTTP method");
    }
    
    @Test
    @DisplayName("Boundary Test: Empty expenses database")
    @Description("Verify that the API handles empty expenses database gracefully")
    @Severity(SeverityLevel.MINOR)
    @Flaky
    void testGetAllExpensesEmptyDatabase() {
        Allure.step("Arrange: Prepare authenticated request for empty database scenario");
        Allure.addAttachment("Test Scenario", "Boundary Test - Empty expenses database");
        Allure.addAttachment("Warning", "This test is expected to fail until database is cleared of all expenses");
        
        // The response should still be successful even when there are no pending expenses
        // It should return an empty data list (not null) with the count as 0
        // This test is expected to fail until the database is cleared of all expenses
        Allure.step("Act: Send authenticated GET request expecting empty results");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses")
        .then()
            .spec(successResponseSpec)
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("count", equalTo(0));
        
        Allure.step("Assert: Validated graceful handling of empty expenses database");
    }
}
