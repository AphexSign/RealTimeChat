package io.ylab.chat.concurrency;

import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("Online User Registry Tests")
class OnlineUserRegistryTest {

    private static final String USERNAME_ALICE = "alice";
    private static final String USERNAME_BOB = "bob";
    private static final String USERNAME_CHARLIE = "charlie";
    private static final String SESSION_ID_1 = "sess1";
    private static final String SESSION_ID_2 = "s1";
    private static final String SESSION_ID_3 = "s2";

    private final OnlineUserRegistry registry = new OnlineUserRegistry();

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    @DisplayName("Register user and check online status")
    void registerAndCheckOnline() {
        registry.register(USERNAME_ALICE, SESSION_ID_1);
        softly.assertThat(registry.isOnline(USERNAME_ALICE)).isTrue();
        softly.assertThat(registry.getOnlineUsers()).containsExactly(USERNAME_ALICE);
        softly.assertThat(registry.getOnlineCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Unregister removes user from registry")
    void unregister_removesUser() {
        registry.register(USERNAME_ALICE, SESSION_ID_1);
        registry.unregister(USERNAME_ALICE);
        softly.assertThat(registry.isOnline(USERNAME_ALICE)).isFalse();
        softly.assertThat(registry.getOnlineUsers()).isEmpty();
    }

    @Test
    @DisplayName("Get online users returns a copy, not affected by modifications")
    void getOnlineUsers_returnsCopy() {
        registry.register(USERNAME_ALICE, SESSION_ID_2);
        registry.register(USERNAME_BOB, SESSION_ID_3);
        Set<String> users = registry.getOnlineUsers();
        users.add(USERNAME_CHARLIE);
        softly.assertThat(registry.getOnlineUsers()).containsOnly(USERNAME_ALICE, USERNAME_BOB);
    }
}