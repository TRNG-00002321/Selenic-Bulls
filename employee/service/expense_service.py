"""
Service for expense-related business operations.
"""
from typing import List, Optional, Tuple
from datetime import datetime
from repository.expense_model import Expense
from repository.approval_model import Approval
from repository.expense_repository import ExpenseRepository
from repository.approval_repository import ApprovalRepository


class ExpenseService:
    """Service for expense-related business operations."""
    
    def __init__(self, expense_repository: ExpenseRepository, approval_repository: ApprovalRepository):
        self.expense_repository = expense_repository
        self.approval_repository = approval_repository
    
    def submit_expense(self, user_id: int, amount: float, description: str, date: str = None) -> Expense:
        """Submit a new expense for the user."""
        if amount <= 0:
            raise ValueError("Amount must be greater than 0")
        
        if not description.strip():
            raise ValueError("Description is required")
        
        # Use current date if none provided
        if not date:
            date = datetime.now().strftime('%Y-%m-%d')
        
        expense = Expense(
            id=None,
            user_id=user_id,
            amount=amount,
            description=description.strip(),
            date=date
        )
        
        return self.expense_repository.create(expense)
    
    def get_user_expenses_with_status(self, user_id: int) -> List[Tuple[Expense, Approval]]:
        """Get all expenses for a user with their approval status."""
        return self.approval_repository.find_expenses_with_status_for_user(user_id)
    
    def get_expense_by_id(self, expense_id: int, user_id: int) -> Optional[Expense]:
        """Get an expense by ID, ensuring it belongs to the user."""
        expense = self.expense_repository.find_by_id(expense_id)
        if expense and expense.user_id == user_id:
            return expense
        return None
    
    def get_expense_with_status(self, expense_id: int, user_id: int) -> Optional[Tuple[Expense, Approval]]:
        """Get expense with its approval status, ensuring it belongs to the user."""
        expense = self.get_expense_by_id(expense_id, user_id)
        if expense:
            approval = self.approval_repository.find_by_expense_id(expense_id)
            if approval:
                return expense, approval
        return None
    
    def update_expense(self, expense_id: int, user_id: int, amount: float, description: str, date: str) -> Optional[Expense]:
        """Update an existing expense if it's still pending."""
        # Get expense and check ownership and status
        result = self.get_expense_with_status(expense_id, user_id)
        if not result:
            return None
        
        expense, approval = result
        
        # Only allow updates if expense is still pending
        if approval.status != 'pending':
            raise ValueError("Cannot edit expense that has been reviewed")
        
        if amount <= 0:
            raise ValueError("Amount must be greater than 0")
        
        if not description.strip():
            raise ValueError("Description is required")
        
        # Update the expense
        expense.amount = amount
        expense.description = description.strip()
        expense.date = date
        
        return self.expense_repository.update(expense)
    
    def delete_expense(self, expense_id: int, user_id: int) -> bool:
        """Delete an expense if it's still pending."""
        # Get expense and check ownership and status
        result = self.get_expense_with_status(expense_id, user_id)
        if not result:
            return False
        
        expense, approval = result
        
        # Only allow deletion if expense is still pending
        if approval.status != 'pending':
            raise ValueError("Cannot delete expense that has been reviewed")
        
        return self.expense_repository.delete(expense_id)
    
    def get_expense_history(self, user_id: int, status_filter: str = None) -> List[Tuple[Expense, Approval]]:
        """Get expense history with optional status filter."""
        all_expenses = self.get_user_expenses_with_status(user_id)
        
        if status_filter and status_filter in ['pending', 'approved', 'denied']:
            return [(expense, approval) for expense, approval in all_expenses 
                   if approval.status == status_filter]
        
        return all_expenses