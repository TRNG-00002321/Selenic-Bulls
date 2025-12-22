import sqlite3
import pytest
from repository.expense_repository import ExpenseRepository
from repository.expense_model import Expense
from repository.database import DatabaseConnection


@pytest.fixture
def repo():
    db = DatabaseConnection(":memory:")
    conn = db.get_connection()

    conn.execute("""
        CREATE TABLE expenses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            amount REAL,
            description TEXT,
            date TEXT
        )
    """)
    conn.execute("""
        CREATE TABLE approvals (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            expense_id INTEGER,
            status TEXT
        )
    """)
    conn.commit()
    return ExpenseRepository(db)


def test_create_expense(repo):
    expense = Expense(None, 1, 10.0, "Lunch", "2025-01-01")
    saved = repo.create(expense)
    assert saved.id is not None


def test_find_by_id_not_found(repo):
    assert repo.find_by_id(999) is None


def test_find_by_user_id(repo):
    repo.create(Expense(None, 1, 10, "Test", "2025-01-01"))
    expenses = repo.find_by_user_id(1)
    assert len(expenses) == 1


def test_delete_expense(repo):
    expense = repo.create(Expense(None, 1, 10, "Test", "2025-01-01"))
    deleted = repo.delete(expense.id)
    assert deleted is True
