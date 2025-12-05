# Employee Expense Management API

A Flask-based REST API for employee expense management, allowing employees to submit, view, and manage their expense reports.

## Features

- **Employee Authentication**: Simple cookie-based authentication
- **Expense Submission**: Submit new expenses with amount, description, and date
- **Expense Management**: View, edit, and delete pending expenses
- **Expense History**: View all expenses with their approval status
- **Status Filtering**: Filter expenses by status (pending, approved, denied)

## Project Structure

```
employee/
├── main.py                          # Main Flask application
├── requirements.txt                 # Python dependencies
├── api/                            # REST API controllers
│   ├── __init__.py                 # API package exports
│   ├── auth.py                     # Authentication utilities
│   ├── auth_controller.py          # Login/logout endpoints
│   └── expense_controller.py       # Expense management endpoints
├── service/                        # Business logic layer
│   ├── __init__.py                 # Service package exports
│   ├── authentication_service.py   # User authentication logic
│   └── expense_service.py          # Expense business operations
└── repository/                     # Data access layer
    ├── __init__.py                 # Repository package exports
    ├── database.py                 # Database connection and setup
    ├── user_model.py               # User data model
    ├── expense_model.py            # Expense data model
    ├── approval_model.py           # Approval data model
    ├── user_repository.py          # User database operations
    ├── expense_repository.py       # Expense database operations
    └── approval_repository.py      # Approval database operations
```

## Installation and Setup

1. **Activate the virtual environment** (already set up):
   ```bash
   source venv/bin/activate  # On Linux/Mac
   ```

2. **Install dependencies** (Flask is already installed):
   ```bash
   pip install -r requirements.txt
   ```

3. **Set database location** (optional):
   ```bash
   export DATABASE_PATH=/path/to/your/database.db
   ```
   If not set, defaults to `expense_manager.db` in the project root.

4. **Run the application**:
   ```bash
   python main.py
   ```

   The API will start on `http://localhost:5000`

## Database Schema

The application uses SQLite with three tables:

- **users**: User accounts (id, username, password, role)
- **expenses**: Expense records (id, user_id, amount, description, date)
- **approvals**: Expense approval status (id, expense_id, status, reviewer, comment, review_date)

## API Endpoints

### Authentication

- **POST** `/api/auth/login` - Employee login
  ```json
  {
    "username": "employee1",
    "password": "password123"
  }
  ```

- **POST** `/api/auth/logout` - Logout
- **GET** `/api/auth/status` - Check authentication status

### Expense Management

- **POST** `/api/expenses` - Submit new expense
  ```json
  {
    "amount": 25.50,
    "description": "Client lunch meeting",
    "date": "2025-10-14"  // Optional, defaults to current date
  }
  ```

- **GET** `/api/expenses` - Get all user expenses
  - Query parameter: `?status=pending|approved|denied` (optional filter)

- **GET** `/api/expenses/<id>` - Get specific expense
- **PUT** `/api/expenses/<id>` - Update expense (only if pending)
- **DELETE** `/api/expenses/<id>` - Delete expense (only if pending)

### Utility

- **GET** `/health` - Health check
- **GET** `/api` - API information

## Sample Data

The application creates sample users on first run:

- **Employee**: `employee1` / `password123`
- **Manager**: `manager1` / `password123` (for future manager app)

## User Stories Implemented

✅ **Employee Login**: Secure authentication with cookies  
✅ **Submit Expenses**: Create new expense reports with amount, description, and date  
✅ **View Status**: See all expenses with their approval status (pending/approved/denied)  
✅ **Edit Pending Expenses**: Modify expenses that haven't been reviewed yet  
✅ **Delete Pending Expenses**: Remove expenses that haven't been reviewed yet  
✅ **View History**: Access complete expense history with filtering options  

## Architecture

The application follows a layered architecture with dependency injection:

- **API Layer**: Flask controllers handling HTTP requests/responses
- **Service Layer**: Business logic and validation
- **Repository Layer**: Database operations and data access
- **Models**: Data classes and database schema

## Environment Variables

- `DATABASE_PATH`: SQLite database file location (optional)

## Development Notes

- Passwords are stored in plain text (should be hashed in production)
- Simple cookie-based authentication (should use JWT or sessions in production)
- No input sanitization beyond basic validation (should be enhanced for production)
- Error handling provides detailed messages (should be sanitized in production)

## Testing the API

You can test the API using curl, Postman, or any HTTP client:

```bash
# Login
curl -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "employee1", "password": "password123"}' \
  -c cookies.txt

# Submit expense
curl -X POST http://localhost:5000/api/expenses \
  -H "Content-Type: application/json" \
  -d '{"amount": 25.50, "description": "Client lunch"}' \
  -b cookies.txt

# Get expenses
curl -X GET http://localhost:5000/api/expenses -b cookies.txt
```