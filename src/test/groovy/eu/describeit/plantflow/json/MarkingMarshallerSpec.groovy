package eu.describeit.plantflow.json

import eu.describeit.plantflow.Marshaller
import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.marshaller.MarshallerTestBase
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class MarkingMarshallerSpec extends Specification {

    def 'Marshaller should serialize and deserialize Marking'() {
        given:
        def p1 = MarshallerTestBase.createPlace(0, 'P1')
        def p2 = MarshallerTestBase.createPlace(1, 'P2')
        def marking = MarshallerTestBase.createMarking(2)

        when:
        def json = Marshaller.toJson(marking)
        log.info('Marking JSON: {}', json)
        def deserialized = Marshaller.fromJson(json, Marking)

        then:
        json.contains('"tokens"')
        deserialized.getTokens().length == 2
        deserialized.getTokenCount(p1) == 0
        deserialized.getTokenCount(p2) == 0
    }

    def 'Marshaller should serialize and deserialize Marking with tokens'() {
        given:
        def p1 = MarshallerTestBase.createPlace(0, 'P1')
        def p2 = MarshallerTestBase.createPlace(1, 'P2')
        def marking = MarshallerTestBase.createMarking(2)
        def token1 = MarshallerTestBase.createToken('token-1', [key1: 'value1'])
        def token2 = MarshallerTestBase.createToken('token-2', [key2: 'value2'])
        marking.addToken(p1, token1)
        marking.addToken(p2, token2)

        when:
        def json = Marshaller.toJson(marking, true)
        log.info('Marking with tokens JSON(pretty): {}', json)
        def deserialized = Marshaller.fromJson(json, Marking)

        then:
        json.contains('"tokens"')
        deserialized.getTokens().length == 2
        deserialized.getTokenCount(p1) == 1
        deserialized.getTokenCount(p2) == 1
    }
}
