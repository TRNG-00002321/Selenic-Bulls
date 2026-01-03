import pytest
import requests
import allure

BASE_URL = "http://127.0.0.1:5000/api/auth"


@pytest.fixture
def session():
    """Returns a requests session to handle cookies automatically."""
    return requests.Session()


# ==================== LOGIN TESTS ====================

@allure.severity(allure.severity_level.CRITICAL)
@allure.description("Happy Path: Verifies login succeeds with valid credentials.")
def test_login_success(session):

    with allure.step("Act: Send login request with valid credentials"):
        payload = {"username": "employee1", "password": "password123"}
        response = session.post(f"{BASE_URL}/login", json=payload)

    with allure.step("Assert: Login succeeds and JWT cookie is set"):
        assert response.status_code == 200
        assert 'jwt_token' in session.cookies
        assert response.json()['message'] == 'Login successful'


@allure.severity(allure.severity_level.NORMAL)
@allure.description("Sad Path: Verifies login fails with invalid password.")
def test_login_invalid_password(session):

    with allure.step("Act: Send login request with invalid password"):
        payload = {"username": "employee1", "password": "wrongpassword"}
        response = session.post(f"{BASE_URL}/login", json=payload)

    with allure.step("Assert: Unauthorized response is returned"):
        assert response.status_code == 401
        assert 'error' in response.json()


@allure.severity(allure.severity_level.NORMAL)
@allure.description("Sad Path: Verifies login fails when username is missing.")
def test_login_missing_username(session):

    with allure.step("Act: Send login request without username"):
        payload = {"password": "password123"}
        response = session.post(f"{BASE_URL}/login", json=payload)

    with allure.step("Assert: Bad request is returned"):
        assert response.status_code == 400


@allure.severity(allure.severity_level.NORMAL)
@allure.description("Sad Path: Verifies login fails when password is missing.")
def test_login_missing_password(session):

    with allure.step("Act: Send login request without password"):
        payload = {"username": "employee1"}
        response = session.post(f"{BASE_URL}/login", json=payload)

    with allure.step("Assert: Bad request is returned"):
        assert response.status_code == 400


@allure.severity(allure.severity_level.NORMAL)
@allure.description("Sad Path: Verifies login fails with empty request body.")
def test_login_empty_body(session):

    with allure.step("Act: Send login request with empty JSON body"):
        response = session.post(f"{BASE_URL}/login", json={})

    with allure.step("Assert: Bad request is returned"):
        assert response.status_code == 400


# ==================== LOGOUT TEST ====================

@allure.severity(allure.severity_level.CRITICAL)
@allure.description("Integration: Verifies logout clears authentication cookie.")
def test_logout_clears_cookie(session):

    with allure.step("Arrange: Login user"):
        session.post(
            f"{BASE_URL}/login",
            json={"username": "employee1", "password": "password123"}
        )

    with allure.step("Act: Logout user"):
        response = session.post(f"{BASE_URL}/logout")

    with allure.step("Assert: Logout succeeds"):
        assert response.status_code == 200

    with allure.step("Assert: Authentication status is false after logout"):
        status_resp = session.get(f"{BASE_URL}/status")
        assert status_resp.json()['authenticated'] is False


# ==================== STATUS CHECK TESTS ====================

@allure.severity(allure.severity_level.CRITICAL)
@allure.description("Happy Path: Verifies status endpoint returns authenticated for valid token.")
def test_status_check_authenticated(session):

    with allure.step("Arrange: Login user"):
        session.post(
            f"{BASE_URL}/login",
            json={"username": "employee1", "password": "password123"}
        )

    with allure.step("Act: Check authentication status"):
        response = session.get(f"{BASE_URL}/status")

    with allure.step("Assert: User is authenticated"):
        assert response.status_code == 200
        assert response.json()['authenticated'] is True


@allure.severity(allure.severity_level.NORMAL)
@allure.description("Sad Path: Verifies status endpoint returns unauthenticated without token.")
def test_status_check_unauthenticated():

    with allure.step("Act: Check status without authentication"):
        response = requests.get(f"{BASE_URL}/status")

    with allure.step("Assert: User is unauthenticated"):
        assert response.json()['authenticated'] is False


@allure.severity(allure.severity_level.NORMAL)
@allure.description("Sad Path: Verifies status endpoint returns unauthenticated for invalid token.")
def test_status_check_invalid_token(session):

    with allure.step("Arrange: Set invalid JWT token cookie"):
        session.cookies.set('jwt_token', 'invalid.garbage.token')

    with allure.step("Act: Check authentication status"):
        response = session.get(f"{BASE_URL}/status")

    with allure.step("Assert: User is unauthenticated"):
        assert response.json()['authenticated'] is False
