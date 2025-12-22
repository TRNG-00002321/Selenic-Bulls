package com.revature.repotests;

import com.revature.repository.DatabaseConnection;
import com.revature.repository.User;
import com.revature.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
class UserRepoTests {

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
    }

    // -------------------
    // findById tests
    // -------------------
    @Test
    @DisplayName("Happy path: findById returns user when exists")
    void testFindByIdFound() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("username")).thenReturn("manager1");
        when(mockResultSet.getString("password")).thenReturn("password123");
        when(mockResultSet.getString("role")).thenReturn("Manager");

        Optional<User> result = userRepository.findById(1);
        assertTrue(result.isPresent());
        assertEquals("manager1", result.get().getUsername());
        assertEquals("Manager", result.get().getRole());
    }

    @Test
    @DisplayName("Sad path: findById returns empty when user does not exist")
    void testFindByIdNotFound() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Optional<User> result = userRepository.findById(99);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Edge case: findById handles SQL exception")
    void testFindByIdThrowsSQLException() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userRepository.findById(1));
        assertTrue(ex.getMessage().contains("Error finding user by ID"));
    }

    // -------------------
    // findByUsername tests
    // -------------------
    @ParameterizedTest
    @CsvSource({
            "manager1, Manager",
            "employee1, Employee"
    })
    @DisplayName("Happy path: findByUsername returns correct user")
    void testFindByUsername(String username, String role) throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("username")).thenReturn(username);
        when(mockResultSet.getString("password")).thenReturn("password123");
        when(mockResultSet.getString("role")).thenReturn(role);

        Optional<User> result = userRepository.findByUsername(username);
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        assertEquals(role, result.get().getRole());
    }

    @Test
    @DisplayName("Sad path: findByUsername returns empty when username not found")
    void testFindByUsernameNotFound() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Optional<User> result = userRepository.findByUsername("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Edge case: findByUsername handles SQL exception")
    void testFindByUsernameThrowsSQLException() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userRepository.findByUsername("manager1"));
        assertTrue(ex.getMessage().contains("Error finding user by username"));
    }
}
