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
    
    
    # explicit wait needed here to ensure cells are loaded
    context.wait.until(EC.visibility_of_all_elements_located(("tag name", "td")))
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
    
@when('I update the {field} to "{value}"')
def step_update_field(context, field, value):
    field_id_map = {
        "description": "edit-description",
        "amount": "edit-amount",
        "date": "edit-date"
    }
    field_id = field_id_map.get(field)
    assert field_id is not None, f"Unknown field name: {field}"
    
    if field == "description":
        description_input = context.wait.until(
            EC.visibility_of_element_located(("id", field_id))
        )
        description_input.clear()
        if value == "WHITESPACE":
            description_input.send_keys("   ")
        else:
            description_input.send_keys(value)
    
    elif field == "amount":
        amount_input = context.wait.until(
            EC.visibility_of_element_located(("id", field_id))
        )
        amount_input.clear()
        amount_input.send_keys(value)
    
    elif field == "date":
        browser = context.driver.capabilities['browserName'].lower()
        date_input = context.wait.until(
            EC.visibility_of_element_located(("id", field_id))
        )
        date_input.clear()
        
        if browser == "chrome":
            # Chrome expects localized format (MM/DD/YYYY)
            if "-" in value and len(value.split("-")) == 3:
                parts = value.split("-")
                mmddyyyy = f"{parts[1]}/{parts[2]}/{parts[0]}"
                date_input.send_keys(mmddyyyy)
            else:
                date_input.send_keys(value)
        
        elif browser == "firefox":
            # Firefox expects ISO format (YYYY-MM-DD)
            date_input.send_keys(value)
        
        else:
            context.driver.execute_script(
                    """
                arguments[0].value = arguments[1];
                arguments[0].dispatchEvent(new Event('input', { bubbles: true }));
                arguments[0].dispatchEvent(new Event('change', { bubbles: true }));
                """, 
                date_input, value
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

@then('I should see a {field} validation error message containing "{error_text}"')
def step_see_field_validation_error_message(context, field, error_text):
    field_id_map = {
        "description": "edit-description",
        "amount": "edit-amount",
        "date": "edit-date"
    }
    field_id = field_id_map.get(field)
    assert field_id is not None, f"Unknown field name: {field}"
    
    input_field = context.wait.until(
        EC.visibility_of_element_located(("id", field_id))
    )
    
    if field == "description" and error_text == "Description is required":
        # Special case: check for custom validation message in the UI
        try:
            message_element = context.driver.find_element("css selector", "div[id='edit-message'] p")
            assert error_text in message_element.text
        except:
            # Fallback to HTML5 validation message
            context.wait.until(lambda d: input_field.get_attribute("validationMessage") != "")
            assert error_text in input_field.get_attribute("validationMessage")
    else:
        # Standard HTML5 validation message check
        context.wait.until(lambda d: input_field.get_attribute("validationMessage") != "")
        validation_message = input_field.get_attribute("validationMessage")
        assert error_text in validation_message, f"Expected '{error_text}' to be in '{validation_message}'"