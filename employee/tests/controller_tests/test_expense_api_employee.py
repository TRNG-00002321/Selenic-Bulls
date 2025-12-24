import pytest
from flask import Flask
from service.expense_service import ExpenseService
from unittest.mock import Mock, patch

@pytest.fixture
def app():
    # 1. Patch the auth modules BEFORE importing the blueprint
    with patch('api.auth.require_employee_auth', side_effect=lambda x: x), \
         patch('api.auth.get_current_user') as mock_user, \
         patch('api.expense_controller.require_employee_auth', side_effect=lambda x: x), \
         patch('api.expense_controller.get_current_user') as mock_user_local:
        
        # 2. Import the blueprint INSIDE the patch context
        from api.expense_controller import expense_bp
        
        mock_user.return_value = Mock(id=1)
        mock_user_local.return_value = Mock(id=1)
        
        app = Flask(__name__)
        app.expense_service = Mock(spec=ExpenseService)
        app.register_blueprint(expense_bp)
        app.testing = True
        yield app


@pytest.fixture
def client(app):
    return app.test_client()






