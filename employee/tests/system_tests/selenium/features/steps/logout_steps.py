import behave.runner
from behave import *
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

use_step_matcher("re")


@given("the user is logged in to the system")
def step_impl(context: behave.runner.Context):
    context.driver.get(f"{context.base_url}/login")
    
    wait = WebDriverWait(context.driver, 10)
    username_field = wait.until(EC.presence_of_element_located((By.ID, "username")))
    password_field = wait.until(EC.presence_of_element_located((By.ID, "password")))
    login_button = wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, "button[type='submit']")))
    
    username_field.clear()
    username_field.send_keys("employee1")
    password_field.clear()
    password_field.send_keys("password123")
    login_button.click()
    
    # Wait for successful login
    wait.until(EC.url_contains("/app"))


@when("the user clicks the logout button")
def step_impl(context: behave.runner.Context):
    logout_button = WebDriverWait(context.driver, 10).until(
        EC.element_to_be_clickable((By.ID, "logout-btn"))
    )
    logout_button.click()


@then("the logout should be successful")
def step_impl(context: behave.runner.Context):
    WebDriverWait(context.driver, 10).until(EC.url_contains("/login"))


@step("the user should be redirected to the login page")
def step_impl(context: behave.runner.Context):
    assert "/login" in context.driver.current_url
    header = WebDriverWait(context.driver, 5).until(
        EC.visibility_of_element_located((By.TAG_NAME, "h2"))
    )
    assert header.text.strip() == "Employee Login"


@step("the user should no longer be authenticated")
def step_impl(context: behave.runner.Context):
    context.driver.get(f"{context.base_url}/app")
    WebDriverWait(context.driver, 10).until(EC.url_contains("/login"))
    assert "/login" in context.driver.current_url


@step("the user tries to access a protected page")
def step_impl(context: behave.runner.Context):
    context.driver.get(f"{context.base_url}/app")
    context.driver.get(f"{context.base_url}/login")

