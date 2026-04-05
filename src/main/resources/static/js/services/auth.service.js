class AuthService {
    constructor() {
        this.token = localStorage.getItem('chatToken');
        this.currentUser = localStorage.getItem('chatUser');
        this.isAuthenticated = !!this.token;
    }

    async login(username, password) {
        try {
            if (!Helpers.validateUsername(username)) {
                throw new Error('Username must be between 3 and 20 characters');
            }
            
            if (!Helpers.validatePassword(password)) {
                throw new Error('Password must be at least 6 characters');
            }

            const data = await AuthApi.login(username, password);
            
            this.setAuthData(data.token, data.username);
            
            return {
                success: true,
                user: data.username
            };
        } catch (error) {
            console.error('Login service error:', error);
            return {
                success: false,
                error: error.message
            };
        }
    }

    async register(username, password) {
        try {
            if (!Helpers.validateUsername(username)) {
                throw new Error('Username must be between 3 and 20 characters');
            }
            
            if (!Helpers.validatePassword(password)) {
                throw new Error('Password must be at least 6 characters');
            }

            const data = await AuthApi.register(username, password);
            
            this.setAuthData(data.token, data.username);
            
            return {
                success: true,
                user: data.username
            };
        } catch (error) {
            console.error('Registration service error:', error);
            return {
                success: false,
                error: error.message
            };
        }
    }

    setAuthData(token, username) {
        this.token = token;
        this.currentUser = username;
        this.isAuthenticated = true;
        
        localStorage.setItem('chatToken', token);
        localStorage.setItem('chatUser', username);
    }

    clearAuthData() {
        this.token = null;
        this.currentUser = null;
        this.isAuthenticated = false;
        
        localStorage.removeItem('chatToken');
        localStorage.removeItem('chatUser');
    }

    getToken() {
        return this.token;
    }

    getCurrentUser() {
        return this.currentUser;
    }

    async checkAuth() {
        if (!this.token) {
            return false;
        }
        
        return await AuthApi.validateToken(this.token);
    }

    logout() {
        this.clearAuthData();
        AuthApi.logout().catch(console.error);
    }
}

const authService = new AuthService();