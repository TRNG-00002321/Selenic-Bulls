Feature: Get All Expenses
    As a manager
    I want to retrieve a list of all expenses
    So that I can review the expense history
    
    @positive
    Scenario: Retrieve all expenses successfully
        When I click the all expenses button
        Then I should see an all expenses header titled "All Expenses"
        And I should see a table title "All Expenses"
        And I should see a table of all expenses with attributes titled "Employee", "Date", "Amount", "Description", "Status", "Reviewer", "Comment"
        And the table should show at least one expense
    
    @positive
    Scenario: Retrieve all expenses filtered by employee
        When I click the all expenses button
        And I filter the expenses by employee ID "1"
        Then I should see a table title "Employee 1 Expenses"
        And I should see a table of all expenses with attributes titled "Employee", "Date", "Amount", "Description", "Status", "Reviewer", "Comment"
        And the table should show at least one expense

    @negative @emptyDatabase @restoreDatabase
    Scenario: Retrieve all expenses when there are none
        When I click the all expenses button
        Then I should see an all expenses header titled "All Expenses"
        And I should see an expenses error message "No expenses found."

    @negative
    Scenario: Retrieve all expenses filtered by non-existent employee
        When I click the all expenses button
        And I filter the expenses by employee ID "9999"
        Then I should see an expenses error message "No expenses found."