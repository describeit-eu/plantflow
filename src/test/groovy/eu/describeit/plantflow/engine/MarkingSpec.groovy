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

    def 'should return false when removing token at exact boundary index equal to tokens length'() {
        given:
        def place = new Place(0, 'start')
        def marking = new Marking(1)
        def token = Token.of([k: 'v'])
        marking.addToken(place, token)

        when: 'removing with index == tokens.length (boundary)'
        def result = marking.removeToken(1, token)

        then:
        !result
        marking.getTokenCount(place) == 1

        when: 'removing with index == tokens.length via Place overload'
        def result2 = marking.removeToken(1, token)

        then:
        !result2
        marking.getTokenCount(place) == 1
    }

    def 'should return false when removing token with negative boundary index'() {
        given:
        def marking = new Marking(2)
        def token = Token.of([k: 'v'])
        marking.addToken(0, token)

        expect:
        !marking.removeToken(-1, token)
        marking.getTokenCount(0) == 1
    }

    def 'getTokens should return actual tokens for valid index (not empty list)'() {
        given:
        def marking = new Marking(2)
        def token1 = Token.of([a: 1])
        def token2 = Token.of([b: 2])
        marking.addToken(0, token1)
        marking.addToken(0, token2)

        expect:
        marking.getTokens(0) == [token1, token2]
        marking.getTokens(0).size() == 2
    }

    def 'getTokens should return unmodifiable list for valid index'() {
        given:
        def marking = new Marking(1)
        marking.addToken(0, Token.of([a: 1]))

        when:
        marking.getTokens(0).add(Token.of([b: 2]))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'getTokens by Place should delegate to index-based getTokens'() {
        given:
        def place = new Place(0, 'start')
        def marking = new Marking(1)
        def token = Token.of([k: 'v'])
        marking.addToken(place, token)

        expect:
        marking.getTokens(place) == [token]
        marking.getTokens(place) == marking.getTokens(0)
    }

    def 'removeToken by Place should delegate to index-based removeToken'() {
        given:
        def place = new Place(0, 'start')
        def marking = new Marking(1)
        def token = Token.of([k: 'v'])
        marking.addToken(place, token)

        expect:
        marking.removeToken(place, token)
        marking.isEmpty(place)
    }

    def 'isEmpty should return true for out-of-bounds indices'() {
        given:
        def marking = new Marking(2)

        expect:
        marking.isEmpty(-1)
        marking.isEmpty(2)
        marking.isEmpty(99)
    }

    def 'isEmpty by Place should delegate to index-based isEmpty'() {
        given:
        def place = new Place(0, 'start')
        def marking = new Marking(1)

        expect:
        marking.isEmpty(place)

        when:
        marking.addToken(place, Token.of())

        then:
        !marking.isEmpty(place)
    }

    def 'getTokenCount should return 0 for out-of-bounds indices'() {
        given:
        def marking = new Marking(2)

        expect:
        marking.getTokenCount(-1) == 0
        marking.getTokenCount(2) == 0
        marking.getTokenCount(99) == 0
    }

    def 'getTokenCount by Place should delegate to index-based getTokenCount'() {
        given:
        def place = new Place(0, 'start')
        def marking = new Marking(1)
        marking.addToken(place, Token.of())

        expect:
        marking.getTokenCount(place) == 1
        marking.getTokenCount(place) == marking.getTokenCount(0)
    }

    def 'getMarkingVector should return correct vector'() {
        given:
        def marking = new Marking(3)
        marking.addToken(0, Token.of())
        marking.addToken(0, Token.of())
        marking.addToken(2, Token.of())

        expect:
        marking.markingVector == [2, 0, 1] as int[]
    }

    def 'getTokens array accessor should return raw token array'() {
        given:
        def marking = new Marking(2)
        def token = Token.of([k: 'v'])
        marking.addToken(0, token)

        expect:
        marking.tokens.length == 2
        marking.tokens[0].length == 1
        marking.tokens[0][0] == token
        marking.tokens[1].length == 0
    }
}
