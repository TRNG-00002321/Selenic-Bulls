Feature: Update Pending Expense
    As an employee
    I want to edit expenses that are still pending 
    so that I can correct mistakes before they are reviewed

    Background:
        Given I am on the employee expenses page
        And I have a pending expense with description "Pizza" and amount "$10.00"

    @positive
    Scenario: Update a pending expense successfully
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I update the description to "Pizza", the amount to "10.00", and the date to "2025-12-29"
        And I click the update expense button
        Then I should see a message "Expense updated successfully!"

    @negative
    Scenario: Cancel updating a pending expense
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I click the cancel button
        Then I should see a my expenses header titled "My Expenses"
        And I should see the expense with description "Pizza" and status "Pending" unchanged in the expenses list
        
    @negative @cancel
    Scenario Outline: Fail to update a pending expense with invalid input values
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I update the <field> to "<value>"
        And I click the update expense button
        Then I should see a <field> validation error message containing "<error_text>"
        
        Examples:
            | field       | value      | error_text              |
            | amount      | -5.00      | 0.01                    |
            | description | WHITESPACE | Description is required |
            | date        | yyyy-12-dd | Please                  |

    @negative @cancel
    Scenario Outline: Fail to update a pending expense with empty required fields
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I clear the "<field>" field
        And I click the update expense button
        Then I should see a <field> validation error message containing "<error_text>"
        
        Examples:
            | field       | error_text      |
            | amount      | Please fill out |
            | description | Please fill out |
            | date        | Please fill out |