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
    Scenario: Fail to update a pending expense with a negative amount
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I update the amount to "-5.00"
        And I click the update expense button
        Then I should see an amount validation error message containing "0.01"
    
    @negative @cancel
    Scenario: Fail to update a pending expense with a space description
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I update the description to "   "
        And I click the update expense button
        Then I should see a message "Description is required"


    @negative @cancel
    Scenario: Fail to update a pending expense with a partial date
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I update the date to "yyyy-12-dd"
        And I click the update expense button
        Then I should see a date validation error message containing "Please"

    @negative @cancel
    Scenario: Fail to update a pending expense with an empty amount
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I clear the "amount" field
        And I click the update expense button
        Then I should see an amount validation error message containing "Please"
    
    @negative @cancel
    Scenario: Fail to update a pending expense with an empty description
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I clear the "description" field
        And I click the update expense button
        Then I should see a description validation error message "Please fill out this field."
    
    @negative @cancel
    Scenario: Fail to update a pending expense with an empty date
        When I click the edit button for the expense with description "Pizza" and status "Pending"
        Then I should see an edit expense header titled "Edit Expense"
        When I clear the "date" field
        And I click the update expense button
        Then I should see a date validation error message containing "Please"