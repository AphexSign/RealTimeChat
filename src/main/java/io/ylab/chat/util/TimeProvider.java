package io.ylab.chat.util;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * A simple utility component that provides the current time.
 */
@Component
public class TimeProvider {

    /**
     * Returns the current date and time from the system clock in the default time zone.
     *
     * @return the current {@link LocalDateTime}
     */
    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Returns the current time in milliseconds since the Unix epoch (January 1, 1970, 00:00:00
     * GMT).
     *
     * @return the current time in milliseconds
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}