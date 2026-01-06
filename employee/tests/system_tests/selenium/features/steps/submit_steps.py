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


@then('the new expense "{description}" should appear in "My Expenses" as "{expected_status}"')
def step_impl(context, description, expected_status):
    # 1. Switch back to the View Expenses section
    # Based on your HTML buttons, we may need to click 'View My Expenses'
    context.driver.find_element(By.ID, "show-expenses").click()

    # 2. Locate the row in the table containing our description
    # XPath searches the table rows for the specific text
    xpath = f"//div[@id='expenses-list']//tr[td[contains(text(), '{description}')]]"
    row = context.wait.until(EC.visibility_of_element_located((By.XPATH, xpath)))

    # 3. Verify the status in that row matches 'PENDING' (orange text)
    assert expected_status in row.text