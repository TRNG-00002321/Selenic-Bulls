package com.revature.unittests.repotests;

import com.revature.repository.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Expense Management System")
@Feature("Repository Data Access")
@Story("As the system, I want to retrieve users by id/username so that authentication and authorization can function correctly")
class UserRepoTests {

    @BeforeAll
    static void setUpClass() {
        Allure.addAttachment(
                "Test Suite Information",
                "Unit tests for UserRepository (findById, findByUsername) using JDBC mocks"
        );
        System.out.println("Starting UserRepoTests suite");
    }

    @AfterAll
    static void tearDownClass() {
        System.out.println("Completed UserRepoTests suite");
    }

    @InjectMocks
    private UserRepository userRepository;

    @Mock
    private DatabaseConnection mockDbConnection;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStmt;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws Exception {
        when(mockDbConnection.getConnection()).thenReturn(mockConnection);
        Allure.step("Test setup completed - DatabaseConnection configured to return mocked Connection");
    }

    // -------------------
    // findById tests
    // -------------------
    @Test
    @DisplayName("Happy path: findById returns user when exists")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies findById maps ResultSet fields into a User when a record exists")
    @Issue("USER-REPO-001")
    void testFindByIdFound() throws Exception {
        Allure.step("Arrange: configure JDBC chain to return a single user row");
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("username")).thenReturn("manager1");
        when(mockResultSet.getString("password")).thenReturn("password123");
        when(mockResultSet.getString("role")).thenReturn("Manager");

        Allure.step("Act: call findById(1)");
        Optional<User> result = userRepository.findById(1);

        Allure.step("Assert: user is present and mapped correctly");
        assertTrue(result.isPresent());
        assertEquals("manager1", result.get().getUsername());
        assertEquals("Manager", result.get().getRole());
        Allure.addAttachment("Mapped User", "id=1, username=manager1, role=Manager");
    }

    @Test
    @DisplayName("Sad path: findById returns empty when user does not exist")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies findById returns Optional.empty when ResultSet has no rows")
    @Issue("USER-REPO-002")
    void testFindByIdNotFound() throws Exception {
        Allure.step("Arrange: ResultSet.next() returns false (no rows)");
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Allure.step("Act: call findById(99)");
        Optional<User> result = userRepository.findById(99);

        Allure.step("Assert: result is empty");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Edge case: findById handles SQL exception")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies findById wraps SQLException in RuntimeException with a helpful message")
    @Issue("USER-REPO-003")
    void testFindByIdThrowsSQLException() throws Exception {
        Allure.step("Arrange: Connection.prepareStatement throws SQLException");
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        Allure.step("Act & Assert: RuntimeException is thrown and message contains context");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> userRepository.findById(1));
        assertTrue(ex.getMessage().contains("Error finding user by ID"));
        Allure.addAttachment("Exception Message", ex.getMessage());
    }

    // -------------------
    // findByUsername tests
    // -------------------
    @ParameterizedTest(name = "username={0}, role={1}")
    @CsvSource({
            "manager1,Manager",
            "employee1,Employee"
    })
    @DisplayName("Happy path: findByUsername returns correct user")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies findByUsername maps ResultSet fields into a User when a record exists for the provided username")
    @Issue("USER-REPO-004")
    void testFindByUsername(String username, String role) throws Exception {
        Allure.step("Arrange: configure JDBC chain to return a single row for username=" + username);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("username")).thenReturn(username);
        when(mockResultSet.getString("password")).thenReturn("password123");
        when(mockResultSet.getString("role")).thenReturn(role);
        Allure.addAttachment("Expected", "username=" + username + ", role=" + role);

        Allure.step("Act: call findByUsername");
        Optional<User> result = userRepository.findByUsername(username);

        Allure.step("Assert: user present and mapped correctly");
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        assertEquals(role, result.get().getRole());
    }

    @Test
    @DisplayName("Sad path: findByUsername returns empty when username not found")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifies findByUsername returns Optional.empty when ResultSet has no rows")
    @Issue("USER-REPO-005")
    void testFindByUsernameNotFound() throws Exception {
        Allure.step("Arrange: ResultSet.next() returns false (no rows)");
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Allure.step("Act: call findByUsername(nonexistent)");
        Optional<User> result = userRepository.findByUsername("nonexistent");

        Allure.step("Assert: result is empty");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Edge case: findByUsername handles SQL exception")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies findByUsername wraps SQLException in RuntimeException with a helpful message")
    @Issue("USER-REPO-006")
    void testFindByUsernameThrowsSQLException() throws Exception {
        Allure.step("Arrange: Connection.prepareStatement throws SQLException");
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        Allure.step("Act & Assert: RuntimeException is thrown and message contains context");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> userRepository.findByUsername("manager1"));
        assertTrue(ex.getMessage().contains("Error finding user by username"));
        Allure.addAttachment("Exception Message", ex.getMessage());
    }
}
