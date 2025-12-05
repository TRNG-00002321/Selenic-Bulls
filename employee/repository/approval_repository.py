"""
Repository for approval-related database operations.
"""
from typing import List, Optional
from .expense_model import Expense
from .approval_model import Approval
from .database import DatabaseConnection


class ApprovalRepository:
    """Repository for approval-related database operations."""
    
    def __init__(self, db_connection: DatabaseConnection):
        self.db_connection = db_connection
    
    def find_by_expense_id(self, expense_id: int) -> Optional[Approval]:
        """Find approval by expense ID."""
        with self.db_connection.get_connection() as conn:
            cursor = conn.execute(
                "SELECT id, expense_id, status, reviewer, comment, review_date FROM approvals WHERE expense_id = ?",
                (expense_id,)
            )
            row = cursor.fetchone()
            if row:
                return Approval(id=row['id'], expense_id=row['expense_id'], 
                              status=row['status'], reviewer=row['reviewer'], 
                              comment=row['comment'], review_date=row['review_date'])
        return None
    
    def find_expenses_with_status_for_user(self, user_id: int) -> List[tuple]:
        """Find all expenses with their approval status for a user."""
        results = []
        with self.db_connection.get_connection() as conn:
            cursor = conn.execute('''
                SELECT e.id, e.amount, e.description, e.date, a.status, a.comment, a.review_date
                FROM expenses e
                JOIN approvals a ON e.id = a.expense_id
                WHERE e.user_id = ?
                ORDER BY e.date DESC
            ''', (user_id,))
            
            for row in cursor.fetchall():
                expense = Expense(id=row['id'], user_id=user_id, 
                                amount=row['amount'], description=row['description'], 
                                date=row['date'])
                approval = Approval(id=None, expense_id=row['id'], 
                                  status=row['status'], reviewer=None, 
                                  comment=row['comment'], review_date=row['review_date'])
                results.append((expense, approval))
        return results
    
    def update_status(self, expense_id: int, status: str, reviewer_id: Optional[int] = None, 
                     comment: Optional[str] = None, review_date: Optional[str] = None) -> bool:
        """Update approval status."""
        with self.db_connection.get_connection() as conn:
            cursor = conn.execute(
                "UPDATE approvals SET status = ?, reviewer = ?, comment = ?, review_date = ? WHERE expense_id = ?",
                (status, reviewer_id, comment, review_date, expense_id)
            )
            conn.commit()
            return cursor.rowcount > 0