class ChatApp {
    constructor() {
        this.init();
    }

    init() {
        console.log('ChatApp initializing...');

        if (authService.isAuthenticated) {
            console.log('User is authenticated, attempting auto-login...');
            authComponent.checkAutoLogin();
        }

        window.addEventListener('beforeunload', () => {
            this.cleanup();
        });

        window.addEventListener('online', () => {
            console.log('Network connection restored');
            if (authService.isAuthenticated && !webSocketService.isConnected) {
                authComponent.initWebSocket();
            }
        });
        
        window.addEventListener('offline', () => {
            console.log('Network connection lost');
            ui.updateStatus('Offline');
        });
        
        console.log('ChatApp initialized');
    }

    cleanup() {
        console.log('Cleaning up before unload...');
        chatComponent.disconnect();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
});