Feature: Expense history retrieval (all and status-filtered)
  As an employee
  I want to view my expense history filtered by status
  So that I can track pending, approved, and denied expenses

  Background:
    Given I am on the employee expenses page

  @api @expenses @all @restore_db
  Scenario: Get all expenses (no status filter)
    When I select the expense status filter "All"
    And I refresh the expense list
    Then I should see the expense list loaded successfully
    And each expense row should display a status
    And the displayed expense count should match the number of rows

  @api @expenses @pending
  Scenario: Get pending expenses (status filter = pending)
    When I select the expense status filter "Pending"
    And I refresh the expense list
    Then I should see the expense list loaded successfully
    And every displayed expense should have status "Pending"

  @api @expenses @approved
  Scenario: Get approved expenses (status filter = approved)
    When I select the expense status filter "Approved"
    And I refresh the expense list
    Then I should see the expense list loaded successfully
    And every displayed expense should have status "Approved"

  @api @expenses @denied
  Scenario: Get denied expenses (status filter = denied)
    When I select the expense status filter "Denied"
    And I refresh the expense list
    Then I should see the expense list loaded successfully
    And every displayed expense should have status "Denied"

  @api @expenses @empty @pending @restore_db
  Scenario: Pending filter returns empty expenses list
    Given I have no expenses in the system
    When I select the expense status filter "Pending"
    And I refresh the expense list
    Then I should see the expense list loaded successfully
    And I should see an empty expense list message
    And the displayed expense count should be "0"

  @api @expenses @empty @approved @restore_db
  Scenario: Approved filter returns empty expenses list
    Given I have no expenses in the system
    When I select the expense status filter "Approved"
    And I refresh the expense list
    Then I should see the expense list loaded successfully
    And I should see an empty expense list message
    And the displayed expense count should be "0"

  @api @expenses @empty @denied @restore_db
  Scenario: Denied filter returns empty expenses list
    Given I have no expenses in the system
    When I select the expense status filter "Denied"
    And I refresh the expense list
    Then I should see the expense list loaded successfully
    And I should see an empty expense list message
    And the displayed expense count should be "0"

  @api @expenses @empty @all @restore_db
  Scenario: All filter returns empty expenses list
    Given I have no expenses in the system
    When I select the expense status filter "All"
    And I refresh the expense list
    Then I should see the expense list loaded successfully
    And I should see an empty expense list message
    And the displayed expense count should be "0"