package eu.describeit.plantflow.engine

import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Gap tests for the engine data model: Marking (de)serialization edge cases,
 * Token deep-merge loops, builder validation, and small value classes.
 */
class EngineModelCoverageGapSpec extends Specification {

    def 'Marking json creator ignores negative and out-of-range token activation entries'() {
        given: 'a two-place token array and token activations with invalid place indexes'
        Token[][] tokens = [[Token.of([a: 1])] as Token[], new Token[0]] as Token[][]

        when: 'the json creator runs with a negative, an over-long, and a valid entry'
        Marking marking = new Marking(tokens, [:], [:], [(-1): [7], 5: [8], 0: [42]], null)

        then: 'only the valid entry is applied and the id counter falls back to 1'
        marking.getTokenActivation(0, 0) == 42
        marking.getNextForkActivationId() == 1
    }

    def 'removeToken skips null entries in the token array'() {
        given: 'a marking whose token array contains a null entry'
        Marking marking = marking(1)
        Token token = Token.of([x: 1])
        marking.tokens[0] = [null, token] as Token[]

        expect: 'a non-matching token is not found, the real token is removed'
        !marking.removeToken(0, Token.of([y: 2]))
        marking.removeToken(0, token)
        marking.getTokenCount(0) == 1
    }

    def 'removeTokenAt tolerates a tag list shorter than the token array'() {
        given: 'a place with one tag but two tokens'
        Marking marking = marking(1)
        marking.addToken(0, Token.of([a: 1]))
        marking.tokens[0] = [Token.of([a: 1]), Token.of([b: 2])] as Token[]

        when: 'the second token is removed at a position beyond the tag list'
        Token removed = marking.removeTokenAt(0, 1)

        then: 'the token is removed and the short tag list is left alone'
        removed != null
        removed.payload == [b: 2]
        marking.getTokenCount(0) == 1
    }

    def 'getTokenActivation returns null for negative positions, untagged places and overflow'() {
        given: 'a marking with one tagged token in place 0 and nothing in place 1'
        Marking marking = marking(2)
        marking.addToken(0, Token.of())

        expect:
        marking.getTokenActivation(0, -1) == null
        marking.getTokenActivation(1, 0) == null
        marking.getTokenActivation(0, 5) == null
    }

    def 'Token deepMerge covers empty base, empty overlay and empty nested maps'() {
        expect: 'empty base skips the base loop'
        Token.of([:]).deepMerge(Token.of([a: 1])).payload == [a: 1]

        and: 'empty overlay skips the overlay loop'
        Token.of([a: 1]).deepMerge(Token.of([:])).payload == [a: 1]

        and: 'an empty nested map in the base is deep-copied entry-loop-free'
        Token.of([n: [:]]).deepMerge(Token.of([m: 1])).payload == [n: [:], m: 1]
    }

    def 'PetriNetBuilder rejects a null join descriptor'() {
        when:
        new PetriNetBuilder().addJoinDescriptor(null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'PetriNetBuilder builds a net with arcs in both directions'() {
        given:
        PetriNetBuilder builder = new PetriNetBuilder()
        Place p1 = builder.addPlace('p1')
        Place p2 = builder.addPlace('p2')
        Transition t1 = builder.addTransition('t1')
        Transition t2 = builder.addTransition('t2')
        builder.connect(p1, t1)
        builder.connect(p1, t2, 2)
        builder.connect(t1, p2)
        builder.connect(t2, p2, 3)
        builder.setStartPlace(p1)
        builder.setEndPlace(p2)

        when:
        PetriNetStructure net = builder.build()

        then:
        net.incidenceMatrix.getInputWeight(p1.index, t1.index) == 1
        net.incidenceMatrix.getInputWeight(p1.index, t2.index) == 2
        net.incidenceMatrix.getOutputWeight(p2.index, t1.index) == 1
        net.incidenceMatrix.getOutputWeight(p2.index, t2.index) == 3
    }

    def 'JoinDescriptor two-arg constructor uses an empty branch order'() {
        expect:
        new JoinDescriptor(3, 7).branchEndPlaceIndexes.isEmpty()
        new JoinDescriptor(3, 7).forkTransitionIndex == 3
        new JoinDescriptor(3, 7).joinTransitionIndex == 7
    }

    def 'PetriNetStructure default join descriptors are empty on a minimal implementation'() {
        given: 'a minimal PetriNetStructure implementation without descriptor overrides'
        PetriNetStructure structure = new PetriNetStructure() {
            @Override
            List<Place> getPlaces() { Collections.emptyList() }

            @Override
            List<Transition> getTransitions() { Collections.emptyList() }

            @Override
            Place getStartPlace() { null }

            @Override
            Place getEndPlace() { null }

            @Override
            IncidenceMatrix getIncidenceMatrix() { null }
        }

        expect:
        structure.joinDescriptors.isEmpty()
        structure.forkDescriptors.isEmpty()
    }

    def 'MarkedPetriNet reports a non-empty net once a token is seeded'() {
        given: 'an execution on a linear net'
        def net = simpleLinearNet()
        def execution = new MarkedPetriNet(net, new DefaultPetriNetEngine())

        expect:
        execution.isNetEmpty()

        when:
        execution.seedToken()

        then:
        !execution.isNetEmpty()
    }

    def 'IncidenceMatrix constructor evaluates null rows in jagged matrices'() {
        when: 'a jagged matrix contains a null row'
        new IncidenceMatrix([[1], null] as int[][], [[0], [0]] as int[][])

        then: 'the null row comparison blows up'
        thrown(NullPointerException)
    }

    def 'IncidenceMatrix trips the index assertion for negative indexes'() {
        given:
        def matrix = new IncidenceMatrix([[1]] as int[][], [[0]] as int[][])

        when:
        matrix.getInputWeight(-1, 0)

        then:
        thrown(AssertionError)

        when:
        matrix.getInputWeight(0, -1)

        then:
        thrown(AssertionError)
    }

    def 'Place and Transition reject whitespace-only labels'() {
        when:
        new Place(0, '   ')

        then:
        thrown(IllegalArgumentException)

        when:
        new Transition(0, '\t ')

        then:
        thrown(IllegalArgumentException)
    }
}
