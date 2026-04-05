package io.ylab.chat.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * Converts between Java objects and Redis Stream field maps. This component serializes a message
 * payload into a {@code Map<String, Object>} suitable for publishing to a Redis Stream via
 * {@code StreamOperations.add(...)}. Each entry in the stream record contains three fields:
 */
@Component
public class StreamMessageConverter {

    private final Jackson2JsonRedisSerializer<Object> serializer;

    /**
     * Constructs a new StreamMessageConverter with the given ObjectMapper.
     *
     * @param objectMapper the Jackson ObjectMapper to use for JSON serialization/deserialization
     */
    public StreamMessageConverter(ObjectMapper objectMapper) {
        this.serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        this.serializer.setObjectMapper(objectMapper);
    }

    /**
     * Converts a payload object into a map of fields ready for Redis Stream storage.
     *
     * @param payload the object to serialize (must be serializable by Jackson)
     * @return a mutable map with three entries (never {@code null})
     */
    public Map<String, Object> toFields(Object payload) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("payload", serializer.serialize(payload));
        fields.put("type", payload.getClass().getSimpleName());
        fields.put("timestamp", System.currentTimeMillis());
        return fields;
    }

    /**
     * Reconstructs an object from Redis Stream field map.
     *
     * @param fields the map of stream fields (typically from {@code StreamRecords.read(...)})
     * @param type   the target class to deserialize into
     * @param <T>    the type of the reconstructed object
     * @return the deserialized object, or {@code null} if no payload is present
     * @throws IllegalArgumentException if the payload bytes cannot be deserialized to the target
     *                                  type
     */
    @SuppressWarnings("unchecked")
    public <T> T fromFields(Map<Object, Object> fields, Class<T> type) {
        byte[] payloadBytes = (byte[]) fields.get("payload");
        if (payloadBytes == null) {
            return null;
        }
        return (T) serializer.deserialize(payloadBytes);
    }
}