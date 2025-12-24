import pytest
from unittest.mock import Mock, patch

from repository import ExpenseRepository, ApprovalRepository
from service.expense_service import ExpenseService
from repository.expense_model import Expense
from repository.approval_model import Approval


@pytest.fixture
def service():
    expense_repo = Mock(spec=ExpenseRepository)
    approval_repo = Mock(spec=ApprovalRepository)
    return ExpenseService(expense_repo, approval_repo)


# =========================
# SUBMIT EXPENSE
# =========================
@pytest.mark.parametrize("amount,description", [
    (10.5, "Lunch"),
    (9999.99, "Conference travel"),
    (0.01, "Parking")
])
def test_submit_expense_happy(service, amount, description):
    service.expense_repository.create.return_value = Expense(
        id=1, user_id=1, amount=amount, description=description, date="2025-01-01"
    )

    expense = service.submit_expense(1, amount, description)
    assert expense.amount == amount
    assert expense.description == description


@pytest.mark.parametrize("amount", [0, -1, -100])
def test_submit_expense_invalid_amount(service, amount):
    with pytest.raises(ValueError):
        service.submit_expense(1, amount, "Valid description")


@pytest.mark.parametrize("description", ["", "   "])
def test_submit_expense_blank_description(service, description):
    with pytest.raises(ValueError):
        service.submit_expense(1, 10, description)


# =========================
# UPDATE EXPENSE
# =========================
@pytest.mark.parametrize("status", ["approved", "denied"])
def test_update_expense_not_pending(service, status):
    expense = Expense(id=1, user_id=1, amount=10, description="Old", date="2025-01-01")
    # Providing all 6 required arguments for Approval
    approval = Approval(1, 1, status, None, None, None)
    
    # Mocking the internal method result
    with patch.object(service, 'get_expense_with_status', return_value=(expense, approval)):
        with pytest.raises(ValueError, match="Cannot edit expense that has been reviewed"):
            service.update_expense(1, 1, 20.0, "New", "2025-01-02")


def test_update_expense_happy(service):
    expense = Expense(id=1, user_id=1, amount=10, description="Old", date="2025-01-01")
    approval = Approval(1, 1, "pending", None, None, None)
    
    with patch.object(service, 'get_expense_with_status', return_value=(expense, approval)):
        service.expense_repository.update.return_value = expense
        result = service.update_expense(1, 1, 20.0, "Updated", "2025-01-02")
        assert result.amount == 20.0
        service.expense_repository.update.assert_called_once()


def test_delete_expense_happy(service):
    expense = Expense(id=1, user_id=1, amount=10, description="Test", date="2025-01-01")
    approval = Approval(1, 1, "pending", None, None, None)
    
    with patch.object(service, 'get_expense_with_status', return_value=(expense, approval)):
        service.expense_repository.delete.return_value = True
        assert service.delete_expense(1, 1) is True


def test_delete_expense_reviewed(service):
    expense = Expense(id=1, user_id=1, amount=10, description="Test", date="2025-01-01")
    approval = Approval(1, 1, "approved", None, None, None)
    
    with patch.object(service, 'get_expense_with_status', return_value=(expense, approval)):
        with pytest.raises(ValueError, match="Cannot delete expense that has been reviewed"):
            service.delete_expense(1, 1)


def test_get_expense_history_filtered(service):
    expense1 = Expense(id=1, user_id=1, amount=10, description="Test1", date="2025-01-01")
    expense2 = Expense(id=2, user_id=1, amount=20, description="Test2", date="2025-01-02")
    approval1 = Approval(1, 1, "pending", None, None, None)
    approval2 = Approval(2, 2, "approved", None, None, None)
    
    service.approval_repository.find_expenses_with_status_for_user.return_value = [
        (expense1, approval1), (expense2, approval2)
    ]
    
    results = service.get_expense_history(1, status_filter="pending")
    assert len(results) == 1
    assert results[0][1].status == "pending"
