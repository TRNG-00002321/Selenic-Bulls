"""
Repository for user-related database operations.
"""
from typing import Optional
from .user_model import User
from .database import DatabaseConnection


class UserRepository:
    """Repository for user-related database operations."""
    
    def __init__(self, db_connection: DatabaseConnection):
        self.db_connection = db_connection
    
    def find_by_username(self, username: str) -> Optional[User]:
        """Find a user by username."""
        with self.db_connection.get_connection() as conn:
            cursor = conn.execute(
                "SELECT id, username, password, role FROM users WHERE username = ?",
                (username,)
            )
            row = cursor.fetchone()
            if row:
                return User(id=row['id'], username=row['username'], 
                          password=row['password'], role=row['role'])
        return None
    
    def find_by_id(self, user_id: int) -> Optional[User]:
        """Find a user by ID."""
        with self.db_connection.get_connection() as conn:
            cursor = conn.execute(
                "SELECT id, username, password, role FROM users WHERE id = ?",
                (user_id,)
            )
            row = cursor.fetchone()
            if row:
                return User(id=row['id'], username=row['username'], 
                          password=row['password'], role=row['role'])
        return None
    
    def create(self, user: User) -> User:
        """Create a new user."""
        with self.db_connection.get_connection() as conn:
            cursor = conn.execute(
                "INSERT INTO users (username, password, role) VALUES (?, ?, ?)",
                (user.username, user.password, user.role)
            )
            user.id = cursor.lastrowid
            conn.commit()
        return user