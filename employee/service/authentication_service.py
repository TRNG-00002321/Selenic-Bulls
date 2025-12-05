"""
Service for user authentication and authorization.
"""
import jwt
from datetime import datetime, timedelta
from typing import Optional, Dict, Any
from repository.user_model import User
from repository.user_repository import UserRepository


class AuthenticationService:
    """Service for user authentication and authorization."""
    
    def __init__(self, user_repository: UserRepository, jwt_secret_key: str = 'your-secret-key'):
        self.user_repository = user_repository
        self.jwt_secret_key = jwt_secret_key
        self.jwt_algorithm = 'HS256'
        self.token_expiry_hours = 24
    
    def authenticate_user(self, username: str, password: str) -> Optional[User]:
        """Authenticate a user with username and password."""
        user = self.user_repository.find_by_username(username)
        if user and user.password == password:
            return user
        return None
    
    def get_user_by_id(self, user_id: int) -> Optional[User]:
        """Get user by ID."""
        return self.user_repository.find_by_id(user_id)
    
    def generate_jwt_token(self, user: User) -> str:
        """Generate a JWT token for the user."""
        payload = {
            'user_id': user.id,
            'username': user.username,
            'role': user.role,
            'exp': datetime.utcnow() + timedelta(hours=self.token_expiry_hours),
            'iat': datetime.utcnow()
        }
        return jwt.encode(payload, self.jwt_secret_key, algorithm=self.jwt_algorithm)
    
    def validate_jwt_token(self, token: str) -> Optional[Dict[str, Any]]:
        """Validate a JWT token and return the payload if valid."""
        try:
            payload = jwt.decode(token, self.jwt_secret_key, algorithms=[self.jwt_algorithm])
            return payload
        except jwt.ExpiredSignatureError:
            return None
        except jwt.InvalidTokenError:
            return None
    
    def get_user_from_token(self, token: str) -> Optional[User]:
        """Get user from JWT token."""
        payload = self.validate_jwt_token(token)
        if payload:
            return self.get_user_by_id(payload['user_id'])
        return None