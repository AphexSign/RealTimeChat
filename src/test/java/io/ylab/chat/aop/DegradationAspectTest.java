package io.ylab.chat.aop;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.aspectj.lang.ProceedingJoinPoint;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
@DisplayName("Degradation Aspect Tests")
class DegradationAspectTest {

    private static final String DATABASE_ERROR_MESSAGE = "DB error";
    private static final String DEGRADED_MODE_FIELD_NAME = "degradedMode";
    private static final int FAILURE_THRESHOLD = 5;
    private static final int FAILURES_BEFORE_DEGRADED = 4;

    @InjectMocks
    private DegradationAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    @DisplayName("First four failures do not degrade, fifth failure enters degraded mode")
    void handlePersistenceFailure_firstFourFailures_enterDegradedAfterFifth() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new RuntimeException(DATABASE_ERROR_MESSAGE));

        for (int i = 1; i <= FAILURES_BEFORE_DEGRADED; i++) {
            Object result = aspect.handlePersistenceFailure(joinPoint);
            softly.assertThat(result).isNull();
            softly.assertThat(aspect.isDegraded()).isFalse();
        }

        Object result = aspect.handlePersistenceFailure(joinPoint);
        softly.assertThat(result).isNull();
        softly.assertThat(aspect.isDegraded()).isTrue();
    }

    @Test
    @DisplayName("In degraded mode, persistence operations are skipped")
    void degradedMode_skipPersistence() throws Throwable {
        Field degraded = DegradationAspect.class.getDeclaredField(DEGRADED_MODE_FIELD_NAME);
        degraded.setAccessible(true);
        AtomicBoolean degradedMode = (AtomicBoolean) degraded.get(aspect);
        degradedMode.set(true);

        Object result = aspect.handlePersistenceFailure(joinPoint);
        softly.assertThat(result).isNull();
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Recovery resets degraded flag and failure count")
    void recover_resetsDegradedAndFailureCount() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new RuntimeException());

        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            aspect.handlePersistenceFailure(joinPoint);
        }

        softly.assertThat(aspect.isDegraded()).isTrue();
        aspect.recover();
        softly.assertThat(aspect.isDegraded()).isFalse();
    }
}