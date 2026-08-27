package eu.describeit.plantflow

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import java.time.Instant

@CompileStatic
@ToString(includeNames = true)
@EqualsAndHashCode
class RecordToken {
    final String id
    final Instant timestamp
    final Map<String, Object> payload

    RecordToken(String id, Instant timestamp, Map<String, Object> payload) {
        this.id = id ?: UUID.randomUUID().toString()
        this.timestamp = timestamp ?: Instant.now()
        this.payload = payload != null ? Collections.unmodifiableMap(new LinkedHashMap<>(payload)) : Collections.emptyMap() as Map<String, Object>
    }

    static RecordToken of(Map<String, Object> payload = [:]) {
        return new RecordToken(UUID.randomUUID().toString(), Instant.now(), payload)
    }

    RecordToken withPayload(Map<String, Object> newPayload) {
        return new RecordToken(this.id, this.timestamp, newPayload)
    }
}
