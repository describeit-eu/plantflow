package eu.describeit.plantflow

import spock.lang.Specification
import java.time.Instant

class RecordTokenSpec extends Specification {

    def 'should create RecordToken with id, timestamp and payload'() {
        given:
        def id = 'tok-1'
        def timestamp = Instant.now()
        def payload = [orderId: 'ORD-123', amount: 99.9]

        when:
        def token = new RecordToken(id, timestamp, payload)

        then:
        token.id == id
        token.timestamp == timestamp
        token.payload == [orderId: 'ORD-123', amount: 99.9]
    }

    def 'should create RecordToken with generated id and current timestamp when only payload is provided'() {
        when:
        def token = RecordToken.of([userId: 'user-1'])

        then:
        token.id != null
        token.timestamp != null
        token.payload == [userId: 'user-1']
    }

    def 'token payload should be immutable'() {
        given:
        def payload = [key: 'value']
        def token = new RecordToken('tok-1', Instant.now(), payload)

        when:
        token.payload.put('key', 'mutated')

        then:
        thrown(UnsupportedOperationException)
    }
}
