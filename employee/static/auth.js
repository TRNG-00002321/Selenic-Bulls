// Authentication JavaScript for Login Page

class AuthManager {
    constructor() {
        this.init();
    }

    init() {
        // Set up event listeners
        this.setupEventListeners();
        
        // Check if already logged in
        // this.checkAuthStatus();
    }

    setupEventListeners() {
        // Login form
        document.getElementById('login-form').addEventListener('submit', (e) => {
            e.preventDefault();
            this.login();
        });
    }

    async checkAuthStatus() {
        try {
            // Check authentication status via cookies (httpOnly cookie is automatically sent)
            const response = await fetch('/api/auth/status');
            const data = await response.json();
            
            if (data.authenticated && data.user.role === 'Employee') {
                // Already logged in as employee, redirect to employee app
                window.location.href = '/app';
            }
            // If not authenticated, stay on login page (no action needed)
        } catch (error) {
            console.error('Auth check failed:', error);
        }
    }

    async login() {
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password }),
            });

            const data = await response.json();

            if (response.ok) {
                console.log('=== Employee Login Success ===');
                console.log('Login response data:', data);
                console.log('JWT token set as httpOnly cookie');
                
                // JWT token is now automatically stored as httpOnly cookie by the server
                // No need to manually store anything in localStorage
                
                if (data.user.role === 'Employee') {
                    this.showMessage('Login successful! Redirecting to employee dashboard...', 'success');
                    // Redirect to employee app after short delay
                    setTimeout(() => {
                        window.location.href = '/app';
                    }, 1000);
                } else {
                    this.showMessage('Access denied - This is the employee portal', 'error');
                }
            } else {
                this.showMessage(data.error || 'Login failed', 'error');
            }
        } catch (error) {
            this.showMessage('Network error. Please try again.', 'error');
        }
    }

    showMessage(message, type) {
        const element = document.getElementById('login-message');
        element.innerHTML = `<p style="color: ${type === 'error' ? 'red' : 'green'}; font-weight: bold;">${message}</p>`;
        
        // Clear message after 5 seconds
        setTimeout(() => {
            element.innerHTML = '';
        }, 5000);
    }
}

// Initialize the auth manager when page loads
const authManager = new AuthManager();