package com.revature.servicetests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.revature.repository.User;
import com.revature.repository.UserRepository;
import com.revature.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

class AuthenticationServiceTests {

    private UserRepository userRepository;
    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authService = new AuthenticationService(userRepository);
    }

    // =========================
    // authenticateUser tests
    // =========================
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

    @ParameterizedTest(name = "username={0}, password={1}")
    @CsvSource({
            "manager1,wrongpass",
            "unknownuser,password123"
    })
    @DisplayName("Sad path: invalid username or password")
    void testAuthenticateUser_Sad(String username, String password) {
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        Optional<User> result = authService.authenticateUser(username, password);

        assertTrue(result.isEmpty());
    }

    // =========================
    // authenticateManager tests
    // =========================
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

    // =========================
    // isManager tests
    // =========================
    @ParameterizedTest(name = "role={0}")
    @CsvSource({
            "Manager",
            "Employee",
            ""
    })
    @DisplayName("isManager returns true only for Manager role")
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

    // =========================
    // validateJwtToken edge case
    // =========================
    @org.junit.jupiter.api.Test
    @DisplayName("validateJwtToken returns empty for null/blank")
    void testValidateJwtToken_NullOrBlank() {
        assertTrue(authService.validateJwtToken(null).isEmpty());
        assertTrue(authService.validateJwtToken("").isEmpty());
        assertTrue(authService.validateJwtToken("   ").isEmpty());
    }
}
