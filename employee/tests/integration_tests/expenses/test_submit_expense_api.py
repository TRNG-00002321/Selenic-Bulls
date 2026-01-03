"""
Test suite for employee expense submission endpoint.
"""
import pytest
import requests
import allure
from typing import Dict, Any

# Base configuration
BASE_URL = "http://localhost:5000"
API_EXPENSES_URL = f"{BASE_URL}/api/expenses"
API_LOGIN_URL = f"{BASE_URL}/api/auth/login"
API_LOGOUT_URL = f"{BASE_URL}/api/auth/logout"

# Test credentials
EMPLOYEE_CREDENTIALS = {
    "username": "employee1",
    "password": "password123"
}

class TestSubmitExpense:
    """Test class for expense submission endpoint."""

    @pytest.fixture(scope="class")
    def auth_session(self) -> requests.Session:
        """Create an authenticated session for the employee."""
        session = requests.Session()

        # Login to get authentication cookie
        with allure.step("Authenticate user"):
            response = session.post(API_LOGIN_URL, json=EMPLOYEE_CREDENTIALS)
            allure.attach(f"Login request: {EMPLOYEE_CREDENTIALS}", name="Login Request")
            allure.attach(f"Login response status: {response.status_code}", name="Login Response")
            allure.attach(str(response.json()), name="Login Response Body")
            assert response.status_code == 200, f"Login failed: {response.json()}"

        yield session

        # Cleanup: Logout after all tests
        with allure.step("Logout user"):
            try:
                logout_response = session.post(API_LOGOUT_URL)
                allure.attach(f"Logout response status: {logout_response.status_code}", name="Logout Response")
            except Exception as e:
                allure.attach(f"Logout failed: {e}", name="Logout Error")
                print(f"Logout failed: {e}")

    @pytest.fixture
    def unauth_session(self) -> requests.Session:
        """Create an unauthenticated session."""
        return requests.Session()

    @allure.id("R-11_001")
    @allure.title("Submit expense with all required fields - Happy Path")
    @allure.description("Test successful expense submission with all fields filled")
    def test_submit_expense_happy(self, auth_session: requests.Session):
        """Happy Path: Successfully submit expense with all fields."""
        # Arrange
        expense_data = {
            "amount": 150.75,
            "description": "Client eating dog meeting",
            "date": "2025-12-30"
        }

        allure.attach(str(expense_data), name="Request Payload", attachment_type=allure.attachment_type.JSON)
        # Act
        with allure.step("Send POST request to expense endpoint"):
            response = auth_session.post(API_EXPENSES_URL, json=expense_data)
            allure.attach(f"Response status: {response.status_code}", name="Response Status")
            allure.attach(str(response.json()), name="Response Body", attachment_type=allure.attachment_type.JSON)

        # Assert
        with allure.step("Verify response"):
            assert response.status_code == 201
            data = response.json()
            assert data["message"] == "Expense submitted successfully"
            assert "expense" in data
            assert data["expense"]["id"] is not None
            assert data["expense"]["amount"] == 150.75
            assert data["expense"]["description"] == "Client eating dog meeting"
            assert data["expense"]["date"] == "2025-12-30"
            assert data["expense"]["status"] == "pending"

    @allure.id("R-11_002")
    @allure.title("Submit expense without optional date field")
    @allure.description("Test expense submission without date (should use current date)")
    def test_submit_expense_without_date(self, auth_session: requests.Session):
        """Happy Path: Submit expense without optional date field."""
        # Arrange
        expense_data = { "amount": 45.00, "description": "Office supplies" }
        allure.attach(str(expense_data), name="Request Payload", attachment_type=allure.attachment_type.JSON)

        # Act
        with allure.step("Send POST request without date"):
            response = auth_session.post(API_EXPENSES_URL, json=expense_data)
            allure.attach(f"Response status: {response.status_code}", name="Response Status")

        # Assert
        with allure.step("Verify successful submission"):
            assert response.status_code == 201
            data = response.json()
            assert data["message"] == "Expense submitted successfully"
            assert data["expense"]["amount"] == 45.00
            assert data["expense"]["description"] == "Office supplies"
            assert data["expense"]["date"] is not None  # Should have current date
            assert data["expense"]["status"] == "pending"
            allure.attach(str(data), name="Response Body", attachment_type=allure.attachment_type.JSON)


    @allure.id("R-11_004")
    @allure.title("Submit expense with missing amount")
    @allure.description("Test expense submission without required amount field")
    def test_submit_expense_missing_amount(self, auth_session: requests.Session):
        """Sad Path: Submit expense with missing amount."""
        # Arrange
        expense_data = {
            "description": "Missing amount expense"
        }

        allure.attach(str(expense_data), name="Request Payload", attachment_type=allure.attachment_type.JSON)

        # Act
        with allure.step("Send request with missing amount"):
            response = auth_session.post(API_EXPENSES_URL, json=expense_data)
            allure.attach(f"Response status: {response.status_code}", name="Response Status")

        # Assert
        with allure.step("Verify validation error"):
            assert response.status_code == 400
            data = response.json()
            assert "error" in data
            assert "Amount and description are required" in data["error"]
            allure.attach(str(data), name="Error Response", attachment_type=allure.attachment_type.JSON)

    @allure.id("R-11_005")
    @allure.title("Submit expense with missing description")
    @allure.description("Test expense submission without required description field")
    def test_submit_expense_missing_description(self, auth_session: requests.Session):
        """Sad Path: Submit expense with missing description."""

        # Arrange
        expense_data = {
            "amount": 75.50
        }

        allure.attach(str(expense_data), name="Request Payload", attachment_type=allure.attachment_type.JSON)

        # Act
        with allure.step("Send request with missing description"):
            response = auth_session.post(API_EXPENSES_URL, json=expense_data)
            allure.attach(f"Response status: {response.status_code}", name="Response Status")

        # Assert
        with allure.step("Verify validation error"):
            assert response.status_code == 400
            data = response.json()
            assert "error" in data
            assert "Amount and description are required" in data["error"]
            allure.attach(str(data), name="Error Response", attachment_type=allure.attachment_type.JSON)


    @allure.id("R-11_007")
    @allure.title("Submit expense with empty JSON body")
    @allure.description("Test expense submission with empty request body")
    def test_submit_expense_empty_body(self, auth_session: requests.Session):
        """Sad Path: Submit expense with empty JSON body."""
        # Act
        with allure.step("Send request with empty body"):
            response = auth_session.post(
                API_EXPENSES_URL,
                data="",
                headers={"Content-Type": "application/json"}
            )
            allure.attach(f"Response status: {response.status_code}", name="Response Status")

        # Assert
        with allure.step("Verify validation error"):
            assert response.status_code == 500
            data = response.json()
            assert data["error"] == "Failed to submit expense"
            allure.attach(str(data), name="Error Response", attachment_type=allure.attachment_type.JSON)

    @allure.id("R-11_008")
    @allure.title("Submit expense with zero amount")
    @allure.description("Test expense submission with amount = 0")
    def test_submit_expense_zero_amount(self, auth_session: requests.Session):
        """Edge Case: Submit expense with 0 amount."""
        # Arrange
        expense_data = {
            "amount": 0,
            "description": "Zero amount expense"
        }

        allure.attach(str(expense_data), name="Request Payload", attachment_type=allure.attachment_type.JSON)

        # Act
        with allure.step("Send request with zero amount"):
            response = auth_session.post(API_EXPENSES_URL, json=expense_data)
            allure.attach(f"Response status: {response.status_code}", name="Response Status")

        # Assert
        with allure.step("Verify response for zero amount"):
            # Accept either success or validation error depending on business rules
            assert response.status_code in [201, 400]
            allure.attach(str(response.json()), name="Response Body", attachment_type=allure.attachment_type.JSON)

    @allure.id("R-11_009")
    @allure.title("Submit expense with negative amount")
    @allure.description("Test expense submission with negative amount value")
    def test_submit_expense_negative_amount(self, auth_session: requests.Session):
        """Edge Case: Submit expense with negative amount."""
        # Arrange
        expense_data = {
            "amount": -50.00,
            "description": "Negative amount expense"
        }

        allure.attach(str(expense_data), name="Request Payload", attachment_type=allure.attachment_type.JSON)

        # Act
        with allure.step("Send request with negative amount"):
            response = auth_session.post(API_EXPENSES_URL, json=expense_data)
            allure.attach(f"Response status: {response.status_code}", name="Response Status")

        # Assert
        with allure.step("Verify response for negative amount"):
            # Accept either success or validation error depending on business rules
            assert response.status_code ==  400
            allure.attach(str(response.json()), name="Response Body", attachment_type=allure.attachment_type.JSON)

    @allure.id("R-11_010")
    @allure.title("Submit expense with very long description")
    @allure.description("Test expense submission with 1000 character description: BUG")
    def test_submit_expense_very_long_description(self, auth_session: requests.Session):
        """Edge Case: Submit expense with very long description."""
        # Arrange
        expense_data = {
            "amount": 100.00,
            "description": "A" * 1000  # 1000 character description
        }

        # Attach only first 100 chars to avoid huge attachments
        allure.attach(str({**expense_data, "description": expense_data["description"][:100] + "..."}),
                      name="Request Payload (truncated)", attachment_type=allure.attachment_type.JSON)

        # Act
        with allure.step("Send request with long description"):
            response = auth_session.post(API_EXPENSES_URL, json=expense_data)
            allure.attach(f"Response status: {response.status_code}", name="Response Status")

        # Assert
        with allure.step("Verify response for long description"):
            assert response.status_code == 400
            allure.attach(str(response.json()), name="Response Body", attachment_type=allure.attachment_type.JSON)

    @allure.id("R-11_011")
    @allure.title("Submit expense with future date")
    @allure.description("Test that the system rejects expense dates in the future (e.g., 2100-01-01) BUG")
    def test_submit_expense_future_date(self, auth_session: requests.Session):
        """Edge Case: Submit expense with a future date."""
        # Arrange
        expense_data = {
            "amount": 200.00,
            "description": "Future date expense",
            "date": "2100-01-01"
        }

        allure.attach(str(expense_data), name="Request Payload", attachment_type=allure.attachment_type.JSON)

        # Act
        with allure.step("Send request with future date"):
            response = auth_session.post(API_EXPENSES_URL, json=expense_data)
            allure.attach(f"Response status: {response.status_code}", name="Response Status")

        # Assert
        with allure.step("Verify request is rejected with 400 Bad Request"):
            assert response.status_code == 400

            data = response.json()
            allure.attach(str(data), name="Response Body", attachment_type=allure.attachment_type.JSON)

            # Ensure the error message is helpful
            assert "date" in str(data).lower()
            assert "future" in str(data).lower()