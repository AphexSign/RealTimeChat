# Real-Time Chat Application

Серверное приложение группового чата на **Spring Boot** с использованием **WebSocket (STOMP)**, многопоточности, AOP, Redis, Bucket4j и Resilience4j.

## 🛠 Технологии

- **Java 17**
- **Spring Boot 3.2**
- **Spring WebSocket + STOMP**
- **Spring Security + JWT**
- **Spring Data JPA** (H2 / PostgreSQL)
- **Spring AOP + AspectJ**
- **Redis** (Bucket4j + Dead Letter Stream)
- **Bucket4j** (rate limiting через Lettuce)
- **Resilience4j** (Circuit Breaker)
- **Micrometer** (метрики)
- **Lombok**
- **Testcontainers + JUnit 5**

## 🏗 Архитектура

Проект построен по **многослойной архитектуре** с чётким разделением ответственности и выносом сквозной логики в AOP.

```text
┌──────────────────┐
│ Web / WS Layer   │ ← @Controller, @MessageMapping, WebSocketEventListener
├──────────────────┤
│   Service Layer  │ ← ChatService, UserService, MessagePersistenceService
│   (обёрнута AOP) │
├──────────────────┤
│ Cross‑cutting    │ ← @WithUserContext, @RateLimited, @Idempotent,
│   (Aspects)      │   @CircuitBreaker, @Async
├──────────────────┤
│ Infrastructure   │ ← ThreadPool, Redis, MeterRegistry, DLQ
├──────────────────┤
│ Persistence      │ ← JPA + Redis Streams (DLQ)
└──────────────────┘
Структура пакетов
io.ylab.chat
├── aop/                 # ChatGuardAspect, DegradationAspect
├── config/              # Security, WebSocket, Async, RateLimiter, Redis
├── controller/          # REST (Auth, Metrics)
├── websocket/           # ChatWebSocketController, WebSocketEventListener
├── service/             # ChatService, UserService, MessagePersistenceService, DLQ
├── context/             # UserContext (ThreadLocal)
├── concurrency/         # MessageIdRegistry, OnlineUserRegistry
├── entity/              # MessageEntity, UserEntity
├── repository/          # JPA репозитории
├── dto/                 # ChatMessageDto, AuthRequest, AuthResponse
├── exception/           # Кастомные исключения
└── util/                # JwtUtil, TimeProvider, RedisConstants
```
### Основная функциональность
- Аутентификация через JWT (REST + WebSocket)
- Групповой чат (одна общая комната с broadcast)
- Отображение онлайн-статуса пользователей
- Идемпотентность сообщений (защита от дублей, TTL 5 мин)
- Rate limiting сообщений через Bucket4j + Redis
- Асинхронное сохранение сообщений в БД (@Async + отдельный пул потоков)
### Отказоустойчивость:
- Circuit Breaker (Resilience4j)
- Degraded Mode (чат работает даже при падении БД)
- Dead Letter Queue на Redis Streams

Сбор метрик через Micrometer

🚀 Установка и запуск
Требования

JDK 17
Maven 3.6+
Redis (локально или Docker)

Запуск Redis через Docker
```
docker run -d -p 6379:6379 --name chat-redis redis:7-alpine
```
Запуск приложения
```
mvn clean spring-boot:run
```
Приложение будет доступно по адресу: http://localhost:8080
H2 Console (для разработки):
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:chatdb
Конфигурация (application.yml)
```YAML
jwt:
  secret: base64Encoded512bitKey...
  expiration: 3600000   # 1 час

rate-limiter:
  messages:
    capacity: 5
    refill-tokens: 5
    refill-duration-seconds: 60

spring:
  data:
    redis:
      host: localhost
      port: 6379
```
📡 API и WebSocket
REST Endpoints

| Метод | URL                    | Описание                          | Аутентификация      |
|-------|------------------------|-----------------------------------|---------------------|
| POST  | `/register`            | Регистрация пользователя          | Нет                 |
| POST  | `/login`               | Авторизация → JWT токен           | Нет                 |
| GET   | `/metrics/online`      | Список онлайн пользователей       | Bearer JWT          |
| POST  | `/metrics/recover`     | Выход из degraded mode            | Bearer JWT          |


WebSocket (STOMP)

Endpoint: /ws (с поддержкой SockJS)
Подписка на сообщения: /topic/chat
Отправка сообщений: /app/chat.send
Приватные ошибки: /user/queue/errors
```

Пример подключения (JavaScript):
```js
JavaScriptconst token = 'your-jwt-token';
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({ Authorization: `Bearer ${token}` }, () => {
    stompClient.subscribe('/topic/chat', (msg) => {
        console.log('Received:', JSON.parse(msg.body));
    });

    stompClient.send('/app/chat.send', {}, JSON.stringify({
        text: "Привет всем!"
    }));
});
```
Формат сообщения (ChatMessageDto):
```JSON
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "sender": "john",
  "text": "Hello, world!",
  "timestamp": 1706473200000
}
```
🧪 Тестирование
```
mvn test
```
Основные тесты:

ChatGuardAspectTest — проверка AOP-аннотаций
MessagePersistenceFallbackTest — Circuit Breaker + DLQ
DegradationAspectTest, OnlineUserRegistryTest и др.


🎯 Аспекты (AOP)
| Аспект              | Аннотация                  | Order | Ответственность                                      |
|---------------------|----------------------------|-------|------------------------------------------------------|
| ChatGuardAspect     | `@WithUserContext`         | 10    | Установка UserContext + проверка аутентификации      |
| ChatGuardAspect     | `@Idempotent`              | 10    | Защита от дублирования сообщений                     |
| ChatGuardAspect     | `@RateLimited`             | 10    | Rate limiting через Bucket4j + Redis                 |
| DegradationAspect   | `execution(* ...saveAsync)`| 4     | Переход в degraded mode при ошибках сохранения       |


⚙️ Многопоточность и отказоустойчивость

Асинхронное сохранение сообщений через @Async и отдельный ThreadPoolTaskExecutor (core=8, max=16)
Dead Letter Queue — Redis Stream chat:dead-letter:stream
Degraded Mode — при 5 последовательных ошибках сохранения чат продолжает работать без записи в БД
Circuit Breaker + fallback в DLQ

📊 Мониторинг и метрики
Доступны метрики Micrometer:

websocket.messages.sent
websocket.connections.total
websocket.online.users (Gauge)

Эндпоинт: GET /metrics (только для авторизованных пользователей)
