"""
Unit tests for AuthenticationService - Employee Login Feature.
Covers happy paths, sad paths, and basic edge cases.
"""
import pytest
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

@pytest.mark.parametrize("username,password,user_id,stored_username", [
    ('employee1', 'password123', 1, 'employee1'),
    ('john_doe', 'secure_pass', 2, 'john_doe'),
])
def test_authenticate_user_success(auth_service, mock_user_repository, username, password, user_id, stored_username):
    """Happy Path: User authenticates with valid credentials."""
    user = User(id=user_id, username=stored_username, password=password, role='Employee')
    mock_user_repository.find_by_username.return_value = user

    result = auth_service.authenticate_user(username, password)

    assert result is not None
    assert result.id == user_id


@pytest.mark.parametrize("username,stored_password,input_password", [
    ("employee1", "correct_pass", "wrong_pass"),
    ("nonexistent", None, "any_pass"),
])
def test_authenticate_user_fails(auth_service, mock_user_repository, username, stored_password, input_password):
    """Sad Path: Authentication fails with invalid credentials."""
    if stored_password is not None:
        user = User(id=1, username=username, password=stored_password, role='Employee')
        mock_user_repository.find_by_username.return_value = user
    else:
        mock_user_repository.find_by_username.return_value = None

    result = auth_service.authenticate_user(username, input_password)

    assert result is None


# ==================== GENERATE_JWT_TOKEN TESTS ====================

def test_generate_jwt_token_success(auth_service):
    """Happy Path: JWT token is generated successfully."""
    user = User(id=1, username='employee1', password='pass', role='Employee')

    token = auth_service.generate_jwt_token(user)

    assert token is not None
    assert isinstance(token, str)


def test_generated_token_contains_correct_payload(auth_service):
    """Happy Path: Token contains correct user information."""
    user = User(id=42, username='john', password='pass', role='Employee')

    token = auth_service.generate_jwt_token(user)
    decoded = jwt.decode(token, auth_service.jwt_secret_key, algorithms=['HS256'])

    assert decoded['user_id'] == 42
    assert decoded['username'] == 'john'
    assert decoded['role'] == 'Employee'


def test_token_has_expiry_time(auth_service):
    """Happy Path: Token has 24-hour expiry time."""
    user = User(id=1, username='employee1', password='pass', role='Employee')
    before = datetime.utcnow()

    token = auth_service.generate_jwt_token(user)
    decoded = jwt.decode(token, auth_service.jwt_secret_key, algorithms=['HS256'])

    exp_time = datetime.utcfromtimestamp(decoded['exp'])
    expected_exp = before + timedelta(hours=24)

    # Allow 5 second tolerance
    assert exp_time > expected_exp - timedelta(seconds=5)
    assert exp_time < expected_exp + timedelta(seconds=5)


def test_generate_token_with_none_user(auth_service):
    """Sad Path: Cannot generate token for None user."""
    with pytest.raises(AttributeError):
        auth_service.generate_jwt_token(None)


# ==================== VALIDATE_JWT_TOKEN TESTS ====================

def test_validate_token_success(auth_service):
    """Happy Path: Valid token is validated successfully."""
    user = User(id=1, username='employee1', password='pass', role='Employee')
    token = auth_service.generate_jwt_token(user)

    payload = auth_service.validate_jwt_token(token)

    assert payload is not None
    assert payload['user_id'] == 1


def test_validate_token_has_required_fields(auth_service):
    """Happy Path: Validated token has all required fields."""
    user = User(id=1, username='employee1', password='pass', role='Employee')
    token = auth_service.generate_jwt_token(user)

    payload = auth_service.validate_jwt_token(token)

    required_fields = ['user_id', 'username', 'role', 'exp', 'iat']
    for field in required_fields:
        assert field in payload


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
    """Sad Path: Validation fails for invalid/expired tokens."""
    if token_data is None:
        payload = auth_service.validate_jwt_token("invalid.token.format")
    else:
        token = jwt.encode(token_data, auth_service.jwt_secret_key, algorithm='HS256')
        payload = auth_service.validate_jwt_token(token)

    assert payload is None


def test_validate_token_wrong_secret(auth_service):
    """Sad Path: Validation fails with wrong secret key."""
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

    payload = auth_service.validate_jwt_token(token)

    assert payload is None


# ==================== GET_USER_BY_ID TESTS ====================

def test_get_user_by_id_success(auth_service, mock_user_repository):
    """Happy Path: User is retrieved by ID."""
    user = User(id=1, username='employee1', password='pass', role='Employee')
    mock_user_repository.find_by_id.return_value = user

    result = auth_service.get_user_by_id(1)

    assert result is not None
    assert result.id == 1


def test_get_user_by_id_not_found(auth_service, mock_user_repository):
    """Sad Path: User not found."""
    mock_user_repository.find_by_id.return_value = None

    result = auth_service.get_user_by_id(999)

    assert result is None


# ==================== GET_USER_FROM_TOKEN TESTS ====================

def test_get_user_from_token_success(auth_service, mock_user_repository):
    """Happy Path: User is retrieved from valid token."""
    user = User(id=1, username='employee1', password='pass', role='Employee')
    mock_user_repository.find_by_id.return_value = user
    token = auth_service.generate_jwt_token(user)

    result = auth_service.get_user_from_token(token)

    assert result is not None
    assert result.username == 'employee1'


def test_get_user_from_token_invalid_token(auth_service):
    """Sad Path: Cannot retrieve user from invalid token."""
    result = auth_service.get_user_from_token("invalid.token")

    assert result is None


# ==================== INTEGRATION TESTS ====================

def test_full_login_flow(auth_service, mock_user_repository):
    """Integration: Complete login workflow succeeds."""
    # Setup
    user = User(id=1, username='employee1', password='password123', role='Employee')
    mock_user_repository.find_by_username.return_value = user
    mock_user_repository.find_by_id.return_value = user

    # Step 1: Authenticate
    authenticated_user = auth_service.authenticate_user('employee1', 'password123')
    assert authenticated_user is not None

    # Step 2: Generate token
    token = auth_service.generate_jwt_token(authenticated_user)
    assert token is not None

    # Step 3: Validate token
    payload = auth_service.validate_jwt_token(token)
    assert payload is not None

    # Step 4: Get user from token
    user_from_token = auth_service.get_user_from_token(token)
    assert user_from_token.username == 'employee1'


def test_login_fails_with_wrong_password(auth_service, mock_user_repository):
    """Integration: Login fails with incorrect password."""
    user = User(id=1, username='employee1', password='correct_pass', role='Employee')
    mock_user_repository.find_by_username.return_value = user

    result = auth_service.authenticate_user('employee1', 'wrong_pass')

    assert result is None


def test_login_fails_with_nonexistent_user(auth_service, mock_user_repository):
    """Integration: Login fails when user doesn't exist."""
    mock_user_repository.find_by_username.return_value = None

    result = auth_service.authenticate_user('nonexistent', 'any_pass')

    assert result is None