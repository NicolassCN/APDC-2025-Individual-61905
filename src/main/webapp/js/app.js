// API endpoints
const API = {
    BASE_URL: window.location.origin + '/rest',
    REGISTER: '/register',
    LOGIN: '/login',
    CHANGE_ROLE: '/changerole',
    CHANGE_STATE: '/changeaccountstate',
    REMOVE_USER: '/removeuseraccount',
    LIST_USERS: '/listusers'
};

// Token management
const TokenManager = {
    setToken: (token) => {
        localStorage.setItem('authToken', token.tokenString);
        localStorage.setItem('username', token.username);
        localStorage.setItem('role', token.role);
    },
    getToken: () => {
        const tokenString = localStorage.getItem('authToken');
        const username = localStorage.getItem('username');
        const role = localStorage.getItem('role');
        
        if (!tokenString || !username || !role) {
            return null;
        }
        
        return {
            tokenString,
            username,
            role
        };
    },
    getUsername: () => localStorage.getItem('username'),
    getRole: () => localStorage.getItem('role'),
    clearToken: () => {
        localStorage.removeItem('authToken');
        localStorage.removeItem('username');
        localStorage.removeItem('role');
    }
};

// API calls
const ApiClient = {
    async register(userData) {
        try {
            const response = await fetch(API.BASE_URL + API.REGISTER, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(userData)
            });
            
            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || 'Registration failed');
            }
            
            return await response.json();
        } catch (error) {
            console.error('Registration error:', error);
            throw error;
        }
    },

    async login(credentials) {
        try {
            const response = await fetch(API.BASE_URL + API.LOGIN, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(credentials)
            });
            
            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || 'Login failed');
            }
            
            const data = await response.json();
            TokenManager.setToken(data);
            return data;
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    },

    async changeRole(roleData) {
        try {
            const token = TokenManager.getToken();
            if (!token) {
                throw new Error('Not authenticated');
            }
            
            const response = await fetch(API.BASE_URL + API.CHANGE_ROLE, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token.tokenString
                },
                body: JSON.stringify(roleData)
            });
            
            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || 'Change role failed');
            }
            
            return await response.json();
        } catch (error) {
            console.error('Change role error:', error);
            throw error;
        }
    },

    async changeAccountState(stateData) {
        try {
            const token = TokenManager.getToken();
            if (!token) {
                throw new Error('Not authenticated');
            }
            
            const response = await fetch(API.BASE_URL + API.CHANGE_STATE, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token.tokenString
                },
                body: JSON.stringify(stateData)
            });
            
            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || 'Change account state failed');
            }
            
            return await response.json();
        } catch (error) {
            console.error('Change account state error:', error);
            throw error;
        }
    },

    async removeUser(userData) {
        try {
            const token = TokenManager.getToken();
            if (!token) {
                throw new Error('Not authenticated');
            }
            
            const response = await fetch(API.BASE_URL + API.REMOVE_USER, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token.tokenString
                },
                body: JSON.stringify(userData)
            });
            
            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || 'Remove user failed');
            }
            
            return await response.json();
        } catch (error) {
            console.error('Remove user error:', error);
            throw error;
        }
    },
    
    logout() {
        TokenManager.clearToken();
        return Promise.resolve();
    },

    async listUsers() {
        try {
            const token = TokenManager.getToken();
            if (!token) {
                throw new Error('Not authenticated');
            }

            const response = await fetch(API.BASE_URL + API.LIST_USERS, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token.tokenString
                },
                body: JSON.stringify({ username: token.username })
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to list users');
            }

            return await response.json();
        } catch (error) {
            console.error('Error listing users:', error);
            throw error;
        }
    }
};

// Form validation
const Validator = {
    validateEmail: (email) => {
        const re = /^[A-Za-z0-9+_.-]+@(.+)$/;
        return re.test(email);
    },
    
    validatePassword: (password) => {
        const re = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/;
        return re.test(password);
    },
    
    validatePhone: (phone) => {
        const re = /^\+?[0-9]{9,}$/;
        return re.test(phone);
    }
};

// UI Helpers
const UI = {
    showMessage: (message, isError = false) => {
        const messageDiv = document.getElementById('message');
        if (messageDiv) {
            messageDiv.textContent = message;
            messageDiv.className = isError ? 'error' : 'success';
            messageDiv.style.display = 'block';
            setTimeout(() => {
                messageDiv.style.display = 'none';
            }, 5000);
        }
    },

    updateAuthUI: () => {
        const token = TokenManager.getToken();
        const username = TokenManager.getUsername();
        const loginSection = document.getElementById('login-section');
        const registerSection = document.getElementById('register-section');
        const userSection = document.getElementById('user-section');
        const adminSection = document.getElementById('admin-section');
        
        if (token) {
            loginSection.style.display = 'none';
            registerSection.style.display = 'none';
            userSection.style.display = 'block';
            document.getElementById('user-name').textContent = username;
            
            if (TokenManager.getRole() === 'ADMIN' || TokenManager.getRole() === 'BACKOFFICE') {
                adminSection.style.display = 'block';
            } else {
                adminSection.style.display = 'none';
            }
        } else {
            loginSection.style.display = 'block';
            registerSection.style.display = 'none';
            userSection.style.display = 'none';
            adminSection.style.display = 'none';
        }
    },
    
    showLoginForm: () => {
        document.getElementById('login-section').style.display = 'block';
        document.getElementById('register-section').style.display = 'none';
    },
    
    showRegisterForm: () => {
        document.getElementById('login-section').style.display = 'none';
        document.getElementById('register-section').style.display = 'block';
    },

    async updateUserList() {
        try {
            const users = await ApiClient.listUsers();
            const userList = document.getElementById('user-list');
            if (!userList) return;

            userList.innerHTML = '';
            
            if (users.length === 0) {
                userList.innerHTML = '<p>No users found.</p>';
                return;
            }

            const table = document.createElement('table');
            table.className = 'user-table';
            
            // Create header
            const thead = document.createElement('thead');
            const headerRow = document.createElement('tr');
            const headers = ['Username', 'Email', 'Full Name', 'Role', 'State', 'Profile'];
            headers.forEach(header => {
                const th = document.createElement('th');
                th.textContent = header;
                headerRow.appendChild(th);
            });
            thead.appendChild(headerRow);
            table.appendChild(thead);
            
            // Create body
            const tbody = document.createElement('tbody');
            users.forEach(user => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${user.username || 'NOT DEFINED'}</td>
                    <td>${user.email || 'NOT DEFINED'}</td>
                    <td>${user.fullName || 'NOT DEFINED'}</td>
                    <td>${user.role || 'NOT DEFINED'}</td>
                    <td>${user.accountState || 'NOT DEFINED'}</td>
                    <td>${user.profile || 'NOT DEFINED'}</td>
                `;
                tbody.appendChild(row);
            });
            table.appendChild(tbody);
            
            userList.appendChild(table);
        } catch (error) {
            console.error('Error updating user list:', error);
            this.showMessage(error.message, true);
        }
    },

    updateAuthUI() {
        const token = TokenManager.getToken();
        const loginSection = document.getElementById('login-section');
        const registerSection = document.getElementById('register-section');
        const userSection = document.getElementById('user-section');
        const adminSection = document.getElementById('admin-section');
        
        if (token) {
            document.getElementById('user-name').textContent = token.username;
            loginSection.style.display = 'none';
            registerSection.style.display = 'none';
            userSection.style.display = 'block';
            
            if (token.role === 'ADMIN' || token.role === 'BACKOFFICE') {
                adminSection.style.display = 'block';
                this.updateUserList();
            } else {
                adminSection.style.display = 'none';
            }
        } else {
            loginSection.style.display = 'block';
            registerSection.style.display = 'none';
            userSection.style.display = 'none';
            adminSection.style.display = 'none';
        }
    }
}; 