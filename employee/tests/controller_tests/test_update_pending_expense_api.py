"""
Integration tests for PUT /api/expenses/<id> endpoint (update_expense function).
Tests cover happy path, sad path, edge cases, and boundary conditions.
"""
import pytest
import requests
from datetime import date
import time


@pytest.fixture
def session():
    """Create requests session for integration testing."""
    return requests.Session()


@pytest.fixture
def base_url():
    """Base URL for the API."""
    return 'http://localhost:5000'


@pytest.fixture
def authenticated_session(session, base_url):
    """Create authenticated session for API calls using existing sample user."""
    login_data = {
        'username': 'employee1',
        'password': 'password123'
    }
    response = session.post(f'{base_url}/api/auth/login', json=login_data)
    assert response.status_code == 200
    yield session
    
    # Logout after test
    session.post(f'{base_url}/api/auth/logout')


@pytest.fixture
def pending_expense(authenticated_session, base_url):
    """Create a pending expense for testing via API."""
    # Create expense via API
    expense_data = {
        'amount': 50.00,
        'description': 'Test expense for update',
        'date': str(date.today())
    }
    response = authenticated_session.post(f'{base_url}/api/expenses', json=expense_data)
    assert response.status_code == 201
    expense_info = response.json()
    expense_id = expense_info['expense']['id']
    
    yield expense_id
    
    # Cleanup - try to delete the expense if it still exists
    try:
        authenticated_session.delete(f'{base_url}/api/expenses/{expense_id}')
    except:
        pass  # Expense might already be deleted by test


@pytest.fixture
def pending_expense_for_unauthenticated(session, base_url):
    """Create a pending expense for testing via API without authentication."""
    expense_data = {
        'amount': 50.00,
        'description': 'Expense to test unauthenticated deletion',
        'date': str(date.today())
    }
    auth_response = session.post(f'{base_url}/api/auth/login', json={
        'username': 'employee1',
        'password': 'password123'
    })
    assert auth_response.status_code == 200
    create_response = session.post(f'{base_url}/api/expenses', json=expense_data)
    assert create_response.status_code == 201
    expense_info = create_response.json()
    pending_expense = expense_info['expense']['id']
    # Logout to make session unauthenticated
    session.post(f'{base_url}/api/auth/logout')
    
    yield pending_expense
    
    # Cleanup - try to delete the expense if it still exists
    try:
        authenticated_session.delete(f'{base_url}/api/expenses/{pending_expense}')
    except:
        pass  # Expense might already be deleted by test



class TestUpdateExpenseHappyPath:
    """Test successful expense update scenarios."""
    
    def test_update_pending_expense_all_fields(self, authenticated_session, base_url, pending_expense):
        """Test successful update of a pending expense with all fields."""
        update_data = {
            'amount': 125.75,
            'description': 'Updated client dinner expense',
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        # Verify response
        assert response.status_code == 200
        response_data = response.json()
        assert 'message' in response_data
        assert response_data['message'] == 'Expense updated successfully'
        assert 'expense' in response_data
        
        # Verify updated expense data
        expense_data = response_data['expense']
        assert expense_data['id'] == pending_expense
        assert float(expense_data['amount']) == 125.75
        assert expense_data['description'] == 'Updated client dinner expense'
        assert expense_data['date'] == '2024-12-30'
    
    def test_update_expense_with_decimal_amount(self, authenticated_session, base_url, pending_expense):
        """Test update with precise decimal amount."""
        update_data = {
            'amount': 99.99,
            'description': 'Precise decimal amount',
            'date': '2024-12-29'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        assert response.status_code == 200
        response_data = response.json()
        assert float(response_data['expense']['amount']) == 99.99


class TestUpdateExpenseSadPath:
    """Test error scenarios for expense updates."""
    
    def test_update_nonexistent_expense(self, authenticated_session, base_url):
        """Test updating an expense that doesn't exist."""
        update_data = {
            'amount': 100.00,
            'description': 'This expense does not exist',
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/999999',
            json=update_data,
            timeout=10
        )
        
        assert response.status_code == 404
        response_data = response.json()
        assert 'error' in response_data
        assert 'not found' in response_data['error'].lower()
    
    def test_update_approved_expense(self, authenticated_session, base_url):
        """Test updating an expense that's already approved."""
        # Note: Approved expense is seeded in the database with ID 2
        update_data = {
            'amount': 100.00,
            'description': 'Trying to update approved expense',
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/2',
            json=update_data,
            timeout=10
        )
        
        # This should succeed since the expense is still pending in our test scenario
        # In a real scenario with manager approval, this would be 400
        assert response.status_code == 400
        response_data = response.json()
        assert 'error' in response_data
        assert 'Cannot edit expense that has been reviewed' in response_data['error']
    
    def test_update_expense_unauthenticated(self, session, base_url, pending_expense_for_unauthenticated):
        """Test updating expense without authentication."""
        update_data = {
            'amount': 100.00,
            'description': 'Unauthorized update attempt',
            'date': '2024-12-30'
        }
        
        response = session.put(
            f'{base_url}/api/expenses/{pending_expense_for_unauthenticated}',
            json=update_data,
            timeout=10
        )
        
        assert response.status_code == 401
        response_data = response.json()
        assert 'error' in response_data
        assert 'authentication required' in response_data['error'].lower()
    
    def test_update_expense_missing_required_fields(self, authenticated_session, base_url, pending_expense):
        """Test update with missing required fields."""
        test_cases = [
            {'description': 'Missing amount and date'},  # Missing amount and date
            {'amount': 100.00},  # Missing description and date
            {'date': '2024-12-30'},  # Missing amount and description
            {'amount': 100.00, 'description': 'Missing date'},  # Missing date
        ]
        
        for update_data in test_cases:
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
            
            assert response.status_code == 400
            response_data = response.json()
            assert 'error' in response_data
            assert 'amount, description, and date are required' in response_data['error'].lower()
    
    def test_update_expense_invalid_json(self, authenticated_session, base_url, pending_expense):
        """Test update with invalid JSON payload."""
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            data='invalid json',
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        
        # Might 
        assert response.status_code in [400, 500]
        response_data = response.json()
        assert 'error' in response_data
    
    def test_update_expense_no_json_data(self, authenticated_session, base_url, pending_expense):
        """Test update with no JSON data."""
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            timeout=10
        )
        
        assert response.status_code in [400, 500]
        response_data = response.json()
        assert 'error' in response_data
        assert 'failed to update expense' in response_data['error'].lower()
        assert 'json' in response_data['details'].lower()


class TestUpdateExpenseEdgeCases:
    """Test edge cases for expense updates."""
    
    def test_update_expense_very_long_description(self, authenticated_session, base_url, pending_expense):
        """Test update with very long description."""
        long_description = 'A' * 1000  # 1000 character description
        update_data = {
            'amount': 50.00,
            'description': long_description,
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        # Should succeed
        assert response.status_code == 200
        response_data = response.json()
        assert len(response_data['expense']['description']) == 1000
        assert response_data["message"] == "Expense updated successfully"
    
    def test_update_expense_special_characters(self, authenticated_session, base_url, pending_expense):
        """Test update with special characters in description."""
        special_description = "Business meal with client @#$%^&*()[]{}|;':\",./<>?`~"
        update_data = {
            'amount': 85.50,
            'description': special_description,
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        assert response.status_code == 200
        response_data = response.json()
        assert response_data['expense']['description'] == special_description
        assert response_data["message"] == "Expense updated successfully"

    def test_update_expense_unicode_characters(self, authenticated_session, base_url, pending_expense):
        """Test update with unicode characters."""
        unicode_description = "Client meeting at café with naïve résumé discussion 🍕"
        update_data = {
            'amount': 45.00,
            'description': unicode_description,
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        assert response.status_code == 200
        response_data = response.json()
        assert response_data['expense']['description'] == unicode_description
        assert response_data["message"] == "Expense updated successfully"
    
    def test_update_expense_future_date(self, authenticated_session, base_url, pending_expense):
        """Test update with future date."""
        update_data = {
            'amount': 75.00,
            'description': 'Future expense',
            'date': '2026-04-01'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        # Bug in the code allows future dates so this should fail since its actually allowing it
        assert response.status_code == 400
        response_data = response.json()
        assert response_data['expense']['date'] == '2026-04-01'
    
    def test_update_expense_past_date(self, authenticated_session, base_url, pending_expense):
        """Test update with very old date."""
        update_data = {
            'amount': 25.00,
            'description': 'Old expense',
            'date': '2020-01-01'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        # No restrictions on past dates, should succeed
        assert response.status_code == 200
        response_data = response.json()
        assert response_data['expense']['date'] == '2020-01-01'


class TestUpdateExpenseBoundaryConditions:
    """Test boundary conditions for expense updates."""
    
    def test_update_expense_zero_amount(self, authenticated_session, base_url, pending_expense):
        """Test update with zero amount."""
        update_data = {
            'amount': 0.0,
            'description': 'Zero amount expense',
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        # Amount must be positive
        assert response.status_code == 400
        response_data = response.json()
        assert 'error' in response_data
        assert 'amount must be greater than 0' in response_data['error'].lower()
        
    def test_update_expense_negative_amount(self, authenticated_session, base_url, pending_expense):
        """Test update with negative amount."""
        update_data = {
            'amount': -50.00,
            'description': 'Negative amount expense',
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        # Amount must be positive
        assert response.status_code == 400
        response_data = response.json()
        assert 'error' in response_data
        assert 'amount must be greater than 0' in response_data['error'].lower()
    
    def test_update_expense_very_large_amount(self, authenticated_session, base_url, pending_expense):
        """Test update with very large amount."""
        update_data = {
            'amount': 999999999.99,
            'description': 'Very large expense',
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        # Large amounts are not restricted
        response.status_code == 200
        response_data = response.json()
        assert float(response_data['expense']['amount']) == 999999999.99
    
    def test_update_expense_invalid_amount_types(self, authenticated_session, base_url, pending_expense):
        """Test update with invalid amount data types."""
        invalid_amounts = [
            'not_a_number',
            '',
            [],
            {},
            'abc.def'
        ]
        
        for invalid_amount in invalid_amounts:
            update_data = {
                'amount': invalid_amount,
                'description': 'Invalid amount test',
                'date': '2024-12-30'
            }
            
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
            
            assert response.status_code == 400
            response_data = response.json()
            assert 'error' in response_data
            assert 'amount must be a valid number' in response_data['error'].lower() 
    
    def test_update_expense_empty_description(self, authenticated_session, base_url, pending_expense):
        """Test update with empty description."""
        update_data = {
            'amount': 50.00,
            'description': '',
            'date': '2024-12-30'
        }
        
        response = authenticated_session.put(
            f'{base_url}/api/expenses/{pending_expense}',
            json=update_data,
            timeout=10
        )
        
        # Empty description is not allowed
        assert response.status_code == 400
        response_data = response.json()
        assert 'error' in response_data
        assert 'description is required' in response_data['error'].lower()
    
    def test_update_expense_invalid_date_format(self, authenticated_session, base_url, pending_expense):
        """Test update with invalid date format."""
        invalid_dates = [
            'invalid_date',  # Not a date
            '2024-13-01',  # Invalid month
            '2024-12-32',  # Invalid day
            '',  # Empty date
        ]
        
        for invalid_date in invalid_dates:
            update_data = {
                'amount': 50.00,
                'description': 'Invalid date test',
                'date': invalid_date
            }
            
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
            
            # Bug in source code allows invalid dates so this should fail since its actually allowing it
            assert response.status_code == 400
            response_data = response.json()
            assert 'error' in response_data