import allure
import pytest
from datetime import datetime
from unittest.mock import MagicMock

from allure_pytest.utils import allure_name

from service.expense_service import ExpenseService
from repository.expense_model import Expense
from repository.expense_repository import ExpenseRepository
from repository.approval_repository import ApprovalRepository

# ==================== FIXTURES ====================

@pytest.fixture
def mock_expense_repository():
    """Fixture for mocked expense repository."""
    return MagicMock(spec=ExpenseRepository)

@pytest.fixture
def mock_approval_repository():
    """Fixture for mocked approval repository."""
    return MagicMock(spec=ApprovalRepository)

@pytest.fixture
def expense_service(mock_expense_repository, mock_approval_repository):
    """Fixture for ExpenseService with mocked dependencies."""
    return ExpenseService(
        expense_repository=mock_expense_repository,
        approval_repository=mock_approval_repository
    )

# ==================== TEST CLASS ====================
@allure.epic("Expense Management")
@allure.feature("Employee Submission")
class TestExpenseService:
    """Test cases for ExpenseService.submit_expense() method."""

    @allure.title("R-10_001: Happy Path Submission")
    @allure.description("Requirement: Users must be able to submit a standard expense with valid data.")
    def test_submit_expense_happy(self, expense_service, mock_expense_repository):
        # Arrange
        user_id = 1
        amount = 100.0
        description = "Business lunch"
        date = "2024-06-01"
        expected_expense = Expense(id=1, user_id=user_id, amount=amount, description=description, date=date)
        mock_expense_repository.create.return_value = expected_expense

        # Act
        with allure.step("Submit valid expense to service"):
            result = expense_service.submit_expense(user_id, amount, description, date)

        # Assert
        with allure.step("Verify expense was created correctly"):
            assert result == expected_expense
            mock_expense_repository.create.assert_called_once()
            call_args = mock_expense_repository.create.call_args[0][0]
            assert call_args.amount == amount

    @allure.title("R-10_003: Rejection of Negative Amount")
    @allure.description("Requirement: System must reject expenses with negative amounts.")
    def test_submit_expense_negative_amount(self, expense_service, mock_expense_repository):
        # Arrange
        user_id = 1
        amount = -50.0
        description = "Office supplies"

        # Act & Assert
        with allure.step("Verify service raises ValueError for negative amount"):
            with pytest.raises(ValueError) as excinfo:
                expense_service.submit_expense(user_id, amount, description)
            assert str(excinfo.value) == "Amount must be greater than 0"
            mock_expense_repository.create.assert_not_called()

    @allure.title("R-10_004: Rejection of Empty Description")
    def test_submit_expense_empty_description(self, expense_service, mock_expense_repository):
        # Arrange
        user_id = 1
        amount = 50.0
        description = ""

        # Act & Assert
        with allure.step("Verify service raises ValueError for missing description"):
            with pytest.raises(ValueError) as excinfo:
                expense_service.submit_expense(user_id, amount, description)
            assert str(excinfo.value) == "Description is required"
            mock_expense_repository.create.assert_not_called()

    @allure.title("R-10_006: Rejection of Future Date")
    @allure.description("Requirement: System must reject expenses with future dates.")
    @allure.issue("BUG-102", "Future dates should not be allowed")
    def test_submit_expense_future_date(self, expense_service, mock_expense_repository):
        """Modified to assert for error based on logical business rules."""
        # Arrange
        user_id = 1
        amount = 100.0
        description = "Future expense"
        date = "2100-12-31" 

        # Act & Assert
        with allure.step("Verify system blocks future dates"):
            with pytest.raises(ValueError) as excinfo:
                expense_service.submit_expense(user_id, amount, description, date)
            # Business rule should block future dates
            assert "future" in str(excinfo.value).lower()

    @allure.story("Boundary Testing")
    @allure.issue( "BUG: System allows long descriptions without validation")
    @allure.description("Test submission of expense with a very long description:")
    @allure.title("R-10_007: Rejection of Long Description")
    def test_submit_expense_very_long_description(self, expense_service, mock_expense_repository):
        # Arrange
        description = "A" * 1001  # Assuming limit is 1000

        # Act & Assert
        with allure.step("Verify system blocks descriptions over character limit"):
            with pytest.raises(ValueError) as excinfo:
                expense_service.submit_expense(1, 100.0, description)
            # Flexible assertion to catch various common error wordings
            error_msg = str(excinfo.value).lower()
            assert any(word in error_msg for word in ["long", "limit", "character", "exceed"])