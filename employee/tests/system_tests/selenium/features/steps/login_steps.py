from behave import given, when, then, step
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


# =====================================================
# GIVEN
# =====================================================

@given("the login service is available")
def step_login_service_available(context):
    """
    Navigate to login page and verify it loads.
    """
    context.driver.get(f"{context.base_url}/login")

    WebDriverWait(context.driver, 10).until(
        EC.presence_of_element_located((By.ID, "username"))
    )

    assert "/login" in context.driver.current_url.lower()
    assert "Employee Login" in context.driver.title


# =====================================================
# WHEN
# =====================================================

@when('the user logs in with username "{username}" and password "{password}"')
def step_user_logs_in(context, username, password):
    """
    Fill in login form and submit.
    """
    wait = WebDriverWait(context.driver, 10)

    username_field = wait.until(
        EC.presence_of_element_located((By.ID, "username"))
    )
    password_field = wait.until(
        EC.presence_of_element_located((By.ID, "password"))
    )
    login_button = wait.until(
        EC.element_to_be_clickable((By.CSS_SELECTOR, "button[type='submit']"))
    )

    username_field.clear()
    username_field.send_keys(username)

    password_field.clear()
    password_field.send_keys(password)

    login_button.click()

@when("the user submits the login form with empty username and empty password")
def step_submit_empty_username_and_password(context):
    wait = WebDriverWait(context.driver, 10)

    username_field = wait.until(
        EC.presence_of_element_located((By.ID, "username"))
    )
    password_field = wait.until(
        EC.presence_of_element_located((By.ID, "password"))
    )
    login_button = wait.until(
        EC.element_to_be_clickable((By.CSS_SELECTOR, "button[type='submit']"))
    )

    username_field.clear()
    password_field.clear()

    login_button.click()

@when('the user submits the login form with username "employee1" and empty password')
def step_submit_empty_password(context):
    wait = WebDriverWait(context.driver, 10)

    username_field = wait.until(
        EC.presence_of_element_located((By.ID, "username"))
    )
    password_field = wait.until(
        EC.presence_of_element_located((By.ID, "password"))
    )
    login_button = wait.until(
        EC.element_to_be_clickable((By.CSS_SELECTOR, "button[type='submit']"))
    )

    username_field.clear()
    username_field.send_keys("employee1")

    password_field.clear()

    login_button.click()


# =====================================================
# THEN — SUCCESS
# =====================================================

@then("the login should be successful")
def step_login_success(context):
    WebDriverWait(context.driver, 10).until(
        EC.url_contains("/app")
    )
    assert "/app" in context.driver.current_url


@step("the user should be authenticated")
def step_user_authenticated(context):
    logout_button = WebDriverWait(context.driver, 5).until(
        EC.visibility_of_element_located((By.ID, "logout-btn"))
    )
    header = WebDriverWait(context.driver, 10).until(
        EC.visibility_of_element_located((By.TAG_NAME, "h1"))
    )

    assert header.text.strip() == "Employee Expense Dashboard"
    assert logout_button.is_displayed()


# =====================================================
# THEN — FAILURE
# =====================================================

@then("the login should fail")
def step_login_failure(context):
    """
    Verify login failure by staying on login page.
    """
    WebDriverWait(context.driver, 5).until(
        EC.url_contains("/login")
    )
    header = WebDriverWait(context.driver, 10).until(
        EC.visibility_of_element_located((By.TAG_NAME, "h2"))
    )

    assert header.text.strip() == "Employee Login"
    assert "/login" in context.driver.current_url


@step("an error message should be returned")
def step_error_message_displayed(context):
    """
    Verify error message is displayed.
    """
    wait = WebDriverWait(context.driver, 5)

    error_container = wait.until(
        EC.visibility_of_element_located((By.ID, "login-message"))
    )

    assert error_container.text.strip() == "Invalid credentials"

@then("a username required validation message is shown")
def step_username_required_validation(context):
    username = WebDriverWait(context.driver, 10).until(
        EC.presence_of_element_located((By.ID, "username"))
    )
    WebDriverWait(context.driver, 3).until(
        lambda d: username.get_attribute("validationMessage") != ""
    )

    assert username.get_attribute("validationMessage").__contains__("fill out this field")

@step("the user remains on the login page")
def step_user_remains_on_login_page(context):
    WebDriverWait(context.driver, 5).until(
        EC.url_contains("/login")
    )

    header = WebDriverWait(context.driver, 10).until(
        EC.visibility_of_element_located((By.TAG_NAME, "h2"))
    )

    assert header.text.strip() == "Employee Login"
    assert "/login" in context.driver.current_url


@then("a password required validation message is shown")
def step_password_required_validation(context):
    password = WebDriverWait(context.driver, 10).until(
        EC.presence_of_element_located((By.ID, "password"))
    )
    WebDriverWait(context.driver, 3).until(
        lambda d: password.get_attribute("validationMessage") != ""
    )

    assert password.get_attribute("validationMessage").__contains__("fill out this field")

