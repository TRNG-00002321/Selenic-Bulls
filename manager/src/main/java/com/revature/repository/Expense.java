package com.revature.repository;

/**
 * Expense model representing an expense submission.
 */
public class Expense {
    private int id;
    private int userId;
    private double amount;
    private String description;
    private String date;
    
    public Expense() {}
    
    public Expense(int id, int userId, double amount, String description, String date) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.description = description;
        this.date = date;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", userId=" + userId +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}