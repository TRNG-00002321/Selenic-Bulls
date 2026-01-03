"""
Unit tests for AuthenticationService - Employee Login Feature.
Covers happy paths, sad paths, and basic edge cases.
"""
import pytest
import allure
from unittest.mock import Mock
from datetime import datetime, timedelta
import jwt

from service.authentication_service import AuthenticationService
from repository.user_model import User


# ==================== FIXTURES ====================

@pytest.fixture
def mock_user_repository():
    """Fixture for mocked user repository."""
    return Mock()


@pytest.fixture
def auth_service(mock_user_repository):
    """Fixture for AuthenticationService with mocked dependencies."""
    return AuthenticationService(mock_user_repository, 'test-secret-key')


# ==================== AUTHENTICATE_USER TESTS ====================

@allure.severity(allure.severity_level.CRITICAL)
@allure.description("[Happy Path] Verifies user can authenticate with valid credentials.")
@pytest.mark.parametrize("username,password,user_id,stored_username", [
    ('employee1', 'password123', 1, 'employee1'),
    ('john_doe', 'secure_pass', 2, 'john_doe'),
])
def test_authenticate_user_success(auth_service, mock_user_repository,
                                   username, password, user_id, stored_username):

    with allure.step(f"Arrange: Mock user repository for username = {username}"):
        user = User(id=user_id, username=stored_username, password=password, role='Employee')
        mock_user_repository.find_by_username.return_value = user

    with allure.step("Act: Authenticate user"):
        result = auth_service.authenticate_user(username, password)

    with allure.step("Assert: Authentication succeeds"):
        assert result is not None
        assert result.id == user_id


@allure.severity(allure.severity_level.NORMAL)
@allure.description("[Sad Path] Verifies authentication fails with invalid credentials.")
@pytest.mark.parametrize("username,stored_password,input_password", [
    ("employee1", "correct_pass", "wrong_pass"),
    ("nonexistent", None, "any_pass"),
])
def test_authenticate_user_fails(auth_service, mock_user_repository,
                                 username, stored_password, input_password):

    with allure.step("Arrange: Setup invalid user scenario"):
        if stored_password:
            user = User(id=1, username=username, password=stored_password, role='Employee')
            mock_user_repository.find_by_username.return_value = user
        else:
            mock_user_repository.find_by_username.return_value = None

    with allure.step("Act: Attempt authentication"):
        result = auth_service.authenticate_user(username, input_password)

    with allure.step("Assert: Authentication fails"):
        assert result is None


# ==================== GENERATE_JWT_TOKEN TESTS ====================

@allure.severity(allure.severity_level.CRITICAL)
@allure.description("[Happy Path] Verifies JWT token is generated successfully.")
def test_generate_jwt_token_success(auth_service):

    with allure.step("Arrange: Create user"):
        user = User(id=1, username='employee1', password='pass', role='Employee')

    with allure.step("Act: Generate JWT token"):
        token = auth_service.generate_jwt_token(user)

    with allure.step("Assert: Token is generated"):
        assert token is not None
        assert isinstance(token, str)


@allure.severity(allure.severity_level.CRITICAL)
@allure.description("[Happy Path] Verifies JWT token contains correct payload fields.")
def test_generated_token_contains_correct_payload(auth_service):

    with allure.step("Arrange: Create user"):
        user = User(id=42, username='john', password='pass', role='Employee')

    with allure.step("Act: Generate and decode token"):
        token = auth_service.generate_jwt_token(user)
        decoded = jwt.decode(token, auth_service.jwt_secret_key, algorithms=['HS256'])

    with allure.step("Assert: Payload contains expected values"):
        assert decoded['user_id'] == 42
        assert decoded['username'] == 'john'
        assert decoded['role'] == 'Employee'


@allure.severity(allure.severity_level.CRITICAL)
@allure.description("[Happy Path] Verifies JWT token expires after 24 hours.")
def test_token_has_expiry_time(auth_service):

    with allure.step("Arrange: Create user and capture current time"):
        user = User(id=1, username='employee1', password='pass', role='Employee')
        before = datetime.utcnow()

    with allure.step("Act: Generate and decode JWT token"):
        token = auth_service.generate_jwt_token(user)
        decoded = jwt.decode(token, auth_service.jwt_secret_key, algorithms=['HS256'])

    with allure.step("Assert: Expiration time is within expected range"):
        exp_time = datetime.utcfromtimestamp(decoded['exp'])
        expected_exp = before + timedelta(hours=24)

        assert exp_time > expected_exp - timedelta(seconds=5)
        assert exp_time < expected_exp + timedelta(seconds=5)



@allure.severity(allure.severity_level.NORMAL)
@allure.description("[Edge Case] Verifies token generation fails when user is None.")
def test_generate_token_with_none_user(auth_service):

    with allure.step("Act & Assert: Generating token with None user raises error"):
        with pytest.raises(AttributeError):
            auth_service.generate_jwt_token(None)

# ==================== VALIDATE_JWT_TOKEN TESTS ====================

@allure.severity(allure.severity_level.CRITICAL)
@allure.description("[Happy Path] Verifies valid JWT token is validated successfully.")
def test_validate_token_success(auth_service):

    with allure.step("Arrange: Create user and generate token"):
        user = User(id=1, username='employee1', password='pass', role='Employee')
        token = auth_service.generate_jwt_token(user)

    with allure.step("Act: Validate token"):
        payload = auth_service.validate_jwt_token(token)

    with allure.step("Assert: Token payload is valid"):
        assert payload is not None
        assert payload['user_id'] == 1



@allure.severity(allure.severity_level.CRITICAL)
@allure.description("[Happy Path] Verifies validated JWT token contains all required fields.")
def test_validate_token_has_required_fields(auth_service):

    with allure.step("Arrange: Generate valid token"):
        user = User(id=1, username='employee1', password='pass', role='Employee')
        token = auth_service.generate_jwt_token(user)

    with allure.step("Act: Validate token"):
        payload = auth_service.validate_jwt_token(token)

    with allure.step("Assert: Required fields are present"):
        for field in ['user_id', 'username', 'role', 'exp', 'iat']:
            assert field in payload


@allure.severity(allure.severity_level.NORMAL)
@allure.description("[Sad Path] Verifies invalid or expired tokens are rejected.")
@pytest.mark.parametrize("token_data", [
    {
        'user_id': 1,
        'username': 'employee1',
        'role': 'Employee',
        'exp': datetime.utcnow() - timedelta(hours=1),
        'iat': datetime.utcnow() - timedelta(hours=25)
    },
    None,
])


def test_validate_token_fails(auth_service, token_data):

    with allure.step("Arrange: Create invalid token"):
        if token_data is None:
            token = "invalid.token.format"
        else:
            token = jwt.encode(token_data, auth_service.jwt_secret_key, algorithm='HS256')

    with allure.step("Act: Validate token"):
        payload = auth_service.validate_jwt_token(token)

    with allure.step("Assert: Validation fails"):
        assert payload is None


@allure.severity(allure.severity_level.NORMAL)
@allure.description("[Sad Path] Verifies token validation fails when secret key is incorrect.")
def test_validate_token_wrong_secret(auth_service):

    with allure.step("Arrange: Create token using wrong secret"):
        token = jwt.encode(
            {
                'user_id': 1,
                'username': 'employee1',
                'role': 'Employee',
                'exp': datetime.utcnow() + timedelta(hours=24),
                'iat': datetime.utcnow()
            },
            'different-secret',
            algorithm='HS256'
        )

    with allure.step("Act: Validate token"):
        payload = auth_service.validate_jwt_token(token)

    with allure.step("Assert: Validation fails"):
        assert payload is None


# ==================== GET_USER_BY_ID TESTS ====================

@allure.severity(allure.severity_level.NORMAL)
@allure.description("[Happy Path] Verifies user is retrieved successfully by ID.")
def test_get_user_by_id_success(auth_service, mock_user_repository):

    with allure.step("Arrange: Mock repository to return user"):
        user = User(id=1, username='employee1', password='pass', role='Employee')
        mock_user_repository.find_by_id.return_value = user

    with allure.step("Act: Retrieve user by ID"):
        result = auth_service.get_user_by_id(1)

    with allure.step("Assert: Correct user is returned"):
        assert result is not None
        assert result.id == 1


@allure.severity(allure.severity_level.NORMAL)
@allure.description("[Sad Path] Verifies None is returned when user is not found.")
def test_get_user_by_id_not_found(auth_service, mock_user_repository):

    with allure.step("Arrange: Mock repository to return None"):
        mock_user_repository.find_by_id.return_value = None

    with allure.step("Act: Retrieve non-existent user"):
        result = auth_service.get_user_by_id(999)

    with allure.step("Assert: Result is None"):
        assert result is None


# ==================== GET_USER_FROM_TOKEN TESTS ====================

@allure.severity(allure.severity_level.CRITICAL)
@allure.description("[Happy Path] Verifies user can be retrieved from a valid JWT token.")
def test_get_user_from_token_success(auth_service, mock_user_repository):

    with allure.step("Arrange: Mock user and generate token"):
        user = User(id=1, username='employee1', password='pass', role='Employee')
        mock_user_repository.find_by_id.return_value = user
        token = auth_service.generate_jwt_token(user)

    with allure.step("Act: Retrieve user from token"):
        result = auth_service.get_user_from_token(token)

    with allure.step("Assert: Correct user is returned"):
        assert result is not None
        assert result.username == 'employee1'


@allure.severity(allure.severity_level.NORMAL)
@allure.description("[Sad Path] Verifies user retrieval fails with invalid token.")
def test_get_user_from_token_invalid_token(auth_service):

    with allure.step("Act: Retrieve user with invalid token"):
        result = auth_service.get_user_from_token("invalid.token")

    with allure.step("Assert: Result is None"):
        assert result is None


# ==================== INTEGRATION TESTS ====================

@allure.severity(allure.severity_level.CRITICAL)
@allure.description("[Happy Path] Integration test: Full login flow succeeds.")
def test_full_login_flow(auth_service, mock_user_repository):

    with allure.step("Arrange: Mock user repository"):
        user = User(id=1, username='employee1', password='password123', role='Employee')
        mock_user_repository.find_by_username.return_value = user
        mock_user_repository.find_by_id.return_value = user

    with allure.step("Act: Authenticate user"):
        authenticated_user = auth_service.authenticate_user('employee1', 'password123')
        assert authenticated_user is not None

    with allure.step("Act: Generate JWT token"):
        token = auth_service.generate_jwt_token(authenticated_user)
        assert token is not None

    with allure.step("Act: Validate token"):
        payload = auth_service.validate_jwt_token(token)
        assert payload is not None

    with allure.step("Assert: Retrieve user from token"):
        user_from_token = auth_service.get_user_from_token(token)
        assert user_from_token.username == 'employee1'


@allure.severity(allure.severity_level.NORMAL)
@allure.description("[Sad Path] Integration test: Login fails with incorrect password.")
def test_login_fails_with_wrong_password(auth_service, mock_user_repository):

    with allure.step("Arrange: Mock user with correct password"):
        user = User(id=1, username='employee1', password='correct_pass', role='Employee')
        mock_user_repository.find_by_username.return_value = user

    with allure.step("Act: Attempt login with wrong password"):
        result = auth_service.authenticate_user('employee1', 'wrong_pass')

    with allure.step("Assert: Login fails"):
        assert result is None


@allure.severity(allure.severity_level.NORMAL)
@allure.description("[Sad Path] Integration test: Login fails when user does not exist.")
def test_login_fails_with_nonexistent_user(auth_service, mock_user_repository):

    with allure.step("Arrange: Mock repository to return None"):
        mock_user_repository.find_by_username.return_value = None

    with allure.step("Act: Attempt login with nonexistent user"):
        result = auth_service.authenticate_user('nonexistent', 'any_pass')

    with allure.step("Assert: Login fails"):
        assert result is None
