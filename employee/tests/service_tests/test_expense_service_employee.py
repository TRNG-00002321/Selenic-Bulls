import pytest
from unittest.mock import Mock
from service.expense_service import ExpenseService
from repository.expense_model import Expense
from repository.approval_model import Approval


@pytest.fixture
def service():
    expense_repo = Mock()
    approval_repo = Mock()
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
    approval = Approval(id=1, expense_id=1, status=status)
    service.get_expense_with_status = Mock(return_value=(expense, approval))

    with pytest.raises(ValueError):
        service.update_expense(1, 1, 20, "New", "2025-01-02")


def test_update_expense_happy(service):
    expense = Expense(id=1, user_id=1, amount=10, description="Old", date="2025-01-01")
    approval = Approval(id=1, expense_id=1, status="pending")
    service.get_expense_with_status = Mock(return_value=(expense, approval))
    service.expense_repository.update.return_value = expense

    updated = service.update_expense(1, 1, 25, "Updated", "2025-01-02")
    assert updated.amount == 25
    assert updated.description == "Updated"


# =========================
# DELETE EXPENSE
# =========================
def test_delete_expense_happy(service):
    expense = Expense(id=1, user_id=1, amount=10, description="Test", date="2025-01-01")
    approval = Approval(id=1, expense_id=1, status="pending")
    service.get_expense_with_status = Mock(return_value=(expense, approval))
    service.expense_repository.delete.return_value = True

    assert service.delete_expense(1, 1) is True


def test_delete_expense_reviewed(service):
    expense = Expense(id=1, user_id=1, amount=10, description="Test", date="2025-01-01")
    approval = Approval(id=1, expense_id=1, status="approved")
    service.get_expense_with_status = Mock(return_value=(expense, approval))

    with pytest.raises(ValueError):
        service.delete_expense(1, 1)


# =========================
# GET EXPENSE HISTORY
# =========================
def test_get_expense_history_filtered(service):
    expense1 = Expense(id=1, user_id=1, amount=10, description="Test1", date="2025-01-01")
    expense2 = Expense(id=2, user_id=1, amount=20, description="Test2", date="2025-01-02")
    approval1 = Approval(id=1, expense_id=1, status="pending")
    approval2 = Approval(id=2, expense_id=2, status="approved")

    service.get_user_expenses_with_status = Mock(return_value=[(expense1, approval1), (expense2, approval2)])
    filtered = service.get_expense_history(1, status_filter="approved")

    assert len(filtered) == 1
    assert filtered[0][1].status == "approved"
