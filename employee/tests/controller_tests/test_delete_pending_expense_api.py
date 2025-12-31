"""
Integration tests for DELETE /api/expenses/<id> endpoint (delete_expense function).
Tests cover happy path, sad path, edge cases, and boundary conditions.
"""
import pytest
import requests
import allure
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
        'amount': 100.00,
        'description': 'Test expense for deletion',
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



@allure.epic("Expense Management System")
@allure.feature("Expense Deletion - API Integration")
@allure.story("As an employee, I want to delete expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestDeleteExpenseHappyPath:
    """Test successful expense deletion scenarios."""
    
    @allure.title("Successfully delete pending expense")
    @allure.description("Test successful deletion of a pending expense via API")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_delete_pending_expense_success(self, authenticated_session, base_url, pending_expense):
        """Test successful deletion of a pending expense."""
        # Verify expense exists before deletion
        get_response = authenticated_session.get(f'{base_url}/api/expenses/{pending_expense}')
        assert get_response.status_code == 200
        
        # Delete the expense
        response = authenticated_session.delete(
            f'{base_url}/api/expenses/{pending_expense}',
            timeout=10
        )
        
        # Verify successful deletion
        assert response.status_code == 200
        response_data = response.json()
        assert 'message' in response_data
        assert 'deleted successfully' in response_data['message'].lower()
        
        # Verify expense no longer exists
        get_response = authenticated_session.get(f'{base_url}/api/expenses/{pending_expense}')
        assert get_response.status_code == 404
        response_data = get_response.json()
        assert 'error' in response_data
        assert 'not found' in response_data['error'].lower()
    
    @allure.title("Verify JSON response structure for deletion")
    @allure.description("Test that deletion response has correct JSON structure")
    @allure.severity(allure.severity_level.NORMAL)
    def test_delete_expense_json_response_structure(self, authenticated_session, base_url, pending_expense):
        """Test that deletion response has correct JSON structure."""
        response = authenticated_session.delete(
            f'{base_url}/api/expenses/{pending_expense}',
            timeout=10
        )
        
        assert response.status_code == 200
        response_data = response.json()
        
        # Verify response structure
        assert isinstance(response_data, dict)
        assert 'message' in response_data
        assert isinstance(response_data['message'], str)
        assert len(response_data['message']) > 0


@allure.epic("Expense Management System")
@allure.feature("Expense Deletion - API Integration")
@allure.story("As an employee, I want to delete expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestDeleteExpenseSadPath:
    """Test error scenarios for expense deletion."""
    
    @allure.title("Fail to delete non-existent expense")
    @allure.description("Test deleting an expense that doesn't exist returns proper 404 error")
    @allure.severity(allure.severity_level.NORMAL)
    def test_delete_nonexistent_expense(self, authenticated_session, base_url):
        """Test deleting an expense that doesn't exist."""
        response = authenticated_session.delete(
            f'{base_url}/api/expenses/999999',
            timeout=10
        )
        
        assert response.status_code == 404
        response_data = response.json()
        assert 'error' in response_data
        assert 'not found' in response_data['error'].lower()
    
    @allure.title("Fail to delete expense without authentication")
    @allure.description("Test that unauthenticated deletion requests are properly rejected")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_delete_expense_unauthenticated_session(self, session, base_url, pending_expense_for_unauthenticated):
        """Test deleting expense without authentication."""
        
        response = session.delete(
            f'{base_url}/api/expenses/{pending_expense_for_unauthenticated}',
            timeout=10
        )
        
        assert response.status_code == 401
        response_data = response.json()
        assert 'error' in response_data
        assert 'authentication required' in response_data['error'].lower()
    
    @allure.title("Fail to delete expense with invalid ID format")
    @allure.description("Test deleting expense with invalid ID format returns proper error")
    @allure.severity(allure.severity_level.NORMAL)
    def test_delete_expense_invalid_id_format(self, authenticated_session, base_url):
        """Test deleting expense with invalid ID format."""
        invalid_ids = ['abc', '12.5', 'null', 'undefined']
        
        for invalid_id in invalid_ids:
            if invalid_id:  # Skip empty string as it would change the URL structure
                response = authenticated_session.delete(
                    f'{base_url}/api/expenses/{invalid_id}',
                    timeout=10
                )
                
                # Should return 404 for invalid ID formats
                assert response.status_code == 404


@allure.epic("Expense Management System")
@allure.feature("Expense Deletion - API Integration")
@allure.story("As an employee, I want to delete expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestDeleteExpenseEdgeCases:
    """Test edge cases for expense deletion."""
    
    @allure.title("Handle negative expense ID in deletion")
    @allure.description("Test deleting expense with negative ID")
    @allure.severity(allure.severity_level.MINOR)
    def test_delete_expense_negative_id(self, authenticated_session, base_url):
        """Test deleting expense with negative ID."""
        response = authenticated_session.delete(
            f'{base_url}/api/expenses/-1',
            timeout=10
        )
        
        assert response.status_code == 404
    
    @allure.title("Handle zero expense ID in deletion")
    @allure.description("Test deleting expense with ID zero")
    @allure.severity(allure.severity_level.MINOR)
    def test_delete_expense_zero_id(self, authenticated_session, base_url):
        """Test deleting expense with ID zero."""
        response = authenticated_session.delete(
            f'{base_url}/api/expenses/0',
            timeout=10
        )
        
        assert response.status_code == 404
        response_data = response.json()
        assert 'error' in response_data
        assert 'not found' in response_data['error'].lower()
    
    @allure.title("Handle very large expense ID in deletion")
    @allure.description("Test deleting expense with maximum 64-bit integer ID")
    @allure.severity(allure.severity_level.MINOR)
    def test_delete_expense_very_large_id(self, authenticated_session, base_url):
        """Test deleting expense with very large ID."""
        large_id = 9223372036854775807  # Maximum 64-bit integer
        response = authenticated_session.delete(
            f'{base_url}/api/expenses/{large_id}',
            timeout=10
        )
        
        assert response.status_code == 404
        response_data = response.json()
        assert 'error' in response_data
        assert 'not found' in response_data['error'].lower()
    
    @allure.title("Handle deletion of already deleted expense")
    @allure.description("Test deleting an expense that was already deleted")
    @allure.severity(allure.severity_level.NORMAL)
    def test_delete_already_deleted_expense(self, authenticated_session, base_url, pending_expense):
        """Test deleting an expense that was already deleted."""
        # First deletion should succeed
        response1 = authenticated_session.delete(
            f'{base_url}/api/expenses/{pending_expense}',
            timeout=10
        )
        assert response1.status_code == 200
        
        # Second deletion should fail
        response2 = authenticated_session.delete(
            f'{base_url}/api/expenses/{pending_expense}',
            timeout=10
        )
        assert response2.status_code == 404
        response_data = response2.json()
        assert 'error' in response_data
        assert 'not found' in response_data['error'].lower()


@allure.epic("Expense Management System")
@allure.feature("Expense Deletion - API Integration")
@allure.story("As an employee, I want to delete expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestDeleteExpenseBoundaryConditions:
    """Test boundary conditions for expense deletion."""
    
    @allure.title("Handle boundary ID values in deletion")
    @allure.description("Test deletion with boundary ID values")
    @allure.severity(allure.severity_level.MINOR)
    def test_delete_expense_boundary_id_values(self, authenticated_session, base_url):
        """Test deletion with boundary ID values."""
        boundary_ids = [0, 2147483648]  # Min and max typical integer values
        
        for boundary_id in boundary_ids:
            response = authenticated_session.delete(
                f'{base_url}/api/expenses/{boundary_id}',
                timeout=10
            )
            
            # Should return 404 for non-existent expenses
            assert response.status_code == 404
            response_data = response.json()
            assert 'error' in response_data
            assert 'not found' in response_data['error'].lower()
    
    @allure.title("Handle special URL characters in deletion")
    @allure.description("Test deletion with URL-encoded characters in path")
    @allure.severity(allure.severity_level.MINOR)
    def test_delete_expense_with_special_url_characters(self, authenticated_session, base_url):
        """Test deletion with URL-encoded characters in path."""
        special_cases = ['%20', '%2F', '%3F']  # space, slash, question mark
        
        for special_case in special_cases:
            response = authenticated_session.delete(
                f'{base_url}/api/expenses/{special_case}',
                timeout=10
            )
            
            # Should handle URL encoding appropriately
            assert response.status_code == 404