# Hooks (setup/teardown) for the Selenium tests
import sqlite3
import os
import pytest
from selenium import webdriver
from selenium.webdriver.chrome.service import Service as ChromeService
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.firefox.options import Options as FirefoxOptions
from selenium.webdriver.edge.options import Options as EdgeOptions
from selenium.webdriver.firefox.service import Service as FirefoxService
from selenium.webdriver.edge.service import Service as EdgeService
from selenium.webdriver.support.ui import WebDriverWait
from webdriver_manager.chrome import ChromeDriverManager
from webdriver_manager.firefox import GeckoDriverManager
from webdriver_manager.microsoft import EdgeChromiumDriverManager
from selenium.webdriver.support import expected_conditions as EC
from repository import DatabaseConnection


def before_feature(context, feature):
    browser = context.config.userdata.get("browser", "chrome").lower()

    if browser == "chrome":
        context.driver = webdriver.Chrome(options=get_chrome_options())
    
    elif browser == "firefox":
        firefox_options = FirefoxOptions()
        firefox_options.add_argument("--headless")
        context.driver = webdriver.Firefox(options=firefox_options)
    
    elif browser == "edge":
        edge_options = EdgeOptions()
        edge_options.add_argument("--headless")
        edge_options.add_argument("--no-sandbox")
        context.driver = webdriver.Edge(options=edge_options)
    
    else:
        raise ValueError(f"Unsupported browser: {browser}")

    context.base_url = "http://localhost:5000"
    context.driver.maximize_window()
    context.driver.implicitly_wait(10)  # seconds
    
    # Initialize wait
    context.wait = WebDriverWait(context.driver, 10)
    
    # Check if the application is running
    context.driver.get(context.base_url + "/health")
    assert "healthy" in context.driver.page_source
    
    #Login before each feature w/ context wait
    context.driver.get(context.base_url)
    username_input_element = context.wait.until(
        EC.visibility_of_element_located(("id", "username"))
    )
    password_input_element = context.wait.until(
        EC.visibility_of_element_located(("id", "password"))
    )
    login_button = context.wait.until(
        EC.element_to_be_clickable(("css selector", "button[type='submit']"))
    )
    
    username_input_element.send_keys("employee1")
    password_input_element.send_keys("password123")
    login_button.click()
    
    # Verify login success by checking for a username display element
    username_display_element = context.wait.until(
        EC.visibility_of_element_located(("id", "username-display"))
    )
    assert "employee1" in username_display_element.text

    
def after_feature(context, feature):
    context.driver.quit()

# restore database (submit an expense with the same data as the one deleted in tests)
# used for particular tests with @restore_db, @cancel tags
def after_scenario(context, scenario):
    if "cancel" in scenario.effective_tags:
        context.wait.until(EC.visibility_of_element_located(("id", "cancel-edit"))).click()
        #context.driver.find_element("id", "cancel-edit").click()
        return
            
    if "restore_db" in scenario.effective_tags:
        db_connection = DatabaseConnection()
        
        # Reset the database to ensure consistency
        with db_connection.get_connection() as conn:
            # First delete all approvals
            conn.execute("DELETE FROM approvals")
            # Then delete all expenses
            conn.execute("DELETE FROM expenses")
            conn.commit()
            
            # Insert test expenses
            conn.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (?, ?, ?, ?, ?)",
                         (1, 1, 10.0, "Pizza", "2025-12-29"))
            conn.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (?, ?, ?, ?, ?)",
                         (2, 1, 25.01, "Notebook", "2025-12-22"))
            conn.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (?, ?, ?, ?, ?)",
                         (3, 1, 500.05, "Hotel", "2025-12-25"))
            conn.commit()
            
            # Insert test approvals
            conn.execute("INSERT INTO approvals (expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?)",
                         (1, "pending", None, None, None))
            conn.execute("INSERT INTO approvals (expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?)",
                         (2, "approved", "2", "Good choice for note taking.", "2025-12-29 14:12:35"))
            conn.execute("INSERT INTO approvals (expense_id, status, reviewer, comment, review_date) VALUES (?, ?, ?, ?, ?)",
                         (3, "denied", "2", "Expense not covered for holidays.", "2025-12-29 14:11:47"))
            conn.commit()

def get_chrome_options():
    """Provides a headless Chrome WebDriver instance"""
    options = ChromeOptions()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--guest")
    return options   
