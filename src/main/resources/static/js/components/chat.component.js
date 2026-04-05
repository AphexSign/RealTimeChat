class ChatComponent {
    constructor() {
        this.ui = ui;
        this.typingTimeout = null;
        this.lastTypingTime = 0;
        this.bindEvents();
    }

    bindEvents() {
        if (ui.elements.sendBtn) {
            ui.elements.sendBtn.addEventListener('click', () => this.sendMessage());
        }

        if (ui.elements.messageInput) {
            ui.elements.messageInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });

            ui.elements.messageInput.addEventListener('input', 
                Helpers.debounce(() => this.handleTyping(), 500)
            );
        }
    }

    init() {
        webSocketService.subscribe('/topic/chat', (message) => {
            if (message.type === 'USER_JOINED' || message.type === 'USER_LEFT') {
                ui.showSystemMessage(message);
            } else {
                ui.showMessage(message);
            }
        });

        webSocketService.subscribe('/user/queue/messages', (message) => {
            this.handlePrivateMessage(message);
        });

        webSocketService.subscribe('/user/queue/errors', (error) => {
            const errorMessage = error.message || error.error || error.description || JSON.stringify(error);

            if (errorMessage.includes('Too many messages')) {
                const match = errorMessage.match(/Retry after (\d+) seconds/);
                const retrySeconds = match ? parseInt(match[1]) : 3;
                ui.showRateLimitWarning(retrySeconds);
            } else {
                // Все остальные ошибки показываем как обычно
                ui.showError(errorMessage, false);
            }
        });

        webSocketService.subscribe('/topic/typing', (typingData) => {
            if (typingData.username !== authService.getCurrentUser()) {
                this.showTypingIndicator(typingData.username);
            }
        });
        
        ui.focusMessageInput();
    }

    sendMessage() {
        const text = ui.getMessageInput();
        
        if (!text || !webSocketService.isActive()) {
            return;
        }
        
        try {
            const messageId = webSocketService.send('/app/chat.send', { text });
            console.log('Message sent with ID:', messageId);
            
            ui.clearMessageInput();
            this.stopTyping();
            
        } catch (error) {
            console.error('Error sending message:', error);
            ui.showError('Failed to send message', false);
        }
    }

    handlePrivateMessage(message) {
        console.log('Private message received:', message);

        const privateMessage = { ...message, isPrivate: true };
        ui.showMessage(privateMessage);
    }

    handleTyping() {
        const text = ui.getMessageInput();
        
        if (text && webSocketService.isActive()) {
            const now = Date.now();

            if (now - this.lastTypingTime > 1000) {
                webSocketService.send('/app/typing', { 
                    isTyping: true 
                });
                this.lastTypingTime = now;
            }

            clearTimeout(this.typingTimeout);
            this.typingTimeout = setTimeout(() => {
                this.stopTyping();
            }, 2000);
        }
    }

    stopTyping() {
        if (webSocketService.isActive()) {
            webSocketService.send('/app/typing', { 
                isTyping: false 
            });
        }
        
        clearTimeout(this.typingTimeout);
        ui.hideTypingIndicator();
    }

    showTypingIndicator(username) {
        ui.showTypingIndicator(username);

        setTimeout(() => {
            ui.hideTypingIndicator();
        }, 3000);
    }

    clearChat() {
        ui.clearMessages();
    }

    disconnect() {
        this.stopTyping();
        webSocketService.disconnect();
    }
}

const chatComponent = new ChatComponent();