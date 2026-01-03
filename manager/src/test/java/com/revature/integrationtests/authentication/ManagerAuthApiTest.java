package com.revature.integrationtests.authentication;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.javalin.http.ContentType;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.*;

@Epic("Expense Management System")
@Feature("Manager Authentication")
@Story("As a manager, I want to login and logout securely so that only managers can access manager features")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ManagerAuthApiTest {

    private static RequestSpecification unAuthRequestSpec;
    private static RequestSpecification authRequestSpec;
    private static ResponseSpecification jsonSuccessResponseSpec;

    @BeforeAll
    static void setup() {
        RestAssured.port = 5001;

        unAuthRequestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();

        jsonSuccessResponseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .expectResponseTime(lessThan(10000L))
                .build();

        // Login once for tests that require an authenticated manager
        String jwtCookie =
                given()
                        .spec(unAuthRequestSpec)
                        .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
                        .when()
                        .post("/api/auth/login")
                        .then()
                        .spec(jsonSuccessResponseSpec)
                        .statusCode(200)
                        .body("success", equalTo(true))
                        .body("user.username", equalTo("manager1"))
                        .body("user.role", notNullValue())
                        .extract()
                        .cookie("jwt");

        authRequestSpec = new RequestSpecBuilder()
                .addRequestSpecification(unAuthRequestSpec)
                .addCookie("jwt", jwtCookie)
                .build();
    }

    @AfterAll
    static void tearDown() {
        given()
                .spec(authRequestSpec)
                .when()
                .post("/api/auth/logout")
                .then()
                .spec(jsonSuccessResponseSpec)
                .statusCode(200)
                .body("success", equalTo(true));

        RestAssured.reset();
    }

    @Nested
    @Feature("Manager Authentication")
    @Story("As a manager, I want to login and logout securely so that only managers can access manager features")
    @Tag("auth")
    @Tag("login")
    @DisplayName("Login: POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("TC-AUTH-API-001: Happy Path - Manager login returns jwt cookie + user payload")
        @Description("Verify that valid manager credentials return 200, set a jwt cookie, and include user data in the response body.")
        @Severity(SeverityLevel.CRITICAL)
        void testManagerLoginSuccess() {
            given()
                    .spec(unAuthRequestSpec)
                    .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(200)
                    .cookie("jwt", notNullValue())
                    .body("success", equalTo(true))
                    .body("message", containsString("Login"))
                    .body("user.id", notNullValue())
                    .body("user.username", equalTo("manager1"))
                    .body("user.role", notNullValue());
        }

        @Test
        @DisplayName("TC-AUTH-API-004: Sad Path - Wrong password returns 401")
        @Description("Verify that invalid manager credentials are rejected with 401 and do not issue a jwt cookie.")
        @Severity(SeverityLevel.CRITICAL)
        void testManagerLoginWrongPassword() {
            given()
                    .spec(unAuthRequestSpec)
                    .body("{\"username\": \"manager1\", \"password\": \"wrongpass\"}")
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(401)
                    .body("success", equalTo(false))
                    .body("error", containsString("Invalid credentials"));
        }

        @Test
        @DisplayName("Edge: Unknown username returns 401")
        @Description("Verify that non-existent manager usernames are rejected.")
        @Severity(SeverityLevel.NORMAL)
        void testManagerLoginUnknownUsername() {
            given()
                    .spec(unAuthRequestSpec)
                    .body("{\"username\": \"does_not_exist\", \"password\": \"password123\"}")
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(401)
                    .body("success", equalTo(false))
                    .body("error", containsString("Invalid credentials"));
        }

        @Test
        @DisplayName("Boundary: Empty string username/password")
        @Description("Verify that empty strings are handled gracefully (commonly 401 or 400 depending on validation policy).")
        @Severity(SeverityLevel.MINOR)
        void testManagerLoginEmptyStrings() {
            given()
                    .spec(unAuthRequestSpec)
                    .body("{\"username\": \"\", \"password\": \"\"}")
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(anyOf(equalTo(400), equalTo(401)))
                    .body("success", equalTo(false));
        }

        @Test
        @DisplayName("TC-AUTH-API-005: Sad Path - Missing username/password returns 400")
        @Description("Verify that malformed login requests are rejected with 400.")
        @Severity(SeverityLevel.NORMAL)
        void testManagerLoginMissingFields() {
            given()
                    .spec(unAuthRequestSpec)
                    .body("{\"username\": null, \"password\": null}")
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(400)
                    .body("success", equalTo(false))
                    .body("error", containsString("required"));
        }

        @Test
        @DisplayName("TC-AUTH-API-006: Sad Path - Invalid JSON format returns 400")
        @Description("Verify that invalid JSON in the request body returns 400.")
        @Severity(SeverityLevel.NORMAL)
        void testManagerLoginInvalidJson() {
            given()
                    .spec(unAuthRequestSpec)
                    .body("{not-valid-json")
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(400)
                    .body("success", equalTo(false))
                    .body("error", containsString("Invalid request format"));
        }
    }

    @Nested
    @Feature("Manager Authentication")
    @Story("As a manager, I want to login and logout securely so that only managers can access manager features")
    @Tag("auth")
    @Tag("status")
    @DisplayName("Status: GET /api/auth/status")
    class StatusTests {

        @Test
        @DisplayName("TC-AUTH-API-002: Happy Path - Auth status reflects authenticated manager when jwt cookie present")
        @Description("Verify that /api/auth/status returns authenticated=true when a valid jwt cookie is supplied.")
        @Severity(SeverityLevel.CRITICAL)
        void testAuthStatusAuthenticated() {
            given()
                    .spec(authRequestSpec)
                    .when()
                    .get("/api/auth/status")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(200)
                    .body("authenticated", equalTo(true))
                    .body("user", notNullValue())
                    .body("user.username", notNullValue())
                    .body("user.role", notNullValue());
        }

        @Test
        @DisplayName("Edge: No jwt cookie returns authenticated=false")
        @Description("Verify that status checks without a cookie return unauthenticated.")
        @Severity(SeverityLevel.NORMAL)
        void testAuthStatusNoCookie() {
            given()
                    .spec(unAuthRequestSpec)
                    .when()
                    .get("/api/auth/status")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(200)
                    .body("authenticated", equalTo(false));
        }

        @Test
        @DisplayName("Boundary: Blank jwt cookie returns authenticated=false")
        @Description("Verify that blank cookie values are treated as missing/invalid.")
        @Severity(SeverityLevel.MINOR)
        void testAuthStatusBlankCookie() {
            given()
                    .spec(unAuthRequestSpec)
                    .cookie("jwt", "")
                    .when()
                    .get("/api/auth/status")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(200)
                    .body("authenticated", equalTo(false));
        }

        @Test
        @DisplayName("Boundary: Whitespace jwt cookie returns authenticated=false")
        @Description("Verify that whitespace cookie values are treated as missing/invalid.")
        @Severity(SeverityLevel.MINOR)
        void testAuthStatusWhitespaceCookie() {
            given()
                    .spec(unAuthRequestSpec)
                    .cookie("jwt", "   ")
                    .when()
                    .get("/api/auth/status")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(200)
                    .body("authenticated", equalTo(false));
        }

        @Test
        @DisplayName("TC-AUTH-API-007: Sad Path - Invalid jwt cookie returns unauthenticated status")
        @Description("Verify that an invalid/expired jwt cookie does not authenticate the user.")
        @Severity(SeverityLevel.NORMAL)
        void testAuthStatusInvalidJwtCookie() {
            given()
                    .spec(unAuthRequestSpec)
                    .cookie("jwt", "expired_or_invalid_session_token")
                    .when()
                    .get("/api/auth/status")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(200)
                    .body("authenticated", equalTo(false));
        }

        @Test
        @DisplayName("Edge: Malformed JWT token returns authenticated=false")
        @Description("Verify that token parse/verification failures are handled gracefully.")
        @Severity(SeverityLevel.MINOR)
        void testAuthStatusMalformedJwt() {
            given()
                    .spec(unAuthRequestSpec)
                    .cookie("jwt", "abc.def.ghi")
                    .when()
                    .get("/api/auth/status")
                    .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(200)
                    .body("authenticated", equalTo(false));
        }
    }

    @Nested
    @Feature("Manager Authentication")
    @Story("As a manager, I want to login and logout securely so that only managers can access manager features")
    @Tag("auth")
    @Tag("logout")
    @DisplayName("Logout: POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("TC-AUTH-API-003: Happy Path - Logout clears session and auth status becomes unauthenticated")
        @Description("Verify that logout clears the cookie such that subsequent status checks (without cookie) are unauthenticated.")
        @Severity(SeverityLevel.CRITICAL)
        void testLogoutClearsCookieAndBecomesUnauthenticated() {
            Response login =
                given()
                    .spec(unAuthRequestSpec)
                    .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
                .when()
                    .post("/api/auth/login")
                .then()
                    .spec(jsonSuccessResponseSpec)
                    .statusCode(200)
                    .cookie("jwt", notNullValue())
                    .extract()
                    .response();

            String jwt = login.getCookie("jwt");

            given()
                .spec(unAuthRequestSpec)
                .cookie("jwt", jwt)
            .when()
                .post("/api/auth/logout")
            .then()
                .spec(jsonSuccessResponseSpec)
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("Logged out"));

            given()
                .spec(unAuthRequestSpec)
            .when()
                .get("/api/auth/status")
            .then()
                .spec(jsonSuccessResponseSpec)
                .statusCode(200)
                .body("authenticated", equalTo(false));
        }

        @Test
        @DisplayName("Edge: Logout without cookie is safe (idempotent)")
        @Description("Verify that calling logout while not logged in still succeeds and does not error.")
        @Severity(SeverityLevel.MINOR)
        void testLogoutWithoutCookieStillSucceeds() {
            given()
                .spec(unAuthRequestSpec)
            .when()
                .post("/api/auth/logout")
            .then()
                .spec(jsonSuccessResponseSpec)
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("Logged out"));
        }

        @Test
        @DisplayName("Edge: Logout with invalid cookie is safe")
        @Description("Verify that logout does not fail when the jwt cookie is invalid.")
        @Severity(SeverityLevel.MINOR)
        void testLogoutWithInvalidCookieStillSucceeds() {
            given()
                .spec(unAuthRequestSpec)
                .cookie("jwt", "not-a-real-token")
            .when()
                .post("/api/auth/logout")
            .then()
                .spec(jsonSuccessResponseSpec)
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("Logged out"));
        }
    }

    @Nested
    @Feature("Manager Authentication")
    @Story("As a manager, I want to login and logout securely so that only managers can access manager features")
    @Tag("auth")
    @Tag("authorization")
    @DisplayName("Authorization (protected routes)")
    class AuthorizationTests {

        @Test
        @DisplayName("TC-AUTH-API-009: Anonymous user cannot access manager-only endpoint (401/403)")
        @Description("Verify protected manager endpoints reject requests without a jwt cookie.")
        @Severity(SeverityLevel.CRITICAL)
        void testAnonymousCannotAccessManagerEndpoints() {
            given()
                .spec(unAuthRequestSpec)
            .when()
                .get("/api/expenses/pending")
            .then()
                .statusCode(anyOf(equalTo(401), equalTo(403)))
                .time(lessThan(10000L))
                .body("title", containsString("Authentication required"));
        }
    }

    @Nested
    @Feature("Manager Authentication")
    @Story("As a manager, I want to login and logout securely so that only managers can access manager features")
    @Tag("auth")
    @Tag("role")
    @DisplayName("Role restriction: employees cannot login via manager endpoint")
    class RoleRestrictionTests {

        @Test
        @DisplayName("TC-AUTH-API-008: Sad Path - Employee credentials cannot login as manager (401)")
        @Description("Verify that employee users cannot login via the manager login endpoint.")
        @Severity(SeverityLevel.CRITICAL)
        void testEmployeeCannotLoginAsManager() {
            given()
                .spec(unAuthRequestSpec)
                .body("{\"username\": \"employee1\", \"password\": \"password123\"}")
            .when()
                .post("/api/auth/login")
            .then()
                .spec(jsonSuccessResponseSpec)
                .statusCode(401)
                .body("success", equalTo(false))
                .body("error", containsString("not a manager"));
        }
    }
}
