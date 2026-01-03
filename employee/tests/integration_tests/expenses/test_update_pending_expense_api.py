"""
Integration tests for PUT /api/expenses/<id> endpoint (update_expense function).
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



@allure.epic("Expense Management System")
@allure.feature("Expense Updates - API Integration")
@allure.story("As an employee, I want to edit expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestUpdateExpenseHappyPath:
    """Test successful expense update scenarios."""
    
    @allure.title("Successfully update pending expense with all fields")
    @allure.description("Test successful update of a pending expense with all fields via API")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_pending_expense_all_fields(self, authenticated_session, base_url, pending_expense):
        """Test successful update of a pending expense with all fields."""
        
        with allure.step("Arrange: Prepare update data with all fields"):
            update_data = {
                'amount': 125.75,
                'description': 'Updated client dinner expense',
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request to update expense"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify response structure and success message"):
            assert response.status_code == 200
            response_data = response.json()
            assert 'message' in response_data
            assert response_data['message'] == 'Expense updated successfully'
            assert 'expense' in response_data
        
        with allure.step("Assert: Verify updated expense data"):
            expense_data = response_data['expense']
            assert expense_data['id'] == pending_expense
            assert float(expense_data['amount']) == 125.75
            assert expense_data['description'] == 'Updated client dinner expense'
            assert expense_data['date'] == '2024-12-30'
    
    @allure.title("Successfully update expense with precise decimal amount")
    @allure.description("Test update with precise decimal amount handling")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_expense_with_decimal_amount(self, authenticated_session, base_url, pending_expense):
        """Test update with precise decimal amount."""
        
        with allure.step("Arrange: Prepare update data with precise decimal amount"):
            update_data = {
                'amount': 99.99,
                'description': 'Precise decimal amount',
                'date': '2024-12-29'
            }
        
        with allure.step("Act: Send PUT request to update expense"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify response and decimal amount precision"):
            assert response.status_code == 200
            response_data = response.json()
            assert float(response_data['expense']['amount']) == 99.99


@allure.epic("Expense Management System")
@allure.feature("Expense Updates - API Integration")
@allure.story("As an employee, I want to edit expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestUpdateExpenseSadPath:
    """Test error scenarios for expense updates."""
    
    @allure.title("Fail to update non-existent expense")
    @allure.description("Test updating an expense that doesn't exist returns proper 404 error")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_nonexistent_expense(self, authenticated_session, base_url):
        """Test updating an expense that doesn't exist."""
        
        with allure.step("Arrange: Prepare update data for non-existent expense"):
            update_data = {
                'amount': 100.00,
                'description': 'This expense does not exist',
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request to non-existent expense ID"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/999999',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify 404 error response"):
            assert response.status_code == 404
            response_data = response.json()
            assert 'error' in response_data
            assert 'not found' in response_data['error'].lower()
    
    @allure.title("Fail to update approved expense")
    @allure.description("Test that approved expenses cannot be updated")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_approved_expense(self, authenticated_session, base_url):
        """Test updating an expense that's already approved."""
        
        with allure.step("Arrange: Prepare update data for approved expense"):
            # Note: Approved expense is seeded in the database with ID 2
            update_data = {
                'amount': 100.00,
                'description': 'Trying to update approved expense',
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request to approved expense ID"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/2',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify 400 error for reviewed expense"):
            # This should succeed since the expense is still pending in our test scenario
            # In a real scenario with manager approval, this would be 400
            assert response.status_code == 400
            response_data = response.json()
            assert 'error' in response_data
            assert 'Cannot edit expense that has been reviewed' in response_data['error']
    
    @allure.title("Fail to update expense without authentication")
    @allure.description("Test that unauthenticated requests are properly rejected")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_expense_unauthenticated(self, session, base_url):
        """Test updating expense without authentication."""
        
        with allure.step("Arrange: Prepare update data for unauthenticated request"):
            update_data = {
                'amount': 100.00,
                'description': 'Unauthorized update attempt',
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request without authentication"):
            response = session.put(
                f'{base_url}/api/expenses/1',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify 401 authentication error"):
            assert response.status_code == 401
            response_data = response.json()
            assert 'error' in response_data
            assert 'authentication required' in response_data['error'].lower()
    
    @allure.title("Fail to update expense with missing required fields")
    @allure.description("Test that requests with missing required fields are rejected")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_expense_missing_required_fields(self, authenticated_session, base_url, pending_expense):
        """Test update with missing required fields."""
        
        with allure.step("Arrange: Prepare test cases with missing required fields"):
            test_cases = [
                {'description': 'Missing amount and date'},  # Missing amount and date
                {'amount': 100.00},  # Missing description and date
                {'date': '2024-12-30'},  # Missing amount and description
                {'amount': 100.00, 'description': 'Missing date'},  # Missing date
            ]
        
        with allure.step("Act & Assert: Test each missing field scenario"):
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
    
    @allure.title("Fail to update expense with invalid JSON payload")
    @allure.description("Test that invalid JSON payloads are properly handled")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_expense_invalid_json(self, authenticated_session, base_url, pending_expense):
        """Test update with invalid JSON payload."""
        
        with allure.step("Arrange: Prepare invalid JSON payload"):
            invalid_json_data = 'invalid json'
            headers = {'Content-Type': 'application/json'}
        
        with allure.step("Act: Send PUT request with invalid JSON"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                data=invalid_json_data,
                headers=headers,
                timeout=10
            )
        
        with allure.step("Assert: Verify error response for invalid JSON"):
            assert response.status_code in [400, 500]
            response_data = response.json()
            assert 'error' in response_data
    
    @allure.title("Fail to update expense with no JSON data")
    @allure.description("Test that requests without JSON data are properly rejected")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_expense_no_json_data(self, authenticated_session, base_url, pending_expense):
        """Test update with no JSON data."""
        
        with allure.step("Act: Send PUT request with no JSON data"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                timeout=10
            )
        
        with allure.step("Assert: Verify error response for missing JSON data"):
            assert response.status_code in [400, 500]
            response_data = response.json()
            assert 'error' in response_data
            assert 'failed to update expense' in response_data['error'].lower()
            assert 'json' in response_data['details'].lower()


@allure.epic("Expense Management System")
@allure.feature("Expense Updates - API Integration")
@allure.story("As an employee, I want to edit expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestUpdateExpenseEdgeCases:
    """Test edge cases for expense updates."""
    
    @allure.title("Handle very long description in expense update")
    @allure.description("Test update with very long description (1000 characters)")
    @allure.severity(allure.severity_level.MINOR)
    def test_update_expense_very_long_description(self, authenticated_session, base_url, pending_expense):
        """Test update with very long description."""
        
        with allure.step("Arrange: Prepare update data with very long description (1000 characters)"):
            long_description = 'A' * 1000  # 1000 character description
            update_data = {
                'amount': 50.00,
                'description': long_description,
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request with long description"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify successful update and description length"):
            # Should succeed
            assert response.status_code == 200
            response_data = response.json()
            assert len(response_data['expense']['description']) == 1000
            assert response_data["message"] == "Expense updated successfully"
    
    @allure.title("Handle special characters in expense description")
    @allure.description("Test update with special characters in description")
    @allure.severity(allure.severity_level.MINOR)
    def test_update_expense_special_characters(self, authenticated_session, base_url, pending_expense):
        """Test update with special characters in description."""
        
        with allure.step("Arrange: Prepare update data with special characters"):
            special_description = "Business meal with client @#$%^&*()[]{}|;':,/./<>?`~"
            update_data = {
                'amount': 85.50,
                'description': special_description,
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request with special characters in description"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify successful update and special character handling"):
            assert response.status_code == 200
            response_data = response.json()
            assert response_data['expense']['description'] == special_description
            assert response_data["message"] == "Expense updated successfully"

    @allure.title("Handle unicode characters in expense description")
    @allure.description("Test update with unicode characters and emojis in description")
    @allure.severity(allure.severity_level.MINOR)
    def test_update_expense_unicode_characters(self, authenticated_session, base_url, pending_expense):
        """Test update with unicode characters."""
        
        with allure.step("Arrange: Prepare update data with unicode characters and emojis"):
            unicode_description = "Client meeting at café with naïve résumé discussion 🍕"
            update_data = {
                'amount': 45.00,
                'description': unicode_description,
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request with unicode characters"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify successful update and unicode character handling"):
            assert response.status_code == 200
            response_data = response.json()
            assert response_data['expense']['description'] == unicode_description
            assert response_data["message"] == "Expense updated successfully"
    
    @allure.title("Handle future date in expense update")
    @allure.description("Test update with future date (should be rejected but is allowed due to bug)")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_expense_future_date(self, authenticated_session, base_url, pending_expense):
        """Test update with future date."""
        
        with allure.step("Arrange: Prepare update data with future date"):
            update_data = {
                'amount': 75.00,
                'description': 'Future expense',
                'date': '2026-04-01'
            }
        
        with allure.step("Act: Send PUT request with future date"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify future date handling (bug allows it)"):
            # Bug in the code allows future dates so this should fail since its actually allowing it
            assert response.status_code == 400
            response_data = response.json()
            assert response_data['expense']['date'] == '2026-04-01'
    
    @allure.title("Handle past date in expense update")
    @allure.description("Test update with very old date (should be allowed)")
    @allure.severity(allure.severity_level.MINOR)
    def test_update_expense_past_date(self, authenticated_session, base_url, pending_expense):
        """Test update with very old date."""
        
        with allure.step("Arrange: Prepare update data with past date"):
            update_data = {
                'amount': 25.00,
                'description': 'Old expense',
                'date': '2020-01-01'
            }
        
        with allure.step("Act: Send PUT request with past date"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify past date is allowed"):
            # No restrictions on past dates, should succeed
            assert response.status_code == 200
            response_data = response.json()
            assert response_data['expense']['date'] == '2020-01-01'


@allure.epic("Expense Management System")
@allure.feature("Expense Updates - API Integration")
@allure.story("As an employee, I want to edit expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestUpdateExpenseBoundaryConditions:
    """Test boundary conditions for expense updates."""
    
    @allure.title("Fail to update expense with zero amount")
    @allure.description("Test that zero amounts are properly rejected")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_expense_zero_amount(self, authenticated_session, base_url, pending_expense):
        """Test update with zero amount."""
        
        with allure.step("Arrange: Prepare update data with zero amount"):
            update_data = {
                'amount': 0.0,
                'description': 'Zero amount expense',
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request with zero amount"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify zero amount is rejected"):
            # Amount must be positive
            assert response.status_code == 400
            response_data = response.json()
            assert 'error' in response_data
            assert 'amount must be greater than 0' in response_data['error'].lower()
        
    @allure.title("Fail to update expense with negative amount")
    @allure.description("Test that negative amounts are properly rejected")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_expense_negative_amount(self, authenticated_session, base_url, pending_expense):
        """Test update with negative amount."""
        
        with allure.step("Arrange: Prepare update data with negative amount"):
            update_data = {
                'amount': -50.00,
                'description': 'Negative amount expense',
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request with negative amount"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify negative amount is rejected"):
            # Amount must be positive
            assert response.status_code == 400
            response_data = response.json()
            assert 'error' in response_data
            assert 'amount must be greater than 0' in response_data['error'].lower()
    
    @allure.title("Handle very large expense amounts")
    @allure.description("Test update with very large amount values")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_expense_very_large_amount(self, authenticated_session, base_url, pending_expense):
        """Test update with very large amount."""
        
        with allure.step("Arrange: Prepare update data with very large amount"):
            update_data = {
                'amount': 999999999.99,
                'description': 'Very large expense',
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request with very large amount"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify large amount is accepted"):
            # Large amounts are not restricted
            response.status_code == 200
            response_data = response.json()
            assert float(response_data['expense']['amount']) == 999999999.99
    
    @allure.title("Fail to update expense with invalid amount types")
    @allure.description("Test that non-numeric amount values are properly rejected")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_expense_invalid_amount_types(self, authenticated_session, base_url, pending_expense):
        """Test update with invalid amount data types."""
        
        with allure.step("Arrange: Prepare test cases with invalid amount types"):
            invalid_amounts = [
                'not_a_number',
                '',
                [],
                {},
                'abc.def'
            ]
        
        with allure.step("Act & Assert: Test each invalid amount type"):
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
    
    @allure.title("Fail to update expense with empty description")
    @allure.description("Test that empty descriptions are properly rejected")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_expense_empty_description(self, authenticated_session, base_url, pending_expense):
        """Test update with empty description."""
        
        with allure.step("Arrange: Prepare update data with empty description"):
            update_data = {
                'amount': 50.00,
                'description': '',
                'date': '2024-12-30'
            }
        
        with allure.step("Act: Send PUT request with empty description"):
            response = authenticated_session.put(
                f'{base_url}/api/expenses/{pending_expense}',
                json=update_data,
                timeout=10
            )
        
        with allure.step("Assert: Verify empty description is rejected"):
            # Empty description is not allowed
            assert response.status_code == 400
            response_data = response.json()
            assert 'error' in response_data
            assert 'description is required' in response_data['error'].lower()
    
    @allure.title("Fail to update expense with invalid date formats")
    @allure.description("Test that invalid date formats are properly rejected (should be rejected but is allowed due to bug)")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_expense_invalid_date_format(self, authenticated_session, base_url, pending_expense):
        """Test update with invalid date format."""
        
        with allure.step("Arrange: Prepare test cases with invalid date formats"):
            invalid_dates = [
                'invalid_date',  # Not a date
                '2024-13-01',  # Invalid month
                '2024-12-32',  # Invalid day
                '',  # Empty date
            ]
        
        with allure.step("Act & Assert: Test each invalid date format"):
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