package com.revature.integrationtests.expenses;

import static org.hamcrest.Matchers.*;
import static io.restassured.RestAssured.given;

import com.revature.repository.ApprovalRepository;
import com.revature.repository.DatabaseConnection;
import org.junit.jupiter.api.*;
import io.javalin.http.ContentType;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.qameta.allure.*;

/**
 * Controller tests for Manager decision endpoints.
 * Focuses on denying expenses via the REST API.
 */
@Epic("Expense Management")
@Feature("Manager Decision Actions")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DenyExpenseControllerAPITest {

    // Reusable specifications to maintain DRY (Don't Repeat Yourself) code
    private static RequestSpecification unAuthRequestSpec;
    private static RequestSpecification authRequestSpec;
    private static ResponseSpecification successResponseSpec;
    private static ApprovalRepository approvalRepo;

    @BeforeAll
    static void dbSetup() {
        // ... your existing RestAssured setup ...

        // Initialize your repository (assuming you have a way to get DatabaseConnection)
        // This allows the test to talk directly to the DB to fix the state
        DatabaseConnection dbConn = new DatabaseConnection();
        approvalRepo = new ApprovalRepository(dbConn);
    }


    /**
     * Set up global test configurations, authentication, and specifications.
     */
    @BeforeAll
    static void setup() {
        // Configure RestAssured to point to the local Javalin server
        RestAssured.port = 5001;

        // Base specification for JSON requests
        unAuthRequestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();

        // AUTHENTICATION FLOW: Obtain JWT via login
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

        // Authenticated specification including the JWT cookie for protected routes
        authRequestSpec = new RequestSpecBuilder()
                .addRequestSpecification(unAuthRequestSpec)
                .addCookie("jwt", sessionCookie)
                .build();

        // Default expectations for successful responses
        successResponseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .expectResponseTime(lessThan(10000L)) // Ensures API performance
                .build();
    }

    /**
     * Cleanup operations to clear session and reset RestAssured settings.
     */
    @AfterAll
    @Step("Cleanup: Logging out and resetting RestAssured")
    static void tearDown() {
        given().spec(authRequestSpec).when().post("/api/auth/logout");
        RestAssured.reset();
    }

    /**
     * Resets Expense #1 to 'pending' after every test.
     */
    @AfterEach
    void resetExpenseState() {
        // reviewerId 0 and null comment to represent a clean pending state
        boolean resetSuccessful = approvalRepo.updateApprovalStatus(1, "pending", 0, null);

        if (!resetSuccessful) {
            System.out.println("Warning: Could not reset expense state for ID 2");
        }
    }

    // --- HAPPY PATHS ---

    /**
     * Validates that a manager can successfully deny a pending expense with a comment.
     */
    @Test
    @Story("Deny Pending Expense")
    @DisplayName("Deny Expense Success")
    void testDenyExpenseSuccess() {
        // Arrange
        int expenseId = 1;
        String requestBody = "{\"comment\": \"Denied due to policy violation\"}";

        // Act & Assert
        given()
                .spec(authRequestSpec)
                .body(requestBody)
        .when()
                .post("/api/expenses/" + expenseId + "/deny")
        .then()
                .spec(successResponseSpec)
                .statusCode(200)
                .body("success", is(true))
                .body("message", containsString("denied"));
    }

    // --- SAD PATHS / EDGE CASES ---

    /**
     * Sad Path: Validates system behavior when attempting to approve a non-existent expense ID.
     */
    @Test
    @Story("Deny Pending Expense")
    @DisplayName("Deny Expense - Non-Existent ID")
    void testDenyNonExistentExpense() {
        // Arrange
        int expenseId = 99999;

        // Act & Assert
        given()
                .spec(authRequestSpec)
                .body("{\"comment\": \"Testing non-existent\"}")
        .when()
                .post("/api/expenses/" + expenseId + "/deny")
        .then()
                .statusCode(anyOf(is(404), is(400)));
    }

    /**
     * Boundary Case: Validates if the system accepts or correctly handles an empty approval comment.
     */
    @Test
    @Story("Boundary Testing")
    @DisplayName("Deny Expense - Empty Comment")
    void testDenyWithEmptyComment() {
        // Arrange
        int expenseId = 1;
        // Act & Assert
        given()
                .spec(authRequestSpec)
                .body("{\"comment\": \"\"}")
        .when()
                .post("/api/expenses/"+ expenseId +"/deny")
        .then()
                .statusCode(200);
    }
}
