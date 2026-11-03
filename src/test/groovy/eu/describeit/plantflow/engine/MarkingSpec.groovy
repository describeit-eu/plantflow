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
        def removed = marking.removeToken(pStart.index, token)
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
        copy.removeToken(pStart.index, token1)

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
        'non-existent token place'  | 0            | { Marking m, Place p, Token t, int idx -> m.removeToken(p.index, t) }
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
        marking.removeToken(place.index, token)
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

    def 'should track pending fork activations per fork transition'() {
        given:
        def marking = new Marking(2)

        expect: 'no pending activations initially'
        marking.getPendingForkActivations(0) == 0
        marking.getPendingForkActivations(5) == 0
        marking.forkActivations.isEmpty()

        when: 'registering fork activations'
        marking.registerForkActivation(0)
        marking.registerForkActivation(0)
        marking.registerForkActivation(3)

        then: 'pending counts accumulate per fork transition'
        marking.getPendingForkActivations(0) == 2
        marking.getPendingForkActivations(3) == 1
        marking.getPendingForkActivations(1) == 0

        when: 'consuming activations'
        def consumed1 = marking.consumeForkActivation(0)
        def consumed2 = marking.consumeForkActivation(0)
        def consumed3 = marking.consumeForkActivation(0)

        then: 'consumption succeeds while pending and fails when exhausted'
        consumed1
        consumed2
        !consumed3
        marking.getPendingForkActivations(0) == 0
        marking.getPendingForkActivations(3) == 1
    }

    def 'registerForkActivation should assign unique increasing activation ids'() {
        given:
        def marking = new Marking(1)

        expect:
        marking.nextForkActivationId == 1

        when:
        def id1 = marking.registerForkActivation(0)
        def id2 = marking.registerForkActivation(0)
        def id3 = marking.registerForkActivation(4)

        then:
        id1 != id2
        id2 == id1 + 1
        id3 == id2 + 1
        marking.nextForkActivationId == id3 + 1
        marking.getPendingForkActivations(0) == 2
        marking.isForkActivationPending(0, id1)
        marking.isForkActivationPending(0, id2)
        marking.isForkActivationPending(4, id3)
        !marking.isForkActivationPending(0, id3)
        !marking.isForkActivationPending(7, id1)
    }

    def 'selectForkActivation should move a pending activation to selected exactly once'() {
        given:
        def marking = new Marking(1)
        def id1 = marking.registerForkActivation(0)
        def id2 = marking.registerForkActivation(0)

        expect:
        !marking.selectForkActivation(0, 999)
        !marking.selectForkActivation(3, id1)
        marking.getSelectedForkActivations(0).size() == 0
        marking.getSelectedForkActivations(0).isEmpty()
        marking.getSelectedForkActivations(9).isEmpty()

        when:
        def selected = marking.selectForkActivation(0, id1)

        then:
        selected
        !marking.isForkActivationPending(0, id1)
        marking.isForkActivationPending(0, id2)
        marking.getPendingForkActivations(0) == 1
        marking.getSelectedForkActivations(0).size() == 1
        marking.getSelectedForkActivations(0) == [id1]

        when: 'the same activation is selected again'
        def reselected = marking.selectForkActivation(0, id1)

        then: 'selection is not repeated'
        !reselected
        marking.getSelectedForkActivations(0).size() == 1
    }

    def 'consumeForkActivation should move the oldest pending activation to selected'() {
        given:
        def marking = new Marking(1)
        def id1 = marking.registerForkActivation(0)
        def id2 = marking.registerForkActivation(0)

        when:
        def consumed = marking.consumeForkActivation(0)

        then:
        consumed
        marking.getSelectedForkActivations(0) == [id1]
        marking.isForkActivationPending(0, id2)
        !marking.isForkActivationPending(0, id1)

        when:
        marking.consumeForkActivation(0)

        then:
        marking.getPendingForkActivations(0) == 0
        marking.getSelectedForkActivations(0) == [id1, id2]
        !marking.consumeForkActivation(0)
        !marking.consumeForkActivation(8)
    }

    def 'token activations should track the fork activation of each token per place'() {
        given:
        def place = new Place(0, 'start')
        def marking = new Marking(2)
        def token1 = Token.of([k: 'v1'])
        def token2 = Token.of([k: 'v2'])

        expect: 'no activation tag initially or for invalid positions'
        marking.getTokenActivation(0, 0) == null
        marking.getTokenActivation(0, -1) == null
        marking.getTokenActivation(0, 5) == null
        marking.getTokenActivation(9, 0) == null
        marking.getTokenActivation(place.index, 0) == null

        when: 'tokens are added with and without activation tags'
        marking.addToken(0, token1, 7)
        marking.addToken(place, token2)

        then:
        marking.getTokenActivation(0, 0) == 7
        marking.getTokenActivation(0, 1) == null
        marking.getTokenActivation(place.index, 0) == 7

        when: 'the tagged token is removed by identity'
        def removed = marking.removeToken(0, token1)

        then: 'the remaining token keeps its own tag'
        removed
        marking.getTokenCount(0) == 1
        marking.getTokenActivation(0, 0) == null

        when: 'a token is removed by position'
        marking.addToken(0, token1, 8)
        def removedAt = marking.removeTokenAt(0, 1)

        then:
        removedAt == token1
        marking.getTokenCount(0) == 1
        marking.getTokenActivation(0, 0) == null

        expect: 'invalid removals return null and change nothing'
        marking.removeTokenAt(0, -1) == null
        marking.removeTokenAt(0, 5) == null
        marking.removeTokenAt(-1, 0) == null
        marking.removeTokenAt(9, 0) == null
        marking.getTokenCount(0) == 1
    }

    def 'copy should preserve token activation tags, selected state, and the activation id counter'() {
        given:
        def marking = new Marking(2)
        def id1 = marking.registerForkActivation(0)
        def id2 = marking.registerForkActivation(0)
        marking.selectForkActivation(0, id1)
        marking.addToken(0, Token.of([k: 'v']), id2)

        when:
        def copy = marking.copy()

        then: 'the copy has identical activation and tag state'
        copy.getPendingForkActivations(0) == 1
        copy.getSelectedForkActivations(0) == [id1]
        copy.getTokenActivation(0, 0) == id2
        copy.nextForkActivationId == marking.nextForkActivationId

        when: 'the copy mutates its own state'
        def newId = copy.registerForkActivation(0)
        copy.selectForkActivation(0, id2)
        copy.removeTokenAt(0, 0)
        copy.addToken(1, Token.of([k: 'other']), newId)

        then: 'the original is unaffected'
        marking.getPendingForkActivations(0) == 1
        marking.isForkActivationPending(0, id2)
        marking.getSelectedForkActivations(0).size() == 1
        marking.getTokenActivation(0, 0) == id2
        marking.getTokenCount(1) == 0
        marking.nextForkActivationId == id2 + 1
        newId == id2 + 1
    }

    def 'setForkActivations should rebuild pending activations from legacy count-based JSON'() {
        given:
        def marking = new Marking(1)

        when: 'legacy counts are applied without pending ids'
        marking.setForkActivations([0: 2, 3: 1])

        then:
        marking.getPendingForkActivations(0) == 2
        marking.getPendingForkActivations(3) == 1

        when: 'legacy counts are applied again while pending ids exist'
        marking.setForkActivations([0: 5])

        then: 'the existing pending ids take precedence'
        marking.getPendingForkActivations(0) == 2

        when: 'null counts are ignored'
        marking.setForkActivations(null)

        then:
        marking.getPendingForkActivations(0) == 2
    }

    def 'copy should preserve fork activation state independently'() {
        given:
        def marking = new Marking(1)
        marking.registerForkActivation(0)
        marking.registerForkActivation(0)

        when:
        def copy = marking.copy()

        then: 'copy starts with the same pending activations'
        copy.getPendingForkActivations(0) == 2

        when: 'the copy consumes its own activation'
        copy.consumeForkActivation(0)
        copy.registerForkActivation(1)

        then: 'the original marking is unaffected'
        marking.getPendingForkActivations(0) == 2
        marking.getPendingForkActivations(1) == 0
        copy.getPendingForkActivations(0) == 1
        copy.getPendingForkActivations(1) == 1
    }

    def 'deserialization skips out-of-range and over-long token activation entries'() {
        given:
        def tokens = [[Token.of([a: 1])] as Token[], [] as Token[]] as Token[][]

        when: 'a marking is created from JSON data with defensive tokenActivations entries'
        def marking = new Marking(tokens, [:], [:],
                [99: [2], 0: [7, 8]], 5)

        then: 'only valid entries within token bounds are applied'
        marking.getTokenActivation(0, 0) == 7
        marking.getTokenCount(0) == 1
        marking.getNextForkActivationId() == 5
    }

    def 'removeToken iterates past non-matching tokens and returns false when absent'() {
        given:
        def marking = new Marking(1)
        marking.addToken(0, Token.of([k: 'a']))
        marking.addToken(0, Token.of([k: 'b']))

        expect:
        !marking.removeToken(0, Token.of([k: 'zzz']))
        marking.getTokenCount(0) == 2
    }

    def 'removeTokenAt tolerates a missing tag list for the place'() {
        given:
        def marking = new Marking(1)
        marking.tokens[0] = [Token.of([k: 'raw'])] as Token[]

        when:
        def removed = marking.removeTokenAt(0, 0)

        then:
        removed.payload == [k: 'raw']
        marking.getTokenCount(0) == 0
    }

    def 'fresh marking exposes empty activation and token views for unknown indices'() {
        given:
        def marking = new Marking(2)

        expect:
        marking.getForkActivations() == [:]
        marking.getPendingForkActivationIds() == [:]
        marking.getSelectedForkActivationIds() == [:]
        marking.getTokenActivations() == [:]
        marking.getTokens(99).isEmpty()
        marking.getTokenActivation(99, 0) == null
    }
}
