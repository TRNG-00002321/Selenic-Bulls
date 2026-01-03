import sqlite3
from unittest.mock import Mock
import pytest
import allure

from repository.user_repository import UserRepository
from repository.user_model import User
from repository.database import DatabaseConnection


# ==================== FIXTURES ====================

@pytest.fixture
def user_repo():
    """Fixture to set up an in-memory database for UserRepository tests."""
    with allure.step("Arrange: Setup in-memory SQLite database"):
        db = DatabaseConnection(":memory:")
        conn = db.get_connection()
        conn.row_factory = sqlite3.Row

        conn.execute("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE,
                password TEXT,
                role TEXT
            )
        """)

        conn.execute(
            "INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, ?)",
            (1, "employee1", "password123", "Employee")
        )
        conn.commit()

        db.get_connection = Mock(return_value=conn)

    return UserRepository(db)


# ==================== FIND BY USERNAME ====================

@allure.severity(allure.severity_level.NORMAL)
@allure.description("Happy & Sad Path: Verifies find_by_username with multiple inputs.")
@pytest.mark.parametrize("username,should_exist", [
    ("employee1", True),     # existing user
    ("unknown", False),      # non-existing user
    ("EMPLOYEE1", False),    # case sensitivity check
])
def test_find_by_username_parameterized(user_repo, username, should_exist):

    with allure.step(f"Act: Find user by username = {username}"):
        user = user_repo.find_by_username(username)

    with allure.step("Assert: Verify expected result"):
        if should_exist:
            assert user is not None
            assert user.username == username
        else:
            assert user is None



# ==================== FIND BY ID ====================

@allure.severity(allure.severity_level.NORMAL)
@allure.description("Happy & Sad Path: Verifies find_by_id with multiple IDs.")
@pytest.mark.parametrize("user_id,should_exist", [
    (1, True),       # existing
    (999, False),    # non-existing
    (0, False),      # boundary
])
def test_find_by_id_parameterized(user_repo, user_id, should_exist):

    with allure.step(f"Act: Find user by ID = {user_id}"):
        user = user_repo.find_by_id(user_id)

    with allure.step("Assert: Verify expected result"):
        if should_exist:
            assert user is not None
            assert user.id == user_id
        else:
            assert user is None

# ==================== CREATE USER ====================

@allure.severity(allure.severity_level.CRITICAL)
@allure.description("Happy Path: Verifies creating multiple valid users.")
@pytest.mark.parametrize("username,password,role", [
    ("alice", "pass1", "Employee"),
    ("bob", "pass2", "Employee"),
])
def test_create_multiple_users(user_repo, username, password, role):

    with allure.step(f"Arrange: Create user {username}"):
        new_user = User(id=None, username=username, password=password, role=role)

    with allure.step("Act: Persist user"):
        saved_user = user_repo.create(new_user)

    with allure.step("Assert: User is saved"):
        assert saved_user.id is not None
        assert saved_user.username == username
