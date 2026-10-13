package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
record Token(String id, Instant timestamp, Map<String, Object> payload) {

    Token(String id, Instant timestamp, Map<String, Object> payload) {
        this.id = id ?: UUID.randomUUID().toString()
        this.timestamp = timestamp ?: Instant.now()
        this.payload = payload != null ? payload.asUnmodifiable() : Collections.unmodifiableMap([:]) as Map<String, Object>
    }

    Token(Map<String, Object> payload) {
        this(null, null, payload)
    }

    static Token of() {
        return of([:])
    }

    static Token of(Map<String, Object> payload) {
        return new Token(payload)
    }

    Token withPayload(Map<String, Object> newPayload) {
        return new Token(this.id, this.timestamp, newPayload)
    }

    Token deepMerge(Token other) {
        if (other == null) {
            return this
        }
        Map<String, Object> merged = deepMergeMaps(this.payload, other.payload)
        return new Token(this.id, this.timestamp, merged)
    }

    private static Map<String, Object> deepMergeMaps(Map<String, Object> base, Map<String, Object> overlay) {
        Map<String, Object> result = [:]
        for (Map.Entry<String, Object> entry : base.entrySet()) {
            result.put(entry.getKey(), deepCopyValue(entry.getValue()))
        }
        for (Map.Entry<String, Object> entry : overlay.entrySet()) {
            String key = entry.getKey()
            Object overlayValue = entry.getValue()
            Object baseValue = result.get(key)
            if (baseValue instanceof Map && overlayValue instanceof Map) {
                @SuppressWarnings(['unchecked', 'rawtypes'])
                Map<String, Object> nested = deepMergeMaps((Map) baseValue, (Map) overlayValue)
                result.put(key, nested)
            } else {
                result.put(key, deepCopyValue(overlayValue))
            }
        }
        return result
    }

    @SuppressWarnings(['unchecked', 'rawtypes'])
    private static Object deepCopyValue(Object value) {
        if (value instanceof Map) {
            Map<String, Object> copy = [:]
            for (Map.Entry<String, Object> entry : ((Map) value).entrySet()) {
                copy.put(entry.getKey(), deepCopyValue(entry.getValue()))
            }
            return copy
        }
        return value
    }
}
