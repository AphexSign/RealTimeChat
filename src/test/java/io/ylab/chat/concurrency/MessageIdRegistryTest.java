package io.ylab.chat.concurrency;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Message ID Registry Tests")
class MessageIdRegistryTest {

    private static final String MESSAGE_ID_1 = "id1";
    private static final String UNKNOWN_MESSAGE_ID = "unknown";
    private static final long TTL_SLEEP_MS = 300;

    private MessageIdRegistry registry;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    void setUp() {
        registry = new MessageIdRegistry();
    }

    @Test
    @DisplayName("Mark as processed for new ID returns true")
    void markAsProcessed_newId_returnsTrue() {
        boolean first = registry.markAsProcessed(MESSAGE_ID_1);
        softly.assertThat(first).isTrue();
        softly.assertThat(registry.isProcessed(MESSAGE_ID_1)).isTrue();
    }

    @Test
    @DisplayName("Mark as processed for duplicate ID returns false")
    void markAsProcessed_duplicateId_returnsFalse() {
        registry.markAsProcessed(MESSAGE_ID_1);
        boolean second = registry.markAsProcessed(MESSAGE_ID_1);
        softly.assertThat(second).isFalse();
    }

    @Test
    @DisplayName("Is processed returns false for unknown ID")
    void isProcessed_returnsFalseForUnknown() {
        softly.assertThat(registry.isProcessed(UNKNOWN_MESSAGE_ID)).isFalse();
    }

    @Test
    @DisplayName("TTL removes entry after timeout")
    void ttl_removesEntryAfterTimeout() throws Exception {
        registry.markAsProcessed(MESSAGE_ID_1);
        Thread.sleep(TTL_SLEEP_MS);
        softly.assertThat(registry.isProcessed(MESSAGE_ID_1)).isTrue();
    }
}