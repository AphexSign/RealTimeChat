package io.ylab.chat.aop.annotation;

import io.ylab.chat.aop.ChatGuardAspect;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Establishes user context before method execution and clears it afterwards. When this annotation
 * is applied, the {@link ChatGuardAspect} extracts the currently authenticated user from
 * {@code SecurityContextHolder}, validates that the user is authenticated (not anonymous), and sets
 * the username into {@code UserContext}. After the method completes, the user context is cleared to
 * avoid leakage across threads.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WithUserContext {

}