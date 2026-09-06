package eu.describeit.plantflow

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
record Token(String id, Instant timestamp, Map<String, Object> payload) {

    Token(String id, Instant timestamp, Map<String, Object> payload) {
        this.id = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString()
        this.timestamp = timestamp ?: Instant.now()
        this.payload = payload != null ? payload.asUnmodifiable() : [:]
    }

    static Token of() {
        return of(Collections.emptyMap())
    }

    static Token of(Map<String, Object> payload) {
        return new Token(UUID.randomUUID().toString(), Instant.now(), payload)
    }

    Token withPayload(Map<String, Object> newPayload) {
        return new Token(this.id, this.timestamp, newPayload)
    }
}
