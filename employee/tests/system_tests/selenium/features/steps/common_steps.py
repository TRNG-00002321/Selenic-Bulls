from behave import given, then
from selenium.webdriver.support import expected_conditions as EC

@given("I am on the employee expenses page")
def step_on_employee_expenses_page(context):
    view_my_expenses_button = context.wait.until(
        EC.element_to_be_clickable(("id", "show-expenses"))
    )
    assert view_my_expenses_button.text == "View My Expenses"
    view_my_expenses_button.click()

    header_element = context.wait.until(
        EC.visibility_of_element_located(("css selector", "div[id='expenses-section'] h3"))
    )
    assert header_element.text == "My Expenses"

@given('I have a pending expense with description "{description}" and amount "{amount}"')
def step_have_pending_expense(context, description, amount):
    table_element = context.wait.until(
        EC.visibility_of_element_located(("css selector", "#expenses-list table"))
    )
    assert table_element is not None, "Expenses table not found"
    
    # Pending expense is the first row after the header
    row = context.wait.until(
        EC.visibility_of_element_located(("css selector", "tbody tr:nth-child(2)"))
    )
    
    data_cells = row.find_elements("tag name", "td")
    
    assert data_cells[0].text == "2025-12-29", "Date of the expense does not match"  # date of the expense
    assert data_cells[1].text == amount, "Amount of the expense does not match"  # amount of the expense
    assert data_cells[2].text == description, "Description of the expense does not match"
    assert data_cells[3].text.lower() == "pending", "Status of the expense is not pending"

@then('I should see the expense with description "{description}" and status "{status}" unchanged in the expenses list')
def step_see_expense_unchanged(context, description, status):
    # Pending expense is the first row after the header
    row = context.wait.until(
        EC.visibility_of_element_located(("css selector", "tbody tr:nth-child(2)"))
    )
    
    data_cells = row.find_elements("tag name", "td")
    
    assert data_cells[0].text == "2025-12-29"  # date of the expense
    assert data_cells[1].text == "$10.00"  # amount of the expense
    assert data_cells[2].text == description
    assert data_cells[3].text.lower() == status.lower()
    