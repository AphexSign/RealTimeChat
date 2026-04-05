class AuthApi {
    static async register(username, password) {
        try {
            const response = await fetch('/register', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json' 
                },
                body: JSON.stringify({ 
                    username: Helpers.sanitizeInput(username), 
                    password 
                })
            });
            
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Registration failed: ${errorText}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Registration API error:', error);
            throw error;
        }
    }

    static async login(username, password) {
        try {
            const response = await fetch('/login', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json' 
                },
                body: JSON.stringify({ 
                    username: Helpers.sanitizeInput(username), 
                    password 
                })
            });
            
            if (!response.ok) {
                throw new Error('Invalid credentials');
            }
            
            return await response.json();
        } catch (error) {
            console.error('Login API error:', error);
            throw error;
        }
    }

    static async validateToken(token) {
        try {
            const response = await fetch('/validate-token', {
                method: 'POST',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                }
            });
            
            return response.ok;
        } catch (error) {
            console.error('Token validation error:', error);
            return false;
        }
    }

    static async logout() {
        try {
            const response = await fetch('/logout', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json' 
                }
            });
            
            return response.ok;
        } catch (error) {
            console.error('Logout API error:', error);
            throw error;
        }
    }
}