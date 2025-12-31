package com.revature.controllertests;

import org.junit.jupiter.api.*;
import java.util.*;
import io.javalin.http.ContentType;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

// Allure Report Annotations
import io.qameta.allure.*;
import io.qameta.allure.junit5.AllureJunit5;

/*
This class will test retrieving pending expenses.
*/
//TODO: JUnit assertions for validating response data list of pending expenses
@Epic("Expense Management System")
@Feature("Manager Expense Review")
@Story("As a manager, I want to view a list of all pending expenses so that I can review them efficiently")
public class GetPendingExpensesAPITest {
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
    @DisplayName("Happy Path: Successfully retrieve pending expenses")
    @Description("Verify that authenticated managers can successfully retrieve all pending expenses with proper response structure")
    @Severity(SeverityLevel.CRITICAL)
    void testGetPendingExpensesSuccess() {
        Allure.step("Arrange: Prepare authenticated request for pending expenses endpoint");
        Allure.addAttachment("Test Scenario", "Happy Path - Successfully retrieve pending expenses");
        
        Allure.step("Act: Send authenticated GET request to /api/expenses/pending");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/pending")
        .then()
            .spec(successResponseSpec)
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("data", isA(List.class))
            .body("count", greaterThanOrEqualTo(0))
            //Fix the count to be equal to the size of the data list instead of hardcoded value
            .body("count", equalTo(1));
        
        Allure.step("Assert: Validated successful response with proper data structure");
    }
    
    @Test
    @DisplayName("Sad Path: Unauthorized access without authentication token")
    @Description("Verify that requests without authentication tokens are properly rejected with appropriate error messages")
    @Severity(SeverityLevel.CRITICAL)
    void testGetPendingExpensesUnauthorized() {
        Allure.step("Arrange: Prepare unauthenticated request (no JWT token)");
        Allure.addAttachment("Test Scenario", "Sad Path - Unauthorized access without authentication token");
        
        // Sending the request without the cookie that contains the JWT token
        // Since the endpoint is protected, it should return 401 Unauthorized or 403 Forbidden
        Allure.step("Act: Send unauthenticated GET request to /api/expenses/pending");
        given()
            .spec(unAuthRequestSpec)
        .when()
            .get("/api/expenses/pending")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403)))
            .time(lessThan(10000L))
            .body("title", containsString("Authentication required"));
        
        Allure.step("Assert: Validated unauthorized access rejection with proper error message");
    }
    
    @Test
    @DisplayName("Sad Path: Invalid authentication token")
    @Description("Verify that requests with invalid or expired authentication tokens are properly rejected")
    @Severity(SeverityLevel.NORMAL)
    void testGetPendingExpensesInvalidAuthToken() {
        Allure.step("Arrange: Prepare request with invalid JWT token");
        Allure.addAttachment("Test Scenario", "Sad Path - Invalid authentication token");
        
        // Sending the request with an invalid/expired cookie
        // Since the endpoint is protected, it should return 401 Unauthorized or 403 Forbidden
        Allure.step("Act: Send request with invalid JWT token to /api/expenses/pending");
        given()
            .spec(unAuthRequestSpec)
            .cookie("jwt", "expired_or_invalid_session_token")
        .when()
            .get("/api/expenses/pending")
        .then()
            .statusCode(anyOf(equalTo(401), equalTo(403)))
            .time(lessThan(10000L))
            .body("title", containsString("Authentication required"));
        
        Allure.step("Assert: Validated invalid token rejection with proper error message");
    }
    
    @Test
    @DisplayName("Edge Case: Full response structure validation")
    @Description("Verify that the API response contains all required fields with correct data types")
    @Severity(SeverityLevel.NORMAL)
    void testGetPendingExpensesResponseStructure() {
        Allure.step("Arrange: Prepare authenticated request for response structure validation");
        Allure.addAttachment("Test Scenario", "Edge Case - Full response structure validation");
        
        Allure.step("Act: Send authenticated GET request to validate response structure");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/pending")
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
    @DisplayName("Boundary Test: Empty pending expenses database")
    @Description("Verify that the API handles empty pending expenses gracefully with appropriate response structure")
    @Severity(SeverityLevel.MINOR)
    @Flaky
    void testGetPendingExpensesEmptyDatabase() {
        Allure.step("Arrange: Prepare authenticated request for empty database scenario");
        Allure.addAttachment("Test Scenario", "Boundary Test - Empty pending expenses database");
        Allure.addAttachment("Warning", "This test is expected to fail until database is cleared of pending expenses");
        
        // The response should still be successful even when there are no pending expenses
        // It should return an empty data list (not null) with the count as 0
        // This test is expected to fail until the database is cleared of all pending expenses
        Allure.step("Act: Send authenticated GET request expecting empty results");
        given()
            .spec(authRequestSpec)
        .when()
            .get("/api/expenses/pending")
        .then()
            .spec(successResponseSpec)
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue())
            .body("count", equalTo(0));
        
        Allure.step("Assert: Validated graceful handling of empty pending expenses");
    }
}
