import pytest
import allure
from pytest_mock import MockerFixture
from typing import List, Tuple

from repository.expense_repository import ExpenseRepository
from repository.approval_repository import ApprovalRepository
from service.expense_service import ExpenseService
from repository.expense_model import Expense
from repository.approval_model import Approval


@allure.epic("Expense Management System")
@allure.feature("Expense Retrieval")
@allure.story("As an employee, I want to view my expenses and their status so I can track my reimbursement requests")
class TestGetterExpenseService:

    @pytest.fixture
    def mock_expense_repository(self, mocker: MockerFixture) -> ExpenseRepository:
        """Create a mock ExpenseRepository for isolated unit testing."""
        return mocker.Mock(spec=ExpenseRepository)

    @pytest.fixture
    def mock_approval_repository(self, mocker: MockerFixture) -> ApprovalRepository:
        """Create a mock ApprovalRepository for isolated unit testing."""
        return mocker.Mock(spec=ApprovalRepository)

    @pytest.fixture
    def service(self, mock_expense_repository, mock_approval_repository):
        """Create ExpenseService instance with mocked dependencies."""
        return ExpenseService(mock_expense_repository, mock_approval_repository)

    @pytest.fixture
    def sample_data(self) -> Tuple[Expense, Approval]:
        """Create a sample pair of expense and approval."""
        expense = Expense(id=1, user_id=100, amount=50.0, description="Test", date="2025-01-01")
        approval = Approval(id=1, expense_id=1, status="pending", reviewer=None, comment=None, review_date=None)
        return expense, approval

    # =========================
    # GET EXPENSE BY ID
    # =========================
    @allure.title("Get expense by ID: {scenario}")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("scenario, expense_id, user_id, mock_return, expected_result", [
        ("Happy Path: Owner retrieves expense", 1, 100, Expense(1, 100, 50.0, "Test", "2025-01-01"), Expense(1, 100, 50.0, "Test", "2025-01-01")),
        ("Sad Path: Unauthorized user", 1, 999, Expense(1, 100, 50.0, "Test", "2025-01-01"), None),
        ("Edge Case: Non-existent expense", 404, 100, None, None),
        ("Boundary: ID is zero", 0, 100, None, None)
    ])
    def test_get_expense_by_id_scenarios(self, service, mock_expense_repository, scenario, expense_id, user_id, mock_return, expected_result):
        mock_expense_repository.find_by_id.return_value = mock_return

        with allure.step(f"Scenario: {scenario}"):
            result = service.get_expense_by_id(expense_id, user_id)

        with allure.step("Verify result"):
            assert result == expected_result
            if expense_id > 0:
                mock_expense_repository.find_by_id.assert_called_with(expense_id)

    # =========================
    # GET EXPENSE WITH STATUS
    # =========================
    @allure.title("Get expense with status: {scenario}")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("scenario, expense_id, user_id, mock_exp, mock_appr, expected_count", [
        ("Happy Path: Valid retrieval", 1, 100, Expense(1, 100, 50.0, "T", "D"), Approval(1, 1, "pending", None, None, None), 2),
        ("Sad Path: Wrong user", 1, 999, Expense(1, 100, 50.0, "T", "D"), Approval(1, 1, "pending", None, None, None), 0),
        ("Edge Case: Missing approval record", 1, 100, Expense(1, 100, 50.0, "T", "D"), None, 0),
        ("Edge Case: Missing expense record", 404, 100, None, None, 0)
    ])
    def test_get_expense_with_status_scenarios(self, service, mock_expense_repository, mock_approval_repository, scenario, expense_id, user_id, mock_exp, mock_appr, expected_count):
        mock_expense_repository.find_by_id.return_value = mock_exp
        mock_approval_repository.find_by_expense_id.return_value = mock_appr

        with allure.step(f"Running: {scenario}"):
            result = service.get_expense_with_status(expense_id, user_id)

        with allure.step("Verify result tuple or None"):
            if expected_count == 2:
                assert isinstance(result, tuple)
                assert len(result) == 2
            else:
                assert result is None

    # =========================
    # GET EXPENSE HISTORY
    # =========================
    @allure.title("Expense history: {scenario}")
    @allure.severity(allure.severity_level.MINOR)
    @pytest.mark.parametrize("scenario, filter_status, mock_data, expected_len", [
        ("Happy Path: Filter 'pending'", "pending", [
            (Expense(1, 1, 10, "A", "D"), Approval(1, 1, "pending", None, None, None)),
            (Expense(2, 1, 20, "B", "D"), Approval(2, 2, "approved", None, None, None))
        ], 1),
        ("Happy Path: No filter (All)", None, [
            (Expense(1, 1, 10, "A", "D"), Approval(1, 1, "pending", None, None, None)),
            (Expense(2, 1, 20, "B", "D"), Approval(2, 2, "approved", None, None, None))
        ], 2),
        ("Sad Path: Filter with no matches", "denied", [
            (Expense(1, 1, 10, "A", "D"), Approval(1, 1, "pending", None, None, None))
        ], 0),
        ("Edge Case: Empty history", None, [], 0)
    ])
    def test_get_expense_history_scenarios(self, service, mock_approval_repository, scenario, filter_status, mock_data, expected_len):
        mock_approval_repository.find_expenses_with_status_for_user.return_value = mock_data

        with allure.step(f"Fetching history for: {scenario}"):
            results = service.get_expense_history(1, status_filter=filter_status)

        with allure.step("Verify filtered list length"):
            assert len(results) == expected_len
            if filter_status and expected_len > 0:
                assert all(r[1].status == filter_status for r in results)
