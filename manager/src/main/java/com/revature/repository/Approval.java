package com.revature.repository;

/**
 * Approval model representing the approval status of an expense.
 */
public class Approval {
    private int id;
    private int expenseId;
    private String status;
    private Integer reviewer;
    private String comment;
    private String reviewDate;
    
    public Approval() {}
    
    public Approval(int id, int expenseId, String status, Integer reviewer, String comment, String reviewDate) {
        this.id = id;
        this.expenseId = expenseId;
        this.status = status;
        this.reviewer = reviewer;
        this.comment = comment;
        this.reviewDate = reviewDate;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getExpenseId() {
        return expenseId;
    }
    
    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getReviewer() {
        return reviewer;
    }
    
    public void setReviewer(Integer reviewer) {
        this.reviewer = reviewer;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public String getReviewDate() {
        return reviewDate;
    }
    
    public void setReviewDate(String reviewDate) {
        this.reviewDate = reviewDate;
    }
    
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }
    
    @Override
    public String toString() {
        return "Approval{" +
                "id=" + id +
                ", expenseId=" + expenseId +
                ", status='" + status + '\'' +
                ", reviewer=" + reviewer +
                ", comment='" + comment + '\'' +
                ", reviewDate='" + reviewDate + '\'' +
                '}';
    }
}