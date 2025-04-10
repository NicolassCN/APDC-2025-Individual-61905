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
    setToken(token) {
        if (!token || !token.tokenString || !token.username || !token.role) {
            console.error('Invalid token structure:', token);
            return;
        }
        console.log('Setting token:', token);
        
        // Verificar se o token já existe
        const existingToken = localStorage.getItem('token');
        if (existingToken) {
            console.log('Existing token found, removing it');
            localStorage.removeItem('token');
        }
        
        // Armazenar o novo token
        localStorage.setItem('token', JSON.stringify(token));
        
        // Verificar se o token foi armazenado corretamente
        const storedToken = localStorage.getItem('token');
        console.log('Stored token in localStorage:', storedToken);
        
        // Verificar se o token pode ser recuperado corretamente
        const retrievedToken = this.getToken();
        console.log('Retrieved token after setting:', retrievedToken);
    },

    getToken() {
        const tokenStr = localStorage.getItem('token');
        if (!tokenStr) {
            console.log('No token found in localStorage');
            return null;
        }
        try {
            const token = JSON.parse(tokenStr);
            if (!token.tokenString || !token.username || !token.role) {
                console.error('Invalid token structure in localStorage:', token);
                return null;
            }
            console.log('Retrieved token from localStorage:', token);
            console.log('Token string:', token.tokenString);
            console.log('Token username:', token.username);
            console.log('Token role:', token.role);
            return token;
        } catch (e) {
            console.error('Error parsing token:', e);
            return null;
        }
    },

    getUsername: () => localStorage.getItem('username'),
    getRole: () => localStorage.getItem('role'),
    clearToken() {
        console.log('Clearing token from localStorage');
        localStorage.removeItem('token');
    }
};

// API calls
const ApiClient = {
    async register(userData, confirmPassword) {
        try {
            // Validar campos obrigatórios
            if (!userData.email || !userData.username || !userData.fullName || 
                !userData.phone || !userData.password || !userData.profile) {
                throw new Error('Todos os campos obrigatórios devem ser preenchidos');
            }
            
            // Validar confirmação de senha
            if (userData.password !== confirmPassword) {
                throw new Error('As senhas não coincidem');
            }
            
            // Validar formato dos campos
            if (!Validator.validateEmail(userData.email)) {
                throw new Error('Formato de email inválido');
            }
            
            if (!Validator.validatePassword(userData.password)) {
                throw new Error('A senha deve ter no mínimo 8 caracteres, incluindo maiúsculas, minúsculas, números e caracteres especiais');
            }
            
            if (!Validator.validatePhone(userData.phone)) {
                throw new Error('Formato de telefone inválido');
            }
            
            // Validar campos opcionais se fornecidos
            if (userData.nif && !/^\d{9}$/.test(userData.nif)) {
                throw new Error('NIF deve conter exatamente 9 dígitos');
            }
            
            if (userData.employerNif && !/^\d{9}$/.test(userData.employerNif)) {
                throw new Error('NIF da entidade empregadora deve conter exatamente 9 dígitos');
            }
            
            if (userData.photoUrl && !userData.photoUrl.toLowerCase().endsWith('.jpg')) {
                throw new Error('A foto deve ser um arquivo JPEG');
            }
            
            // Criar objeto RegisterData com o formato esperado pelo backend
            const registerData = {
                user: userData,
                confirmPassword: confirmPassword
            };
            
            const response = await fetch(API.BASE_URL + API.REGISTER, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(registerData)
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
            console.log('Attempting login with:', credentials.username);
            const response = await fetch(API.BASE_URL + API.LOGIN, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(credentials)
            });
            
            if (!response.ok) {
                const errorData = await response.text();
                console.error('Login failed:', errorData);
                throw new Error(errorData || 'Login failed');
            }
            
            const data = await response.json();
            console.log('Login successful, token received:', data);
            console.log('Token string:', data.tokenString);
            console.log('Token username:', data.username);
            console.log('Token role:', data.role);
            
            // Verificar se o token tem o formato correto
            if (!data.tokenString || !data.username || !data.role) {
                console.error('Invalid token format received from server:', data);
                throw new Error('Invalid token format received from server');
            }
            
            // Armazenar o token
            TokenManager.setToken(data);
            
            // Verificar se o token foi armazenado corretamente
            const storedToken = TokenManager.getToken();
            console.log('Stored token:', storedToken);
            
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
            
            // Check permissions based on user role
            if (token.role === 'ENDUSER') {
                throw new Error('ENDUSER cannot change roles');
            } else if (token.role === 'BACKOFFICE') {
                // BACKOFFICE can only change ENDUSER to PARTNER or vice versa
                if (roleData.newRole !== 'ENDUSER' && roleData.newRole !== 'PARTNER') {
                    throw new Error('BACKOFFICE can only change roles between ENDUSER and PARTNER');
                }
            }
            // ADMIN can change any role to any role, no additional checks needed
            
            console.log('Sending change role request with token:', token.tokenString);
            console.log('Request body:', JSON.stringify({
                targetUsername: roleData.username,
                newRole: roleData.newRole
            }));
            
            const response = await fetch(API.BASE_URL + API.CHANGE_ROLE, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': token.tokenString
                },
                body: JSON.stringify({
                    targetUsername: roleData.username,
                    newRole: roleData.newRole
                })
            });
            
            if (!response.ok) {
                const errorData = await response.text();
                console.error('Change role failed:', errorData);
                throw new Error(errorData || 'Change role failed');
            }
            
            const data = await response.json();
            console.log('Change role response:', data);
            return data;
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
            
            // Check permissions based on user role
            if (token.role === 'ENDUSER') {
                throw new Error('ENDUSER cannot change account states');
            }
            // BACKOFFICE and ADMIN can change any account state, no additional checks needed
            
            console.log('Sending change state request with token:', token.tokenString);
            const response = await fetch(API.BASE_URL + API.CHANGE_STATE, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': token.tokenString
                },
                body: JSON.stringify({
                    targetUsername: stateData.username,
                    newState: stateData.newState
                })
            });
            
            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(errorData || 'Change state failed');
            }
            
            return await response.json();
        } catch (error) {
            console.error('Change state error:', error);
            throw error;
        }
    },

    async removeUser(username) {
        try {
            const token = TokenManager.getToken();
            if (!token) {
                throw new Error('Not authenticated');
            }
            
            // Check permissions based on user role
            if (token.role === 'ENDUSER') {
                throw new Error('ENDUSER cannot remove users');
            }
            if (token.role === 'BACKOFFICE') {
                throw new Error('BACKOFFICE cannot remove users');
            }
            // Only ADMIN can remove users
            
            console.log('Sending remove user request with token:', token.tokenString);
            const response = await fetch(API.BASE_URL + API.REMOVE_USER, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': token.tokenString
                },
                body: JSON.stringify({
                    targetUsername: username
                })
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
            
            // Check permissions based on user role
            if (token.role === 'ENDUSER') {
                throw new Error('ENDUSER cannot list users');
            }
            // BACKOFFICE and ADMIN can list users
            
            console.log('Sending list users request with token:', token.tokenString);
            console.log('Request body:', JSON.stringify({
                username: token.username
            }));
            
            const response = await fetch(API.BASE_URL + API.LIST_USERS, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': token.tokenString
                },
                body: JSON.stringify({
                    username: token.username
                })
            });
            
            if (!response.ok) {
                const errorData = await response.text();
                console.error('List users failed:', errorData);
                throw new Error(errorData || 'List users failed');
            }
            
            const data = await response.json();
            console.log('List users response:', data);
            return data;
        } catch (error) {
            console.error('List users error:', error);
            throw error;
        }
    }
};

// Validator object for input validation
const Validator = {
    validateEmail: (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },
    
    validatePassword: (password) => {
        // At least 8 characters, 1 uppercase, 1 lowercase, 1 number, 1 special character
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
        return passwordRegex.test(password);
    },
    
    validatePhone: (phone) => {
        // Portuguese phone number format: +351XXXXXXXXX or 9XXXXXXXX
        const phoneRegex = /^(\+351)?9[1236]\d{7}$/;
        return phoneRegex.test(phone);
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
        console.log('Updating UI based on authentication state');
        const token = TokenManager.getToken();
        console.log('Token for UI update:', token);
        
        const loginSection = document.getElementById('login-section');
        const registerSection = document.getElementById('register-section');
        const userSection = document.getElementById('user-section');
        const adminSection = document.getElementById('admin-section');
        const userList = document.getElementById('user-list');
        
        console.log('UI elements:', {
            loginSection: loginSection ? 'found' : 'not found',
            registerSection: registerSection ? 'found' : 'not found',
            userSection: userSection ? 'found' : 'not found',
            adminSection: adminSection ? 'found' : 'not found',
            userList: userList ? 'found' : 'not found'
        });

        if (token) {
            console.log('User is logged in, updating UI for authenticated user');
            // User is logged in
            loginSection.style.display = 'none';
            registerSection.style.display = 'none';
            userSection.style.display = 'block';
            
            const userNameElement = document.getElementById('user-name');
            if (userNameElement) {
                userNameElement.textContent = token.username;
                console.log('Updated user name to:', token.username);
            } else {
                console.error('User name element not found');
            }

            // Show admin section if user is ADMIN or BACKOFFICE
            if (token.role === 'ADMIN' || token.role === 'BACKOFFICE') {
                console.log('User is ADMIN or BACKOFFICE, showing admin section');
                adminSection.style.display = 'block';
                userList.style.display = 'block';
                // Refresh user list
                ApiClient.listUsers()
                    .then(users => {
                        console.log('User list retrieved successfully:', users);
                        UI.updateUserList(users);
                    })
                    .catch(error => {
                        console.error('Error retrieving user list:', error);
                        UI.showMessage(error.message, true);
                    });
            } else {
                console.log('User is not ADMIN or BACKOFFICE, hiding admin section');
                adminSection.style.display = 'none';
                userList.style.display = 'none';
            }
        } else {
            console.log('User is not logged in, showing login form');
            // User is not logged in
            loginSection.style.display = 'block';
            registerSection.style.display = 'none';
            userSection.style.display = 'none';
            adminSection.style.display = 'none';
            userList.style.display = 'none';
        }
    },
    
    showLoginForm: () => {
        console.log('Showing login form');
        const loginSection = document.getElementById('login-section');
        const registerSection = document.getElementById('register-section');
        
        if (loginSection && registerSection) {
            loginSection.style.display = 'block';
            registerSection.style.display = 'none';
        } else {
            console.error('Login or register section not found');
        }
    },
    
    showRegisterForm: () => {
        console.log('Showing register form');
        const loginSection = document.getElementById('login-section');
        const registerSection = document.getElementById('register-section');
        
        if (loginSection && registerSection) {
            loginSection.style.display = 'none';
            registerSection.style.display = 'block';
        } else {
            console.error('Login or register section not found');
        }
    },

    async updateUserList(users) {
        try {
            console.log('Updating user list with:', users);
            const userList = document.getElementById('user-list');
            if (!userList) {
                console.error('User list element not found');
                return;
            }

            userList.innerHTML = '';
            
            if (users.length === 0) {
                userList.innerHTML = '<p>No users found.</p>';
                return;
            }

            const token = TokenManager.getToken();
            if (!token) {
                console.error('No token found, cannot update user list');
                return;
            }

            // Filter users based on role permissions
            let filteredUsers = users;
            
            if (token.role === 'ENDUSER') {
                // ENDUSER can only see other ENDUSER accounts with PUBLIC profile and ACTIVATED state
                filteredUsers = users.filter(user => 
                    user.role === 'ENDUSER' && 
                    user.profile === 'PUBLIC' && 
                    user.accountState === 'ACTIVATED'
                );
            } else if (token.role === 'BACKOFFICE') {
                // BACKOFFICE can see all ENDUSER accounts regardless of profile or state
                filteredUsers = users.filter(user => user.role === 'ENDUSER');
            }
            // ADMIN can see all users, no filtering needed

            console.log('Filtered users:', filteredUsers);

            if (filteredUsers.length === 0) {
                userList.innerHTML = '<p>No users found matching your permissions.</p>';
                return;
            }

            const table = document.createElement('table');
            table.className = 'user-table';
            
            // Create header
            const thead = document.createElement('thead');
            const headerRow = document.createElement('tr');
            
            // Define headers based on role
            let headers = ['Username', 'Email', 'Full Name'];
            
            if (token.role === 'ENDUSER') {
                // ENDUSER can only see username, email, and full name
                headers = ['Username', 'Email', 'Full Name'];
            } else if (token.role === 'BACKOFFICE') {
                // BACKOFFICE can see all attributes of ENDUSER accounts
                headers = ['Username', 'Email', 'Full Name', 'Role', 'State', 'Profile', 'Actions'];
            } else if (token.role === 'ADMIN') {
                // ADMIN can see all attributes of all users
                headers = ['Username', 'Email', 'Full Name', 'Role', 'State', 'Profile', 'Actions'];
            }
            
            headers.forEach(header => {
                const th = document.createElement('th');
                th.textContent = header;
                headerRow.appendChild(th);
            });
            thead.appendChild(headerRow);
            table.appendChild(thead);
            
            // Create body
            const tbody = document.createElement('tbody');
            filteredUsers.forEach(user => {
                console.log('Processing user:', user);
                const row = document.createElement('tr');
                
                // Create cells for user data based on role
                let cells = [];
                
                if (token.role === 'ENDUSER') {
                    // ENDUSER can only see username, email, and full name
                    cells = [
                        user.username || 'NOT DEFINED',
                        user.email || 'NOT DEFINED',
                        user.fullName || 'NOT DEFINED'
                    ];
                } else if (token.role === 'BACKOFFICE') {
                    // BACKOFFICE can see all attributes of ENDUSER accounts
                    cells = [
                        user.username || 'NOT DEFINED',
                        user.email || 'NOT DEFINED',
                        user.fullName || 'NOT DEFINED',
                        user.role || 'NOT DEFINED',
                        user.accountState || 'NOT DEFINED',
                        user.profile || 'NOT DEFINED'
                    ];
                } else if (token.role === 'ADMIN') {
                    // ADMIN can see all attributes of all users
                    cells = [
                        user.username || 'NOT DEFINED',
                        user.email || 'NOT DEFINED',
                        user.fullName || 'NOT DEFINED',
                        user.role || 'NOT DEFINED',
                        user.accountState || 'NOT DEFINED',
                        user.profile || 'NOT DEFINED'
                    ];
                }
                
                cells.forEach(cellData => {
                    const td = document.createElement('td');
                    td.textContent = cellData;
                    row.appendChild(td);
                });
                
                // Add action buttons for BACKOFFICE and ADMIN
                if (token.role === 'BACKOFFICE' || token.role === 'ADMIN') {
                    const actionsCell = document.createElement('td');
                    const actionsDiv = document.createElement('div');
                    actionsDiv.className = 'action-buttons';
                    
                    // Change Role button
                    const changeRoleBtn = document.createElement('button');
                    changeRoleBtn.className = 'action-button change-role';
                    changeRoleBtn.textContent = 'Change Role';
                    changeRoleBtn.addEventListener('click', () => {
                        document.getElementById('change-role-username').value = user.username;
                        document.getElementById('change-role-new-role').value = user.role || '';
                    });
                    
                    // Change State button
                    const changeStateBtn = document.createElement('button');
                    changeStateBtn.className = 'action-button change-state';
                    changeStateBtn.textContent = 'Change State';
                    changeStateBtn.addEventListener('click', () => {
                        document.getElementById('change-state-username').value = user.username;
                        document.getElementById('change-state-new-state').value = user.accountState || '';
                    });
                    
                    // Remove User button
                    const removeUserBtn = document.createElement('button');
                    removeUserBtn.className = 'action-button remove';
                    removeUserBtn.textContent = 'Remove';
                    removeUserBtn.addEventListener('click', () => {
                        document.getElementById('remove-user-username').value = user.username;
                    });
                    
                    actionsDiv.appendChild(changeRoleBtn);
                    actionsDiv.appendChild(changeStateBtn);
                    actionsDiv.appendChild(removeUserBtn);
                    actionsCell.appendChild(actionsDiv);
                    row.appendChild(actionsCell);
                }
                
                tbody.appendChild(row);
            });
            table.appendChild(tbody);
            
            userList.appendChild(table);
        } catch (error) {
            console.error('Error updating user list:', error);
            this.showMessage(error.message, true);
        }
    }
};

// Event handlers
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM content loaded, initializing event handlers');
    const registerForm = document.getElementById('register-form');
    const loginForm = document.getElementById('login-form');
    const optionalFieldsToggle = document.getElementById('optionalFieldsToggle');
    const optionalFields = document.getElementById('optionalFields');
    const showRegisterLink = document.getElementById('show-register');
    const showLoginLink = document.getElementById('show-login');
    const logoutButton = document.getElementById('logoutButton');
    const changeRoleForm = document.getElementById('change-role-form');
    const changeStateForm = document.getElementById('change-state-form');
    const removeUserForm = document.getElementById('remove-user-form');
    
    console.log('Form elements:', {
        registerForm: registerForm ? 'found' : 'not found',
        loginForm: loginForm ? 'found' : 'not found',
        optionalFieldsToggle: optionalFieldsToggle ? 'found' : 'not found',
        optionalFields: optionalFields ? 'found' : 'not found',
        showRegisterLink: showRegisterLink ? 'found' : 'not found',
        showLoginLink: showLoginLink ? 'found' : 'not found',
        logoutButton: logoutButton ? 'found' : 'not found',
        changeRoleForm: changeRoleForm ? 'found' : 'not found',
        changeStateForm: changeStateForm ? 'found' : 'not found',
        removeUserForm: removeUserForm ? 'found' : 'not found'
    });
    
    if (optionalFieldsToggle) {
        optionalFieldsToggle.addEventListener('click', () => {
            optionalFields.classList.toggle('hidden');
            optionalFieldsToggle.textContent = optionalFields.classList.contains('hidden') 
                ? 'Mostrar campos opcionais' 
                : 'Ocultar campos opcionais';
        });
    }
    
    if (showRegisterLink) {
        showRegisterLink.addEventListener('click', (e) => {
            e.preventDefault();
            UI.showRegisterForm();
        });
    }
    
    if (showLoginLink) {
        showLoginLink.addEventListener('click', (e) => {
            e.preventDefault();
            UI.showLoginForm();
        });
    }
    
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            try {
                const formData = new FormData(registerForm);
                const userData = {
                    username: formData.get('username'),
                    email: formData.get('email'),
                    password: formData.get('password'),
                    fullName: formData.get('fullName'),
                    phone: formData.get('phone'),
                    profile: formData.get('profile'),
                    citizenCardNumber: formData.get('citizenCardNumber') || undefined,
                    nif: formData.get('nif') || undefined,
                    employer: formData.get('employer') || undefined,
                    jobTitle: formData.get('jobTitle') || undefined,
                    address: formData.get('address') || undefined,
                    employerNif: formData.get('employerNif') || undefined,
                    photoUrl: formData.get('photoUrl') || undefined
                };
                
                const confirmPassword = formData.get('confirmPassword');
                
                await ApiClient.register(userData, confirmPassword);
                UI.showMessage('Registro realizado com sucesso!', false);
                registerForm.reset();
                UI.showLoginForm();
            } catch (error) {
                UI.showMessage(error.message, true);
            }
        });
    }
    
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            try {
                const formData = new FormData(loginForm);
                const loginData = {
                    username: formData.get('username'),
                    password: formData.get('password')
                };
                
                const token = await ApiClient.login(loginData);
                TokenManager.setToken(token);
                UI.updateAuthUI();
                UI.showMessage('Login realizado com sucesso!', false);
                loginForm.reset();
            } catch (error) {
                UI.showMessage(error.message, true);
            }
        });
    }
    
    if (logoutButton) {
        console.log('Adding logout button event listener');
        logoutButton.addEventListener('click', () => {
            console.log('Logout button clicked');
            ApiClient.logout();
            UI.updateAuthUI();
            UI.showMessage('Logout realizado com sucesso!', false);
        });
    }
    
    if (changeRoleForm) {
        console.log('Adding change role form event listener');
        changeRoleForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            try {
                const formData = new FormData(changeRoleForm);
                const roleData = {
                    username: formData.get('username'),
                    newRole: formData.get('newRole')
                };
                
                await ApiClient.changeRole(roleData);
                UI.showMessage('Role changed successfully!', false);
                changeRoleForm.reset();
                
                // Refresh user list
                const users = await ApiClient.listUsers();
                UI.updateUserList(users);
            } catch (error) {
                UI.showMessage(error.message, true);
            }
        });
    }
    
    if (changeStateForm) {
        console.log('Adding change state form event listener');
        changeStateForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            try {
                const formData = new FormData(changeStateForm);
                const stateData = {
                    username: formData.get('username'),
                    newState: formData.get('newState')
                };
                
                await ApiClient.changeAccountState(stateData);
                UI.showMessage('Account state changed successfully!', false);
                changeStateForm.reset();
                
                // Refresh user list
                const users = await ApiClient.listUsers();
                UI.updateUserList(users);
            } catch (error) {
                UI.showMessage(error.message, true);
            }
        });
    }
    
    if (removeUserForm) {
        console.log('Adding remove user form event listener');
        removeUserForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            try {
                const formData = new FormData(removeUserForm);
                const username = formData.get('username');
                
                await ApiClient.removeUser(username);
                UI.showMessage('User removed successfully!', false);
                removeUserForm.reset();
                
                // Refresh user list
                const users = await ApiClient.listUsers();
                UI.updateUserList(users);
            } catch (error) {
                UI.showMessage(error.message, true);
            }
        });
    }
    
    // Initialize UI based on authentication state
    console.log('Initializing UI based on authentication state');
    UI.updateAuthUI();
}); 