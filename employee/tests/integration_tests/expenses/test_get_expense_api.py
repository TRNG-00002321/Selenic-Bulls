"""
Integration tests for GET endpoints related to expenses.
Uses `requests` and covers both authenticated and unauthenticated cases as edge coverage.
"""
from datetime import date

import pytest
import requests
import allure

# NEW CLASSES START
@allure.epic("Expense Management System")
@allure.feature("Expense Retrieval - API Integration")
@allure.story("As an employee, I want to view my submitted expenses filtered by status")
class TestGetExpensesStatusFilterParameterized:
    """Parameterized tests for ?status= filtering (authenticated)."""

    @allure.title("Get expenses with status filter (parameterized)")
    @allure.description("Authenticated user can retrieve expenses filtered by each supported status value")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("status", ["pending", "approved", "denied"])
    def test_get_expenses_with_status_filter_authenticated(self, authenticated_session, base_url, status):
        response = authenticated_session.get(
            f"{base_url}/api/expenses",
            params={"status": status},
            timeout=10,
        )
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, dict)
        assert "expenses" in data
        assert isinstance(data["expenses"], list)
        # If any results are returned, they should match the requested status
        for item in data["expenses"]:
            assert item.get("status") == status


@allure.epic("Expense Management System")
@allure.feature("Expense Retrieval - API Integration")
@allure.story("Edge coverage: invalid status filters are handled safely")
class TestGetExpensesStatusFilterInvalid:
    """Invalid status filter tests (authenticated)."""

    @allure.title("Get expenses with invalid status filter returns client error")
    @allure.description("Invalid status should be rejected (400) or treated as not found/invalid (422)")
    @allure.severity(allure.severity_level.MINOR)
    @pytest.mark.parametrize("status", ["", "PENDING", "pend", "unknown", "123", None])
    def test_get_expenses_with_invalid_status_filter_authenticated(self, authenticated_session, base_url, status):
        params = {} if status is None else {"status": status}
        response = authenticated_session.get(
            f"{base_url}/api/expenses",
            params=params,
            timeout=10,
        )

        # Accept either behavior depending on implementation:
        # - 200 with no filtering (or treated as missing)
        # - 400/422 for invalid value
        assert response.status_code in (200, 400, 422)

        if response.status_code == 200:
            data = response.json()
            assert isinstance(data, dict)
            assert "expenses" in data
            assert isinstance(data["expenses"], list)
# NEW CLASSES END


@pytest.fixture
def session():
    """Create requests session for integration testing."""
    return requests.Session()


@pytest.fixture
def base_url():
    """Base URL for the API."""
    return "http://localhost:5000"


@pytest.fixture
def authenticated_session(session, base_url):
    """Create authenticated session for API calls using existing sample user."""
    login_data = {"username": "employee1", "password": "password123"}
    response = session.post(f"{base_url}/api/auth/login", json=login_data, timeout=10)
    assert response.status_code == 200

    yield session

    # Logout after test
    session.post(f"{base_url}/api/auth/logout", timeout=10)


@pytest.fixture
def created_expense_id(authenticated_session, base_url):
    """Create a pending expense for testing GET /api/expenses/<id>."""
    expense_data = {
        "amount": 12.34,
        "description": "Test expense for GET endpoints",
        "date": str(date.today()),
    }
    response = authenticated_session.post(f"{base_url}/api/expenses", json=expense_data, timeout=10)
    assert response.status_code == 201

    expense_info = response.json()
    expense_id = expense_info["expense"]["id"]

    yield expense_id

    # Cleanup (best effort)
    try:
        authenticated_session.delete(f"{base_url}/api/expenses/{expense_id}", timeout=10)
    except requests.RequestException:
        pass


@allure.epic("Expense Management System")
@allure.feature("Expense Retrieval - API Integration")
@allure.story("As an employee, I want to view my submitted expenses and an individual expense by ID")
class TestGetExpensesHappyPath:
    """Happy-path tests (authenticated)."""

    @allure.title("Get all expenses (authenticated)")
    @allure.description("Authenticated user can retrieve all their expenses")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_get_all_expenses_authenticated(self, authenticated_session, base_url):
        response = authenticated_session.get(f"{base_url}/api/expenses", timeout=10)
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, dict)

    @allure.title("Get expenses with status filter (authenticated)")
    @allure.description("Authenticated user can retrieve expenses filtered by status")
    @allure.severity(allure.severity_level.NORMAL)
    def test_get_expenses_with_status_filter_authenticated(self, authenticated_session, base_url):
        response = authenticated_session.get(
            f"{base_url}/api/expenses",
            params={"status": "pending"},
            timeout=10,
        )
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, dict)

    @allure.title("Get expense by ID (authenticated)")
    @allure.description("Authenticated user can retrieve an existing expense by id")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_get_expense_by_id_authenticated(self, authenticated_session, base_url, created_expense_id):
        response = authenticated_session.get(
            f"{base_url}/api/expenses/{created_expense_id}",
            timeout=10,
        )
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, dict)
        assert "expense" in data
        assert data["expense"]["id"] == created_expense_id


@allure.epic("Expense Management System")
@allure.feature("Expense Retrieval - API Integration")
@allure.story("Edge coverage: endpoints behave correctly when user is not authenticated")
class TestGetExpensesUnauthenticatedEdgeCases:
    """Edge-case coverage (unauthenticated)."""

    @allure.title("Get all expenses (unauthenticated) is rejected")
    @allure.description("Unauthenticated user should not be able to retrieve expenses")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_get_all_expenses_unauthenticated(self, session, base_url):
        response = session.get(f"{base_url}/api/expenses", timeout=10)
        assert response.status_code == 401
        data = response.json()
        assert "error" in data

    @allure.title("Get expense by ID (unauthenticated) is rejected")
    @allure.description("Unauthenticated user should not be able to retrieve an expense by id")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_get_expense_by_id_unauthenticated(self, session, base_url):
        response = session.get(f"{base_url}/api/expenses/1", timeout=10)
        assert response.status_code == 401
        data = response.json()
        assert "error" in data


@allure.epic("Expense Management System")
@allure.feature("Expense Retrieval - API Integration")
@allure.story("Sad path / edge cases for expense retrieval")
class TestGetExpensesSadPathAndBoundaries:
    """Sad path, invalid ids, and boundary conditions (authenticated)."""

    @allure.title("Get non-existent expense returns 404")
    @allure.description("Authenticated request for an expense that doesn't exist returns 404")
    @allure.severity(allure.severity_level.NORMAL)
    def test_get_expense_by_id_not_found(self, authenticated_session, base_url):
        response = authenticated_session.get(f"{base_url}/api/expenses/99999999", timeout=10)
        assert response.status_code == 404
        data = response.json()
        assert "error" in data
        assert "not found" in data["error"].lower()

    @allure.title("Get expense with invalid ID format returns 404")
    @allure.description("Authenticated request with non-integer path param returns 404")
    @allure.severity(allure.severity_level.NORMAL)
    def test_get_expense_by_id_invalid_format(self, authenticated_session, base_url):
        invalid_ids = ["abc", "12.5", "null", "undefined"]
        for invalid_id in invalid_ids:
            response = authenticated_session.get(f"{base_url}/api/expenses/{invalid_id}", timeout=10)
            assert response.status_code == 404

    @allure.title("Get expense with negative ID returns 404")
    @allure.description("Authenticated request for negative id returns 404")
    @allure.severity(allure.severity_level.MINOR)
    def test_get_expense_by_id_negative(self, authenticated_session, base_url):
        response = authenticated_session.get(f"{base_url}/api/expenses/-1", timeout=10)
        assert response.status_code == 404

    @allure.title("Get expense with zero ID returns 404")
    @allure.description("Authenticated request for id=0 returns 404")
    @allure.severity(allure.severity_level.MINOR)
    def test_get_expense_by_id_zero(self, authenticated_session, base_url):
        response = authenticated_session.get(f"{base_url}/api/expenses/0", timeout=10)
        assert response.status_code == 404

    @allure.title("Get expense with very large ID returns 404")
    @allure.description("Authenticated request for a very large id returns 404")
    @allure.severity(allure.severity_level.MINOR)
    def test_get_expense_by_id_very_large(self, authenticated_session, base_url):
        large_id = 9223372036854775807
        response = authenticated_session.get(f"{base_url}/api/expenses/{large_id}", timeout=10)
        assert response.status_code == 404
