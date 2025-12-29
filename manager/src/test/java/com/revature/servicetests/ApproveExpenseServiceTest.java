// User Story: As a manager I want to approve or deny an expense with optional comments
// so that I can provide feedback on expense decisions

package com.revature.servicetests;

import com.revature.repository.ApprovalRepository;
import com.revature.repository.ExpenseRepository;
import com.revature.service.ExpenseService;

import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApproveExpenseServiceTest {

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    private ExpenseService expenseService;
    @BeforeEach
    public void setUp() {
        // Create the service with mocked dependencies
        expenseService = new ExpenseService(expenseRepository, approvalRepository);
    }

    // ==================== APPROVE EXPENSE TESTS ====================

    @Story("As a manager I want to approve an expense with optional comments for feedback on expense decisions")
    @Test
    @DisplayName("R-9_001: Happy Path - Approve Expense with Comment")
    void testApproveExpenseWithCommentHappy() {
        // Arrange
        int expenseId = 1;
        int managerId = 100;
        String comment = "Approved for reimbursement";

        // Stub the repository to return true when updating approval status
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, "approved", managerId, comment))
                .thenReturn(true);
        // Act
        boolean result = expenseService.approveExpense(expenseId, managerId, comment);

        // Assert - Verify the result is true
        Assertions.assertTrue(result, "Approve expense should return true");

        // Verify the repository method was called exactly once with correct parameters
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, "approved", managerId, comment);
        // Verify no other interactions with the repository
        Mockito.verifyNoMoreInteractions(approvalRepository);
    }

    @Story("As a manager I want to approve an expense without comments")
    @Test
    @DisplayName("R-9_002: Happy Path - Approve Expense without Comment")
    void testApproveExpenseWithoutCommentHappy() {
        // Arrange
        int expenseId = 2;
        int managerId = 101;
        String comment = null;  // No comment provided

        // Stub the repository to return true
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, "approved", managerId, comment))
                .thenReturn(true);

        // Act
        boolean result = expenseService.approveExpense(expenseId, managerId, comment);

        // Assert
        Assertions.assertTrue(result, "Approve expense without comment should return true");

        // Verify the repository was called with null comment
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, "approved", managerId, comment);

        // Verify repository was called with null, not empty string
        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(approvalRepository).updateApprovalStatus(
                Mockito.eq(expenseId),
                Mockito.eq("approved"),
                Mockito.eq(managerId),
                commentCaptor.capture()
        );
        Assertions.assertNull(commentCaptor.getValue(), "Comment should be null");
    }

    @Story("As a manager I want to approve an expense")
    @Test
    @DisplayName("R-9_003: Sad Path - Repository Throws Exception")
    void testApproveExpenseRepositoryExceptionSad() {
        // Arrange
        int expenseId = 3;
        int managerId = 102;
        String comment = "Please expedite approval";

        // Stub the repository to throw an exception
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, "approved", managerId, comment))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
            expenseService.approveExpense(expenseId, managerId, comment);
        });

        Assertions.assertEquals("Database error", exception.getMessage());

        // Verify the repository method was called
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, "approved", managerId, comment);
    }

    @Story("As a manager I want to approve an expense")
    @Test
    @DisplayName("TC_APR_004: Sad Path - Invalid IDs Return False")
    void testApproveExpenseInvalidIdsSad() {
        // Arrange
        int expenseId = -1;     // Invalid expense ID
        int managerId = -100;   // Invalid manager user ID
        String comment = "Invalid IDs test";

        // Stub the repository to return false for invalid IDs
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, "approved", managerId, comment))
                .thenReturn(false);

        // Act
        boolean result = expenseService.approveExpense(expenseId, managerId, comment);

        // Assert
        Assertions.assertFalse(result, "Approve with invalid IDs should return false");

        // Verify the repository was called
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, "approved", managerId, comment);
    }

    @Story("As a manager I want to approve an expense")
    @Test
    @DisplayName("TC_APR_005: Edge Case - Empty String Comment")
    void testApproveExpenseEmptyCommentEdge() {
        // Arrange
        int expenseId = 4;
        int managerId = 103;
        String comment = "";  // Empty string comment

        // Stub the repository
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, "approved", managerId, comment))
                .thenReturn(true);

        // Act
        boolean result = expenseService.approveExpense(expenseId, managerId, comment);

        // Assert
        Assertions.assertTrue(result);

        // Verify empty string was passed, not null
        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(approvalRepository).updateApprovalStatus(
                Mockito.eq(expenseId),
                Mockito.eq("approved"),
                Mockito.eq(managerId),
                commentCaptor.capture()
        );
        Assertions.assertEquals("", commentCaptor.getValue(), "Comment should be empty string");
    }

    // ==================== DENY EXPENSE TESTS ====================

    @Story("As a manager I want to deny an expense with optional comments for feedback on expense decisions")
    @ParameterizedTest(name = "{index} => expenseId={0}, managerId={1}, comment={2}")
    @CsvSource({
            "10, 200, 'Denied due to policy violation'",
            "11, 201, ''",
            "12, 202, "  // null comment
    })
    @DisplayName("TC_DEN_001: Happy Path - Deny Expense with Optional Comment")
    void testDenyExpenseHappy(int expenseId, int managerId, String comment) {
        // Arrange
        // Stub the repository to return true
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, "denied", managerId, comment))
                .thenReturn(true);

        // Act
        boolean result = expenseService.denyExpense(expenseId, managerId, comment);

        // Assert
        Assertions.assertTrue(result,
                "Deny expense should return true for expenseId=" + expenseId);

        // Verify the repository was called
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, "denied", managerId, comment);
    }

    @Story("As a manager I want to deny an expense")
    @Test
    @DisplayName("TC_DEN_002: Sad Path - Repository Throws Exception")
    void testDenyExpenseRepositoryExceptionSad() {
        // Arrange
        int expenseId = 13;
        int managerId = 203;
        String comment = "Repository exception test";

        // Stub the repository to throw an exception
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, "denied", managerId, comment))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
            expenseService.denyExpense(expenseId, managerId, comment);
        });

        Assertions.assertEquals("Database error", exception.getMessage());

        // Verify the repository was called
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, "denied", managerId, comment);
    }

    @Story("As a manager I want to deny an expense")
    @Test
    @DisplayName("TC_DEN_003: Edge Case - Very Long Comment")
    void testDenyExpenseLongCommentEdge() {
        // Arrange
        int expenseId = 14;
        int managerId = 204;

        // Generate a long comment (local to this test)
        StringBuilder longCommentBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longCommentBuilder.append("This is a very long comment. ");
        }
        String longComment = longCommentBuilder.toString();

        // Stub the repository to throw an exception for long comments
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, "denied", managerId, longComment))
                .thenThrow(new RuntimeException("Comment too long"));

        // Act & Assert
        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
            expenseService.denyExpense(expenseId, managerId, longComment);
        });

        Assertions.assertEquals("Comment too long", exception.getMessage());

        // Verify the repository was called with the long comment
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, "denied", managerId, longComment);
    }

    @Story("As a manager I want to deny an expense")
    @Test
    @DisplayName("TC_DEN_005: Boundary - Zero IDs")
    void testDenyExpenseZeroIdsBoundary() {
        // Arrange
        int expenseId = 0;
        int managerId = 0;
        String comment = "Zero IDs test";

        // Stub the repository to return false
        Mockito.when(approvalRepository.updateApprovalStatus(expenseId, "denied", managerId, comment))
                .thenReturn(false);

        // Act
        boolean result = expenseService.denyExpense(expenseId, managerId, comment);

        // Assert
        Assertions.assertFalse(result, "Deny with zero IDs should return false");

        // Verify the repository was called with zero IDs
        Mockito.verify(approvalRepository, Mockito.times(1))
                .updateApprovalStatus(expenseId, "denied", managerId, comment);
    }

    // ==================== VERIFICATION TESTS ====================

    @Story("As a manager I want to approve an expense")
    @Test
    @DisplayName("Verify approve passes correct status string to repository")
    void testApproveExpensePassesCorrectStatus() {
        // Arrange
        int expenseId = 100;
        int managerId = 300;
        String comment = "Test";

        Mockito.when(approvalRepository.updateApprovalStatus(
                        Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(true);

        // Act
        expenseService.approveExpense(expenseId, managerId, comment);

        // Assert - Verify "approved" status was passed (not "approve" or "APPROVED")
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(approvalRepository).updateApprovalStatus(
                Mockito.eq(expenseId),
                statusCaptor.capture(),
                Mockito.eq(managerId),
                Mockito.eq(comment)
        );
        Assertions.assertEquals("approved", statusCaptor.getValue(),
                "Status should be exactly 'approved'");
    }

    @Story("As a manager I want to deny an expense")
    @Test
    @DisplayName("Verify deny passes correct status string to repository")
    void testDenyExpensePassesCorrectStatus() {
        // Arrange
        int expenseId = 101;
        int managerId = 301;
        String comment = "Test";

        Mockito.when(approvalRepository.updateApprovalStatus(
                        Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(true);

        // Act
        expenseService.denyExpense(expenseId, managerId, comment);

        // Assert - Verify "denied" status was passed (not "deny" or "DENIED")
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(approvalRepository).updateApprovalStatus(
                Mockito.eq(expenseId),
                statusCaptor.capture(),
                Mockito.eq(managerId),
                Mockito.eq(comment)
        );
        Assertions.assertEquals("denied", statusCaptor.getValue(),
                "Status should be exactly 'denied'");
    }
}