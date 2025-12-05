# Manager Expense Management API

A Javalin-based REST API for manager operations in the expense management system, allowing managers to review, approve, deny, and report on employee expenses.

## Features

- **Manager Authentication**: Secure login and session management for managers
- **Expense Review**: View all pending expenses submitted by employees
- **Expense Approval/Denial**: Approve or deny expenses with optional comments
- **Expense Reporting**: Generate CSV reports for all expenses or by employee
- **Expense History**: View all expenses with filtering options

## Project Structure

```
manager/
├── pom.xml                          # Maven project configuration
├── src/
│   └── main/
│       ├── java/
│       │   └── com/revature/
│       │       ├── Main.java                       # Main application entry point
│       │       ├── api/
│       │       │   ├── AuthenticationMiddleware.java   # Auth middleware for manager endpoints
│       │       │   ├── ExpenseController.java         # REST endpoints for expense review
│       │       │   └── ReportController.java          # REST endpoints for reporting
│       │       ├── repository/
│       │       │   ├── DatabaseConnection.java        # Database connection and setup
│       │       │   ├── User.java                      # User data model
│       │       │   ├── Expense.java                   # Expense data model
│       │       │   ├── Approval.java                  # Approval data model
│       │       │   ├── ExpenseWithUser.java           # Expense + user info for reporting
│       │       │   ├── UserRepository.java            # User DB operations
│       │       │   ├── ExpenseRepository.java         # Expense DB operations
│       │       │   └── ApprovalRepository.java        # Approval DB operations
│       │       └── service/
│       │           ├── AuthenticationService.java     # Manager authentication logic
│       │           └── ExpenseService.java            # Expense business operations
│       └── resources/
│           ├── login.html                        # Manager login page
│           ├── manager.html                      # Manager dashboard UI
│           ├── manager.js                        # Dashboard JavaScript
│           └── auth.js                           # Login/auth JavaScript
```

## Installation and Setup

1. **Build the project** (requires Java 17+ and Maven):
   ```bash
   mvn clean package
   ```

2. **Run the application**:
   ```bash
   mvn exec:java -Dexec.mainClass="com.revature.Main"
   ```
   The API will start on `http://localhost:5001`

## Database Schema

The application uses SQLite with three tables:

- **users**: User accounts (id, username, password, role)
- **expenses**: Expense records (id, user_id, amount, description, date)
- **approvals**: Expense approval status (id, expense_id, status, reviewer, comment, review_date)

## API Endpoints

### Authentication

- **POST** `/api/auth/login` - Manager login
- **POST** `/api/auth/logout` - Logout
- **GET** `/api/auth/status` - Check authentication status

### Expense Management

- **GET** `/api/expenses/pending` - Get all pending expenses for review
- **POST** `/api/expenses/{expenseId}/approve` - Approve an expense (with optional comment)
- **POST** `/api/expenses/{expenseId}/deny` - Deny an expense (with optional comment)
- **GET** `/api/expenses` - Get all expenses (with optional filters)
- **GET** `/api/expenses/{expenseId}` - Get details for a specific expense

### Reporting

- **GET** `/api/reports/expenses/csv` - Download CSV report of all expenses
- **GET** `/api/reports/expenses/employee/{employeeId}/csv` - Download CSV report for a specific employee

### Utility

- **GET** `/health` - Health check
- **GET** `/api` - API information

## Sample Data

The application expects users and expenses to be present in the database. Sample manager credentials (if seeded):

- **Manager**: `manager1` / `password123`

## User Stories Implemented

✅ **Manager Login**: Secure authentication for managers  
✅ **Review Expenses**: View all pending expenses submitted by employees  
✅ **Approve/Deny Expenses**: Approve or deny expenses with comments  
✅ **View All Expenses**: Access all expenses with filtering options  
✅ **Generate Reports**: Download CSV reports for all or specific employees  

## Architecture

The application follows a layered architecture with dependency injection:

- **API Layer**: Javalin controllers handling HTTP requests/responses
- **Service Layer**: Business logic and validation
- **Repository Layer**: Database operations and data access
- **Models**: Data classes and database schema

## Environment Variables

- `DATABASE_PATH`: SQLite database file location (optional, defaults to `expense_manager.db`)

## Development Notes

- Passwords are stored in plain text (should be hashed in production)
- Simple cookie-based authentication (should use JWT or sessions in production)
- No input sanitization beyond basic validation (should be enhanced for production)
- Error handling provides detailed messages (should be sanitized in production)

## Testing the API

You can test the API using curl, Postman, or any HTTP client:

```bash
# Login
curl -X POST http://localhost:5001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "manager1", "password": "password123"}' \
  -c cookies.txt

# Get pending expenses
curl -X GET http://localhost:5001/api/expenses/pending -b cookies.txt

# Approve an expense
curl -X POST http://localhost:5001/api/expenses/123/approve \
  -H "Content-Type: application/json" \
  -d '{"comment": "Looks good"}' \
  -b cookies.txt

# Download all expenses report
curl -X GET http://localhost:5001/api/reports/expenses/csv -b cookies.txt -o all_expenses.csv
```
