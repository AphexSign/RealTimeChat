package io.ylab.chat.exception;

/**
 * Exception thrown to indicate that an operation was attempted without proper authorization.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Constructs a new {@code UnauthorizedException} with the specified detail message.
     *
     * @param message the detail message explaining the reason for the unauthorized access
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
