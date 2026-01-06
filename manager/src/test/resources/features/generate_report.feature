Feature: Generate CSV Reports
  As a manager
  I want to generate different CSV reports
  So that I can analyze expense data offline

  Background:
    Given I am on the login page
    When I login with valid credentials
    And I navigate to the reports page

  # =========================
  # QUICK REPORTS
  # =========================

  @positive @quick
  Scenario Outline: Generate quick CSV reports
    When I generate the "<report>" report
    Then a CSV file should be downloaded
    And the CSV file should contain the expected data

    Examples:
      | report                |
      | All Expenses          |
      | Pending Expenses      |

  # =========================
  # EMPLOYEE REPORT
  # =========================

  @positive @employee
  Scenario: Generate employee CSV report
    When I generate an employee report for ID "1"
    Then a CSV file should be downloaded
    And the CSV file should contain the expected data

  @negative @employee
  Scenario: Generate employee CSV report without specifying an employee ID
    When the user submits the expense by employee report without specifying the employee ID
    Then an employee ID required validation message is shown
    And the user remains on the report page


  # =========================
  # CATEGORY REPORT
  # =========================

  @positive @category
  Scenario: Generate category CSV report
    When I generate a category report for "Travel"
    Then a CSV file should be downloaded
    And the CSV file should contain the expected data


  @negative @category
  Scenario: Generate category CSV report without specifying a category
    When the user submits the expense by category report without specifying the category
    Then a category required validation message is shown
    And the user remains on the report page


  # =========================
  # DATE RANGE REPORT
  # =========================

  @positive @date
  Scenario: Generate date range CSV report
    When I generate a date range report from "12/01/2025" to "12/30/2025"
    Then a CSV file should be downloaded
    And the CSV file should contain the expected data


  @negative @date
  Scenario: Generate date range CSV report with empty dates
    When the user submits the expense by date range report without specifying start and end dates
    Then a date required validation message is shown
    And the user remains on the report page

