import pytest
from pytest_mock import MockerFixture
import allure
from service.expense_service import ExpenseService
from repository.expense_repository import ExpenseRepository
from repository.approval_repository import ApprovalRepository
from repository.expense_model import Expense
from repository.approval_model import Approval


@allure.epic("Expense Management System")
@allure.feature("Expense Deletion")
@allure.story("As an employee, I want to delete expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestDeleteExpenseService:
    
    @pytest.fixture
    def mock_expense_repository(self, mocker: MockerFixture) -> ExpenseRepository:
        """Create a mock ExpenseRepository for isolated unit testing."""
        return mocker.Mock(spec=ExpenseRepository)
    
    @pytest.fixture
    def mock_approval_repository(self, mocker: MockerFixture) -> ApprovalRepository:
        """Create a mock ApprovalRepository for isolated unit testing."""
        return mocker.Mock(spec=ApprovalRepository)
    
    @pytest.fixture
    def expense_service(self, mock_expense_repository: ExpenseRepository, 
                       mock_approval_repository: ApprovalRepository) -> ExpenseService:
        """Create ExpenseService instance with mocked dependencies."""
        return ExpenseService(mock_expense_repository, mock_approval_repository)
    
    @pytest.fixture
    def sample_expense(self) -> Expense:
        """Create a sample expense object for testing."""
        return Expense(
            id = 1,
            user_id = 100,
            amount = 50.0,
            description = "Test expense",
            date = "2025-12-20"
        )
    
    @pytest.fixture
    def sample_approval(self) -> Approval:
        """Create a sample approval object for testing."""
        return Approval(
            id = 1,
            expense_id = 1,
            status = "pending",
            reviewer = None,
            comment = None,
            review_date = None
        )
    
    # Happy Path Tests
    @allure.title("Successfully delete pending expense")
    @allure.description("Test that a pending expense can be successfully deleted by the owner")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("expense_id, user_id, expected_result", [
        (1, 100, True),  # Valid expense deletion
        (5, 200, True),  # Different valid expense deletion
        (10, 150, True), # Another valid scenario
    ])
    def test_delete_expense_happy_path(self, expense_service: ExpenseService, 
                                     mock_expense_repository: ExpenseRepository,
                                     mock_approval_repository: ApprovalRepository,
                                     sample_expense: Expense, sample_approval: Approval,
                                     expense_id: int, user_id: int, expected_result: bool):
        #********** Arrange **********#
        
        # Set up sample expense and approval, will be different per paramaterized test case
        # Changes predefined fixture values to match test case, this is done because we want to test multiple scenarios
        # So why not just create new instances? Because we want to leverage the fixture system for consistency and reusability
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Stub the dependencies to return the sample data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.delete.return_value = True
        
        
        #********** Act **********#
        
        result = expense_service.delete_expense(expense_id, user_id)
        
        
        #********** Assert **********#
        
        # Verify the result matches expected outcome
        assert result == expected_result
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # Finally, since all checks passed, the delete method is called.
        mock_expense_repository.delete.assert_called_once_with(expense_id)
    
    
    # Sad Path Tests
    @allure.title("Fail to delete non-existent expense")
    @allure.description("Test that attempting to delete a non-existent expense returns False")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("expense_id,user_id,expected_result", [
        (999, 100, False),  # Non-existent expense ID
        (0, 100, False),    # Invalid expense ID
        (-1, 100, False),   # Negative expense ID
    ])
    def test_delete_expense_nonexistent_expense(self, expense_service: ExpenseService,
                                              mock_expense_repository: ExpenseRepository,
                                              mock_approval_repository: ApprovalRepository,
                                              expense_id: int, user_id: int, expected_result: bool):
        #********** Arrange **********#
        
        # Stub the repository to return None, simulating non-existent expense
        mock_expense_repository.find_by_id.return_value = None
        
        # Why dont we set up sample approval? Because if the expense doesn't exist, the approval lookup should not even be attempted.
        # Why dont we set up sample expense? Because we are simulating a non-existent expense scenario.
        
        #********** Act **********#
        
        result = expense_service.delete_expense(expense_id, user_id)
        
        #********** Assert **********#
        
        # Verify the result matches expected outcome
        assert result == expected_result
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The control flow never reaches the approval repository call since the expense does not exist.
        mock_approval_repository.find_by_expense_id.assert_not_called()
        # The control flow never reaches the repository call since it has a service layer check for expense existence first.
        mock_expense_repository.delete.assert_not_called()
    
    @allure.title("Fail to delete expense not owned by user")
    @allure.description("Test that a user cannot delete expenses that don't belong to them")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("expense_id,actual_user_id,requesting_user_id,expected_result", [
        (1, 100, 200, False),  # Different user attempting deletion
        (2, 150, 300, False),  # Another unauthorized attempt
        (3, 500, 100, False),  # Yet another unauthorized attempt
    ])
    def test_delete_expense_unauthorized_user(self, expense_service: ExpenseService,
                                            mock_expense_repository: ExpenseRepository,
                                            mock_approval_repository: ApprovalRepository,
                                            sample_expense: Expense,
                                            expense_id: int, actual_user_id: int, 
                                            requesting_user_id: int, expected_result: bool):
        #**********Arrange**********#
        
        # Set up sample expense with different user_id than the requesting user
        # Why dont we set up sample approval? Because if the expense is not owned by the user, the approval lookup should not even be attempted.
        sample_expense.id = expense_id
        sample_expense.user_id = actual_user_id
        
        # Stub the repository to return the sample expense
        mock_expense_repository.find_by_id.return_value = sample_expense
        
        
        #********** Act **********#
        
        result = expense_service.delete_expense(expense_id, requesting_user_id)
        
        
        #********** Assert **********#
        
        # Verify the result matches expected outcome
        assert result == expected_result
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The control flow never reaches the approval repository call since the user does not own the expense.
        mock_approval_repository.find_by_expense_id.assert_not_called()
        # The control flow never reaches the repository call since it has a service layer check for ownership first.
        mock_expense_repository.delete.assert_not_called()
    
    @allure.title("Fail to delete reviewed expense")
    @allure.description("Test that expenses that have been reviewed cannot be deleted")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("expense_id,user_id,approval_status,expected_exception", [
        (1, 100, "approved", "Cannot delete expense that has been reviewed"),
        (2, 100, "denied", "Cannot delete expense that has been reviewed"),
        (3, 200, "approved", "Cannot delete expense that has been reviewed"),
    ])
    def test_delete_expense_reviewed_expense(self, expense_service: ExpenseService,
                                           mock_expense_repository: ExpenseRepository,
                                           mock_approval_repository: ApprovalRepository,
                                           sample_expense: Expense, sample_approval: Approval,
                                           expense_id: int, user_id: int, 
                                           approval_status: str, expected_exception: str):
        #********** Arrange **********#
        
        # Set up sample expense and approval with reviewed status
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = approval_status
        
        # Stub the dependencies to return the sample data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        
        #********** Act & Assert **********#
        
        # Verify that attempting to delete raises the expected exception
        # Using pytest.raises context manager to check for exception
        # Code inside this block should raise the specified exception
        with pytest.raises(ValueError, match=expected_exception):
            expense_service.delete_expense(expense_id, user_id)
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # The control flow never reaches the repository call since the expense has already been reviewed.
        mock_expense_repository.delete.assert_not_called()
    
    
    # Edge Case Tests
    @allure.title("Handle missing approval record")
    @allure.description("Test behavior when expense exists but has no approval record")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("expense_id,user_id,expected_result", [
        (1, 100, False),
        (5, 200, False),
    ])
    def test_delete_expense_missing_approval(self, expense_service: ExpenseService,
                                           mock_expense_repository: ExpenseRepository,
                                           mock_approval_repository: ApprovalRepository,
                                           sample_expense: Expense,
                                           expense_id: int, user_id: int, expected_result: bool):
        #********** Arrange **********#
        
        # Set up sample expense
        # Why dont we set up sample approval? Because we are simulating a missing approval record scenario.
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        
        # Stub the dependencies to return the sample expense and None for approval
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = None
        
        
        #********** Act **********#
        
        result = expense_service.delete_expense(expense_id, user_id)
        
        
        #********** Assert **********#
        
        # Verify the result matches expected outcome
        assert result == expected_result
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # The control flow never reaches the repository call since the expense lacks an approval record.
        mock_expense_repository.delete.assert_not_called()
    
    @allure.title("Handle repository deletion failure")
    @allure.description("Test behavior when repository delete operation fails")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("expense_id,user_id,delete_result", [
        (1, 100, False),  # Repository delete fails
        (2, 150, False),  # Another deletion failure
    ])
    def test_delete_expense_repository_failure(self, expense_service: ExpenseService,
                                             mock_expense_repository: ExpenseRepository,
                                             mock_approval_repository: ApprovalRepository,
                                             sample_expense: Expense, sample_approval: Approval,
                                             expense_id: int, user_id: int, delete_result: bool):
        #********** Arrange **********#
        
        # Set up sample expense and approval
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Stub the dependencies to return the sample data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.delete.return_value = delete_result
        
        
        #********** Act **********#
        
        result = expense_service.delete_expense(expense_id, user_id)
        
        
        #********** Assert **********#
        
        # Verify the result matches expected outcome
        assert result == delete_result
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # The service layer then attempts to delete the expense by calling this method.
        mock_expense_repository.delete.assert_called_once_with(expense_id)
    
    
    # Boundary Tests
    @allure.title("Test boundary values for expense and user IDs")
    @allure.description("Test delete operation with boundary values for IDs")
    @allure.severity(allure.severity_level.MINOR)
    @pytest.mark.parametrize("expense_id,user_id,expected_success", [
        (1, 1, True),           # Minimum valid IDs
        (999999, 999999, True), # Large valid IDs
        (2147483647, 2147483647, True), # Maximum 32-bit integer values
    ])
    def test_delete_expense_boundary_values(self, expense_service: ExpenseService,
                                          mock_expense_repository: ExpenseRepository,
                                          mock_approval_repository: ApprovalRepository,
                                          sample_expense: Expense, sample_approval: Approval,
                                          expense_id: int, user_id: int, expected_success: bool):
        #********** Arrange **********#
        
        # Set up sample expense and approval
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Stub the dependencies to return the sample data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.delete.return_value = True
        
        
        #********** Act **********#
        
        result = expense_service.delete_expense(expense_id, user_id)
        
        
        #********** Assert **********#
        
        # Verify the result matches expected outcome
        assert result == expected_success
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # The service layer then attempts to delete the expense by calling this method.
        mock_expense_repository.delete.assert_called_once_with(expense_id)
    
    @allure.title("Test all valid approval status transitions")
    @allure.description("Comprehensive test of approval status boundary conditions")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("status,should_allow_deletion,should_raise_exception", [
        ("pending", True, False),   # Only pending should allow deletion
        ("approved", False, True),  # Approved should raise exception
        ("denied", False, True),    # Denied should raise exception
    ])
    def test_delete_expense_status_boundaries(self, expense_service: ExpenseService,
                                            mock_expense_repository: ExpenseRepository,
                                            mock_approval_repository: ApprovalRepository,
                                            sample_expense: Expense, sample_approval: Approval,
                                            status: str, should_allow_deletion: bool, 
                                            should_raise_exception: bool):
        #********** Arrange **********#
        
        # Set up sample expense and approval with varying status
        expense_id, user_id = 1, 100
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = status
        
        # Stub the dependencies to return the sample data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.delete.return_value = True
        
        
        #********** Act & Assert **********#
        
        # Verify behavior based on approval status, denied and approved should raise exceptions
        if should_raise_exception:
            # Verify that attempting to delete raises the expected exception
            # Using pytest.raises context manager to check for exception
            # Code inside this block should raise the specified exception
            with pytest.raises(ValueError, match="Cannot delete expense that has been reviewed"):
                expense_service.delete_expense(expense_id, user_id)
            
            # Mocking: verify the behavior of the repositories (times called with correct params)
            # The control flow never reaches the repository call since the expense has already been reviewed.
            mock_expense_repository.delete.assert_not_called()
        # Otherwise, deletion should proceed successfully
        else:
            result = expense_service.delete_expense(expense_id, user_id)
            # Verify the result matches expected outcome
            assert result == should_allow_deletion
            # Mocking: verify the behavior of the repositories (times called with correct params)
            # Finally, since all checks passed, the delete method is called.
            mock_expense_repository.delete.assert_called_once_with(expense_id)
        
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        