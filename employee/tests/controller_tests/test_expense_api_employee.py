import pytest
from flask import Flask
from api.expense_controller import expense_bp
from service.expense_service import ExpenseService
from unittest.mock import Mock, patch


@pytest.fixture
def app():
    app = Flask(__name__)
    app.expense_service = Mock(spec=ExpenseService)
    app.register_blueprint(expense_bp)
    app.testing = True
    return app


@pytest.fixture
def client(app):
    return app.test_client()


def test_get_expenses_empty(client, app):
    app.expense_service.get_expense_history.return_value = []
    response = client.get("/api/expenses")
    assert response.status_code == 200
    assert response.json["count"] == 0


def test_submit_expense_invalid_amount(client):
    response = client.post("/api/expenses", json={"amount": "abc", "description": "Test"})
    assert response.status_code == 400
    assert "error" in response.json


def test_submit_expense_missing_json(client):
    response = client.post("/api/expenses")
    assert response.status_code == 400


def test_get_expense_not_found(client, app):
    app.expense_service.get_expense_with_status.return_value = None
    response = client.get("/api/expenses/999")
    assert response.status_code == 404
    assert "error" in response.json
