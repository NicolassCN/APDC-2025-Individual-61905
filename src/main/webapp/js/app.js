// API endpoints
const API_BASE_URL = '/rest/user';
const API_ENDPOINTS = {
    LOGIN: `${API_BASE_URL}/login`,
    REGISTER: `${API_BASE_URL}/register`,
    CHANGE_ROLE: `${API_BASE_URL}/changeRole`,
    CHANGE_STATE: `${API_BASE_URL}/changeAccountState`,
    REMOVE_USER: `${API_BASE_URL}/removeUserAccount`,
    LIST_USERS: `${API_BASE_URL}/all`
};

// Token management
const TOKEN_KEY = 'auth_token';

// Utility functions
const getToken = () => localStorage.getItem(TOKEN_KEY);
const setToken = token => localStorage.setItem(TOKEN_KEY, token);
const removeToken = () => localStorage.removeItem(TOKEN_KEY);

// API calls
const api = {
    async login(data) {
        const response = await fetch(API_ENDPOINTS.LOGIN, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'Login failed');
        return result;
    },

    async register(data) {
        const response = await fetch(API_ENDPOINTS.REGISTER, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'Registration failed');
        return result;
    },

    async changeRole(data) {
        const response = await fetch(API_ENDPOINTS.CHANGE_ROLE, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify(data)
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'Role change failed');
        return result;
    },

    async changeAccountState(data) {
        const response = await fetch(API_ENDPOINTS.CHANGE_STATE, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify(data)
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'Account state change failed');
        return result;
    },

    async removeUser(data) {
        const response = await fetch(API_ENDPOINTS.REMOVE_USER, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify(data)
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'User removal failed');
        return result;
    },

    async listUsers() {
        const response = await fetch(API_ENDPOINTS.LIST_USERS, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            }
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'Failed to fetch users');
        return result;
    }
};

// Validation
const Validator = {
    email(email) {
        return /^[A-Za-z0-9+_.-]+@(.+)$/.test(email);
    },

    password(password) {
        return /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\S+$).{8,}$/.test(password);
    },

    phone(phone) {
        return /^\+?[1-9]\d{1,14}$/.test(phone);
    }
};

// UI management
const UI = {
    showMessage(message, isError = false) {
        const messageDiv = document.getElementById('message');
        messageDiv.textContent = message;
        messageDiv.className = `message ${isError ? 'error' : 'success'}`;
        messageDiv.style.display = 'block';
        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 5000);
    },

    showSection(sectionId) {
        document.querySelectorAll('.section').forEach(section => {
            section.style.display = 'none';
        });
        document.getElementById(sectionId).style.display = 'block';
    },

    updateUserList(users) {
        const userList = document.getElementById('user-list');
        if (!userList) return;

        const table = document.createElement('table');
        table.className = 'user-table';

        // Create table header
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        ['Username', 'Email', 'Name', 'Phone', 'State', 'Profile', 'Role', 'Actions'].forEach(text => {
            const th = document.createElement('th');
            th.textContent = text;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        // Create table body
        const tbody = document.createElement('tbody');
        users.forEach(user => {
            const tr = document.createElement('tr');
            
            // Add user data
            ['username', 'email', 'name', 'phone', 'state', 'profile', 'role'].forEach(field => {
                const td = document.createElement('td');
                td.textContent = user[field] || 'N/A';
                tr.appendChild(td);
            });

            // Add action buttons
            const actionsTd = document.createElement('td');
            actionsTd.className = 'action-buttons';

            // Change Role button
            const changeRoleBtn = document.createElement('button');
            changeRoleBtn.textContent = 'Change Role';
            changeRoleBtn.className = 'action-button change-role';
            changeRoleBtn.onclick = () => {
                document.getElementById('change-role-username').value = user.username;
                document.getElementById('change-role-form').scrollIntoView();
            };

            // Change State button
            const changeStateBtn = document.createElement('button');
            changeStateBtn.textContent = 'Change State';
            changeStateBtn.className = 'action-button change-state';
            changeStateBtn.onclick = () => {
                document.getElementById('change-state-username').value = user.username;
                document.getElementById('change-state-form').scrollIntoView();
            };

            // Remove User button
            const removeUserBtn = document.createElement('button');
            removeUserBtn.textContent = 'Remove User';
            removeUserBtn.className = 'action-button remove';
            removeUserBtn.onclick = () => {
                document.getElementById('remove-user-username').value = user.username;
                document.getElementById('remove-user-form').scrollIntoView();
            };

            actionsTd.appendChild(changeRoleBtn);
            actionsTd.appendChild(changeStateBtn);
            actionsTd.appendChild(removeUserBtn);
            tr.appendChild(actionsTd);

            tbody.appendChild(tr);
        });
        table.appendChild(tbody);

        // Replace existing content
        userList.innerHTML = '';
        userList.appendChild(table);
    }
};

// Event Handlers
document.addEventListener('DOMContentLoaded', () => {
    // Login form handler
    document.getElementById('login-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            const data = {
                identifier: e.target.identifier.value,
                password: e.target.password.value
            };
            const result = await api.login(data);
            setToken(result.token.validity.verifier);
            UI.showMessage('Login successful');
            UI.showSection('admin-section');
            api.listUsers().then(users => UI.updateUserList(users));
        } catch (error) {
            UI.showMessage(error.message, true);
        }
    });

    // Register form handler
    document.getElementById('register-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            if (!Validator.email(e.target.email.value)) {
                throw new Error('Invalid email format');
            }
            if (!Validator.password(e.target.password.value)) {
                throw new Error('Password does not meet requirements');
            }
            if (e.target.password.value !== e.target.confirmPassword.value) {
                throw new Error('Passwords do not match');
            }
            if (!Validator.phone(e.target.phone.value)) {
                throw new Error('Invalid phone number format');
            }

            const data = {
                email: e.target.email.value,
                username: e.target.username.value,
                fullName: e.target.fullName.value,
                phone: e.target.phone.value,
                password: e.target.password.value,
                confirmPassword: e.target.confirmPassword.value,
                profile: e.target.profile.value,
                citizenCardNumber: e.target.citizenCardNumber?.value,
                taxId: e.target.nif?.value,
                employer: e.target.employer?.value,
                jobTitle: e.target.jobTitle?.value,
                address: e.target.address?.value,
                employerTaxId: e.target.employerNif?.value,
                photo: e.target.photo?.value
            };

            await api.register(data);
            UI.showMessage('Registration successful');
            UI.showSection('login-section');
        } catch (error) {
            UI.showMessage(error.message, true);
        }
    });

    // Change Role form handler
    document.getElementById('change-role-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            const data = {
                username: e.target.username.value,
                newRole: e.target.newRole.value
            };
            await api.changeRole(data);
            UI.showMessage('Role changed successfully');
            api.listUsers().then(users => UI.updateUserList(users));
        } catch (error) {
            UI.showMessage(error.message, true);
        }
    });

    // Change Account State form handler
    document.getElementById('change-state-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            const data = {
                username: e.target.username.value,
                newState: e.target.newState.value
            };
            await api.changeAccountState(data);
            UI.showMessage('Account state changed successfully');
            api.listUsers().then(users => UI.updateUserList(users));
        } catch (error) {
            UI.showMessage(error.message, true);
        }
    });

    // Remove User form handler
    document.getElementById('remove-user-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            const data = {
                username: e.target.username.value
            };
            await api.removeUser(data);
            UI.showMessage('User removed successfully');
            api.listUsers().then(users => UI.updateUserList(users));
        } catch (error) {
            UI.showMessage(error.message, true);
        }
    });

    // Navigation handlers
    document.getElementById('show-register')?.addEventListener('click', (e) => {
        e.preventDefault();
        UI.showSection('register-section');
    });

    document.getElementById('show-login')?.addEventListener('click', (e) => {
        e.preventDefault();
        UI.showSection('login-section');
    });

    // Optional fields toggle
    document.getElementById('toggleOptional')?.addEventListener('click', () => {
        const optionalFields = document.getElementById('optionalFields');
        const toggleButton = document.getElementById('toggleOptional');
        if (optionalFields.classList.contains('hidden')) {
            optionalFields.classList.remove('hidden');
            toggleButton.textContent = 'Hide Optional Fields';
        } else {
            optionalFields.classList.add('hidden');
            toggleButton.textContent = 'Show Optional Fields';
        }
    });

    // Check for existing token and show appropriate section
    const token = getToken();
    if (token) {
        UI.showSection('admin-section');
        api.listUsers().then(users => UI.updateUserList(users));
    } else {
        UI.showSection('login-section');
    }
});