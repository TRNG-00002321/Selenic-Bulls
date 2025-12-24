package com.revature.servicetests;

import static org.junit.jupiter.api.Assertions.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.revature.repository.User;
import com.revature.repository.UserRepository;
import com.revature.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("service")
@Tag("auth")
class AuthenticationServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationService authService;

    private static final String JWT_SECRET_FOR_TEST = "your-secret-key-change-in-production";
    private static final String JWT_ISSUER_FOR_TEST = "expense-manager";

    @Nested
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
        void testAuthenticateUser_Happy(String username, String password) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            Optional<User> result = authService.authenticateUser(username, password);

            assertTrue(result.isPresent());
            assertEquals(username, result.get().getUsername());
        }

        @Tag("sad")
        @DisplayName("Sad path: unknown user returns empty")
        @Test
        void testAuthenticateUser_Sad_UnknownUser() {
            when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

            Optional<User> result = authService.authenticateUser("unknownuser", "password123");

            assertTrue(result.isEmpty());
        }

        @Tag("sad")
        @DisplayName("Sad path: wrong password returns empty")
        @Test
        void testAuthenticateUser_Sad_WrongPassword() {
            User user = new User();
            user.setUsername("manager1");
            user.setPassword("password123");

            when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(user));

            Optional<User> result = authService.authenticateUser("manager1", "wrongpass");

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
        void testAuthenticateManager(String username, String password, boolean isManagerFlag) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(isManagerFlag ? "Manager" : "Employee");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            Optional<User> result = authService.authenticateManager(username, password);

            if (isManagerFlag) {
                assertTrue(result.isPresent());
                assertEquals("Manager", result.get().getRole());
            } else {
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
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
        void testIsManager(String role) {
            User user = new User();
            user.setRole(role);

            boolean result = authService.isManager(user);

            if ("Manager".equals(role)) {
                assertTrue(result);
            } else {
                assertFalse(result);
            }
        }

        @Tag("sad")
        @DisplayName("isManager returns false for null user")
        @Test
        void testIsManager_NullUser() {
            assertFalse(authService.isManager(null));
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
        @Test
        void testCreateJwtToken_Basic() {
            User user = new User();
            user.setId(123);
            user.setUsername("manager1");
            user.setRole("Manager");

            String token = authService.createJwtToken(user);

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
        void testValidateJwtToken_NullOrBlank(String input) {
            assertTrue(authService.validateJwtToken(input).isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Tag("happy")
        @DisplayName("Happy path: validateJwtToken verifies token and loads user by subject(id)")
        @Test
        void testValidateJwtToken_Happy() {
            User user = new User();
            user.setId(42);
            user.setUsername("manager1");
            user.setRole("Manager");

            String token = authService.createJwtToken(user);

            when(userRepository.findById(42)).thenReturn(Optional.of(user));

            Optional<User> result = authService.validateJwtToken(token);

            assertTrue(result.isPresent());
            assertEquals(42, result.get().getId());
            verify(userRepository).findById(42);
        }

        @Tag("sad")
        @DisplayName("Sad path: validateJwtToken returns empty for invalid token string")
        @Test
        void testValidateJwtToken_Sad_InvalidToken() {
            Optional<User> result = authService.validateJwtToken("not-a-jwt");

            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Tag("sad")
        @DisplayName("Sad path: validateJwtToken returns empty when subject is not an int")
        @Test
        void testValidateJwtToken_Sad_SubjectNotNumeric() {
            String tokenWithBadSubject = JWT.create()
                    .withIssuer(JWT_ISSUER_FOR_TEST)
                    .withSubject("abc")
                    .sign(Algorithm.HMAC256(JWT_SECRET_FOR_TEST));

            Optional<User> result = authService.validateJwtToken(tokenWithBadSubject);

            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
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
        void testValidateAuthentication_Sad_MissingOrWrongPrefix(String header) {
            Optional<User> result = authService.validateAuthentication(header);
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Tag("sad")
        @DisplayName("validateAuthentication returns empty for non-numeric id")
        @Test
        void testValidateAuthentication_Sad_NonNumericId() {
            Optional<User> result = authService.validateAuthentication("Bearer abc");
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Tag("happy")
        @DisplayName("validateAuthentication happy path: Bearer {id} returns user from repository")
        @Test
        void testValidateAuthentication_Happy() {
            User user = new User();
            user.setId(7);
            user.setUsername("employee1");
            user.setRole("Employee");

            when(userRepository.findById(7)).thenReturn(Optional.of(user));

            Optional<User> result = authService.validateAuthentication("Bearer 7");

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
        @Test
        void testValidateManagerAuthenticationLegacy_Happy_Manager() {
            User manager = new User();
            manager.setId(11);
            manager.setUsername("manager1");
            manager.setRole("Manager");

            when(userRepository.findById(11)).thenReturn(Optional.of(manager));

            Optional<User> result = authService.validateManagerAuthenticationLegacy("Bearer 11");

            assertTrue(result.isPresent());
            assertEquals(11, result.get().getId());
            assertEquals("Manager", result.get().getRole());
        }

        @Tag("manager")
        @Tag("sad")
        @DisplayName("validateManagerAuthenticationLegacy returns empty when user is not manager")
        @Test
        void testValidateManagerAuthenticationLegacy_Sad_NotManager() {
            User employee = new User();
            employee.setId(12);
            employee.setUsername("employee1");
            employee.setRole("Employee");

            when(userRepository.findById(12)).thenReturn(Optional.of(employee));

            Optional<User> result = authService.validateManagerAuthenticationLegacy("Bearer 12");

            assertTrue(result.isEmpty());
        }

        @Tag("sad")
        @DisplayName("validateManagerAuthenticationLegacy returns empty when header invalid")
        @Test
        void testValidateManagerAuthenticationLegacy_Sad_InvalidHeader() {
            Optional<User> result = authService.validateManagerAuthenticationLegacy("Basic 12");
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @Tag("manager")
    class ManagerAuthTests {

        // =========================
        // validateManagerAuthentication (JWT) tests
        // =========================
        @Tag("happy")
        @DisplayName("validateManagerAuthentication returns manager when token valid + user is manager")
        @Test
        void testValidateManagerAuthentication_Happy_Manager() {
            User manager = new User();
            manager.setId(99);
            manager.setUsername("manager1");
            manager.setRole("Manager");

            String token = authService.createJwtToken(manager);

            when(userRepository.findById(99)).thenReturn(Optional.of(manager));

            Optional<User> result = authService.validateManagerAuthentication(token);

            assertTrue(result.isPresent());
            assertEquals("Manager", result.get().getRole());
        }

        @Tag("sad")
        @DisplayName("validateManagerAuthentication returns empty when token valid but user not manager")
        @Test
        void testValidateManagerAuthentication_Sad_NotManager() {
            User employee = new User();
            employee.setId(100);
            employee.setUsername("employee1");
            employee.setRole("Employee");

            String token = authService.createJwtToken(employee);

            when(userRepository.findById(100)).thenReturn(Optional.of(employee));

            Optional<User> result = authService.validateManagerAuthentication(token);

            assertTrue(result.isEmpty());
        }

        @Tag("sad")
        @DisplayName("validateManagerAuthentication returns empty when token invalid")
        @Test
        void testValidateManagerAuthentication_Sad_InvalidToken() {
            Optional<User> result = authService.validateManagerAuthentication("not-a-jwt");
            assertTrue(result.isEmpty());
            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @Tag("user")
    class UserLookupTests {

        // =========================
        // getUserById tests
        // =========================
        @Tag("happy")
        @DisplayName("getUserById delegates to repository.findById")
        @Test
        void testGetUserById() {
            User user = new User();
            user.setId(5);

            when(userRepository.findById(5)).thenReturn(Optional.of(user));

            Optional<User> result = authService.getUserById(5);

            assertTrue(result.isPresent());
            assertEquals(5, result.get().getId());
            verify(userRepository).findById(5);
        }
    }
}
