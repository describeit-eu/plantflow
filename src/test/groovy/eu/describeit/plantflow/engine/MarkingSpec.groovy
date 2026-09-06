package eu.describeit.plantflow.engine

import eu.describeit.plantflow.RecordToken
import spock.lang.Specification

class MarkingSpec extends Specification {

    def 'should manage tokens in vector indexed places'() {
        given:
        def pStart = new Place('P_start', 0, 'start')
        def pEnd = new Place('P_end', 1, 'end')
        def marking = new Marking([pStart, pEnd])
        def token = RecordToken.of([data: 'initial'])

        when:
        marking.addToken(pStart, token)

        then:
        marking.getTokenCount(pStart) == 1
        marking.getTokenCount(0) == 1
        marking.getTokenCount(pEnd) == 0
        marking.getTokens(pStart) == [token]
        marking.getTokens(0) == [token]
        marking.getTokens('P_start') == [token]
        marking.getMarkingVector() == [1, 0] as int[]

        when:
        def removed = marking.removeToken(pStart, token)
        marking.addToken(pEnd, token)

        then:
        removed
        marking.isEmpty(pStart)
        !marking.isEmpty(pEnd)
        marking.getTokens(pEnd) == [token]
        marking.getMarkingVector() == [0, 1] as int[]
    }

    def 'should create independent deep copy of marking'() {
        given:
        def pStart = new Place('P_start', 0, 'start')
        def pEnd = new Place('P_end', 1, 'end')
        def marking = new Marking([pStart, pEnd])
        def token1 = RecordToken.of([k: 'v1'])
        def token2 = RecordToken.of([k: 'v2'])
        marking.addToken(pStart, token1)

        when:
        def copy = marking.copy()

        then: 'copy has identical initial state'
        copy.places == marking.places
        copy.getTokens(pStart) == [token1]
        copy.getTokens(pEnd).isEmpty()
        copy.markingVector == marking.markingVector

        when: 'mutating the copy'
        copy.addToken(pEnd, token2)
        copy.removeToken(pStart, token1)

        then: 'original marking remains unchanged'
        marking.getTokens(pStart) == [token1]
        marking.getTokenCount(pStart) == 1
        marking.getTokens(pEnd).isEmpty()
        marking.getTokenCount(pEnd) == 0
        marking.markingVector == [1, 0] as int[]

        and: 'copy reflects only its own mutations'
        copy.getTokens(pStart).isEmpty()
        copy.getTokenCount(pStart) == 0
        copy.getTokens(pEnd) == [token2]
        copy.getTokenCount(pEnd) == 1
        copy.markingVector == [0, 1] as int[]
    }

    def 'should handle place queries safely for #scenario'() {
        given:
        def place = new Place('P_0', 0, 'start')
        def marking = new Marking([place])

        expect:
        marking.getTokens((String) placeId) == expectedTokens
        marking.getTokenCount((String) placeId) == expectedCount
        marking.isEmpty((String) placeId) == expectedEmpty

        where:
        scenario            | placeId       | expectedTokens | expectedCount | expectedEmpty
        'existing place'    | 'P_0'         | []             | 0             | true
        'nonexistent place' | 'nonexistent' | []             | 0             | true
        'empty place id'    | ''            | []             | 0             | true
        'null place id'     | null          | []             | 0             | true
    }

    def 'should ignore null token when adding tokens by index or place'() {
        given:
        def place = new Place('P_0', 0, 'start')
        def marking = new Marking([place])

        when:
        marking.addToken(0, null)
        marking.addToken(place, null)

        then:
        marking.isEmpty(place)
        marking.getTokenCount(place) == 0
        marking.getTokens(place).isEmpty()
    }
}
