package com.revature.servicetests;

import static org.junit.jupiter.api.Assertions.*;

import com.revature.repository.User;
import com.revature.repository.UserRepository;
import com.revature.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationService authService;

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

    // =========================
    // validateJwtToken edge case
    // =========================
    @DisplayName("validateJwtToken returns empty for null/blank")
    @ParameterizedTest(name = "input={0}")
    @CsvSource({
            "null",
            "'              '",
            "''"
    })
    void testValidateJwtToken_NullOrBlank(String input) {
        assertTrue(authService.validateJwtToken(input).isEmpty());
    }
}
