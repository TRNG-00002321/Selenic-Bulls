"""
Expense management endpoints.
"""
from flask import Blueprint, request, jsonify, current_app
from api.auth import require_employee_auth, get_current_user
from service.expense_service import ExpenseService


expense_bp = Blueprint('expense', __name__, url_prefix='/api/expenses')


def get_expense_service() -> ExpenseService:
    """Get expense service from Flask app context."""
    return current_app.expense_service


@expense_bp.route('', methods=['POST'])
@require_employee_auth
def submit_expense():
    """Submit a new expense."""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': 'JSON data required'}), 400
        
        amount = data.get('amount')
        description = data.get('description')
        date = data.get('date')  # Optional, will use current date if not provided
        
        if amount is None or description is None:
            return jsonify({'error': 'Amount and description are required'}), 400
        
        try:
            amount = float(amount)
        except (ValueError, TypeError):
            return jsonify({'error': 'Amount must be a valid number'}), 400
        
        current_user = get_current_user()
        expense_service = get_expense_service()
        
        expense = expense_service.submit_expense(
            user_id=current_user.id,
            amount=amount,
            description=description,
            date=date
        )
        
        return jsonify({
            'message': 'Expense submitted successfully',
            'expense': {
                'id': expense.id,
                'amount': expense.amount,
                'description': expense.description,
                'date': expense.date,
                'status': 'pending'
            }
        }), 201
        
    except ValueError as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        return jsonify({'error': 'Failed to submit expense', 'details': str(e)}), 500


@expense_bp.route('', methods=['GET'])
@require_employee_auth
def get_expenses():
    """Get all expenses for the current user."""
    try:
        status_filter = request.args.get('status')  # Optional filter: pending, approved, denied
        
        current_user = get_current_user()
        expense_service = get_expense_service()
        
        expenses_with_status = expense_service.get_expense_history(
            user_id=current_user.id,
            status_filter=status_filter
        )
        
        expenses_data = []
        for expense, approval in expenses_with_status:
            expenses_data.append({
                'id': expense.id,
                'amount': expense.amount,
                'description': expense.description,
                'date': expense.date,
                'status': approval.status,
                'comment': approval.comment,
                'review_date': approval.review_date
            })
        
        return jsonify({
            'expenses': expenses_data,
            'count': len(expenses_data)
        })
        
    except Exception as e:
        return jsonify({'error': 'Failed to retrieve expenses', 'details': str(e)}), 500


@expense_bp.route('/<int:expense_id>', methods=['GET'])
@require_employee_auth
def get_expense(expense_id):
    """Get a specific expense by ID."""
    try:
        current_user = get_current_user()
        expense_service = get_expense_service()
        
        result = expense_service.get_expense_with_status(expense_id, current_user.id)
        
        if not result:
            return jsonify({'error': 'Expense not found'}), 404
        
        expense, approval = result
        
        return jsonify({
            'expense': {
                'id': expense.id,
                'amount': expense.amount,
                'description': expense.description,
                'date': expense.date,
                'status': approval.status,
                'comment': approval.comment,
                'review_date': approval.review_date
            }
        })
        
    except Exception as e:
        return jsonify({'error': 'Failed to retrieve expense', 'details': str(e)}), 500


@expense_bp.route('/<int:expense_id>', methods=['PUT'])
@require_employee_auth
def update_expense(expense_id):
    """Update an existing expense (only if pending)."""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': 'JSON data required'}), 400
        
        amount = data.get('amount')
        description = data.get('description')
        date = data.get('date')
        
        if amount is None or description is None or date is None:
            return jsonify({'error': 'Amount, description, and date are required'}), 400
        
        try:
            amount = float(amount)
        except (ValueError, TypeError):
            return jsonify({'error': 'Amount must be a valid number'}), 400
        
        current_user = get_current_user()
        expense_service = get_expense_service()
        
        updated_expense = expense_service.update_expense(
            expense_id=expense_id,
            user_id=current_user.id,
            amount=amount,
            description=description,
            date=date
        )
        
        if not updated_expense:
            return jsonify({'error': 'Expense not found'}), 404
        
        return jsonify({
            'message': 'Expense updated successfully',
            'expense': {
                'id': updated_expense.id,
                'amount': updated_expense.amount,
                'description': updated_expense.description,
                'date': updated_expense.date
            }
        })
        
    except ValueError as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        return jsonify({'error': 'Failed to update expense', 'details': str(e)}), 500


@expense_bp.route('/<int:expense_id>', methods=['DELETE'])
@require_employee_auth
def delete_expense(expense_id):
    """Delete an expense (only if pending)."""
    try:
        current_user = get_current_user()
        expense_service = get_expense_service()
        
        success = expense_service.delete_expense(expense_id, current_user.id)
        
        if not success:
            return jsonify({'error': 'Expense not found'}), 404
        
        return jsonify({'message': 'Expense deleted successfully'})
        
    except ValueError as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        return jsonify({'error': 'Failed to delete expense', 'details': str(e)}), 500