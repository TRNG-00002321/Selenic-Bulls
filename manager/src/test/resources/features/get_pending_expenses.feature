Feature: Get Pending Expenses
    As a manager
    I want to retrieve a list of pending expenses
    So that I can review and approve or deny them
    
    @positive
    Scenario: Retrieve pending expenses successfully
        When I click the pending expenses button
        Then I should see a pending expenses header titled "Pending Expenses for Review"
        And I should see a table of pending expenses with attributes titled "Employee", "Date", "Amount", "Description", "Actions"    
        And the table should show at least one pending expense

    
    @negative @emptyDatabase @restoreDatabase
    Scenario: Retrieve pending expenses when there are none
        When I click the pending expenses button
        Then I should see a pending expenses header titled "Pending Expenses for Review"
        And I should see a pending expenses error message "No pending expenses found."


    
