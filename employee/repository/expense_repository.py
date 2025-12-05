"""
Repository for expense-related database operations.
"""
from typing import List, Optional
from .expense_model import Expense
from .database import DatabaseConnection


class ExpenseRepository:
    """Repository for expense-related database operations."""
    
    def __init__(self, db_connection: DatabaseConnection):
        self.db_connection = db_connection
    
    def create(self, expense: Expense) -> Expense:
        """Create a new expense and its initial approval record."""
        with self.db_connection.get_connection() as conn:
            # Insert expense
            cursor = conn.execute(
                "INSERT INTO expenses (user_id, amount, description, date) VALUES (?, ?, ?, ?)",
                (expense.user_id, expense.amount, expense.description, expense.date)
            )
            expense.id = cursor.lastrowid
            
            # Create initial approval record with 'pending' status
            conn.execute(
                "INSERT INTO approvals (expense_id, status) VALUES (?, 'pending')",
                (expense.id,)
            )
            
            conn.commit()
        return expense
    
    def find_by_id(self, expense_id: int) -> Optional[Expense]:
        """Find an expense by ID."""
        with self.db_connection.get_connection() as conn:
            cursor = conn.execute(
                "SELECT id, user_id, amount, description, date FROM expenses WHERE id = ?",
                (expense_id,)
            )
            row = cursor.fetchone()
            if row:
                return Expense(id=row['id'], user_id=row['user_id'], 
                             amount=row['amount'], description=row['description'], 
                             date=row['date'])
        return None
    
    def find_by_user_id(self, user_id: int) -> List[Expense]:
        """Find all expenses for a user."""
        expenses = []
        with self.db_connection.get_connection() as conn:
            cursor = conn.execute(
                "SELECT id, user_id, amount, description, date FROM expenses WHERE user_id = ? ORDER BY date DESC",
                (user_id,)
            )
            for row in cursor.fetchall():
                expenses.append(Expense(id=row['id'], user_id=row['user_id'], 
                                      amount=row['amount'], description=row['description'], 
                                      date=row['date']))
        return expenses
    
    def update(self, expense: Expense) -> Expense:
        """Update an existing expense."""
        with self.db_connection.get_connection() as conn:
            conn.execute(
                "UPDATE expenses SET amount = ?, description = ?, date = ? WHERE id = ?",
                (expense.amount, expense.description, expense.date, expense.id)
            )
            conn.commit()
        return expense
    
    def delete(self, expense_id: int) -> bool:
        """Delete an expense and its approval record."""
        with self.db_connection.get_connection() as conn:
            # Delete approval record first (foreign key constraint)
            conn.execute("DELETE FROM approvals WHERE expense_id = ?", (expense_id,))
            # Delete expense
            cursor = conn.execute("DELETE FROM expenses WHERE id = ?", (expense_id,))
            conn.commit()
            return cursor.rowcount > 0