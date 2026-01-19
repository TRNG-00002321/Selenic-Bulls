@logout
Feature: Employee Logout
  As an authenticated employee
  I want to log out of the system
  So that I can securely end my session

  Background:
    Given the user is logged in to the system

  @smoke @positive
  Scenario: Successful logout
    When the user clicks the logout button
    Then the logout should be successful
    And the user should be redirected to the login page
    And the user should no longer be authenticated

  @negative
  Scenario: Access protected page after logout
    When the user clicks the logout button
    And the user tries to access a protected page
    Then the user should be redirected to the login page