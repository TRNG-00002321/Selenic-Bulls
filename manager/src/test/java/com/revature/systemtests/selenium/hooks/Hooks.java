package com.revature.systemtests.selenium.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.revature.repository.DatabaseConnection;
import com.revature.systemtests.selenium.utils.TestContext;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;

public class Hooks {
    private static TestContext context;
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static DatabaseConnection databaseConnection;

    // For database state management
    private static Connection connection;

    @BeforeAll
    public static void globalSetup() {
        System.out.println("========================================");
        System.out.println("Starting test execution...");
        System.out.println("========================================");

        context = TestContext.getInstance();
        driver = context.getDriver();
        wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
        databaseConnection = new DatabaseConnection();

        // Check if application is running
        driver.get(context.getBaseUrl() + "/health");
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("healthy"), "App should be running");

        // Login setup
        driver.get(context.getBaseUrl());

        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.cssSelector("button[type='submit']"));

        usernameInput.sendKeys("manager1");
        passwordInput.sendKeys("password123");
        loginButton.click();

        // Verify login success by checking for a username display element
        WebElement usernameElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username-display")));
        assertEquals("manager1", usernameElement.getText(), "Logged in username should match");
    }

    @AfterAll
    public static void globalTeardown() {
        System.out.println("========================================");
        System.out.println("Test execution complete.");
        System.out.println("========================================");

        // ✅ Prevent NPE if setup failed early
        if (context != null) {
            context.tearDown();
            context = null;
        }
    }

    @Before("@emptyDatabase")
    public static void emptyDatabase() {
        try {
            // Start transaction to enable rollback
            databaseConnection = new DatabaseConnection();
            connection = databaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Delete from approvals first due to foreign key constraints
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM approvals")) {
                int approvalsDeleted = stmt.executeUpdate();
                System.out.println("Deleted " + approvalsDeleted + " approval records");
            }

            // Delete from expenses
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM expenses")) {
                int expensesDeleted = stmt.executeUpdate();
                System.out.println("Deleted " + expensesDeleted + " expense records");
            }

            // Commit the deletions
            connection.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to empty database", e);
        }
    }

    @After("@restoreDatabase")
    public static void restoreDatabase() {
        emptyDatabase();
        try {
            if (connection != null) {
                // If connection was started, seed with default test data
                seedDefaultTestData();
            }
        } catch (SQLException e) {
            System.err.println("Failed to restore database: " + e.getMessage());
            try {
                // Fallback: seed with default test data
                seedDefaultTestData();
            } catch (SQLException seedEx) {
                System.err.println("Failed to seed default test data: " + seedEx.getMessage());
            }
            throw new RuntimeException("Failed to restore database", e);
        } finally {
            // Clean up resources
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Failed to close transaction connection: " + e.getMessage());
            }

            // Reset transaction state
            connection = null;
        }
    }

    @Before("@seedPendingExpense")
    public static void seedPendingExpense() {
        try (Connection conn = databaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            String insertExpenseSql = "INSERT INTO expenses (user_id, amount, description, date) VALUES (?, ?, ?, ?)";

            int pendingExpenseId;

            // Pending expense
            try (PreparedStatement stmt = conn.prepareStatement(insertExpenseSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, 1); // user_id
                stmt.setDouble(2, 1500.00);
                stmt.setString(3, "Travel Flight");
                stmt.setString(4, "2026-01-05");
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                rs.next();
                pendingExpenseId = rs.getInt(1);
            }

            // Insert corresponding approval
            String insertApprovalSql = "INSERT INTO approvals (expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(insertApprovalSql)) {
                stmt.setInt(1, pendingExpenseId);
                stmt.setString(2, "pending");
                stmt.setObject(3, null); // no reviewer yet
                stmt.setObject(4, null); // no comment yet
                stmt.setObject(5, null); // no review date yet
                stmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed pending expenses", e);
        }
    }

    /**
     * Seed database with default test data: one pending, one approved, one denied expense
     */
    private static void seedDefaultTestData() throws SQLException {
        try (Connection conn = databaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            String insertExpenseSql = "INSERT INTO expenses (user_id, amount, description, date) VALUES (?, ?, ?, ?)";

            int pendingExpenseId, approvedExpenseId, deniedExpenseId;

            // Pending expense
            try (PreparedStatement stmt = conn.prepareStatement(insertExpenseSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, 1); // user_id
                stmt.setDouble(2, 10.0);
                stmt.setString(3, "Pizza");
                stmt.setString(4, "2025-12-29");
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                rs.next();
                pendingExpenseId = rs.getInt(1);
            }

            // Approved expense
            try (PreparedStatement stmt = conn.prepareStatement(insertExpenseSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, 1); // user_id
                stmt.setDouble(2, 25.01);
                stmt.setString(3, "Notebook");
                stmt.setString(4, "2025-12-22");
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                rs.next();
                approvedExpenseId = rs.getInt(1);
            }

            // Denied expense
            try (PreparedStatement stmt = conn.prepareStatement(insertExpenseSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, 1); // user_id
                stmt.setDouble(2, 500.05);
                stmt.setString(3, "Hotel");
                stmt.setString(4, "2025-12-25");
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                rs.next();
                deniedExpenseId = rs.getInt(1);
            }

            // Insert corresponding approvals
            String insertApprovalSql = "INSERT INTO approvals (expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?)";

            // Pending approval
            try (PreparedStatement stmt = conn.prepareStatement(insertApprovalSql)) {
                stmt.setInt(1, pendingExpenseId);
                stmt.setString(2, "pending");
                stmt.setObject(3, null);
                stmt.setObject(4, null);
                stmt.setObject(5, null);
                stmt.executeUpdate();
            }

            // Approved approval
            try (PreparedStatement stmt = conn.prepareStatement(insertApprovalSql)) {
                stmt.setInt(1, approvedExpenseId);
                stmt.setString(2, "approved");
                stmt.setInt(3, 2); // assuming manager user ID is 2
                stmt.setString(4, "Good choice for note taking.");
                stmt.setString(5, "2025-12-29 14:12:35");
                stmt.executeUpdate();
            }

            // Denied approval
            try (PreparedStatement stmt = conn.prepareStatement(insertApprovalSql)) {
                stmt.setInt(1, deniedExpenseId);
                stmt.setString(2, "denied");
                stmt.setInt(3, 2); // assuming manager user ID is 2
                stmt.setString(4, "Expense not covered for holidays.");
                stmt.setString(5, "2025-12-29 14:11:47");
                stmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Seeded database with default test data: 1 pending, 1 approved, 1 denied expense");
        }
    }
}
