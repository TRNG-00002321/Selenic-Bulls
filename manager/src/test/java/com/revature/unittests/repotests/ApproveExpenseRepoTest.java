package com.revature.unittests.repotests;
// User Story testing: As a manager i want to approve or deny an expense with optional comments so that i can provide feedback on expense decisions
// Testing these methods in ApprovalRepository: updateApprovalStatus(...), updateApprovalStatus, createApproval, mapRowToApproval, and findByExpenseId()

import com.revature.repository.*;
import com.revature.repository.ExpenseRepository;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApproveExpenseRepoTest {
    @Mock
    private ApprovalRepository approvalRepository;
    // Test methods for:UpdateApprovalStatus(): Happy Path, Sad Path, Edge Cases
    @Story( "As a manager I want to approve or deny an expense with optional comments so that i can provide feedback on expense decisions" )
    @DisplayName( "Happy Path: Update Approval Status:approved or denied" )
    @ParameterizedTest(name = "expenseId={0}, status={1}, reviewerId={2}, comment={3}" )
    @CsvSource({
            "1, approved, 100, Approved for reimbursement",
            "2, denied, 101, Denied due to policy violation",
            "3, approved, 102, ",
            "4, denied, 103, Insufficient documentation"
    })
    void testUpdateApprovalStatusHappy( int expenseId, String status, int reviewerId, String comment ) {
        // Arrange
        // Mocking the repository to return true when updating approval status :Stubbing
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, status, reviewerId, comment))
                .thenReturn(true);
        // Act: Update the approval status, capturing the result
        boolean result = approvalRepository.updateApprovalStatus(expenseId, status, reviewerId, comment);
        // Assert: Verify the result is true
        Assertions.assertTrue(result);
        // Verify if method was called exactly once
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, status, reviewerId, comment);
    }
    // Sad Path: Invalid expense ID
    @Story( "As a manager I want to approve or deny an expense with optional comments so that i can provide feedback on expense decisions" )
    @ParameterizedTest(name = "expenseId={0}, status={1}, reviewerId={2}, comment={3}" )
    @DisplayName( "Sad Path: Update Approval Status: Invalid Expense ID" )
    @CsvSource({
            "-1, approved, 100, Approved for reimbursement",
            "0, denied, 101, Denied due to policy violation"
    })

    void testUpdateApprovalStatusSad_InvalidExpenseId( int expenseId, String status, int reviewerId, String comment ) {
        // Arrange
        // Mocking the repository to return false when updating approval status with invalid expense ID :Stubbing
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, status, reviewerId, comment))
                .thenReturn(false);
        // Act: Update the approval status, capturing the result
        boolean result = approvalRepository.updateApprovalStatus(expenseId, status, reviewerId, comment);
        // Assert: Verify the result is false
        Assertions.assertFalse(result);
        // Verify if method was called exactly once
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, status, reviewerId, comment);

    }

    // Test createApproval
    @Story( "As a manager I want to approve or deny an expense with optional comments so that i can provide feedback on expense decisions" )
    @Test
    @DisplayName( "Happy Path: Create Approval Record" )
    void testCreateApprovalHappy() {
        // Arrange
        int expenseId = 5;
        String status = "pending";
        Approval mockApproval = new Approval(1, expenseId, status, 0, null, null);
        // Mocking the repository to return a mock Approval object when creating approval
        Mockito.when(approvalRepository.createApproval(expenseId, status))
                .thenReturn(mockApproval);
        // Act: Create the approval, capturing the result
        Approval result = approvalRepository.createApproval(expenseId, status);
        // Assert: Verify the result matches the mock Approval
        Assertions.assertEquals(mockApproval, result);
        Assertions.assertEquals(expenseId, result.getExpenseId());
        Assertions.assertEquals(status, result.getStatus());

        // Verify if method was called exactly once
        Mockito.verify(approvalRepository, Mockito.times(1))
                .createApproval(expenseId, status);

    }

    // Sad path: Create approval with invalid expense ID
    @Story( "As a manager I want to approve or deny an expense with optional comments so that i can provide feedback on expense decisions" )
    @Test
    @DisplayName( "Sad Path: Create Approval Record with Invalid Expense ID" )
    void testCreateApprovalSad_InvalidExpenseId() {
        // Arrange
        int expenseId = -10; // Invalid expense ID
        String status = "pending";
        // Mocking the repository to throw an exception when creating approval with invalid expense ID
        Mockito.when(approvalRepository.createApproval(expenseId, status))
                .thenThrow(new IllegalArgumentException("Invalid expense ID"));
        // Act & Assert: Expect an exception when creating the approval
        Exception e = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            approvalRepository.createApproval(expenseId, status);
        });
        Assertions.assertEquals("Invalid expense ID", e.getMessage());
        // Verify if method was called exactly once
        Mockito.verify(approvalRepository, Mockito.times(1))
                .createApproval(expenseId, status);
    }
}
