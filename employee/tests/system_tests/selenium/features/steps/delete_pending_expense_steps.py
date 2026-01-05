import pytest
from behave import when, then
from selenium.webdriver.support import expected_conditions as EC

@when('I click the delete button for the expense with description "{description}" and status "{status}"')
def step_click_delete_button(context, description, status):
    # Locate the expenses table
    table_element = context.wait.until(
        EC.visibility_of_element_located(("css selector", "#expenses-list table"))
    )
    assert table_element is not None
    
    # Pending expense is the first row after the header
    row = context.wait.until(
        EC.visibility_of_element_located(("css selector", "tbody tr:nth-child(2)"))
    )
    
    data_cells = row.find_elements("tag name", "td")
    
    assert data_cells[2].text == description, "Description of the expense does not match"
    assert data_cells[3].text.lower() == status.lower(), "Status of the expense does not match"
    
    delete_button = context.wait.until(
        EC.element_to_be_clickable(("xpath", "//button[normalize-space()='Delete']"))
    )
    delete_button.click()

@when("I confirm the deletion")
def step_confirm_deletion(context):
    # wait for browser popup and accept it
    alert = context.wait.until(EC.alert_is_present())
    assert alert.text == "Are you sure you want to delete this expense?"
    alert.accept()

@when("I cancel the deletion")
def step_cancel_deletion(context):
    # wait for browser popup and dismiss it
    alert = context.wait.until(EC.alert_is_present())
    assert alert.text == "Are you sure you want to delete this expense?"
    alert.dismiss()

@then('I should see a success alert message "{message}"')
def step_see_success_popup(context, message):
    alert = context.wait.until(EC.alert_is_present())
    assert alert.text == message
    alert.accept()

@then('I should not see the expense with description "{description}" and status "{status}" in the expenses list')
def step_not_see_expense(context, description, status):
    # Pending expense was the first row after the header
    row = context.wait.until(
        EC.visibility_of_element_located(("css selector", "tbody tr:nth-child(2)"))
    )
    
    data_cells = row.find_elements("tag name", "td")
    
    assert data_cells[1].text != "10.00"  # amount of deleted expense
    assert data_cells[2].text != description
    assert data_cells[3].text.lower() != status.lower()
    