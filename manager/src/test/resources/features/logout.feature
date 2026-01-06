@logout @ui
Feature: User Logout
  As a logged-in user
  I want to log out
  So that my session is ended and protected pages are no longer accessible

  Background:
    Given the user is logged in

  @smoke @positive
  Scenario: Successful logout from the manager dashboard
    When the user clicks the logout button
    Then the user should be redirected to the login page

  @regression @negative
  Scenario: User cannot access the manager dashboard after logging out
    When the user clicks the logout button
    And the user navigates to the manager dashboard
    Then the user should be redirected to the login page
