import pytest
from pytest_mock import MockerFixture
import allure
from service.expense_service import ExpenseService
from repository.expense_repository import ExpenseRepository
from repository.approval_repository import ApprovalRepository
from repository.expense_model import Expense
from repository.approval_model import Approval


@allure.epic("Expense Management System")
@allure.feature("Expense Updates")
@allure.story("As an employee, I want to edit expenses that are still pending so that I can correct mistakes before they are reviewed")
class TestUpdateExpenseService:
    
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
            id=1,
            user_id=100,
            amount=50.0,
            description="Original expense",
            date="2025-12-20"
        )
    
    @pytest.fixture
    def sample_approval(self) -> Approval:
        """Create a sample approval object for testing."""
        return Approval(
            id=1,
            expense_id=1,
            status="pending",
            reviewer=None,
            comment=None,
            review_date=None
        )
    
    # Happy Path Tests
    @allure.title("Successfully update pending expense")
    @allure.description("Test that a pending expense can be successfully updated by the owner")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("expense_id,user_id,new_amount,new_description,new_date", [
        (1, 100, 75.0, "Updated expense description", "2025-12-21"),
        (2, 200, 120.5, "Business meal", "2025-12-19"),
        (3, 150, 25.99, "Office supplies", "2025-12-22"),
    ])
    def test_update_expense_happy_path(self, expense_service: ExpenseService,
                                     mock_expense_repository: ExpenseRepository,
                                     mock_approval_repository: ApprovalRepository,
                                     sample_expense: Expense, sample_approval: Approval,
                                     expense_id: int, user_id: int, new_amount: float,
                                     new_description: str, new_date: str):
        #********** Arrange **********#
        
        # Set up sample expense and approval with pending status
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Prepare expected updated expense
        expected_expense = Expense(
            id=expense_id,
            user_id=user_id,
            amount=new_amount,
            description=new_description,
            date=new_date
        )
        
        # Stub the dependencies to return the sample/expected data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.update.return_value = expected_expense
        
        
        #********** Act **********#
        
        result = expense_service.update_expense(expense_id, user_id, new_amount, new_description, new_date)
        
        
        #********** Assert **********#
        
        # Verify the result matches expected updated expense
        assert result is not None
        assert result.amount == new_amount
        assert result.description == new_description
        assert result.date == new_date
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # The service layer then attempts to update the expense by calling this method.
        mock_expense_repository.update.assert_called_once()
        
        # Verify the expense object was properly modified before update
        # call_args is a tuple where the first element is a tuple of positional args
        # which means the first argument to the update method is the updated expense object
        updated_expense = mock_expense_repository.update.call_args[0][0]
        assert updated_expense.amount == new_amount
        assert updated_expense.description == new_description
        assert updated_expense.date == new_date
    
    @allure.title("Successfully update expense with whitespace in description")
    @allure.description("Test that description whitespace is properly trimmed during update")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("expense_id,user_id,description_with_whitespace,expected_description", [
        (1, 100, "  Trimmed description  ", "Trimmed description"),
        (2, 200, "\t\nTabbed and newlined\t\n", "Tabbed and newlined"),
        (3, 150, "   Leading and trailing   ", "Leading and trailing"),
    ])
    def test_update_expense_whitespace_handling(self, expense_service: ExpenseService,
                                              mock_expense_repository: ExpenseRepository,
                                              mock_approval_repository: ApprovalRepository,
                                              sample_expense: Expense, sample_approval: Approval,
                                              expense_id: int, user_id: int,
                                              description_with_whitespace: str, expected_description: str):
        #********** Arrange **********#
        
        # Set up sample expense and approval with pending status
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Stub the dependencies to return the sample data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.update.return_value = sample_expense
        
        
        #********** Act **********#
        
        result = expense_service.update_expense(expense_id, user_id, 50.0, description_with_whitespace, "2025-12-20")
        
        
        #********** Assert **********#
        
        # Verify the result is not None and description is trimmed
        assert result is not None
        assert result.description == expected_description
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # The service layer then attempts to update the expense by calling this method.
        mock_expense_repository.update.assert_called_once()
        
        # Verify the expense object was properly modified before update
        # call_args is a tuple where the first element is a tuple of positional args
        # which means the first argument to the update method is the updated expense object
        updated_expense = mock_expense_repository.update.call_args[0][0]
        assert updated_expense.description == expected_description
    
    
    # Sad Path Tests
    @allure.title("Fail to update non-existent expense")
    @allure.description("Test that updating a non-existent expense returns None")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("expense_id,user_id,amount,description,date", [
        (999, 100, 50.0, "Test description", "2025-12-20"),
        (0, 100, 25.0, "Another test", "2025-12-21"),
        (-1, 200, 75.0, "Invalid ID test", "2025-12-19"),
    ])
    def test_update_expense_nonexistent_expense(self, expense_service: ExpenseService,
                                              mock_expense_repository: ExpenseRepository,
                                              mock_approval_repository: ApprovalRepository,
                                              expense_id: int, user_id: int, amount: float,
                                              description: str, date: str):
        #********** Arrange **********#
        
        # No sample expense needed here as we are simulating non-existence
        
        # Stub the dependency to return None for non-existent expense
        mock_expense_repository.find_by_id.return_value = None
        
        
        #********** Act **********#
        
        result = expense_service.update_expense(expense_id, user_id, amount, description, date)
        
        #********** Assert **********#
        
        # Verify the result is None
        assert result is None
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # Since expense does not exist, approval repository should not be called. The control flow never gets transferred there.
        mock_approval_repository.find_by_expense_id.assert_not_called()
        # Since expense does not exist, update should not be called. The control flow never gets transferred there.
        mock_expense_repository.update.assert_not_called()
    
    @allure.title("Fail to update expense not owned by user")
    @allure.description("Test that a user cannot update expenses that don't belong to them")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("expense_id,actual_user_id,requesting_user_id,amount,description,date", [
        (1, 100, 200, 50.0, "Test description", "2025-12-20"),
        (2, 150, 300, 25.0, "Another test", "2025-12-21"),
        (3, 500, 100, 75.0, "Unauthorized test", "2025-12-19"),
    ])
    def test_update_expense_unauthorized_user(self, expense_service: ExpenseService,
                                            mock_expense_repository: ExpenseRepository,
                                            mock_approval_repository: ApprovalRepository,
                                            sample_expense: Expense,
                                            expense_id: int, actual_user_id: int, requesting_user_id: int,
                                            amount: float, description: str, date: str):
        #********** Arrange **********#
        
        # Set up sample expense owned by a different user
        sample_expense.id = expense_id
        sample_expense.user_id = actual_user_id
        
        # Stub the dependency to return the sample expense
        mock_expense_repository.find_by_id.return_value = sample_expense
        
        
        #********** Act **********#
        
        result = expense_service.update_expense(expense_id, requesting_user_id, amount, description, date)
        
        
        #********** Assert **********#
        
        # Verify the result is None since user is not authorized
        assert result is None
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # Since user is not authorized, approval repository should not be called. The control flow never gets transferred there.
        mock_approval_repository.find_by_expense_id.assert_not_called()
        # Since user is not authorized, update should not be called. The control flow never gets transferred there.
        mock_expense_repository.update.assert_not_called()
    
    @allure.title("Fail to update reviewed expense")
    @allure.description("Test that expenses that have been reviewed cannot be updated")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("expense_id,user_id,approval_status,amount,description,date,expected_exception", [
        (1, 100, "approved", 50.0, "Test description", "2025-12-20", "Cannot edit expense that has been reviewed"),
        (2, 200, "denied", 25.0, "Another test", "2025-12-21", "Cannot edit expense that has been reviewed"),
        (3, 150, "approved", 75.0, "Third test", "2025-12-19", "Cannot edit expense that has been reviewed"),
    ])
    def test_update_expense_reviewed_expense(self, expense_service: ExpenseService,
                                           mock_expense_repository: ExpenseRepository,
                                           mock_approval_repository: ApprovalRepository,
                                           sample_expense: Expense, sample_approval: Approval,
                                           expense_id: int, user_id: int, approval_status: str,
                                           amount: float, description: str, date: str, expected_exception: str):
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
        
        # Verify that attempting to update raises the expected exception
        # Using pytest.raises context manager to check for exception
        # Code inside this block should raise the specified exception
        with pytest.raises(ValueError, match=expected_exception):
            expense_service.update_expense(expense_id, user_id, amount, description, date)
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # Since expense is reviewed, update should not be called. The control flow never gets transferred there.
        mock_expense_repository.update.assert_not_called()
    
    @allure.title("Fail to update with invalid amount")
    @allure.description("Test that invalid amounts are rejected during update")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("expense_id,user_id,invalid_amount,description,date,expected_exception", [
        (1, 100, 0.0, "Test description", "2025-12-20", "Amount must be greater than 0"),
        (2, 200, -10.0, "Another test", "2025-12-21", "Amount must be greater than 0"),
        (3, 150, -0.01, "Negative test", "2025-12-19", "Amount must be greater than 0"),
    ])
    def test_update_expense_invalid_amount(self, expense_service: ExpenseService,
                                         mock_expense_repository: ExpenseRepository,
                                         mock_approval_repository: ApprovalRepository,
                                         sample_expense: Expense, sample_approval: Approval,
                                         expense_id: int, user_id: int, invalid_amount: float,
                                         description: str, date: str, expected_exception: str):
        #********** Arrange **********#
        
        # Set up sample expense and approval with pending status
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Stub the dependencies to return the sample data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        
        
        #********** Act & Assert **********#
        
        # Verify that attempting to update raises the expected exception
        # Using pytest.raises context manager to check for exception
        # Code inside this block should raise the specified exception
        with pytest.raises(ValueError, match=expected_exception):
            expense_service.update_expense(expense_id, user_id, invalid_amount, description, date)
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # Since amount is invalid, update should not be called. The control flow never gets transferred there.
        mock_expense_repository.update.assert_not_called()
    
    @allure.title("Fail to update with empty description")
    @allure.description("Test that empty or whitespace-only descriptions are rejected")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize("expense_id,user_id,amount,invalid_description,date,expected_exception", [
        (1, 100, 50.0, "", "2025-12-20", "Description is required"),
        (2, 200, 25.0, "   ", "2025-12-21", "Description is required"),
        (3, 150, 75.0, "\t\n", "2025-12-19", "Description is required"),
    ])
    def test_update_expense_empty_description(self, expense_service: ExpenseService,
                                            mock_expense_repository: ExpenseRepository,
                                            mock_approval_repository: ApprovalRepository,
                                            sample_expense: Expense, sample_approval: Approval,
                                            expense_id: int, user_id: int, amount: float,
                                            invalid_description: str, date: str, expected_exception: str):
        #********** Arrange **********#
        
        # Set up sample expense and approval with pending status
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Stub the dependencies to return the sample data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        
        
        #********** Act & Assert **********#
        
        # Verify that attempting to update raises the expected exception
        # Using pytest.raises context manager to check for exception
        # Code inside this block should raise the specified exception
        with pytest.raises(ValueError, match=expected_exception):
            expense_service.update_expense(expense_id, user_id, amount, invalid_description, date)
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # Since description is invalid, update should not be called. The control flow never gets transferred there.
        mock_expense_repository.update.assert_not_called()
    
    
    # Edge Case Tests
    @allure.title("Handle missing approval record")
    @allure.description("Test behavior when expense exists but has no approval record")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("expense_id,user_id,amount,description,date", [
        (1, 100, 50.0, "Test description", "2025-12-20"),
        (2, 200, 25.0, "Another test", "2025-12-21"),
    ])
    def test_update_expense_missing_approval(self, expense_service: ExpenseService,
                                           mock_expense_repository: ExpenseRepository,
                                           mock_approval_repository: ApprovalRepository,
                                           sample_expense: Expense,
                                           expense_id: int, user_id: int, amount: float,
                                           description: str, date: str):
        #********** Arrange **********#
        
        # Set up sample expense
        # No approval record will be set up to simulate missing approval
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        
        # Stub the dependencies to return the sample expense but no approval
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = None
        
        
        #********** Act **********#
        
        result = expense_service.update_expense(expense_id, user_id, amount, description, date)
        
        
        #********** Assert **********#
        
        # Verify the result is None due to missing approval record
        assert result is None
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # Since approval record is missing, update should not be called. The control flow never gets transferred there.
        mock_expense_repository.update.assert_not_called()
    
    @allure.title("Handle repository update failure")
    @allure.description("Test behavior when repository update operation fails")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("expense_id,user_id,amount,description,date", [
        (1, 100, 50.0, "Test description", "2025-12-20"),
        (2, 200, 25.0, "Another test", "2025-12-21"),
    ])
    def test_update_expense_repository_failure(self, expense_service: ExpenseService,
                                             mock_expense_repository: ExpenseRepository,
                                             mock_approval_repository: ApprovalRepository,
                                             sample_expense: Expense, sample_approval: Approval,
                                             expense_id: int, user_id: int, amount: float,
                                             description: str, date: str):
        #********** Arrange **********#
        
        # Set up sample expense and approval with pending status
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Stub the dependencies to return the sample data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.update.side_effect = Exception("Database error")
        
        
        #********** Act & Assert **********#
        
        # Verify that attempting to update raises the expected exception
        # Using pytest.raises context manager to check for exception
        # Code inside this block should raise the specified exception
        with pytest.raises(Exception, match="Database error"):
            expense_service.update_expense(expense_id, user_id, amount, description, date)
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # The service layer then attempts to update the expense by calling this method. Still gets called despite failure.
        mock_expense_repository.update.assert_called_once()
    
    
    # Boundary Tests
    @allure.title("Test boundary values for expense and user IDs")
    @allure.description("Test update operation with boundary values for IDs")
    @allure.severity(allure.severity_level.MINOR)
    @pytest.mark.parametrize("expense_id,user_id,amount,description,date", [
        (1, 1, 50.0, "Min ID test", "2025-12-20"),
        (999999, 999999, 100.0, "Large ID test", "2025-12-21"),
        (2147483647, 2147483647, 75.0, "Max 32-bit int", "2025-12-22"),
    ])
    def test_update_expense_boundary_ids(self, expense_service: ExpenseService,
                                       mock_expense_repository: ExpenseRepository,
                                       mock_approval_repository: ApprovalRepository,
                                       sample_expense: Expense, sample_approval: Approval,
                                       expense_id: int, user_id: int, amount: float,
                                       description: str, date: str):
        #********** Arrange **********#
        
        # Set up sample expense and approval with pending status
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Prepare expected updated expense
        expected_expense = Expense(
            id=expense_id,
            user_id=user_id,
            amount=amount,
            description=description,
            date=date
        )
        
        # Stub the dependencies to return the sample/expected data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.update.return_value = expected_expense
        
        
        #********** Act **********#
        
        result = expense_service.update_expense(expense_id, user_id, amount, description, date)
        
        
        #********** Assert **********#
        
        # Verify the result matches expected updated expense
        assert result is not None
        assert result.id == expense_id
        assert result.user_id == user_id
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # The service layer then attempts to update the expense by calling this method.
        mock_expense_repository.update.assert_called_once()
    
    @allure.title("Test boundary values for amounts")
    @allure.description("Test update operation with boundary amount values")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("expense_id,user_id,amount,description,date", [
        (1, 100, 0.01, "Minimum amount test", "2025-12-20"),
        (2, 200, 999999.99, "Large amount test", "2025-12-21"),
        (3, 150, 1e6, "Very large amount", "2025-12-22"),
    ])
    def test_update_expense_boundary_amounts(self, expense_service: ExpenseService,
                                           mock_expense_repository: ExpenseRepository,
                                           mock_approval_repository: ApprovalRepository,
                                           sample_expense: Expense, sample_approval: Approval,
                                           expense_id: int, user_id: int, amount: float,
                                           description: str, date: str):
        #********** Arrange **********#
        
        # Set up sample expense and approval with pending status
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = "pending"
        
        # Prepare expected updated expense
        expected_expense = Expense(
            id=expense_id,
            user_id=user_id,
            amount=amount,
            description=description,
            date=date
        )
        
        # Stub the dependencies to return the sample/expected data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.update.return_value = expected_expense
        
        
        #********** Act **********#
        
        result = expense_service.update_expense(expense_id, user_id, amount, description, date)
        
        
        #********** Assert **********#
        
        # Verify the result matches expected updated expense
        assert result is not None
        assert result.amount == amount
        
        # Mocking: verify the behavior of the repositories (times called with correct params)
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)
        # The service layer then attempts to update the expense by calling this method.
        mock_expense_repository.update.assert_called_once()
    
    @allure.title("Test all approval status boundaries")
    @allure.description("Comprehensive test of approval status boundary conditions for updates")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.parametrize("status,should_allow_update,should_raise_exception", [
        ("pending", True, False),   # Only pending should allow update
        ("approved", False, True),  # Approved should raise exception
        ("denied", False, True),    # Denied should raise exception
    ])
    def test_update_expense_status_boundaries(self, expense_service: ExpenseService,
                                            mock_expense_repository: ExpenseRepository,
                                            mock_approval_repository: ApprovalRepository,
                                            sample_expense: Expense, sample_approval: Approval,
                                            status: str, should_allow_update: bool, 
                                            should_raise_exception: bool):
        #********** Arrange **********#
        
        # Set up sample expense and approval with varying status
        expense_id, user_id = 1, 100
        amount, description, date = 50.0, "Test description", "2025-12-20"
        sample_expense.id = expense_id
        sample_expense.user_id = user_id
        sample_approval.expense_id = expense_id
        sample_approval.status = status
        
        # Prepare expected updated expense
        expected_expense = Expense(
            id=expense_id,
            user_id=user_id,
            amount=amount,
            description=description,
            date=date
        )
        
        # Stub the dependencies to return the sample/expected data
        mock_expense_repository.find_by_id.return_value = sample_expense
        mock_approval_repository.find_by_expense_id.return_value = sample_approval
        mock_expense_repository.update.return_value = expected_expense
        
        
        #********** Act & Assert **********#
        
        # Verify behavior based on approval status, approved and denied should raise exceptions
        if should_raise_exception:
            # Verify that attempting to update raises the expected exception
            # Using pytest.raises context manager to check for exception
            # Code inside this block should raise the specified exception
            with pytest.raises(ValueError, match="Cannot edit expense that has been reviewed"):
                expense_service.update_expense(expense_id, user_id, amount, description, date)
            # Mocking: verify the behavior of the repositories (times called with correct params)
            # Ensure update was not called, service should have exited before that point
            mock_expense_repository.update.assert_not_called()
        # Otherwise, pending should allow update
        else:
            result = expense_service.update_expense(expense_id, user_id, amount, description, date)
            # Verify the result matches expected updated expense
            assert result is not None
            assert result.amount == amount
            # Mocking: verify the behavior of the repositories (times called with correct params)
            # The service layer attempts to update the expense by calling this method.
            mock_expense_repository.update.assert_called_once()
        
        # The service layer checks for expense existence first and calls this particular repository method.
        mock_expense_repository.find_by_id.assert_called_once_with(expense_id)
        # The service layer then checks the approval status by calling this method.
        mock_approval_repository.find_by_expense_id.assert_called_once_with(expense_id)