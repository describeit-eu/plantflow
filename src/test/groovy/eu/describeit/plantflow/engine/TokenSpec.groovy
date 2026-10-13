package eu.describeit.plantflow.engine


import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

import static java.time.Instant.ofEpochMilli

class TokenSpec extends Specification {

    @Unroll
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

    @Unroll
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
        'map constructor'         | { -> new Token((Map<String, Object>) [user: 'bob']) } | [user: 'bob']
        'map constructor null'    | { -> new Token((Map<String, Object>) null) }          | [:]
    }

    @Unroll
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

    def 'deepMerge should return same token when other is null'() {
        given:
        def token = new Token('tok-1', ofEpochMilli(1000), [a: 1])

        when:
        def merged = token.deepMerge(null)

        then:
        merged.is(token)
    }

    def 'deepMerge should merge flat payloads with overlay winning on conflicts'() {
        given:
        def base = new Token('tok-1', ofEpochMilli(1000), [a: 1, b: 2, shared: 'base'])
        def overlay = new Token('tok-2', ofEpochMilli(2000), [c: 3, shared: 'overlay'])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.id == 'tok-1'
        merged.timestamp == ofEpochMilli(1000)
        merged.payload == [a: 1, b: 2, shared: 'overlay', c: 3]
    }

    def 'deepMerge should recursively merge nested maps'() {
        given:
        def base = new Token('tok-1', ofEpochMilli(1000), [
            config: [host: 'localhost', port: 8080],
            flat: 'base'
        ])
        def overlay = new Token('tok-2', ofEpochMilli(2000), [
            config: [port: 9090, ssl: true],
            extra: 'overlay'
        ])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.payload.config == [host: 'localhost', port: 9090, ssl: true]
        merged.payload.flat == 'base'
        merged.payload.extra == 'overlay'
    }

    def 'deepMerge should handle empty payloads'() {
        given:
        def empty = Token.of()
        def withData = Token.of([key: 'value'])

        expect:
        empty.deepMerge(withData).payload == [key: 'value']
        withData.deepMerge(empty).payload == [key: 'value']
        empty.deepMerge(Token.of()).payload == [:]
    }

    def 'deepMerge should preserve base token id and timestamp'() {
        given:
        def baseTs = ofEpochMilli(1000)
        def overlayTs = ofEpochMilli(2000)
        def base = new Token('base-id', baseTs, [a: 1])
        def overlay = new Token('overlay-id', overlayTs, [b: 2])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.id == 'base-id'
        merged.timestamp == baseTs
    }

    def 'deepMerge should handle deeply nested maps with multiple levels'() {
        given:
        def base = Token.of([
            level1: [
                level2: [
                    level3: [a: 1, b: 2],
                    other: 'base'
                ]
            ]
        ])
        def overlay = Token.of([
            level1: [
                level2: [
                    level3: [b: 99, c: 3]
                ]
            ]
        ])

        when:
        def merged = base.deepMerge(overlay)

        then:
        def l1 = merged.payload.level1 as Map
        def l2 = l1.level2 as Map
        def l3 = l2.level3 as Map
        l3 == [a: 1, b: 99, c: 3]
        l2.other == 'base'
    }

    def 'deepMerge should not merge when overlay value is not a map but base is'() {
        given:
        def base = Token.of([key: [nested: 'value']])
        def overlay = Token.of([key: 'flat'])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.payload.key == 'flat'
    }

    def 'deepMerge should not merge when base value is not a map but overlay is'() {
        given:
        def base = Token.of([key: 'flat'])
        def overlay = Token.of([key: [nested: 'value']])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.payload.key == [nested: 'value']
    }

    def 'deepMerge should deep copy empty nested map from base'() {
        given:
        def base = Token.of([config: [:], other: 'value'])
        def overlay = Token.of([config: [key: 'val']])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.payload.config == [key: 'val']
        merged.payload.other == 'value'
    }

    def 'deepMerge should preserve base nested map when overlay has empty map at same key'() {
        given:
        def base = Token.of([config: [host: 'localhost', port: 8080]])
        def overlay = Token.of([config: [:]])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.payload.config == [host: 'localhost', port: 8080]
    }

    def 'deepMerge should handle both tokens having empty nested maps at same key'() {
        given:
        def base = Token.of([config: [:], flat: 'base'])
        def overlay = Token.of([config: [:], other: 'overlay'])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.payload.config == [:]
        merged.payload.flat == 'base'
        merged.payload.other == 'overlay'
    }

    def 'deepMerge should deep copy nested maps with multiple empty levels'() {
        given:
        def base = Token.of([
            level1: [level2: [:], keep: 'base']
        ])
        def overlay = Token.of([
            level1: [level2: [key: 'val'], add: 'overlay']
        ])

        when:
        def merged = base.deepMerge(overlay)

        then:
        def l1 = merged.payload.level1 as Map
        l1.level2 == [key: 'val']
        l1.keep == 'base'
        l1.add == 'overlay'
    }

    def 'deepMerge should handle deeply nested empty maps in deepCopyValue'() {
        given:
        def base = Token.of([
            a: [b: [c: [:]]]
        ])
        def overlay = Token.of([
            a: [b: [c: [d: 'deep']]]
        ])

        when:
        def merged = base.deepMerge(overlay)

        then:
        def a = merged.payload.a as Map
        def b = a.b as Map
        def c = b.c as Map
        c == [d: 'deep']
    }

    def 'deepMerge should not share references between result and source tokens'() {
        given:
        def base = Token.of([nested: [a: 1]])
        def overlay = Token.of([nested: [b: 2]])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.payload.nested == [a: 1, b: 2]
        base.payload.nested == [a: 1]
        overlay.payload.nested == [b: 2]
    }

    def 'deepMerge should copy non-map values directly without deep copy'() {
        given:
        def base = Token.of([list: [1, 2, 3], num: 42, str: 'hello'])
        def overlay = Token.of([bool: true])

        when:
        def merged = base.deepMerge(overlay)

        then:
        merged.payload.list == [1, 2, 3]
        merged.payload.num == 42
        merged.payload.str == 'hello'
        merged.payload.bool == true
    }
}
