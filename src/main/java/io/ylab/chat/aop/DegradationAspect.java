package io.ylab.chat.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aspect for handling system degradation in case of repeated failures in message persistence.
 */
@Slf4j
@Aspect
@Order(4)
@Component
public class DegradationAspect {

    /**
     * Flag indicating whether the system is in degraded mode.
     */
    private final AtomicBoolean degradedMode = new AtomicBoolean(false);

    /**
     * Counter for tracking consecutive failures in message persistence.
     */
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * The threshold for consecutive failures before entering degraded mode.
     */
    private static final int FAILURE_THRESHOLD = 5;

    /**
     * Around advice for handling failures in the `saveAsync` method of
     * {@code MessagePersistenceService}.
     *
     * @param joinPoint the {@link ProceedingJoinPoint} representing the intercepted method
     * @return the result of the method execution, or {@code null} if persistence is skipped
     * @throws Throwable if an error occurs during method execution
     */
    @Around("execution(* io.ylab.chat.service.MessagePersistenceService.saveAsync(..))")
    public Object handlePersistenceFailure(ProceedingJoinPoint joinPoint) throws Throwable {
        if (degradedMode.get()) {
            log.warn("System in degraded mode - skipping persistence");
            return null;
        }
        try {
            Object result = joinPoint.proceed();

            if (failureCount.get() > 0) {
                failureCount.set(0);
                log.info("Persistence recovered - failure count reset");
            }

            return result;

        } catch (RejectedExecutionException e) {
            handleFailure("Thread pool exhausted", e);
            return null;

        } catch (Exception e) {
            handleFailure("Database persistence failed", e);
            return null;
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void attemptRecovery() {
        if (degradedMode.get()) {
            try {
                if (checkDatabaseConnection()) {
                    recover();
                    log.info("Automatic recovery from degraded mode succeeded");
                }
            } catch (Exception e) {
                log.debug("Recovery check failed, staying in degraded mode");
            }
        }
    }

    private boolean checkDatabaseConnection() {
        return true;
    }

    /**
     * Handles a failure in message persistence.
     *
     * @param reason the reason for the failure
     * @param e      the exception that caused the failure
     */
    private void handleFailure(String reason, Exception e) {
        int failures = failureCount.incrementAndGet();

        log.error("{} (failure {}/{}): {}", reason, failures, FAILURE_THRESHOLD, e.getMessage());

        if (failures >= FAILURE_THRESHOLD && !degradedMode.get()) {
            degradedMode.set(true);
            log.error("ENTERING DEGRADED MODE - Chat continues without persistence");
        }
    }

    /**
     * Checks if the system is currently in degraded mode.
     *
     * @return {@code true} if the system is in degraded mode, {@code false} otherwise
     */
    public boolean isDegraded() {
        return degradedMode.get();
    }

    /**
     * Recovers the system from degraded mode.
     */
    public void recover() {
        degradedMode.set(false);
        failureCount.set(0);
        log.info("System recovered from degraded mode");
    }
}