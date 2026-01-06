Feature: Submit New Expense
  As an employee
  I want to submit a new expense reimbursement request
  So that I can be paid back for business expenses

  @submitExpense
  Scenario: Successfully submit an expense
    Given the employee clicks the "Submit New Expense" button
    When the employee enters an amount of "125.50"
    And the employee enters "Team Lunch at Olive Garden" as the description
    And the employee enters a date of "2026-01-04"
    And the employee clicks the "Submit Expense" button
    Then a success message "Expense submitted successfully" should be displayed