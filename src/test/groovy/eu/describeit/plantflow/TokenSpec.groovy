package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import spock.lang.Specification
import java.time.Instant

import static java.time.Instant.ofEpochMilli

class TokenSpec extends Specification {

    def 'should initialize Token with fallback defaults when values are null or empty: #scenario'() {
        when:
        def token = new Token(inputId, inputTimestamp, inputPayload)

        then:
        (expectedId == null) ? (token.id != null && !token.id.empty) : (token.id == expectedId)
        (expectedTimestamp == null) ? (token.timestamp != null) : (token.timestamp == expectedTimestamp)
        token.payload == expectedPayload

        where:
        scenario                              | inputId | inputTimestamp       | inputPayload | expectedId | expectedTimestamp  | expectedPayload
        'all null parameters'                 | null    | null                 | null         | null       | null               | [:]
        'empty id with nulls'                 | ''      | null                 | null         | null       | null               | [:]
        'explicit id, null timestamp/payload' | 't-100' | null                 | null         | 't-100'    | null               | [:]
        'explicit timestamp, null id/payload' | null    | ofEpochMilli(1000)   | null         | null       | ofEpochMilli(1000) | [:]
        'explicit payload, null id/timestamp' | null    | null                 | [key: 'val'] | null       | null               | [key: 'val']
        'all explicit values provided'        | 't-200' | ofEpochMilli(2000)   | [order: 123] | 't-200'    | ofEpochMilli(2000) | [order: 123]
    }

    def 'should create Token via of() factory with default and custom payloads: #scenario'() {
        when:
        def token = factoryCall()

        then:
        token.id != null
        token.timestamp != null
        token.payload == expectedPayload

        where:
        scenario                  | factoryCall                               | expectedPayload
        'no arguments (default)'  | { -> Token.of() }                         | [:]
        'custom payload provided' | { -> Token.of([user: 'alice']) }          | [user: 'alice']
        'null payload provided'   | { -> Token.of(null) }                     | [:]
    }

    def 'should create a new token with updated payload using withPayload: #scenario'() {
        given:
        def originalTimestamp = ofEpochMilli(5000)
        def original = new Token('tok-orig', originalTimestamp, [a: 1])

        when:
        def updated = original.withPayload(newPayload)

        then:
        updated.id == 'tok-orig'
        updated.timestamp == originalTimestamp
        updated.payload == expectedPayload

        where:
        scenario            | newPayload   | expectedPayload
        'new map payload'   | [b: 2, c: 3] | [b: 2, c: 3]
        'empty map payload' | [:]          | [:]
        'null payload'      | null         | [:]
    }

    def 'token payload should be immutable'() {
        given:
        def payload = [key: 'value']
        def token = new Token('tok-1', Instant.now(), payload)

        when:
        token.payload.put('key', 'mutated')

        then:
        thrown(UnsupportedOperationException)
    }

    def 'should satisfy equals, hashCode, and toString contracts'() {
        given:
        def ts = ofEpochMilli(1000)
        def token1 = new Token('tok-1', ts, [x: 10])
        def token2 = new Token('tok-1', ts, [x: 10])
        def token3 = new Token('tok-2', ts, [x: 10])

        expect:
        token1 == token2
        token1.hashCode() == token2.hashCode()
        token1 != token3
        token1.toString().contains('tok-1')
        token1.toString().contains('x=10') || token1.toString().contains('x:10')
    }
}
