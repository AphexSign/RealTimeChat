class UIComponent {
    constructor() {
        this.elements = {
            status: document.getElementById('status'),
            authSection: document.getElementById('authSection'),
            chatSection: document.getElementById('chatSection'),
            messages: document.getElementById('messages'),
            messageInput: document.getElementById('messageInput'),
            authError: document.getElementById('authError'),
            username: document.getElementById('username'),
            password: document.getElementById('password'),
            loginBtn: document.getElementById('loginBtn'),
            registerBtn: document.getElementById('registerBtn'),
            sendBtn: document.getElementById('sendBtn')
        };

        this.rateLimitActive = false;
        this.rateLimitTimer = null;
        this.rateLimitMessage = null;
    }

    showAuthSection() {
        this.clearRateLimit();          // <-- add this
        this.elements.authSection.style.display = 'block';
        this.elements.chatSection.style.display = 'none';
        this.updateStatus('Offline');
    }

    showChatSection() {
        this.clearRateLimit();          // <-- add this
        this.elements.authSection.style.display = 'none';
        this.elements.chatSection.style.display = 'flex';
    }

    updateStatus(status) {
        if (this.elements.status) {
            this.elements.status.textContent = status;
        }
    }

    showMessage(messageData) {
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message';
        
        const time = Helpers.formatTime(messageData.timestamp);
        const isCurrentUser = messageData.sender === authService.getCurrentUser();
        
        messageDiv.innerHTML = `
            <div class="sender" style="color: ${isCurrentUser ? '#764ba2' : '#667eea'}">
                ${isCurrentUser ? 'You' : Helpers.escapeHtml(messageData.sender)}
            </div>
            <div class="text">${Helpers.escapeHtml(messageData.text)}</div>
            <div class="time">${time}</div>
        `;
        
        if (isCurrentUser) {
            messageDiv.style.backgroundColor = '#f0f0ff';
        }
        
        this.elements.messages.appendChild(messageDiv);
        this.scrollToBottom();
    }

    showSystemMessage(systemData) {
        const messageDiv = document.createElement('div');
        messageDiv.className = 'system-message';
        
        const action = systemData.type === 'USER_JOINED' ? 'joined' : 'left';
        messageDiv.textContent = `${systemData.username} ${action} the chat (${systemData.onlineCount} online)`;
        
        this.elements.messages.appendChild(messageDiv);
        this.scrollToBottom();
    }

    showError(message, isAuthError = true) {
        const errorDiv = isAuthError ? this.elements.authError : document.createElement('div');
        
        if (!isAuthError) {
            errorDiv.className = 'error';
            this.elements.messages.appendChild(errorDiv);
        }
        
        errorDiv.innerHTML = `<div class="error">${Helpers.escapeHtml(message)}</div>`;
        
        if (!isAuthError) {
            setTimeout(() => {
                if (errorDiv.parentNode) {
                    errorDiv.parentNode.removeChild(errorDiv);
                }
            }, 5000);
        } else {
            setTimeout(() => {
                errorDiv.innerHTML = '';
            }, 5000);
        }
    }

    clearMessages() {
        this.elements.messages.innerHTML = '';
    }

    clearInputs() {
        if (this.elements.username) this.elements.username.value = '';
        if (this.elements.password) this.elements.password.value = '';
        if (this.elements.messageInput) this.elements.messageInput.value = '';
    }

    getMessageInput() {
        return this.elements.messageInput ? this.elements.messageInput.value.trim() : '';
    }

    clearMessageInput() {
        if (this.elements.messageInput) {
            this.elements.messageInput.value = '';
        }
    }

    focusMessageInput() {
        if (this.elements.messageInput) {
            this.elements.messageInput.focus();
        }
    }

    scrollToBottom() {
        if (this.elements.messages) {
            this.elements.messages.scrollTop = this.elements.messages.scrollHeight;
        }
    }

    showLoading(message = 'Loading...') {
        const loadingDiv = document.createElement('div');
        loadingDiv.className = 'loading';
        loadingDiv.textContent = message;
        this.elements.messages.appendChild(loadingDiv);
    }

    showTypingIndicator(username) {
        let indicator = document.getElementById('typing-indicator');
        
        if (!indicator) {
            indicator = document.createElement('div');
            indicator.id = 'typing-indicator';
            indicator.className = 'typing-indicator';
            this.elements.messages.appendChild(indicator);
        }
        
        indicator.textContent = `${username} is typing...`;
    }

    hideTypingIndicator() {
        const indicator = document.getElementById('typing-indicator');
        if (indicator && indicator.parentNode) {
            indicator.parentNode.removeChild(indicator);
        }
    }

    enableInputs() {
        if (this.elements.messageInput) this.elements.messageInput.disabled = false;
        if (this.elements.sendBtn) this.elements.sendBtn.disabled = false;
    }

    disableInputs() {
        if (this.elements.messageInput) this.elements.messageInput.disabled = true;
        if (this.elements.sendBtn) this.elements.sendBtn.disabled = true;
    }


    showRateLimitWarning(seconds) {
        // Сбрасываем предыдущее предупреждение, если оно активно
        this.clearRateLimit();

        // Блокируем поле ввода и кнопку отправки
        this.disableInputs();

        // Создаём элемент предупреждения
        const warningDiv = document.createElement('div');
        warningDiv.className = 'rate-limit-warning';
        this.elements.messages.appendChild(warningDiv);
        this.rateLimitMessage = warningDiv;

        const updateMessage = () => {
            if (seconds <= 0) {
                warningDiv.textContent = '✅ Можно снова отправлять сообщения.';
                setTimeout(() => {
                    if (warningDiv.parentNode) {
                        warningDiv.parentNode.removeChild(warningDiv);
                    }
                    this.rateLimitMessage = null;
                }, 2000);
                this.enableInputs();
                if (this.rateLimitTimer) {
                    clearInterval(this.rateLimitTimer);
                    this.rateLimitTimer = null;
                }
            } else {
                warningDiv.textContent = `⏳ Слишком много сообщений. Подождите ${seconds} сек.`;
            }
        };

        updateMessage();

        this.rateLimitTimer = setInterval(() => {
            seconds--;
            updateMessage();
        }, 1000);
    }

    clearRateLimit() {
        if (this.rateLimitTimer) {
            clearInterval(this.rateLimitTimer);
            this.rateLimitTimer = null;
        }
        if (this.rateLimitMessage && this.rateLimitMessage.parentNode) {
            this.rateLimitMessage.parentNode.removeChild(this.rateLimitMessage);
            this.rateLimitMessage = null;
        }
        this.enableInputs();
    }
}
const ui = new UIComponent();