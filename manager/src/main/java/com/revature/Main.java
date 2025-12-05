package com.revature;

import com.revature.api.AuthenticationMiddleware;
import com.revature.api.ExpenseController;
import com.revature.api.ReportController;
import com.revature.repository.DatabaseConnection;
import com.revature.repository.UserRepository;
import com.revature.repository.ExpenseRepository;
import com.revature.repository.User;
import com.revature.repository.ApprovalRepository;
import com.revature.service.AuthenticationService;
import com.revature.service.ExpenseService;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.util.Map;

/**
 * Main application class for the Revature Expense Manager (Manager App).
 * Sets up dependency injection and configures Javalin web server with REST endpoints.
 */
public class Main {
    private static final int PORT = 5001;
    
    public static void main(String[] args) {
        // Initialize dependencies using constructor dependency injection
        DatabaseConnection databaseConnection = new DatabaseConnection();
        
        // Repository layer
        UserRepository userRepository = new UserRepository(databaseConnection);
        ExpenseRepository expenseRepository = new ExpenseRepository(databaseConnection);
        ApprovalRepository approvalRepository = new ApprovalRepository(databaseConnection);
        
        // Service layer
        AuthenticationService authenticationService = new AuthenticationService(userRepository);
        ExpenseService expenseService = new ExpenseService(expenseRepository, approvalRepository);
        
        // API layer
        AuthenticationMiddleware authMiddleware = new AuthenticationMiddleware(authenticationService);
        ExpenseController expenseController = new ExpenseController(expenseService);
        ReportController reportController = new ReportController(expenseService);
        
        // Configure and start Javalin application
        Javalin app = Javalin.create(config -> {
            // Enable CORS for cross-origin requests from frontend
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowHost("http://127.0.0.1:5000");
                    it.allowHost("http://localhost:5000");
                    it.allowCredentials = true;
                });
            });
            
            // Enable static file serving from resources
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/";
                staticFiles.location = Location.CLASSPATH;
            });
            
            // Enable request logging
            config.bundledPlugins.enableDevLogging();
        });
        
        // Global exception handling
        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.json(java.util.Map.of(
                "success", false,
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        });
        
        // Root redirect to manager dashboard
        app.get("/", ctx -> ctx.redirect("/manager.html"));
        
        // Authentication status endpoint (no auth required)
        app.get("/api/auth/status", ctx -> {
            String jwtToken = ctx.cookie("jwt");
            
            java.util.Optional<com.revature.repository.User> managerOpt = authenticationService.validateManagerAuthentication(jwtToken);
            
            if (managerOpt.isPresent()) {
                com.revature.repository.User manager = managerOpt.get();
                ctx.json(java.util.Map.of(
                    "authenticated", true,
                    "user", java.util.Map.of(
                        "id", manager.getId(),
                        "username", manager.getUsername(),
                        "role", manager.getRole()
                    )
                ));
            } else {
                ctx.json(java.util.Map.of("authenticated", false));
            }
        });
        
        // Manager login endpoint (no auth required)
        app.post("/api/auth/login", ctx -> {
            try {
                // Parse login request
                // @SuppressWarnings("unchecked")
                User loginData = ctx.bodyAsClass(User.class);
                System.out.println("Login attempt for user: " + loginData.getUsername());
                String username = loginData.getUsername();
                String password = loginData.getPassword();

                if (username == null || password == null) {
                    ctx.status(400);
                    ctx.json(Map.of(
                        "success", false,
                        "error", "Username and password are required"
                    ));
                    return;
                }
                
                // Authenticate manager
                System.out.println("Attempting authentication for user: " + username);
                java.util.Optional<com.revature.repository.User> managerOpt = authenticationService.authenticateManager(username, password);
                
                if (managerOpt.isPresent()) {
                    System.out.println("Authentication successful for user: " + username);
                    com.revature.repository.User manager = managerOpt.get();
                    
                    // Create JWT token
                    String jwtToken = authenticationService.createJwtToken(manager);
                    
                    // Set JWT as HTTP-only cookie
                    ctx.cookie("jwt", jwtToken, 24 * 60 * 60); // 24 hours expiry
                    
                    ctx.status(200);
                    ctx.json(Map.of(
                        "success", true,
                        "message", "Login successful",
                        "user", Map.of(
                            "id", manager.getId(),
                            "username", manager.getUsername(),
                            "role", manager.getRole()
                        )
                    ));
                } else {
                    ctx.status(401);
                    ctx.json(Map.of(
                        "success", false,
                        "error", "Invalid credentials or user is not a manager"
                    ));
                }
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "error", "Invalid request format"
                ));
            }
        });
        
        // Manager logout endpoint (no auth required)
        app.post("/api/auth/logout", ctx -> {
            // Clear the JWT cookie
            ctx.removeCookie("jwt");
            ctx.json(Map.of(
                "success", true,
                "message", "Logged out successfully"
            ));
        });
        
        // Protected routes - require manager authentication
        app.before("/api/expenses/*", authMiddleware.validateManager());
        app.before("/api/reports/*", authMiddleware.validateManager());
        
        // Expense management endpoints
        app.get("/api/expenses", expenseController::getAllExpenses);
        app.get("/api/expenses/pending", expenseController::getPendingExpenses);
        app.get("/api/expenses/employee/{employeeId}", expenseController::getExpensesByEmployee);
        app.post("/api/expenses/{expenseId}/approve", expenseController::approveExpense);
        app.post("/api/expenses/{expenseId}/deny", expenseController::denyExpense);
        
        // Report generation endpoints
        app.get("/api/reports/expenses/csv", reportController::generateAllExpensesReport);
        app.get("/api/reports/expenses/pending/csv", reportController::generatePendingExpensesReport);
        app.get("/api/reports/expenses/employee/{employeeId}/csv", reportController::generateEmployeeExpensesReport);
        app.get("/api/reports/expenses/category/{category}/csv", reportController::generateCategoryExpensesReport);
        app.get("/api/reports/expenses/daterange/csv", reportController::generateDateRangeExpensesReport);
        
        // Root route - serve manager dashboard
        
        // Health check endpoint
        app.get("/health", ctx -> ctx.json(java.util.Map.of(
            "status", "healthy",
            "service", "expense-manager-api",
            "version", "1.0.0"
        )));
        
        // Start the server
        app.start(PORT);
        
        System.out.println("   Expense Manager API (Manager App) started successfully!");
        System.out.println("   Server running on: http://localhost:" + PORT);
        System.out.println("   Health check: http://localhost:" + PORT + "/health");
        System.out.println("   API Documentation:");
        System.out.println("   Authentication Status: GET /api/auth/status");
        System.out.println("   Pending Expenses: GET /api/expenses/pending");
        System.out.println("   All Expenses: GET /api/expenses");
        System.out.println("   Employee Expenses: GET /api/expenses/employee/{employeeId}");
        System.out.println("   Approve Expense: POST /api/expenses/{expenseId}/approve");
        System.out.println("   Deny Expense: POST /api/expenses/{expenseId}/deny");
        System.out.println("   CSV Reports: GET /api/reports/expenses/csv");
        System.out.println("   More reports available at /api/reports/expenses/...");
    }
}