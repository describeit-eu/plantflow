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

    Token(Instant timestamp, Map<String, Object> payload) {
        this(null, timestamp, payload)
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
}
