"""
Main Flask application with dependency injection setup.
"""
import os
from flask import Flask
from repository import (
    DatabaseConnection, 
    UserRepository, 
    ExpenseRepository, 
    ApprovalRepository
)
from service import AuthenticationService, ExpenseService
from api import auth_bp, expense_bp


def create_app():
    """Create and configure the Flask application."""
    app = Flask(__name__, static_folder='static', static_url_path='/static')
    
    # Configure Flask
    app.config['SECRET_KEY'] = 'your-secret-key-change-this-in-production'
    app.config['JSON_SORT_KEYS'] = False
    
    # Initialize database connection
    db_connection = DatabaseConnection()
    db_connection.initialize_database()
    
    # Initialize repositories
    user_repository = UserRepository(db_connection)
    expense_repository = ExpenseRepository(db_connection)
    approval_repository = ApprovalRepository(db_connection)
    
    # Initialize services
    jwt_secret_key = app.config['SECRET_KEY']  # Use Flask's secret key for JWT
    auth_service = AuthenticationService(user_repository, jwt_secret_key)
    expense_service = ExpenseService(expense_repository, approval_repository)
    
    # Inject services into Flask app context
    app.auth_service = auth_service
    app.expense_service = expense_service
    
    # Register blueprints
    app.register_blueprint(auth_bp)
    app.register_blueprint(expense_bp)
    
    # Add basic health check endpoint
    @app.route('/health')
    def health_check():
        return {'status': 'healthy', 'message': 'Employee Expense Management API is running'}
    
    # Add basic API info endpoint
    @app.route('/api')
    def api_info():
        return {
            'service': 'Employee Expense Management API',
            'version': '1.0.0',
            'endpoints': {
                'authentication': '/api/auth',
                'expenses': '/api/expenses',
                'health': '/health'
            }
        }
    
    # Serve the login page
    @app.route('/')
    @app.route('/login')
    def login_page():
        return app.send_static_file('login.html')
    
    # Serve the main expense management app
    @app.route('/app')
    def expense_app():
        return app.send_static_file('employee.html')
    
    return app


def create_sample_data():
    """Create sample users for testing (call this manually if needed)."""
    from repository import DatabaseConnection, User, UserRepository
    
    db_connection = DatabaseConnection()
    db_connection.initialize_database()
    
    user_repo = UserRepository(db_connection)
    
    # Create a sample employee user if it doesn't exist
    if not user_repo.find_by_username('employee1'):
        sample_employee = User(
            id=None,
            username='employee1',
            password='password123',  # In production, this should be hashed
            role='Employee'
        )
        user_repo.create(sample_employee)
        print("Created sample employee: employee1/password123")
    



if __name__ == '__main__':
    app = create_app()
    
    # Create sample data for testing
    create_sample_data()
    
    print("Starting Employee Expense Management API...")
    print("Available endpoints:")
    print("  POST /api/auth/login - Employee login")
    print("  POST /api/auth/logout - Employee logout")
    print("  GET  /api/auth/status - Check auth status")
    print("  POST /api/expenses - Submit new expense")
    print("  GET  /api/expenses - Get all user expenses")
    print("  GET  /api/expenses/<id> - Get specific expense")
    print("  PUT  /api/expenses/<id> - Update expense (if pending)")
    print("  DELETE /api/expenses/<id> - Delete expense (if pending)")
    print("  GET  /health - Health check")
    print("  GET  /api - API info")
    print()
    print("Sample credentials:")
    print("  Employee: employee1/password123")
    
    app.run(host='0.0.0.0', port=5000)
