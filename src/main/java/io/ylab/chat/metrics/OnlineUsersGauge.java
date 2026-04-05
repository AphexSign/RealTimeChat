package io.ylab.chat.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.ylab.chat.concurrency.OnlineUserRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer gauge that tracks the current number of online WebSocket users.
 *
 * @see OnlineUserRegistry
 * @see io.micrometer.core.instrument.Gauge
 * @see io.micrometer.core.instrument.MeterRegistry
 */
@Component
public class OnlineUsersGauge {

    private final OnlineUserRegistry registry;
    private final MeterRegistry meterRegistry;

    /**
     * Constructs an OnlineUsersGauge and registers the online users gauge metric.
     *
     * @param registry      the registry that tracks online user IDs and counts
     * @param meterRegistry the Micrometer registry where the gauge will be registered
     */
    public OnlineUsersGauge(OnlineUserRegistry registry, MeterRegistry meterRegistry) {
        this.registry = registry;
        this.meterRegistry = meterRegistry;

        Gauge.builder("websocket.online.users", registry::getOnlineCount)
            .description("Current number of online users")
            .register(meterRegistry);
    }
}