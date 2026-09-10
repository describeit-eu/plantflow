package eu.describeit.plantflow.engine


import spock.lang.Specification

class MarkingSpec extends Specification {

    def 'should manage tokens in vector indexed places'() {
        given:
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def marking = new Marking(2)
        def token = Token.of([data: 'initial'])

        when:
        marking.addToken(pStart, token)

        then:
        marking.getTokenCount(pStart) == 1
        marking.getTokenCount(0) == 1
        marking.getTokenCount(pEnd) == 0
        marking.getTokens(pStart) == [token]
        marking.getTokens(0) == [token]
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
        def pStart = new Place(0, 'start')
        def pEnd = new Place(1, 'end')
        def marking = new Marking(2)
        def token1 = Token.of([k: 'v1'])
        def token2 = Token.of([k: 'v2'])
        marking.addToken(pStart, token1)

        when:
        def copy = marking.copy()

        then: 'copy has identical initial state'
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
        def marking = new Marking(1)

        expect:
        marking.getTokens(placeIndex) == expectedTokens
        marking.getTokenCount(placeIndex) == expectedCount
        marking.isEmpty(placeIndex) == expectedEmpty

        where:
        scenario                 | placeIndex | expectedTokens | expectedCount | expectedEmpty
        'existing place'         | 0          | []             | 0             | true
        'out of bound index'     | 1          | []             | 0             | true
        'negative place index'   | -1         | []             | 0             | true
        'large place index'      | 99         | []             | 0             | true
    }

    def 'should ignore null token when adding tokens by index or place'() {
        given:
        def place = new Place(0, 'start')
        def marking = new Marking(1)

        when:
        marking.addToken(0, null)
        marking.addToken(place, null)

        then:
        marking.isEmpty(place)
        marking.getTokenCount(place) == 0
        marking.getTokens(place).isEmpty()
    }

    def 'should handle addToken with out of bounds or negative indices gracefully'() {
        given:
        def place = new Place(0, 'start')
        def marking = new Marking(1)
        def token = Token.of([k: 'v'])

        when:
        marking.addToken(invalidIndex, token)

        then:
        marking.isEmpty(place)
        marking.getTokenCount(place) == 0

        where:
        invalidIndex << [-1, 1, 100]
    }

    def 'should return false when removing token with invalid index or non-existent token: #scenario'() {
        given:
        def place = new Place(0, 'start')
        def marking = new Marking(1)
        def token1 = Token.of([id: 1])
        def token2 = Token.of([id: 2])
        marking.addToken(place, token1)

        expect:
        removeCall(marking, place, token2, invalidIndex) == false
        marking.getTokenCount(place) == 1

        where:
        scenario                    | invalidIndex | removeCall
        'negative index'            | -1           | { Marking m, Place p, Token t, int idx -> m.removeToken(idx, t) }
        'out of bounds index'       | 5            | { Marking m, Place p, Token t, int idx -> m.removeToken(idx, t) }
        'non-existent token index'  | 0            | { Marking m, Place p, Token t, int idx -> m.removeToken(idx, t) }
        'non-existent token place'  | 0            | { Marking m, Place p, Token t, int idx -> m.removeToken(p, t) }
    }
}
