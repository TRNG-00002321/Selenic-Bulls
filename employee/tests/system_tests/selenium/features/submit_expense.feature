Feature: Submit New Expense
  As an employee
  I want to submit a new expense reimbursement request
  So that I can be paid back for business expenses

  @restore_db
  Scenario: Successfully submit an expense
    Given the employee clicks the "Submit New Expense" button
    When the employee enters an amount of "125.50"
    And the employee enters "Team Lunch at Olive Garden" as the description
    And the employee enters a date of "2026-01-04"
    And the employee clicks the "Submit Expense" button
    Then a success message "Expense submitted successfully" should be displayed

  @negative
  Scenario Outline: Fail to submit an expense with invalid input values
    Given the employee clicks the "Submit New Expense" button
    When the employee enters invalid <field> value "<value>"
    And the employee clicks the "Submit Expense" button
    Then a <field> validation error message containing "<error_text>" should be displayed for submit form
    
    Examples:
      | field       | value      | error_text              |
      | amount      | -25.00     | 0.01                    |
      | description | WHITESPACE | Description is required |
      | date        | yyyy-01-dd | Please                  |

  @negative  
  Scenario Outline: Fail to submit an expense with empty required fields
    Given the employee clicks the "Submit New Expense" button
    When the employee leaves the "<field>" field empty
    And the employee clicks the "Submit Expense" button
    Then a <field> validation error message containing "<error_text>" should be displayed for submit form
    
    Examples:
      | field       | error_text      |
      | amount      | Please          |
      | description | Please          |
      | date        | Please          |