class WebSocketService {
    constructor() {
        this.stompClient = null;
        this.isConnected = false;
        this.subscriptions = new Map();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
    }

    connect(token, onConnect, onError) {
        return new Promise((resolve, reject) => {
            try {
                const socket = new SockJS('/ws');
                this.stompClient = Stomp.over(socket);

                this.stompClient.debug = null;
                
                const headers = {
                    'Authorization': `Bearer ${token}`
                };
                
                this.stompClient.connect(
                    headers,
                    (frame) => {
                        this.isConnected = true;
                        this.reconnectAttempts = 0;
                        
                        console.log('WebSocket connected:', frame);
                        
                        if (onConnect) onConnect(frame);
                        resolve(frame);
                    },
                    (error) => {
                        console.error('WebSocket connection error:', error);
                        this.isConnected = false;
                        
                        if (onError) onError(error);
                        

                        if (this.reconnectAttempts < this.maxReconnectAttempts) {
                            this.reconnectAttempts++;
                            console.log(`Reconnecting in ${this.reconnectDelay}ms... Attempt ${this.reconnectAttempts}`);
                            
                            setTimeout(() => {
                                this.connect(token, onConnect, onError)
                                    .then(resolve)
                                    .catch(reject);
                            }, this.reconnectDelay);
                        } else {
                            reject(new Error('Max reconnection attempts reached'));
                        }
                    }
                );
                
            } catch (error) {
                console.error('WebSocket initialization error:', error);
                reject(error);
            }
        });
    }

    subscribe(destination, callback) {
        if (!this.stompClient || !this.isConnected) {
            throw new Error('WebSocket is not connected');
        }
        
        const subscription = this.stompClient.subscribe(destination, (message) => {
            try {
                const data = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error('Error parsing WebSocket message:', error);
            }
        });
        
        this.subscriptions.set(destination, subscription);
        
        return subscription;
    }

    unsubscribe(destination) {
        const subscription = this.subscriptions.get(destination);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(destination);
        }
    }

    send(destination, message, headers = {}) {
        if (!this.stompClient || !this.isConnected) {
            throw new Error('WebSocket is not connected');
        }
        
        const messageId = Helpers.generateUUID();
        const messageWithId = {
            ...message,
            messageId,
            timestamp: Date.now()
        };

        const sendHeaders = {
            ...headers,
            'Authorization': `Bearer ${authService.getToken()}`
        };
        
        this.stompClient.send(destination, sendHeaders, JSON.stringify(messageWithId));
        
        return messageId;
    }

    sendToUser(username, destination, message) {
        const userDestination = `/user/${username}/${destination}`;
        return this.send(userDestination, message);
    }

    disconnect() {
        if (this.stompClient) {

            this.subscriptions.forEach((subscription, destination) => {
                subscription.unsubscribe();
            });
            this.subscriptions.clear();
            
            this.stompClient.disconnect(() => {
                console.log('WebSocket disconnected');
            });
            
            this.stompClient = null;
            this.isConnected = false;
        }
    }

    getConnectionStatus() {
        return this.isConnected ? 'Connected' : 'Disconnected';
    }

    isActive() {
        return this.isConnected && this.stompClient !== null;
    }
}

const webSocketService = new WebSocketService();