package com.revature.api;

import com.revature.repository.ExpenseWithUser;
import com.revature.repository.User;
import com.revature.service.ExpenseService;
import io.javalin.http.Context;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.InternalServerErrorResponse;

import java.util.List;
import java.util.Map;

/**
 * REST controller for expense management operations.
 * Handles expense approval, denial, and viewing operations for managers.
 */
public class ExpenseController {
    private final ExpenseService expenseService;
    
    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }
    
    /**
     * Get all pending expenses for manager review.
     * GET /api/expenses/pending
     */
    public void getPendingExpenses(Context ctx) {
        try {
            List<ExpenseWithUser> pendingExpenses = expenseService.getPendingExpenses();
            ctx.json(Map.of(
                "success", true,
                "data", pendingExpenses,
                "count", pendingExpenses.size()
            ));
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to retrieve pending expenses: " + e.getMessage());
        }
    }
    
    /**
     * Approve an expense.
     * POST /api/expenses/{expenseId}/approve
     * Request body: { "comment": "optional comment" }
     */
    public void approveExpense(Context ctx) {
        try {
            int expenseId = ctx.pathParamAsClass("expenseId", Integer.class).get();
            User manager = AuthenticationMiddleware.getAuthenticatedManager(ctx);
            
            // Get optional comment from request body
            String comment = null;
            try {
                Map<String, Object> requestBody = ctx.bodyAsClass(Map.class);
                comment = (String) requestBody.get("comment");
            } catch (Exception e) {
                // Ignore - comment is optional
            }
            
            boolean success = expenseService.approveExpense(expenseId, manager.getId(), comment);
            
            if (success) {
                ctx.json(Map.of(
                    "success", true,
                    "message", "Expense approved successfully"
                ));
            } else {
                throw new NotFoundResponse("Expense not found or could not be approved");
            }
            
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("Invalid expense ID format");
        } catch (Exception e) {
            if (e instanceof NotFoundResponse) {
                throw e;
            }
            throw new InternalServerErrorResponse("Failed to approve expense: " + e.getMessage());
        }
    }
    
    /**
     * Deny an expense.
     * POST /api/expenses/{expenseId}/deny
     * Request body: { "comment": "optional comment" }
     */
    public void denyExpense(Context ctx) {
        try {
            int expenseId = ctx.pathParamAsClass("expenseId", Integer.class).get();
            User manager = AuthenticationMiddleware.getAuthenticatedManager(ctx);
            
            // Get optional comment from request body
            String comment = null;
            try {
                Map<String, Object> requestBody = ctx.bodyAsClass(Map.class);
                comment = (String) requestBody.get("comment");
            } catch (Exception e) {
                // Ignore - comment is optional
            }
            
            boolean success = expenseService.denyExpense(expenseId, manager.getId(), comment);
            
            if (success) {
                ctx.json(Map.of(
                    "success", true,
                    "message", "Expense denied successfully"
                ));
            } else {
                throw new NotFoundResponse("Expense not found or could not be denied");
            }
            
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("Invalid expense ID format");
        } catch (Exception e) {
            if (e instanceof NotFoundResponse) {
                throw e;
            }
            throw new InternalServerErrorResponse("Failed to deny expense: " + e.getMessage());
        }
    }
    
    /**
     * Get all expenses (for general viewing).
     * GET /api/expenses
     */
    public void getAllExpenses(Context ctx) {
        try {
            List<ExpenseWithUser> allExpenses = expenseService.getAllExpenses();
            ctx.json(Map.of(
                "success", true,
                "data", allExpenses,
                "count", allExpenses.size()
            ));
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to retrieve expenses: " + e.getMessage());
        }
    }
    
    /**
     * Get expenses for a specific employee.
     * GET /api/expenses/employee/{employeeId}
     */
    public void getExpensesByEmployee(Context ctx) {
        try {
            int employeeId = ctx.pathParamAsClass("employeeId", Integer.class).get();
            List<ExpenseWithUser> expenses = expenseService.getExpensesByEmployee(employeeId);
            
            ctx.json(Map.of(
                "success", true,
                "data", expenses,
                "count", expenses.size(),
                "employeeId", employeeId
            ));
            
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("Invalid employee ID format");
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to retrieve expenses for employee: " + e.getMessage());
        }
    }
}