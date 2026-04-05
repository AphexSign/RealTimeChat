package io.ylab.chat.aop.annotation;

import io.ylab.chat.aop.ChatGuardAspect;
import io.ylab.chat.concurrency.MessageIdRegistry;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method should be idempotent with respect to message processing. When this
 * annotation is applied, the {@link ChatGuardAspect} ensures that the same message (identified by
 * its {@code messageId}) is processed only once. If the method's first parameter is a
 * {@code ChatMessageDto}, the aspect automatically generates a {@code messageId} if missing and
 * uses {@link MessageIdRegistry} to detect duplicates. Duplicate messages are silently ignored (the
 * method is not invoked).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

}