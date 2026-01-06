@login
Feature: Employee Login
  As an employee
  I want to log in to the system
  So that I can access protected features

  Background:
    Given the login service is available

  @smoke @positive
  Scenario: Successful login with valid credentials
    When the user logs in with username "employee1" and password "password123"
    Then the login should be successful
    And the user should be authenticated

  @negative
  Scenario: Login fails with incorrect password
    When the user logs in with username "employee1" and password "wrongpassword"
    Then the login should fail
    And an error message should be returned

  @negative
  Scenario: Login fails with non-existent username
    When the user logs in with username "unknown_user" and password "password123"
    Then the login should fail
    And an error message should be returned

  @negative
  Scenario: Login fails with empty username and password
    When the user submits the login form with empty username and empty password
    Then a username required validation message is shown
    And the user remains on the login page


  @negative
  Scenario: Login fails with empty password
    When the user submits the login form with username "employee1" and empty password
    Then a password required validation message is shown
    And the user remains on the login page


