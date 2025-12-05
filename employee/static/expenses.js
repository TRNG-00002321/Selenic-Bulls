// Employee Expense Dashboard JavaScript

class ExpenseManager {
    constructor() {
        this.currentUser = null;
        this.init();
    }

    init() {
        // Check authentication first
        this.checkAuthStatus();
    }

    async checkAuthStatus() {
        try {
            // Check authentication status via cookies (httpOnly cookie is automatically sent)
            const response = await fetch('/api/auth/status');
            const data = await response.json();
            
            if (data.authenticated && data.user.role === 'Employee') {
                this.currentUser = data.user;
                this.initializeApp();
            } else {
                // Not authenticated or not an employee, redirect to login
                window.location.href = '/login';
            }
        } catch (error) {
            console.error('Auth check failed:', error);
            window.location.href = '/login';
        }
    }

    getAuthHeaders() {
        // With httpOnly cookies, authentication is automatic - just return content type
        return {
            'Content-Type': 'application/json'
        };
    }

    initializeApp() {
        // Hide loading section and show main content
        document.getElementById('loading-section').style.display = 'none';
        document.getElementById('header').style.display = 'block';
        document.getElementById('navigation').style.display = 'block';
        
        // Display username
        document.getElementById('username-display').textContent = this.currentUser.username;
        
        // Set up event listeners
        this.setupEventListeners();
        
        // Set today's date as default
        const today = new Date().toISOString().split('T')[0];
        document.getElementById('date').value = today;
        
        // Show expenses by default
        this.showExpensesSection();
    }

    setupEventListeners() {
        // Logout button
        document.getElementById('logout-btn').addEventListener('click', () => {
            this.logout();
        });

        // Navigation buttons
        document.getElementById('show-submit').addEventListener('click', () => {
            this.showSection('submit-expense-section');
        });

        document.getElementById('show-expenses').addEventListener('click', () => {
            this.showExpensesSection();
        });

        // Submit expense form
        document.getElementById('expense-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.submitExpense();
        });

        // Cancel submit
        document.getElementById('cancel-submit').addEventListener('click', () => {
            this.hideAllSections();
            document.getElementById('expense-form').reset();
            const today = new Date().toISOString().split('T')[0];
            document.getElementById('date').value = today;
        });

        // Edit expense form
        document.getElementById('edit-expense-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.updateExpense();
        });

        // Cancel edit
        document.getElementById('cancel-edit').addEventListener('click', () => {
            this.showExpensesSection();
        });

        // Filter and refresh
        document.getElementById('status-filter').addEventListener('change', () => {
            this.loadExpenses();
        });

        document.getElementById('refresh-expenses').addEventListener('click', () => {
            this.loadExpenses();
        });
    }

    async logout() {
        try {
            await fetch('/api/auth/logout', { 
                method: 'POST',
                headers: this.getAuthHeaders()
            });
            // httpOnly cookie is cleared by the server
            window.location.href = '/login';
        } catch (error) {
            console.error('Logout failed:', error);
            // Even if logout fails, redirect to login page
            window.location.href = '/login';
        }
    }

    async submitExpense() {
        const amount = document.getElementById('amount').value;
        const description = document.getElementById('description').value;
        const date = document.getElementById('date').value;

        try {
            const response = await fetch('/api/expenses', {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ amount: parseFloat(amount), description, date }),
            });

            const data = await response.json();

            if (response.ok) {
                this.showMessage('submit-message', 'Expense submitted successfully!', 'success');
                document.getElementById('expense-form').reset();
                
                // Set today's date again
                const today = new Date().toISOString().split('T')[0];
                document.getElementById('date').value = today;
                
                // Auto-switch to expenses view after 2 seconds
                setTimeout(() => {
                    this.showExpensesSection();
                }, 2000);
            } else {
                this.showMessage('submit-message', data.error || 'Failed to submit expense', 'error');
            }
        } catch (error) {
            this.showMessage('submit-message', 'Network error. Please try again.', 'error');
        }
    }

    async loadExpenses() {
        const statusFilter = document.getElementById('status-filter').value;
        const url = statusFilter ? `/api/expenses?status=${statusFilter}` : '/api/expenses';

        try {
            const response = await fetch(url, {
                headers: this.getAuthHeaders()
            });
            const data = await response.json();

            if (response.ok) {
                this.displayExpenses(data.expenses);
            } else {
                this.showMessage('expenses-list', data.error || 'Failed to load expenses', 'error');
            }
        } catch (error) {
            this.showMessage('expenses-list', 'Network error. Please try again.', 'error');
        }
    }

    displayExpenses(expenses) {
        const container = document.getElementById('expenses-list');
        
        if (expenses.length === 0) {
            container.innerHTML = '<p>No expenses found.</p>';
            return;
        }

        let html = '<table border="1" cellpadding="8" cellspacing="0" width="100%">';
        html += '<tr style="background-color: #f0f0f0;"><th>Date</th><th>Amount</th><th>Description</th><th>Status</th><th>Comment</th><th>Actions</th></tr>';

        expenses.forEach(expense => {
            const statusColor = expense.status === 'approved' ? 'green' : 
                              expense.status === 'denied' ? 'red' : 'orange';
            
            html += `<tr>
                <td>${expense.date}</td>
                <td>$${expense.amount.toFixed(2)}</td>
                <td>${expense.description}</td>
                <td style="color: ${statusColor}; font-weight: bold;">${expense.status.toUpperCase()}</td>
                <td>${expense.comment || '-'}</td>
                <td>`;
            
            if (expense.status === 'pending') {
                html += `<button onclick="expenseManager.editExpense(${expense.id})">Edit</button> `;
                html += `<button onclick="expenseManager.deleteExpense(${expense.id})" style="color: red;">Delete</button>`;
            } else {
                html += '-';
            }
            
            html += '</td></tr>';
        });

        html += '</table>';
        container.innerHTML = html;
    }

    async editExpense(expenseId) {
        try {
            const response = await fetch(`/api/expenses/${expenseId}`, {
                headers: this.getAuthHeaders()
            });
            const data = await response.json();

            if (response.ok) {
                const expense = data.expense;
                
                // Populate edit form
                document.getElementById('edit-expense-id').value = expense.id;
                document.getElementById('edit-amount').value = expense.amount;
                document.getElementById('edit-description').value = expense.description;
                document.getElementById('edit-date').value = expense.date;
                
                this.showSection('edit-expense-section');
            } else {
                alert(data.error || 'Failed to load expense details');
            }
        } catch (error) {
            alert('Network error. Please try again.');
        }
    }

    async updateExpense() {
        const expenseId = document.getElementById('edit-expense-id').value;
        const amount = document.getElementById('edit-amount').value;
        const description = document.getElementById('edit-description').value;
        const date = document.getElementById('edit-date').value;

        try {
            const response = await fetch(`/api/expenses/${expenseId}`, {
                method: 'PUT',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ 
                    amount: parseFloat(amount), 
                    description, 
                    date 
                }),
            });

            const data = await response.json();

            if (response.ok) {
                this.showMessage('edit-message', 'Expense updated successfully!', 'success');
                setTimeout(() => {
                    this.showExpensesSection();
                }, 1500);
            } else {
                this.showMessage('edit-message', data.error || 'Failed to update expense', 'error');
            }
        } catch (error) {
            this.showMessage('edit-message', 'Network error. Please try again.', 'error');
        }
    }

    async deleteExpense(expenseId) {
        if (!confirm('Are you sure you want to delete this expense?')) {
            return;
        }

        try {
            const response = await fetch(`/api/expenses/${expenseId}`, {
                method: 'DELETE',
                headers: this.getAuthHeaders()
            });

            const data = await response.json();

            if (response.ok) {
                alert('Expense deleted successfully!');
                this.loadExpenses();
            } else {
                alert(data.error || 'Failed to delete expense');
            }
        } catch (error) {
            alert('Network error. Please try again.');
        }
    }

    showExpensesSection() {
        this.showSection('expenses-section');
        this.loadExpenses();
    }

    showSection(sectionId) {
        this.hideAllSections();
        document.getElementById(sectionId).style.display = 'block';
    }

    hideAllSections() {
        const sections = [
            'submit-expense-section',
            'expenses-section', 
            'edit-expense-section'
        ];
        
        sections.forEach(id => {
            document.getElementById(id).style.display = 'none';
        });
    }

    showMessage(elementId, message, type) {
        const element = document.getElementById(elementId);
        element.innerHTML = `<p style="color: ${type === 'error' ? 'red' : 'green'}; font-weight: bold;">${message}</p>`;
        
        // Clear message after 5 seconds
        setTimeout(() => {
            element.innerHTML = '';
        }, 5000);
    }
}

// Initialize the app when page loads
const expenseManager = new ExpenseManager();