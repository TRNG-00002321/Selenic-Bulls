"""
Authentication endpoints for login and logout.
"""
from flask import Blueprint, request, jsonify, make_response, current_app
from service.authentication_service import AuthenticationService


auth_bp = Blueprint('auth', __name__, url_prefix='/api/auth')


def get_auth_service() -> AuthenticationService:
    """Get authentication service from Flask app context."""
    return current_app.auth_service


@auth_bp.route('/login', methods=['POST'])
def login():
    """Employee login endpoint."""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': 'JSON data required'}), 400
        
        username = data.get('username')
        password = data.get('password')
        
        if not username or not password:
            return jsonify({'error': 'Username and password required'}), 400
        
        auth_service = get_auth_service()
        user = auth_service.authenticate_user(username, password)
        
        if not user:
            return jsonify({'error': 'Invalid credentials'}), 401
        
        # Generate JWT token
        token = auth_service.generate_jwt_token(user)
        
        # Create response with user data
        response_data = {
            'message': 'Login successful',
            'user': {
                'id': user.id,
                'username': user.username,
                'role': user.role
            }
        }
        response = make_response(jsonify(response_data))
        
        # Set JWT token as httpOnly cookie
        response.set_cookie(
            'jwt_token',
            token,
            httponly=True,
            secure=False,  # Set to True in production with HTTPS
            samesite='Lax',
            max_age=24*60*60  # 24 hours in seconds
        )
        
        return response
        
    except Exception as e:
        return jsonify({'error': 'Login failed', 'details': str(e)}), 500


@auth_bp.route('/logout', methods=['POST'])
def logout():
    """Employee logout endpoint."""
    response = make_response(jsonify({'message': 'Logout successful'}))
    
    # Clear the JWT token cookie
    response.set_cookie(
        'jwt_token',
        '',
        httponly=True,
        secure=False,  # Set to True in production with HTTPS
        samesite='Lax',
        expires=0  # Expire immediately
    )
    
    return response


@auth_bp.route('/status', methods=['GET'])
def status():
    """Check authentication status."""
    # Check for JWT token in cookies
    token = request.cookies.get('jwt_token')
    
    if not token:
        return jsonify({'authenticated': False}), 200
    
    try:
        auth_service = get_auth_service()
        user = auth_service.get_user_from_token(token)
        
        if user:
            return jsonify({
                'authenticated': True,
                'user': {
                    'id': user.id,
                    'username': user.username,
                    'role': user.role
                }
            })
        
    except Exception:
        pass
    
    return jsonify({'authenticated': False}), 200