package io.ylab.chat.aop.annotation;

import io.ylab.chat.aop.ChatGuardAspect;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies rate limiting to the annotated method. Methods marked with this annotation are protected
 * by a token bucket rate limiter, managed by the {@link ChatGuardAspect}. The rate limit is applied
 * per user (based on {@code UserContext.getCurrentUsername()}) using a configurable bucket from
 * {@code BucketConfigurationSupplier}. If the rate limit is exceeded, the method invocation is
 * blocked and an HTTP 429 (Too Many Requests) response is thrown.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimited {

}