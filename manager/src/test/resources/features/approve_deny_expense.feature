Feature: Manager Expense Review
  As a manager
  I want to review pending expenses
  So that I can approve or deny reimbursement requests

  Background:
    Given the manager is viewing the pending expenses list

  @positive @seedPendingExpense @restoreDatabase
  Scenario: Successfully approve a pending flight expense
    When the manager clicks "Review" for the "Travel Flight" expense
    And the manager clicks the "Approve" button in the modal
    Then the expense should no longer appear in the pending list

  @negative @seedPendingExpense @restoreDatabase
  Scenario: Successfully deny a pending flight expense
    When the manager clicks "Review" for the "Travel Flight" expense
    And the manager clicks the "Deny" button in the modal
    Then the expense should no longer appear in the pending list

  @audit @seedPendingExpense @restoreDatabase
  Scenario: Verify approved expense with comment persists in All Expenses
    When the manager clicks "Review" for the "Travel Flight" expense
    And the manager enters "Standard economy flight approved" into the comment field
    And the manager clicks the "Approve" button in the modal
    And the manager navigates to the "All Expenses" section
    Then the "Travel Flight" expense should have a status of "APPROVED"
    And the comment "Standard economy flight approved" should be visible in the record