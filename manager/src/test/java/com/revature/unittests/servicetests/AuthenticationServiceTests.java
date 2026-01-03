package com.revature.unittests.servicetests;

import static org.junit.jupiter.api.Assertions.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.revature.repository.User;
import com.revature.repository.UserRepository;
import com.revature.service.AuthenticationService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Expense Management System")
@Feature("Authentication Service")
@Story("As a manager, I want secure authentication and authorization so that only permitted users can access manager features")
@Tag("service")
@Tag("auth")
class AuthenticationServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationService authService;

    private static final String JWT_SECRET_FOR_TEST = "your-secret-key-change-in-production";
    private static final String JWT_ISSUER_FOR_TEST = "expense-manager";

    @BeforeAll
    static void setUpClass() {
        Allure.addAttachment(
                "Test Suite Information",
                "Unit tests for AuthenticationService (authentication, role checks, and token validation)"
        );
        System.out.println("Starting AuthenticationServiceTests suite");
    }

    @AfterAll
    static void tearDownClass() {
        System.out.println("Completed AuthenticationServiceTests suite");
    }

    @Nested
    @Feature("Login")
    @Story("As a user, I want to log in with valid credentials so that I can access the system")
    @Tag("login")
    class LoginTests {

        // =========================
        // authenticateUser tests
        // =========================
        @Tag("happy")
        @ParameterizedTest(name = "username={0}, password={1}")
        @CsvSource({
                "manager1,password123",
                "employee1,password123"
        })
        @DisplayName("Happy path: valid username/password")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Verifies authenticateUser returns a user when username/password are correct and repository returns a matching user")
        @Issue("AUTH-SERVICE-LOGIN-001")
        void testAuthenticateUser_Happy(String username, String password) {
            Allure.step("Arrange: stub repository to return user for username=" + username);
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            Allure.addAttachment("Input", "username=" + username + ", password=" + password);

            Allure.step("Act: call authenticateUser");
            Optional<User> result = authService.authenticateUser(username, password);

            Allure.step("Assert: user is present and username matches");
            assertTrue(result.isPresent());
            assertEquals(username, result.get().getUsername());
        }

        @Tag("sad")
        @DisplayName("Sad path: unknown user returns empty")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies authenticateUser returns Optional.empty when repository has no user for the provided username")
        @Issue("AUTH-SERVICE-LOGIN-002")
        @Test
        void testAuthenticateUser_Sad_UnknownUser() {
            Allure.step("Arrange: stub repository to return empty for unknown user");
            when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

            Allure.step("Act: call authenticateUser");
            Optional<User> result = authService.authenticateUser("unknownuser", "password123");

            Allure.step("Assert: result is empty");
            assertTrue(result.isEmpty());
        }

        @Tag("sad")
        @DisplayName("Sad path: wrong password returns empty")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies authenticateUser returns Optional.empty when password does not match stored password")
        @Issue("AUTH-SERVICE-LOGIN-003")
        @Test
        void testAuthenticateUser_Sad_WrongPassword() {
            Allure.step("Arrange: stub repository to return user with known password");
            User user = new User();
            user.setUsername("manager1");
            user.setPassword("password123");

            when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(user));

            Allure.step("Act: call authenticateUser with wrong password");
            Optional<User> result = authService.authenticateUser("manager1", "wrongpass");

            Allure.step("Assert: result is empty");
            assertTrue(result.isEmpty());
        }

        // =========================
        // authenticateManager tests
        // =========================
        @Tag("manager")
        @ParameterizedTest(name = "username={0}, password={1}, isManager={2}")
        @CsvSource({
                "manager1,password123,true",
                "employee1,password123,false"
        })
        @DisplayName("Parameterized: authenticateManager checks role")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Verifies authenticateManager only returns a result when credentials are valid and role is Manager")
        @Issue("AUTH-SERVICE-ROLE-001")
        void testAuthenticateManager(String username, String password, boolean isManagerFlag) {
            Allure.step("Arrange: stub repository user with role based on isManagerFlag=" + isManagerFlag);
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(isManagerFlag ? "Manager" : "Employee");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            Allure.step("Act: call authenticateManager");
            Optional<User> result = authService.authenticateManager(username, password);

            Allure.step("Assert: present only when role is Manager");
            if (isManagerFlag) {
                assertTrue(result.isPresent());
                assertEquals("Manager", result.get().getRole());
            } else {
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
    @Feature("Role Checking")
    @Story("As the system, I need to determine whether a user is a manager so that access control can be enforced")
    @Tag("role")
    class RoleTests {

        // =========================
        // isManager tests
        // =========================
        @DisplayName("isManager returns true only for Manager role")
        @ParameterizedTest(name = "role={0}")
        @CsvSource({
                "Manager",
                "Employee",
                "''"
        })
        @Severity(SeverityLevel.MINOR)
        @Description("Verifies isManager returns true only when role equals 'Manager'")
        @Issue("AUTH-SERVICE-ROLE-002")
        void testIsManager(String role) {
            Allure.step("Arrange: build user with role=" + role);
            User user = new User();
            user.setRole(role);

            Allure.step("Act: call isManager");
            boolean result = authService.isManager(user);

            Allure.step("Assert: true only for 'Manager'");
            if ("Manager".equals(role)) {
                assertTrue(result);
            } else {
                assertFalse(result);
            }
        }

        @Tag("sad")
        @DisplayName("isManager returns false for null user")
        @Severity(SeverityLevel.MINOR)
        @Description("Verifies isManager is null-safe and returns false for null user")
        @Issue("AUTH-SERVICE-ROLE-003")
        @Test
        void testIsManager_NullUser() {
            Allure.step("Act & Assert: null user returns false");
            assertFalse(authService.isManager(null));
        }
    }

    @Nested
    @Feature("JWT")
    @Story("As the system, I want to create and validate JWTs so that authenticated users can be trusted across requests")
    @Tag("legacy")
    class LegacyHeaderAuthTests {

        // =========================
        // validateAuthentication (legacy header) tests
        // =========================
        @Tag("sad")
        @DisplayName("validateAuthentication returns empty if header missing or not Bearer")
        @ParameterizedTest(name = "header={0}")
        @CsvSource({
                "null",
                "Basic abc",
                "'Bearer'",
                "'Bearer'"
        })
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies validateAuthentication rejects missing/invalid Authorization header formats and does not call repository")
        @Issue("AUTH-SERVICE-LEGACY-001")
        void testValidateAuthentication_Sad_MissingOrWrongPrefix(String header) {
            Allure.step("Act: call validateAuthentication with header=" + header);
            Optional<User> result = authService.validateAuthentication(header);

            Allure.step("Assert: result is empty and repository not called");
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Tag("sad")
        @DisplayName("validateAuthentication returns empty for non-numeric id")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies validateAuthentication rejects Bearer tokens where the id portion is not numeric")
        @Issue("AUTH-SERVICE-LEGACY-002")
        @Test
        void testValidateAuthentication_Sad_NonNumericId() {
            Allure.step("Act: call validateAuthentication with non-numeric id");
            Optional<User> result = authService.validateAuthentication("Bearer abc");

            Allure.step("Assert: result is empty and repository not called");
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Tag("happy")
        @DisplayName("validateAuthentication happy path: Bearer {id} returns user from repository")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Verifies validateAuthentication parses a numeric id and returns the user from repository.findById")
        @Issue("AUTH-SERVICE-LEGACY-003")
        @Test
        void testValidateAuthentication_Happy() {
            Allure.step("Arrange: stub repository to return a user for id=7");
            User user = new User();
            user.setId(7);
            user.setUsername("employee1");
            user.setRole("Employee");

            when(userRepository.findById(7)).thenReturn(Optional.of(user));
            Allure.addAttachment("Authorization Header", "Bearer 7");

            Allure.step("Act: call validateAuthentication");
            Optional<User> result = authService.validateAuthentication("Bearer 7");

            Allure.step("Assert: user is present and repository called");
            assertTrue(result.isPresent());
            assertEquals(7, result.get().getId());
            verify(userRepository).findById(7);
        }

        // =========================
        // validateManagerAuthenticationLegacy (Bearer) tests
        // =========================
        @Tag("manager")
        @Tag("happy")
        @DisplayName("validateManagerAuthenticationLegacy returns manager for Bearer {id} when user is manager")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Verifies validateManagerAuthenticationLegacy returns a user only when role is Manager")
        @Issue("AUTH-SERVICE-LEGACY-004")
        @Test
        void testValidateManagerAuthenticationLegacy_Happy_Manager() {
            Allure.step("Arrange: stub repository to return a Manager for id=11");
            User manager = new User();
            manager.setId(11);
            manager.setUsername("manager1");
            manager.setRole("Manager");

            when(userRepository.findById(11)).thenReturn(Optional.of(manager));
            Allure.addAttachment("Authorization Header", "Bearer 11");

            Allure.step("Act: call validateManagerAuthenticationLegacy");
            Optional<User> result = authService.validateManagerAuthenticationLegacy("Bearer 11");

            Allure.step("Assert: present and role Manager");
            assertTrue(result.isPresent());
            assertEquals(11, result.get().getId());
            assertEquals("Manager", result.get().getRole());
        }

        @Tag("manager")
        @Tag("sad")
        @DisplayName("validateManagerAuthenticationLegacy returns empty when user is not manager")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies validateManagerAuthenticationLegacy returns empty when the resolved user role is not Manager")
        @Issue("AUTH-SERVICE-LEGACY-005")
        @Test
        void testValidateManagerAuthenticationLegacy_Sad_NotManager() {
            Allure.step("Arrange: stub repository to return an Employee for id=12");
            User employee = new User();
            employee.setId(12);
            employee.setUsername("employee1");
            employee.setRole("Employee");

            when(userRepository.findById(12)).thenReturn(Optional.of(employee));
            Allure.addAttachment("Authorization Header", "Bearer 12");

            Allure.step("Act: call validateManagerAuthenticationLegacy");
            Optional<User> result = authService.validateManagerAuthenticationLegacy("Bearer 12");

            Allure.step("Assert: result is empty");
            assertTrue(result.isEmpty());
        }

        @Tag("sad")
        @DisplayName("validateManagerAuthenticationLegacy returns empty when header invalid")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies validateManagerAuthenticationLegacy rejects invalid Authorization header formats")
        @Issue("AUTH-SERVICE-LEGACY-006")
        @Test
        void testValidateManagerAuthenticationLegacy_Sad_InvalidHeader() {
            Allure.step("Act: call validateManagerAuthenticationLegacy with non-Bearer header");
            Optional<User> result = authService.validateManagerAuthenticationLegacy("Basic 12");

            Allure.step("Assert: result is empty and repository not called");
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @Tag("jwt")
    class JwtTests {

        // =========================
        // createJwtToken tests
        // =========================
        @Tag("happy")
        @DisplayName("createJwtToken returns a non-blank JWT string")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Verifies createJwtToken produces a well-formed JWT (header.payload.signature)")
        @Issue("AUTH-SERVICE-JWT-001")
        @Test
        void testCreateJwtToken_Basic() {
            Allure.step("Arrange: create a manager user");
            User user = new User();
            user.setId(123);
            user.setUsername("manager1");
            user.setRole("Manager");

            Allure.step("Act: create JWT");
            String token = authService.createJwtToken(user);
            Allure.addAttachment("JWT", token);

            Allure.step("Assert: token is non-blank and has three dot-separated parts");
            assertNotNull(token);
            assertFalse(token.trim().isEmpty());
            assertEquals(3, token.split("\\.").length, "JWT should have header.payload.signature");
        }

        // =========================
        // validateJwtToken tests
        // =========================
        @Tag("sad")
        @DisplayName("validateJwtToken returns empty for null/blank")
        @ParameterizedTest(name = "input={0}")
        @CsvSource(value = { "NULL", "''", "'   '" }, nullValues = "NULL")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies validateJwtToken rejects null/blank inputs without calling repository")
        @Issue("AUTH-SERVICE-JWT-002")
        void testValidateJwtToken_NullOrBlank(String input) {
            Allure.step("Act: validateJwtToken on null/blank input");
            assertTrue(authService.validateJwtToken(input).isEmpty());

            Allure.step("Assert: repository is not called");
            verifyNoInteractions(userRepository);
        }

        @Tag("happy")
        @DisplayName("Happy path: validateJwtToken verifies token and loads user by subject(id)")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Verifies validateJwtToken validates signature/claims and fetches user by subject ID")
        @Issue("AUTH-SERVICE-JWT-003")
        @Test
        void testValidateJwtToken_Happy() {
            Allure.step("Arrange: create user and token; stub repository findById");
            User user = new User();
            user.setId(42);
            user.setUsername("manager1");
            user.setRole("Manager");

            String token = authService.createJwtToken(user);
            Allure.addAttachment("JWT", token);

            when(userRepository.findById(42)).thenReturn(Optional.of(user));

            Allure.step("Act: validateJwtToken");
            Optional<User> result = authService.validateJwtToken(token);

            Allure.step("Assert: user returned and repository called with subject id");
            assertTrue(result.isPresent());
            assertEquals(42, result.get().getId());
            verify(userRepository).findById(42);
        }

        @Tag("sad")
        @DisplayName("Sad path: validateJwtToken returns empty for invalid token string")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies validateJwtToken returns empty when token is not parseable/valid")
        @Issue("AUTH-SERVICE-JWT-004")
        @Test
        void testValidateJwtToken_Sad_InvalidToken() {
            Allure.step("Act: validate invalid token string");
            Optional<User> result = authService.validateJwtToken("not-a-jwt");

            Allure.step("Assert: empty and repository not called");
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Tag("sad")
        @DisplayName("Sad path: validateJwtToken returns empty when subject is not an int")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies validateJwtToken returns empty when subject claim is non-numeric")
        @Issue("AUTH-SERVICE-JWT-005")
        @Test
        void testValidateJwtToken_Sad_SubjectNotNumeric() {
            Allure.step("Arrange: create token with non-numeric subject");
            String tokenWithBadSubject = JWT.create()
                    .withIssuer(JWT_ISSUER_FOR_TEST)
                    .withSubject("abc")
                    .sign(Algorithm.HMAC256(JWT_SECRET_FOR_TEST));
            Allure.addAttachment("JWT", tokenWithBadSubject);

            Allure.step("Act: validateJwtToken");
            Optional<User> result = authService.validateJwtToken(tokenWithBadSubject);

            Allure.step("Assert: empty and repository not called");
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @Feature("Manager Authorization (JWT)")
    @Story("As the system, I want to allow manager-only access using JWTs so that manager endpoints are protected")
    @Tag("manager")
    class ManagerAuthTests {

        // =========================
        // validateManagerAuthentication (JWT) tests
        // =========================
        @Tag("happy")
        @DisplayName("validateManagerAuthentication returns manager when token valid + user is manager")
        @Severity(SeverityLevel.CRITICAL)
        @Description("Verifies validateManagerAuthentication returns a user when the JWT is valid and the resolved user has role Manager")
        @Issue("AUTH-SERVICE-MANAGER-001")
        @Test
        void testValidateManagerAuthentication_Happy_Manager() {
            Allure.step("Arrange: create Manager user + token; stub repository lookup");
            User manager = new User();
            manager.setId(99);
            manager.setUsername("manager1");
            manager.setRole("Manager");

            String token = authService.createJwtToken(manager);
            Allure.addAttachment("JWT", token);

            when(userRepository.findById(99)).thenReturn(Optional.of(manager));

            Allure.step("Act: call validateManagerAuthentication(token)");
            Optional<User> result = authService.validateManagerAuthentication(token);

            Allure.step("Assert: manager is present and has role Manager");
            assertTrue(result.isPresent());
            assertEquals("Manager", result.get().getRole());
        }

        @Tag("sad")
        @DisplayName("validateManagerAuthentication returns empty when token valid but user not manager")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies validateManagerAuthentication returns empty when token is valid but resolved user role is not Manager")
        @Issue("AUTH-SERVICE-MANAGER-002")
        @Test
        void testValidateManagerAuthentication_Sad_NotManager() {
            Allure.step("Arrange: create Employee user + token; stub repository lookup");
            User employee = new User();
            employee.setId(100);
            employee.setUsername("employee1");
            employee.setRole("Employee");

            String token = authService.createJwtToken(employee);
            Allure.addAttachment("JWT", token);

            when(userRepository.findById(100)).thenReturn(Optional.of(employee));

            Allure.step("Act: call validateManagerAuthentication(token)");
            Optional<User> result = authService.validateManagerAuthentication(token);

            Allure.step("Assert: empty result because role is not Manager");
            assertTrue(result.isEmpty());
        }

        @Tag("sad")
        @DisplayName("validateManagerAuthentication returns empty when token invalid")
        @Severity(SeverityLevel.NORMAL)
        @Description("Verifies validateManagerAuthentication returns empty when the provided token is invalid/unparseable")
        @Issue("AUTH-SERVICE-MANAGER-003")
        @Test
        void testValidateManagerAuthentication_Sad_InvalidToken() {
            Allure.step("Act: call validateManagerAuthentication with invalid token string");
            Optional<User> result = authService.validateManagerAuthentication("not-a-jwt");

            Allure.step("Assert: empty and repository not called");
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @Feature("User Lookup")
    @Story("As the system, I want to retrieve users by id so that downstream authentication flows can load user details")
    @Tag("user")
    class UserLookupTests {

        // =========================
        // getUserById tests
        // =========================
        @Tag("happy")
        @DisplayName("getUserById delegates to repository.findById")
        @Severity(SeverityLevel.MINOR)
        @Description("Verifies getUserById delegates to UserRepository.findById and returns the same Optional")
        @Issue("AUTH-SERVICE-USER-001")
        @Test
        void testGetUserById() {
            Allure.step("Arrange: stub repository to return user for id=5");
            User user = new User();
            user.setId(5);

            when(userRepository.findById(5)).thenReturn(Optional.of(user));

            Allure.step("Act: call getUserById(5)");
            Optional<User> result = authService.getUserById(5);

            Allure.step("Assert: user is present and repository called");
            assertTrue(result.isPresent());
            assertEquals(5, result.get().getId());
            verify(userRepository).findById(5);
        }
    }
}
