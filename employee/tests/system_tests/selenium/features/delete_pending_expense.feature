Feature: Delete Pending Expense
    As an employee
    I want to delete expenses that are still pending 
    so that I can remove mistakes before they are reviewed

    Background:
        Given I am on the employee expenses page 
        And I have a pending expense with description "Pizza" and amount "$10.00"

    @positive @restore_db
    Scenario: Delete a pending expense successfully
        When I click the delete button for the expense with description "Pizza" and status "Pending"
        And I confirm the deletion
        Then I should see a success alert message "Expense deleted successfully!"
        And I should not see the expense with description "Pizza" and status "Pending" in the expenses list
    
    @negative
    Scenario: Cancel deleting a pending expense
        When I click the delete button for the expense with description "Pizza" and status "Pending"
        And I cancel the deletion
        Then I should see the expense with description "Pizza" and status "Pending" unchanged in the expenses list