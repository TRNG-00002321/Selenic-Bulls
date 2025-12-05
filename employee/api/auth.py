"""
Authentication utilities and decorators for Flask API.
"""
from functools import wraps
from flask import request, jsonify, current_app
from service.authentication_service import AuthenticationService


def get_auth_service() -> AuthenticationService:
    """Get authentication service from Flask app context."""
    return current_app.auth_service


def require_employee_auth(f):
    """Decorator to require employee authentication via JWT token in cookies."""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        # Check for JWT token in cookies
        token = request.cookies.get('jwt_token')
        
        if not token:
            return jsonify({'error': 'Authentication required'}), 401
        
        # Verify token and get user
        auth_service = get_auth_service()
        user = auth_service.get_user_from_token(token)
        
        if not user or user.role != 'Employee':
            return jsonify({'error': 'Access denied'}), 403
        
        # Add user to request context
        request.current_user = user
        return f(*args, **kwargs)
    
    return decorated_function


def get_current_user():
    """Get the current authenticated user from request context."""
    return getattr(request, 'current_user', None)