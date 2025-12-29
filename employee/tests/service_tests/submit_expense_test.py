"""
User Story: As an employee, I want to submit an expense report so that I can get reimbursed for work-related expenses.
"""

from datetime import datetime
import pytest
from unittest.mock import MagicMock, patch
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

class TestExpenseService:
    """Test cases for ExpenseService.submit_expense() method."""

    def test_submit_expense_happy(self, expense_service, mock_expense_repository):
        """R-10_001: Submit expense with valid data - Happy path"""
        # Arrange
        user_id = 1
        amount = 100.0
        description = "Business lunch"
        date = "2024-06-01"
        expected_expense = Expense(
            id=1,
            user_id=user_id,
            amount=amount,
            description=description,
            date=date
        )
        # Mock the repository to return the expected expense
        mock_expense_repository.create.return_value = expected_expense

        # Act
        result = expense_service.submit_expense(user_id, amount, description, date)

        # Assert - Verify return value
        assert result == expected_expense

        # Assert - Verify repository was called once
        mock_expense_repository.create.assert_called_once()

        # Assert - Verify correct data passed to repository
        call_args = mock_expense_repository.create.call_args[0][0]
        assert call_args.user_id == user_id
        assert call_args.amount == amount
        assert call_args.description == description
        assert call_args.date == date
        assert call_args.id is None

    def test_submit_expense_repository_sad(self, expense_service, mock_expense_repository):
        """R-10_002: Submit expense when repository fails - Error handling"""
        # Arrange
        user_id = 1
        amount = 100.0
        description = "Business lunch"

        # Mock repository to raise an exception
        mock_expense_repository.create.side_effect = Exception("Database connection failed")

        # Act & Assert
        with pytest.raises(Exception) as excinfo:
            expense_service.submit_expense(user_id, amount, description)

        assert "Database connection failed" in str(excinfo.value)
        # Assert - Verify repository was called once
        mock_expense_repository.create.assert_called_once()

    def test_submit_expense_negative_amount(self, expense_service, mock_expense_repository):
        """R-10_003: Submit expense with negative amount - Rejection"""
        # Arrange
        user_id = 1
        amount = -50.0
        description = "Office supplies"

        # Act & Assert
        with pytest.raises(ValueError) as excinfo:
            expense_service.submit_expense(user_id, amount, description)

        assert str(excinfo.value) == "Amount must be greater than 0"

        # Assert - Verify repository was NOT called (validation failed)
        mock_expense_repository.create.assert_not_called()

    def test_submit_expense_empty_description(self, expense_service, mock_expense_repository):
        """R-10_004: Submit expense with empty description - Rejection"""
        # Arrange
        user_id = 1
        amount = 50.0
        description = ""

        # Act & Assert
        with pytest.raises(ValueError) as excinfo:
            expense_service.submit_expense(user_id, amount, description)

        assert str(excinfo.value) == "Description is required"

        # Assert - Verify repository was NOT called (validation failed)
        mock_expense_repository.create.assert_not_called()

    def test_submit_expense_future_date(self, expense_service, mock_expense_repository):
        """R-10_006: Submit expense with future date - Date logic validation
        """
        # Arrange
        user_id = 1
        amount = 100.0
        description = "Future expense"
        date = "2025-12-31"  # Future date

        expected_expense = Expense(
            id=1,
            user_id=user_id,
            amount=amount,
            description=description,
            date=date
        )
        mock_expense_repository.create.return_value = expected_expense

        # Act
        result = expense_service.submit_expense(user_id, amount, description, date)

        # Assert - Verify return value
        assert result == expected_expense
        assert result.date == "2025-12-31"

        # Assert - Verify repository was called
        mock_expense_repository.create.assert_called_once()

        # Assert - Verify future date was passed to repository
        call_args = mock_expense_repository.create.call_args[0][0]
        assert call_args.date == date

    def test_submit_expense_very_long_description(self, expense_service, mock_expense_repository):
        """R-10_007: Submit expense with very long description - Length validation
        """
        # Arrange
        user_id = 1
        amount = 100.0
        description = "A" * 10000  # Very long description

        expected_expense = Expense(
            id=1,
            user_id=user_id,
            amount=amount,
            description=description,
            date=datetime.now().strftime('%Y-%m-%d')
        )
        mock_expense_repository.create.return_value = expected_expense

        # Act
        result = expense_service.submit_expense(user_id, amount, description)

        # Assert - Verify return value
        assert result == expected_expense
        assert len(result.description) == 10000

        # Assert - Verify repository was called
        mock_expense_repository.create.assert_called_once()

        # Assert - Verify long description was passed to repository
        call_args = mock_expense_repository.create.call_args[0][0]
        assert len(call_args.description) == 10000
        assert call_args.description == description