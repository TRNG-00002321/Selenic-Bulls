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
 * Focuses on approving the REST API.
 */
@Epic("Expense Management")
@Feature("Manager Decision Actions")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApproveExpenseAPITest {

    // Reusable specifications to maintain DRY (Don't Repeat Yourself) code
    private static RequestSpecification unAuthRequestSpec;
    private static RequestSpecification authRequestSpec;
    private static ResponseSpecification successResponseSpec;
    private static ApprovalRepository approvalRepo;

    @BeforeAll
    static void dbSetup() {
        // This allows the test to talk directly to the DB to fix the state
        DatabaseConnection dbConn = new DatabaseConnection();
        approvalRepo = new ApprovalRepository(dbConn);
    }

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

    @AfterAll
    @Step("Cleanup: Logging out and resetting RestAssured")
    static void tearDown() {
        given().spec(authRequestSpec).when().post("/api/auth/logout");
        RestAssured.reset();
    }

    /**
     * Resets Expense #1 to 'pending' after every test.
     * This makes the tests repeatable.
     */
    @AfterEach
    void resetExpenseState() {
        // reviewerId 0 and null comment to represent a clean pending state
        boolean resetSuccessful = approvalRepo.updateApprovalStatus(1, "pending", 0, null);

        if (!resetSuccessful) {
            System.out.println("Warning: Could not reset expense state for ID 1");
        }
    }

    // --- HAPPY PATHS ---

    /**
     * TC-CTRL-004: Validates that a manager can successfully approve a pending expense.
     */
    @Test
    @Story("Approve Pending Expense")
    @DisplayName("Approve Expense Success (POST /api/expenses/1/approve)")
    @Description(" TC-CTRL-004: Managers must be able to approve pending expenses.")
    void testApproveExpenseSuccess() {
        // Arrange
        int expenseId = 1;
        String requestBody = "{\"comment\": \"Approved for reimbursement\"}";

        // Act & Assert
        given()
                .spec(authRequestSpec)
                .body(requestBody)
        .when()
                .post("/api/expenses/" + expenseId + "/approve")
        .then()
                .spec(successResponseSpec)
                .statusCode(200)
                .body("success", is(true))
                .body("message", containsString("approved"));
    }


    // --- SAD PATHS / EDGE CASES ---

    /**
     * Sad Path: Validates system behavior when attempting to approve a non-existent expense ID.
     */
    @Test
    @Story("Approve Pending Expense")
    @DisplayName("Approve Expense - Non-Existent ID")
    void testApproveNonExistentExpense() {
        // Arrange
        int expenseId = 99999;

        // Act & Assert
        given()
                .spec(authRequestSpec)
                .body("{\"comment\": \"Testing non-existent\"}")
        .when()
                .post("/api/expenses/" + expenseId + "/approve")
        .then()
                .statusCode(anyOf(is(404), is(400)));
    }

    /**
     * Boundary Case: Validates if the system accepts or correctly handles an empty approval comment.
     */
    @Test
    @Story("Approve Pending Expenses with empty comment")
    @DisplayName("Approve Expense - Empty Comment")
    void testApproveWithEmptyComment() {
        // Arrange:
        int expenseId = 1;

        // Act & Assert
        given()
                .spec(authRequestSpec)
                .body("{\"comment\": \"\"}")
        .when()
                .post("/api/expenses/" + expenseId + "/approve")
        .then()
                .statusCode(200);
    }

    /**
     * Negative Test: Ensures the server returns a 400 Bad Request when receiving invalid JSON.
     */
    @Test
    @Story("Edge Testing")
    @DisplayName("Approve Expense - Malformed_JSON")
    void testApproveWithMalformedJson() {
        // Arrange:
        int expenseId = 1;
        String malformedJson = "{\"comment\": \"Missing bracket\"";

        // Act & Assert
        given()
                .spec(authRequestSpec)
                .body(malformedJson)
        .when()
                .post("/api/expenses/"+ expenseId +"/approve")
        .then()
                .statusCode(400);
    }
}