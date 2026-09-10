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
        def marking = new Marking([p1, p2])

        when:
        def json = Marshaller.toJson(marking)
        def deserialized = Marshaller.fromJson(json, Marking)

        log.info('Marking JSON: {}', json)

        then:
        json.contains('"places"')
        json.contains('"tokens"')
        deserialized.places.size() == 2
        deserialized.getTokenCount(p1) == 0
        deserialized.getTokenCount(p2) == 0
    }

    def 'Marshaller should serialize and deserialize Marking with tokens'() {
        given:
        def p1 = new Place(0, 'P1')
        def p2 = new Place(1, 'P2')
        def marking = new Marking([p1, p2])
        def token1 = new Token('token-1', null, [key1: 'value1'])
        def token2 = new Token('token-2', null, [key2: 'value2'])
        marking.addToken(p1, token1)
        marking.addToken(p2, token2)

        when:
        def json = Marshaller.toJson(marking, true)
        def deserialized = Marshaller.fromJson(json, Marking)

        log.info('Marking with tokens JSON(pretty): {}', json)

        then:
        json.contains('"places"')
        json.contains('"tokens"')
        deserialized.places.size() == 2
        deserialized.getTokenCount(p1) == 1
        deserialized.getTokenCount(p2) == 1
    }
}
