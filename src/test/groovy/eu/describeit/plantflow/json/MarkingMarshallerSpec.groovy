package eu.describeit.plantflow.json

import eu.describeit.plantflow.Marshaller
import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Token
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class MarkingMarshallerSpec extends Specification {

    def 'Marshaller should serialize and deserialize Marking'() {
        given:
        def p1 = new Place(0, 'P1')
        def p2 = new Place(1, 'P2')
        def marking = new Marking(2)

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
        def p1 = new Place(0, 'P1')
        def p2 = new Place(1, 'P2')
        def marking = new Marking(2)
        def token1 = new Token('token-1', null, [key1: 'value1'])
        def token2 = new Token('token-2', null, [key2: 'value2'])
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
