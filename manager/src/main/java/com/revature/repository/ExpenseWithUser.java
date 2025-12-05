package com.revature.repository;

/**
 * ExpenseWithUser model representing an expense with associated user information.
 * Used for manager views that need both expense and employee details.
 */
public class ExpenseWithUser {
    private Expense expense;
    private User user;
    private Approval approval;
    
    public ExpenseWithUser() {}
    
    public ExpenseWithUser(Expense expense, User user, Approval approval) {
        this.expense = expense;
        this.user = user;
        this.approval = approval;
    }
    
    // Getters and setters
    public Expense getExpense() {
        return expense;
    }
    
    public void setExpense(Expense expense) {
        this.expense = expense;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Approval getApproval() {
        return approval;
    }
    
    public void setApproval(Approval approval) {
        this.approval = approval;
    }
    
    @Override
    public String toString() {
        return "ExpenseWithUser{" +
                "expense=" + expense +
                ", user=" + user +
                ", approval=" + approval +
                '}';
    }
}