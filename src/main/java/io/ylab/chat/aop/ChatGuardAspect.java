package io.ylab.chat.aop;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.ylab.chat.aop.annotation.Idempotent;
import io.ylab.chat.aop.annotation.RateLimited;
import io.ylab.chat.aop.annotation.WithUserContext;
import io.ylab.chat.concurrency.MessageIdRegistry;
import io.ylab.chat.context.UserContext;
import io.ylab.chat.context.UserInfo;
import io.ylab.chat.dto.ChatMessageDto;
import io.ylab.chat.exception.RateLimitExceededException;
import io.ylab.chat.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Aspect that provides cross-cutting concerns for chat message processing.
 */
@Slf4j
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class ChatGuardAspect {

    private final ProxyManager<byte[]> bucketProxyManager;
    private final Supplier<BucketConfiguration> bucketConfigurationSupplier;
    private final MessageIdRegistry messageIdRegistry;

    /**
     * Intercepts methods annotated with {@link WithUserContext}. Retrieves the currently
     * authenticated user from the {@link SecurityContextHolder} and stores the username in
     * {@link UserContext}. After the method execution, the user context is cleared.
     *
     * @param jp the proceeding join point for the intercepted method
     * @return the result of the method invocation
     * @throws UnauthorizedException if no authentication is found, the user is not authenticated,
     *                               or the principal is the anonymous user
     * @throws Throwable             if the intercepted method throws an exception
     */
    @Around("@annotation(io.ylab.chat.aop.annotation.WithUserContext)")
    public Object withUserContext(ProceedingJoinPoint jp) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(
            auth.getPrincipal())) {
            throw new UnauthorizedException("User not authenticated");
        }

        String username = auth.getName();
        UserContext.setUserInfo(UserInfo.builder().username(username).build());

        try {
            return jp.proceed();
        } finally {
            UserContext.clear();
            log.debug("User context cleared for {}", username);
        }
    }

    /**
     * Intercepts methods annotated with {@link Idempotent}.
     * <p>
     * Ensures that the same message (identified by {@code messageId}) is processed only once. If
     * the first argument of the intercepted method is a {@link ChatMessageDto}, its message ID is
     * validated. A missing or empty message ID is generated automatically. The
     * {@link MessageIdRegistry} is consulted to check if the message has already been processed.
     *
     * @param jp the proceeding join point for the intercepted method
     * @return the result of the method invocation, or {@code null} if the message is a duplicate
     * @throws Throwable if the intercepted method throws an exception
     */
    @Around("@annotation(io.ylab.chat.aop.annotation.Idempotent)")
    public Object idempotent(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        if (args.length == 0 || !(args[0] instanceof ChatMessageDto dto)) {
            return jp.proceed();
        }

        String messageId = dto.getMessageId();
        if (messageId == null || messageId.trim().isEmpty()) {
            messageId = UUID.randomUUID().toString();
            dto.setMessageId(messageId);
        }

        if (!messageIdRegistry.markAsProcessed(messageId)) {
            log.warn("Duplicate message ignored: {}", messageId);
            return null;
        }
        return jp.proceed();
    }

    /**
     * Intercepts methods annotated with {@link RateLimited}. Applies rate limiting per user based
     * on a token bucket algorithm. The bucket key is derived from the current username (or
     * "anonymous" if none is set). If a token is successfully consumed, the method proceeds;
     * otherwise, a {@link ResponseStatusException} with HTTP 429 (Too Many Requests) is thrown.
     *
     * @param jp the proceeding join point for the intercepted method
     * @return the result of the method invocation if rate limit is not exceeded
     * @throws ResponseStatusException with {@code TOO_MANY_REQUESTS} when the rate limit is
     *                                 exceeded, including the recommended retry-after interval
     * @throws Throwable               if the intercepted method throws an exception
     */
    @Around("@annotation(io.ylab.chat.aop.annotation.RateLimited)")
    public Object rateLimited(ProceedingJoinPoint jp) throws Throwable {
        String username = UserContext.getCurrentUsername();
        if (username == null) {
            username = "anonymous";
        }

        String bucketKey = "rate_limit:chat:message:" + username;
        byte[] keyBytes = bucketKey.getBytes(StandardCharsets.UTF_8);

        Bucket bucket = bucketProxyManager.builder()
            .build(keyBytes, bucketConfigurationSupplier.get());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            log.debug("Allowed for {}, remaining: {}", username, probe.getRemainingTokens());
            return jp.proceed();
        }

        long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

        log.warn("Rate limit exceeded for {}, retry after {}s", username, waitSeconds);

        throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many messages. Retry after " + waitSeconds + " seconds",
            new RateLimitExceededException("Rate limit exceeded")
        );
    }
}