package io.ylab.chat.exception;

/**
 * Exception thrown to indicate that a rate limit has been exceeded.
 */
public class RateLimitExceededException extends RuntimeException {

    /**
     * Constructs a new {@code RateLimitExceededException} with the specified detail message.
     *
     * @param message the detail message explaining the reason for the rate limit being exceeded
     */
    public RateLimitExceededException(String message) {
        super(message);
    }
}
