from behave import *
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC


@given('the employee clicks the "Submit New Expense" button')
def step_impl(context):
    # From your Dashboard HTML: <button id="show-submit">Submit New Expense</button>
    # We click this to reveal the form
    submit_nav_btn = context.wait.until(
        EC.element_to_be_clickable((By.ID, "show-submit"))
    )
    submit_nav_btn.click()

    # Ensure the section is visible (display: block) before the 'When' steps start
    context.wait.until(
        EC.visibility_of_element_located((By.ID, "submit-expense-section"))
    )

@when('the employee enters an amount of "{amount}"')
def step_impl(context, amount):
    # From your HTML: <input type="number" id="amount">
    amount_field = context.driver.find_element(By.ID, "amount")
    amount_field.clear()
    amount_field.send_keys(amount)


@when('the employee enters "{description}" as the description')
def step_impl(context, description):
    # From your HTML: <input type="text" id="description">
    desc_field = context.driver.find_element(By.ID, "description")
    desc_field.send_keys(description)

@when('the employee enters a date of "{date}"')
def step_impl(context, date):
    # From your HTML: <input type="date" id="date">
    date_field = context.driver.find_element(By.ID, "date")
    date_field.send_keys(date)


@when('the employee clicks the "Submit Expense" button')
def step_impl(context):
    # From your HTML: <button type="submit">Submit Expense</button> inside #expense-form
    context.driver.find_element(By.CSS_SELECTOR, "#expense-form button[type='submit']").click()


@then('a success message "{expected_msg}" should be displayed')
def step_impl(context, expected_msg):
    # From your HTML: <div id="submit-message"></div>
    msg_element = context.wait.until(
        EC.visibility_of_element_located((By.ID, "submit-message"))
    )
    assert expected_msg in msg_element.text


@when('the employee enters invalid {field} value "{value}"')
def step_enter_invalid_field_value(context, field, value):
    # Fill out fields in order: amount, description, date
    # Fill preceding fields with valid values before testing target field
    
    if field == "amount":
        amount_field = context.driver.find_element(By.ID, "amount")
        amount_field.clear()
        amount_field.send_keys(value)
    
    elif field == "description":
        # Fill amount with valid value first
        amount_field = context.driver.find_element(By.ID, "amount")
        amount_field.clear()
        amount_field.send_keys("50.00")
        
        # Then fill description with invalid value
        desc_field = context.driver.find_element(By.ID, "description")
        desc_field.clear()
        if value == "WHITESPACE":
            desc_field.send_keys("   ")
        else:
            desc_field.send_keys(value)
    
    elif field == "date":
        # Fill amount with valid value first
        amount_field = context.driver.find_element(By.ID, "amount")
        amount_field.clear()
        amount_field.send_keys("50.00")
        
        # Fill description with valid value
        desc_field = context.driver.find_element(By.ID, "description")
        desc_field.clear()
        desc_field.send_keys("Valid Description")
        
        # Then fill date with invalid value
        date_field = context.driver.find_element(By.ID, "date")
        date_field.clear()
        date_field.send_keys(value)

@when('the employee leaves the "{field}" field empty')
def step_leave_field_empty(context, field):
    field_id_map = {
        "amount": "amount",
        "description": "description", 
        "date": "date"
    }
    field_id = field_id_map.get(field)
    assert field_id is not None, f"Unknown field name: {field}"
    
    # Fill out fields in order: amount, description, date
    # Fill preceding fields with valid values before leaving target field empty
    
    if field == "amount":
        # Just clear the amount field
        input_field = context.driver.find_element(By.ID, field_id)
        input_field.clear()
    
    elif field == "description":
        # Fill amount with valid value first
        amount_field = context.driver.find_element(By.ID, "amount")
        amount_field.clear()
        amount_field.send_keys("50.00")
        
        # Leave description empty
        input_field = context.driver.find_element(By.ID, field_id)
        input_field.clear()
    
    elif field == "date":
        # Fill amount with valid value first
        amount_field = context.driver.find_element(By.ID, "amount")
        amount_field.clear()
        amount_field.send_keys("50.00")
        
        # Fill description with valid value
        desc_field = context.driver.find_element(By.ID, "description")
        desc_field.clear()
        desc_field.send_keys("Valid Description")
        
        # Leave date empty
        input_field = context.driver.find_element(By.ID, field_id)
        input_field.clear()

@then('a {field} validation error message containing "{error_text}" should be displayed for submit form')
def step_see_submit_validation_error(context, field, error_text):
    field_id_map = {
        "amount": "amount",
        "description": "description",
        "date": "date"
    }
    field_id = field_id_map.get(field)
    assert field_id is not None, f"Unknown field name: {field}"
    
    input_field = context.wait.until(
        EC.visibility_of_element_located((By.ID, field_id))
    )
    
    if field == "description" and error_text == "Description is required":
        # Special case: check for custom validation message in the UI
        try:
            message_element = context.driver.find_element(By.ID, "submit-message")
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