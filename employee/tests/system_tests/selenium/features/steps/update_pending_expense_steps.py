from behave import when, then
from selenium.webdriver.support import expected_conditions as EC

@when('I click the edit button for the expense with description "{description}" and status "{status}"')
def step_click_edit_button(context, description, status):
    # Locate the expenses table
    table_element = context.wait.until(
        EC.visibility_of_element_located(("css selector", "#expenses-list table"))
    )
    assert table_element is not None, "Expenses table not found"
    
    # Pending expense is the first row after the header
    row = context.wait.until(
        EC.visibility_of_element_located(("css selector", "tbody tr:nth-child(2)"))
    )
    
    data_cells = row.find_elements("tag name", "td")
    
    assert data_cells[2].text == description, "Description of the expense does not match"
    assert data_cells[3].text.lower() == status.lower(), "Status of the expense does not match"
    
    edit_button = context.wait.until(
        EC.element_to_be_clickable(("xpath", "//button[normalize-space()='Edit']"))
    )
    edit_button.click()

@when('I update the description to "{description}", the amount to "{amount}", and the date to "{date}"')
def step_update_expense_details(context, description, amount, date):
    browser = context.driver.capabilities['browserName'].lower()
    
    description_input = context.wait.until(
        EC.visibility_of_element_located(("id", "edit-description"))
    )
    amount_input = context.wait.until(
        EC.visibility_of_element_located(("id", "edit-amount"))
    )
    date_input = context.wait.until(
        EC.visibility_of_element_located(("id", "edit-date"))
    )
    
    description_input.clear()
    amount_input.clear()
    date_input.clear()
    amount_input.send_keys(amount)
    description_input.send_keys(description)
    
    if browser == "chrome":
        # Chrome expects localized format (MM/DD/YYYY)
        parts = date.split("-")
        mmddyyyy = f"{parts[1]}/{parts[2]}/{parts[0]}"
        date_input.send_keys(mmddyyyy)
    
    elif browser == "firefox":
        # Firefox expects ISO format (YYYY-MM-DD)
        date_input.send_keys(date)
    
    else:
        context.driver.execute_script(
                """
            arguments[0].value = arguments[1];
            arguments[0].dispatchEvent(new Event('input', { bubbles: true }));
            arguments[0].dispatchEvent(new Event('change', { bubbles: true }));
            """, 
            date_input, date
        )

@when("I click the update expense button")
def step_click_update_expense_button(context):
    context.driver.find_element("css selector", "form[id='edit-expense-form'] button[type='submit']").click()

@when("I click the cancel button")
def step_click_cancel_button(context):
    context.driver.find_element("id", "cancel-edit").click()
    
@when('I update the amount to "{amount}"')
def step_update_amount(context, amount):
    amount_input = context.wait.until(
        EC.visibility_of_element_located(("id", "edit-amount"))
    )
    amount_input.clear()
    amount_input.send_keys(amount)

@when('I update the description to "{description}"')
def step_update_description(context, description):
    description_input = context.wait.until(
        EC.visibility_of_element_located(("id", "edit-description"))
    )
    description_input.clear()
    description_input.send_keys(description)

@when('I update the date to "{date}"')
def step_update_date(context, date):
    browser = context.driver.capabilities['browserName'].lower()
    
    date_input = context.wait.until(
        EC.visibility_of_element_located(("id", "edit-date"))
    )
    date_input.clear()
    
    if browser == "chrome":
        # Chrome expects localized format (MM/DD/YYYY)
        parts = date.split("-")
        mmddyyyy = f"{parts[1]}/{parts[2]}/{parts[0]}"
        date_input.send_keys(mmddyyyy)
    
    elif browser == "firefox":
        # Firefox expects ISO format (YYYY-MM-DD)
        date_input.send_keys(date)
    
    else:
        context.driver.execute_script(
                """
            arguments[0].value = arguments[1];
            arguments[0].dispatchEvent(new Event('input', { bubbles: true }));
            arguments[0].dispatchEvent(new Event('change', { bubbles: true }));
            """, 
            date_input, date
        )

@when('I clear the "{field_name}" field')
def step_clear_field(context, field_name):
    field_id_map = {
        "description": "edit-description",
        "amount": "edit-amount",
        "date": "edit-date"
    }
    field_id = field_id_map.get(field_name)
    assert field_id is not None, f"Unknown field name: {field_name}"
    
    input_field = context.wait.until(
        EC.visibility_of_element_located(("id", field_id))
    )
    input_field.clear()

@then('I should see a message "{message}"')
def step_see_a_message(context, message):
    message_element = context.driver.find_element("css selector", "div[id='edit-message'] p")
    assert message_element.text == message


@then('I should see an edit expense header titled "{header_title}"')
def step_see_edit_expense_header(context, header_title):
    header_element = context.wait.until(
        EC.visibility_of_element_located(("css selector", "div[id='edit-expense-section'] h3"))
    )
    assert header_element.text == header_title

@then('I should see a my expenses header titled "{header_title}"')
def step_see_my_expenses_header(context, header_title):
    header_element = context.wait.until(
        EC.visibility_of_element_located(("css selector", "div[id='expenses-section'] h3"))
    )
    assert header_element.text == header_title

@then('I should see a date validation error message containing "{error_message}"')
def step_see_date_validation_error_message(context, error_message):
    date_input = context.wait.until(
        EC.visibility_of_element_located(("id", "edit-date"))
    )
    context.wait.until(lambda d: date_input.get_attribute("validationMessage") != "")
    assert error_message in date_input.get_attribute("validationMessage")

@then('I should see an amount validation error message containing "{error_message}"')
def step_see_amount_validation_error_message(context, error_message):
    amount_input = context.wait.until(
        EC.visibility_of_element_located(("id", "edit-amount"))
    )
    context.wait.until(lambda d: amount_input.get_attribute("validationMessage") != "")
    assert error_message in amount_input.get_attribute("validationMessage")

@then('I should see a description validation error message "{error_message}"')
def step_see_description_validation_error_message(context, error_message):
    description_input = context.wait.until(
        EC.visibility_of_element_located(("id", "edit-description"))
    )
    context.wait.until(lambda d: description_input.get_attribute("validationMessage") != "")
    assert description_input.get_attribute("validationMessage") == error_message