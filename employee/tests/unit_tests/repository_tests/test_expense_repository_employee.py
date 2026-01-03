import sqlite3
import pytest
import allure
from unittest.mock import Mock

from repository.expense_repository import ExpenseRepository
from repository.expense_model import Expense
from repository.database import DatabaseConnection


@pytest.fixture
def repo():
    # Setup in-memory database for repository testing
    db = DatabaseConnection(":memory:")
    conn = db.get_connection()

    conn.row_factory = sqlite3.Row

    conn.execute("""
        CREATE TABLE IF NOT EXISTS expenses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            amount REAL,
            description TEXT,
            date TEXT
        )
    """)
    # Seed initial data for finding
    conn.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (1, 100, 50.0, 'Initial', '2025-01-01')")
    conn.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (2, 100, 20.0, 'Second', '2025-01-02')")
    conn.commit()

    db.get_connection = Mock(return_value=conn)
    return ExpenseRepository(db)

@allure.epic("Expense Management System")
@allure.feature("Expense Repository")
class TestExpenseRepositoryFind:

    # =========================
    # FIND BY ID
    # =========================
    @allure.story("Find Expense by unique ID")
    @allure.title("Scenario: {scenario}")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("scenario, expense_id, expected_found", [
        ("Happy Path: Existing ID", 1, True),
        ("Sad Path: Non-existent ID", 999, False),
        ("Edge Case: Negative ID", -1, False),
        ("Boundary Case: Zero ID", 0, False)
    ])
    def test_find_by_id_scenarios(self, repo, scenario, expense_id, expected_found):
        with allure.step(f"Executing search for ID: {expense_id}"):
            result = repo.find_by_id(expense_id)

        with allure.step("Verify if record was found"):
            if expected_found:
                assert result is not None
                assert result.id == expense_id
            else:
                assert result is None

    # =========================
    # FIND BY USER ID
    # =========================
    @allure.story("Find all Expenses for a User")
    @allure.title("Scenario: {scenario}")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("scenario, user_id, expected_count", [
        ("Happy Path: User with multiple expenses", 100, 2),
        ("Sad Path: User with no expenses", 200, 0),
        ("Edge Case: Non-existent user", 404, 0),
        ("Boundary Case: Large User ID", 999999, 0)
    ])
    def test_find_by_user_id_scenarios(self, repo, scenario, user_id, expected_count):
        with allure.step(f"Retrieving expenses for User ID: {user_id}"):
            results = repo.find_by_user_id(user_id)

        with allure.step(f"Verify count of returned records is {expected_count}"):
            assert len(results) == expected_count
            if expected_count > 0:
                assert all(r.user_id == user_id for r in results)

    # =========================
    # DETAILED MAPPING TESTS (Lines 43-45)
    # =========================
    @allure.story("Verify database to model mapping")
    @allure.title("Verify mapping for: {description}")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("expense_id, user_id, amount, description, date", [
        (1, 100, 50.0, "Initial", "2025-01-01"),
        (2, 100, 20.0, "Second", "2025-01-02"),
    ])
    def test_find_by_id_mapping_details(self, repo, expense_id, user_id, amount, description, date):
        """Specifically targets lines 43-45 to ensure all Expense fields are correctly returned."""
        with allure.step(f"Retrieving record {expense_id} and checking field mapping"):
            result = repo.find_by_id(expense_id)

        with allure.step("Assert all model fields match seeded database values"):
            assert result is not None
            assert result.id == expense_id
            assert result.user_id == user_id
            assert result.amount == amount
            assert result.description == description
            assert result.date == date
