class AuthComponent {
    constructor() {
        this.ui = ui;
        this.bindEvents();
    }

    bindEvents() {
        if (ui.elements.loginBtn) {
            ui.elements.loginBtn.addEventListener('click', () => this.handleLogin());
        }
        
        if (ui.elements.registerBtn) {
            ui.elements.registerBtn.addEventListener('click', () => this.handleRegister());
        }

        if (ui.elements.username && ui.elements.password) {
            ui.elements.username.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') this.handleLogin();
            });
            
            ui.elements.password.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') this.handleLogin();
            });
        }
    }

    async handleLogin() {
        const username = ui.elements.username.value;
        const password = ui.elements.password.value;
        
        if (!username || !password) {
            ui.showError('Please enter both username and password');
            return;
        }
        
        ui.updateStatus('Connecting...');
        ui.disableInputs();
        
        try {
            const result = await authService.login(username, password);
            
            if (result.success) {
                console.log('Login successful, connecting WebSocket...');

                await this.connectWebSocket();
            } else {
                ui.showError(result.error);
                ui.updateStatus('Offline');
            }
        } catch (error) {
            ui.showError('Login failed. Please try again.');
            ui.updateStatus('Offline');
            console.error('Login error:', error);
        } finally {
            ui.enableInputs();
        }
    }

    async handleRegister() {
        const username = ui.elements.username.value;
        const password = ui.elements.password.value;
        
        if (!username || !password) {
            ui.showError('Please enter both username and password');
            return;
        }
        
        ui.updateStatus('Registering...');
        ui.disableInputs();
        
        try {
            const result = await authService.register(username, password);
            
            if (result.success) {
                console.log('Registration successful, connecting WebSocket...');

                await this.connectWebSocket();
            } else {
                ui.showError(result.error);
                ui.updateStatus('Offline');
            }
        } catch (error) {
            ui.showError('Registration failed. Please try again.');
            ui.updateStatus('Offline');
            console.error('Registration error:', error);
        } finally {
            ui.enableInputs();
        }
    }

    async connectWebSocket() {
        try {
            const token = authService.getToken();
            
            if (!token) {
                throw new Error('No authentication token');
            }
            
            ui.updateStatus('Connecting to chat...');
            

            await webSocketService.connect(
                token,
                (frame) => {
                    console.log('WebSocket connected:', frame);
                    ui.updateStatus('Connected');
                    ui.showChatSection();
                    

                    chatComponent.init();
                },
                (error) => {
                    console.error('WebSocket connection error:', error);
                    ui.showError('Connection failed: ' + error.message);
                    ui.updateStatus('Connection Failed');

                    authService.clearAuthData();
                }
            );
            
        } catch (error) {
            ui.showError('WebSocket connection error: ' + error.message);
            ui.updateStatus('Connection Failed');
            console.error('WebSocket init error:', error);
        }
    }

    logout() {
        authService.logout();
        webSocketService.disconnect();
        ui.showAuthSection();
        ui.clearMessages();
        ui.clearInputs();
    }

    checkAutoLogin() {
        if (authService.isAuthenticated) {
            authService.checkAuth().then(isValid => {
                if (isValid) {
                    console.log('Auto-login successful');
                    this.connectWebSocket();
                } else {
                    authService.clearAuthData();
                }
            }).catch(() => {
                authService.clearAuthData();
            });
        }
    }
}

const authComponent = new AuthComponent();