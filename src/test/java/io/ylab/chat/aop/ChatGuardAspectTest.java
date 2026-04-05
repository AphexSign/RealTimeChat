package io.ylab.chat.aop;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.ylab.chat.concurrency.MessageIdRegistry;
import io.ylab.chat.context.UserContext;
import io.ylab.chat.dto.ChatMessageDto;
import io.ylab.chat.exception.UnauthorizedException;
import java.util.function.Supplier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
@DisplayName("Chat Guard Aspect Tests")
class ChatGuardAspectTest {

    private static final String TEST_USER = "testUser";
    private static final String UNAUTHORIZED_MESSAGE = "User not authenticated";
    private static final String DUPLICATE_MESSAGE_ID = "dup";
    private static final String PROCEED_RESULT = "ok";
    private static final String EXPECTED_METHOD_RESULT = "result";

    @Mock
    private ProxyManager<byte[]> bucketProxyManager;

    @Mock
    private Supplier<BucketConfiguration> bucketConfigurationSupplier;

    @Mock
    private MessageIdRegistry messageIdRegistry;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectSoftAssertions
    private SoftAssertions softly;

    private ChatGuardAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new ChatGuardAspect(bucketProxyManager, bucketConfigurationSupplier,
            messageIdRegistry);
        SecurityContextHolder.clearContext();
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        UserContext.clear();
    }

    @Test
    @DisplayName("Authenticated user - sets context and proceeds")
    void withUserContext_authenticated_setsContextAndProceeds() throws Throwable {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(TEST_USER);
        when(auth.getName()).thenReturn(TEST_USER);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(joinPoint.proceed()).thenReturn(EXPECTED_METHOD_RESULT);

        Object result = aspect.withUserContext(joinPoint);

        verify(joinPoint).proceed();
        softly.assertThat(result).isEqualTo(EXPECTED_METHOD_RESULT);
    }

    @Test
    @DisplayName("Unauthenticated user - throws UnauthorizedException")
    void withUserContext_unauthenticated_throwsUnauthorized() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(null);

        softly.assertThatThrownBy(() -> aspect.withUserContext(joinPoint))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage(UNAUTHORIZED_MESSAGE);
    }

    @Test
    @DisplayName("First message without messageId - generates ID and proceeds")
    void idempotent_firstMessage_withoutMessageId_generatesAndProceeds() throws Throwable {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessageId(null);

        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});
        when(messageIdRegistry.markAsProcessed(anyString())).thenReturn(true);
        when(joinPoint.proceed()).thenReturn(PROCEED_RESULT);

        Object result = aspect.idempotent(joinPoint);

        softly.assertThat(dto.getMessageId())
            .isNotNull()
            .isNotBlank();

        verify(messageIdRegistry).markAsProcessed(dto.getMessageId());
        softly.assertThat(result).isEqualTo(PROCEED_RESULT);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("Duplicate message ID - returns null and skips processing")
    void idempotent_duplicateMessage_returnsNull() throws Throwable {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessageId(DUPLICATE_MESSAGE_ID);
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});
        when(messageIdRegistry.markAsProcessed(DUPLICATE_MESSAGE_ID)).thenReturn(false);

        Object result = aspect.idempotent(joinPoint);

        softly.assertThat(result).isNull();
        verify(joinPoint, never()).proceed();
    }
}