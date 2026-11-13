package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Gap tests for the engine: synchronizing join enablement/firing through
 * DefaultPetriNetEngine, ForkJoinSupport edge cases, constant guards, and
 * action handlers returning Token or Map results.
 */
class EngineCoverageGapSpec extends Specification {

    def engine = new DefaultPetriNetEngine()

    private PetriNetStructure buildJoinNet() {
        PetriNetBuilder builder = new PetriNetBuilder()
        Place start = builder.addPlace('start')
        Transition fork = builder.addTransition('T_fork_1')
        builder.connect(start, fork)
        Place b1 = builder.addPlace('b1end')
        Place b2 = builder.addPlace('b2end')
        builder.connect(fork, b1)
        builder.connect(fork, b2)
        Transition join = builder.addTransition('T_join')
        builder.connect(b1, join)
        builder.connect(b2, join)
        Place joinPlace = builder.addPlace('joinPlace')
        builder.connect(join, joinPlace)
        Place end = builder.addPlace('end')
        Transition toEnd = builder.addTransition('toEnd')
        builder.connect(joinPlace, toEnd)
        builder.connect(toEnd, end)
        builder.setStartPlace(start)
        builder.setEndPlace(end)
        builder.addJoinDescriptor(new JoinDescriptor(fork.index, join.index, [b1.index, b2.index]))
        return builder.build()
    }

    def 'engine disables a join until the full correlated activation set is present, then fires it'() {
        given: 'a join net with one registered fork activation'
        PetriNetStructure net = buildJoinNet()
        Transition fork = net.transitions.find { it.label == 'T_fork_1' }
        Transition join = net.transitions.find { it.label == 'T_join' }
        Place b1 = net.places.find { it.label == 'b1end' }
        Place b2 = net.places.find { it.label == 'b2end' }
        Place joinPlace = net.places.find { it.label == 'joinPlace' }
        def registry = registry()
        def ctx = context()
        Marking marking = marking(net.places.size())
        int activation = marking.registerForkActivation(fork.index)

        when: 'only one branch end holds a correlated token'
        marking.addToken(b1, Token.of([branch: 'a']), activation)

        then: 'the join is not enabled'
        !engine.isEnabled(net, join, marking, registry, ctx)

        when: 'both branch ends hold tokens but for different activations, so the join set is incomplete despite sufficient tokens'
        int other = marking.registerForkActivation(fork.index)
        marking.addToken(b2, Token.of([branch: 'b']), other)

        then: 'the join is still not enabled because no single activation is complete'
        !engine.isEnabled(net, join, marking, registry, ctx)

        when: 'the second branch end completes the correlated set'
        marking.addToken(b2, Token.of([branch: 'b']), activation)

        then: 'the join is enabled and firing consumes exactly the correlated set and selects the activation'
        engine.isEnabled(net, join, marking, registry, ctx)
        engine.fire(net, join, marking, registry, ctx)
        marking.getTokenCount(joinPlace) == 1
        marking.getTokenCount(b1) == 0
        marking.getTokenCount(b2) == 1
        marking.getPendingForkActivations(fork.index) == 1
        marking.getSelectedForkActivations(fork.index) == [activation]
        marking.getTokens(joinPlace)[0].payload == [branch: 'b']
    }

    def 'constant guard keys force enablement without consulting the registry'() {
        given: 'a transition guarded by the constant false and one by true'
        def netTrue = simpleLinearNet('start', 'end', 'tTrue', null, 'true')
        def netFalse = simpleLinearNet('start', 'end', 'tFalse', null, 'false')
        def reg = registry()
        def ctx = context()
        Marking mTrue = marking(2)
        mTrue.addToken(netTrue.startPlace, Token.of())
        Marking mFalse = marking(2)
        mFalse.addToken(netFalse.startPlace, Token.of())

        expect:
        engine.isEnabled(netTrue, netTrue.transitions[0], mTrue, reg, ctx)
        !engine.isEnabled(netFalse, netFalse.transitions[0], mFalse, reg, ctx)
        !engine.fire(netFalse, netFalse.transitions[0], mFalse, reg, ctx)
    }

    def 'action handler returning a Token uses it directly as the output token'() {
        given: 'a linear net whose action returns a fresh Token'
        def net = simpleLinearNet('start', 'end', 'act', 'tokenAction', null)
        def reg = registry { r ->
            r.registerAction('tokenAction') { ExecutionContext c, Token t -> Token.of([result: 'token']) }
        }
        Marking marking = marking(net.places.size())
        marking.addToken(net.startPlace, Token.of([in: 1]))

        when: 'the action transition fires'
        boolean fired = engine.fire(net, net.transitions[0], marking, reg, context())

        then: 'the produced token is exactly the returned Token'
        fired
        marking.getTokens(net.endPlace).size() == 1
        marking.getTokens(net.endPlace)[0].payload == [result: 'token']
    }

    def 'action handler returning a Map merges it into the consumed token payload'() {
        given: 'a linear net whose action returns a plain map'
        def net = simpleLinearNet('start', 'end', 'act', 'mapAction', null)
        def reg = registry { r ->
            r.registerAction('mapAction') { ExecutionContext c, Token t -> [added: true] }
        }
        Marking marking = marking(net.places.size())
        marking.addToken(net.startPlace, Token.of([in: 1]))

        when: 'the action transition fires'
        boolean fired = engine.fire(net, net.transitions[0], marking, reg, context())

        then: 'the produced token carries the returned map as its payload'
        fired
        marking.getTokens(net.endPlace)[0].payload == [added: true]
    }

    def 'findJoinDescriptor matches the join transition and misses others'() {
        given: 'a join net'
        PetriNetStructure net = buildJoinNet()
        Transition join = net.transitions.find { it.label == 'T_join' }
        Transition fork = net.transitions.find { it.label == 'T_fork_1' }

        expect: 'the join transition is found, other transitions are not'
        ForkJoinSupport.findJoinDescriptor(net, join.index) != null
        ForkJoinSupport.findJoinDescriptor(net, fork.index) == null
    }

    def 'hasCompleteJoinSet is true for transitions without a join descriptor'() {
        given: 'a join net and its fork transition'
        PetriNetStructure net = buildJoinNet()
        Transition fork = net.transitions.find { it.label == 'T_fork_1' }
        Marking marking = marking(net.places.size())

        expect:
        ForkJoinSupport.hasCompleteJoinSet(net, fork, marking, net.places)
    }

    def 'findCompleteJoinActivation returns null when no activations are pending'() {
        given: 'a join net, a fresh marking and its join descriptor'
        PetriNetStructure net = buildJoinNet()
        Transition join = net.transitions.find { it.label == 'T_join' }
        JoinDescriptor descriptor = net.joinDescriptors[0]
        Marking marking = marking(net.places.size())
        List<Place> inputs = [net.places.find { it.label == 'b1end' }, net.places.find { it.label == 'b2end' }]

        expect: 'no pending activations means no complete join set'
        ForkJoinSupport.findCompleteJoinActivation(marking, inputs, descriptor) == null
        !ForkJoinSupport.hasCompleteJoinSet(net, join, marking, inputs)
    }

    def 'findCompleteJoinActivation skips incomplete activations and returns the complete one'() {
        given: 'two pending activations where only the second has tokens in every branch end'
        PetriNetStructure net = buildJoinNet()
        JoinDescriptor descriptor = net.joinDescriptors[0]
        Place b1 = net.places.find { it.label == 'b1end' }
        Place b2 = net.places.find { it.label == 'b2end' }
        Marking marking = marking(net.places.size())
        marking.registerForkActivation(net.transitions.find { it.label == 'T_fork_1' }.index)
        int second = marking.registerForkActivation(net.transitions.find { it.label == 'T_fork_1' }.index)
        marking.addToken(b1, Token.of([run: 1]), 1)
        marking.addToken(b1, Token.of([run: 2]), second)
        marking.addToken(b2, Token.of([run: 2]), second)

        expect: 'activation 1 is incomplete, activation 2 is complete'
        ForkJoinSupport.findCompleteJoinActivation(marking, [b1, b2], descriptor) == second
    }

    def 'findCompleteJoinActivation treats a null input place list as vacuously complete'() {
        given: 'a pending activation and a null input place list'
        PetriNetStructure net = buildJoinNet()
        JoinDescriptor descriptor = net.joinDescriptors[0]
        Marking marking = marking(net.places.size())
        int activation = marking.registerForkActivation(net.transitions.find { it.label == 'T_fork_1' }.index)

        expect: 'the activation is complete because no input place is missing a token'
        ForkJoinSupport.findCompleteJoinActivation(marking, null, descriptor) == activation
    }

    def 'findCompleteJoinActivation returns null when no activation is complete'() {
        given: 'a pending activation with a token in only one branch end'
        PetriNetStructure net = buildJoinNet()
        JoinDescriptor descriptor = net.joinDescriptors[0]
        Place b1 = net.places.find { it.label == 'b1end' }
        Place b2 = net.places.find { it.label == 'b2end' }
        Marking marking = marking(net.places.size())
        marking.registerForkActivation(net.transitions.find { it.label == 'T_fork_1' }.index)
        marking.addToken(b1, Token.of([run: 1]), 1)

        expect:
        ForkJoinSupport.findCompleteJoinActivation(marking, [b1, b2], descriptor) == null
    }

    def 'consumeJoinInputTokens without diagram branch order keeps the given input order'() {
        given: 'a join descriptor without branch end place indexes'
        PetriNetStructure net = buildJoinNet()
        Transition fork = net.transitions.find { it.label == 'T_fork_1' }
        Transition join = net.transitions.find { it.label == 'T_join' }
        JoinDescriptor descriptor = new JoinDescriptor(fork.index, join.index)
        Place b1 = net.places.find { it.label == 'b1end' }
        Place b2 = net.places.find { it.label == 'b2end' }
        Marking marking = marking(net.places.size())
        marking.addToken(b1, Token.of([branch: 'a']), 1)
        marking.addToken(b2, Token.of([branch: 'b']), 1)

        when: 'the reversed input order is consumed'
        List<Token> consumed = ForkJoinSupport.consumeJoinInputTokens(net, descriptor, marking, [b2, b1], 1)

        then: 'tokens are consumed in the given input order'
        consumed*.payload == [[branch: 'b'], [branch: 'a']]
    }

    def 'consumeJoinInputTokens keeps unmatched diagram-order indexes out of the ordered prefix'() {
        given: 'a join descriptor whose branch order contains an unknown place index'
        PetriNetStructure net = buildJoinNet()
        Transition fork = net.transitions.find { it.label == 'T_fork_1' }
        Transition join = net.transitions.find { it.label == 'T_join' }
        Place b1 = net.places.find { it.label == 'b1end' }
        Place b2 = net.places.find { it.label == 'b2end' }
        JoinDescriptor descriptor = new JoinDescriptor(fork.index, join.index, [999, b1.index])
        Marking marking = marking(net.places.size())
        marking.addToken(b1, Token.of([branch: 'a']), 1)
        marking.addToken(b2, Token.of([branch: 'b']), 1)

        when: 'consumption happens with the bogus diagram order'
        List<Token> consumed = ForkJoinSupport.consumeJoinInputTokens(net, descriptor, marking, [b1, b2], 1)

        then: 'b1 is consumed first per diagram order, b2 follows as remaining input'
        consumed*.payload == [[branch: 'a'], [branch: 'b']]
    }

    def 'consumeJoinInputTokens with a null activation id throws instead of consuming'() {
        given: 'a join net with untagged tokens in both branch ends'
        PetriNetStructure net = buildJoinNet()
        JoinDescriptor descriptor = net.joinDescriptors[0]
        Place b1 = net.places.find { it.label == 'b1end' }
        Place b2 = net.places.find { it.label == 'b2end' }
        Marking marking = marking(net.places.size())
        marking.addToken(b1, Token.of([branch: 'a']))
        marking.addToken(b2, Token.of([branch: 'b']))

        when:
        ForkJoinSupport.consumeJoinInputTokens(net, descriptor, marking, [b1, b2], null)

        then:
        thrown(IllegalStateException)
        marking.getTokenCount(b1) == 1
        marking.getTokenCount(b2) == 1
    }

    def 'consumeJoinInputTokens tolerates a weighted arc draining more tokens than present'() {
        given: 'a join net with input weight 2 on the first branch end but a single correlated token'
        PetriNetBuilder builder = new PetriNetBuilder()
        Place start = builder.addPlace('start')
        Transition fork = builder.addTransition('T_fork_1')
        builder.connect(start, fork)
        Place b1 = builder.addPlace('b1end')
        Place b2 = builder.addPlace('b2end')
        builder.connect(fork, b1)
        builder.connect(fork, b2)
        Transition join = builder.addTransition('T_join')
        builder.connect(b1, join, 2)
        builder.connect(b2, join)
        Place joinPlace = builder.addPlace('joinPlace')
        builder.connect(join, joinPlace)
        Place end = builder.addPlace('end')
        builder.setStartPlace(start)
        builder.setEndPlace(end)
        builder.addJoinDescriptor(new JoinDescriptor(fork.index, join.index, [b1.index, b2.index]))
        PetriNetStructure net = builder.build()
        JoinDescriptor descriptor = net.joinDescriptors[0]
        Marking marking = marking(net.places.size())
        marking.addToken(b1, Token.of([branch: 'a']), 1)
        marking.addToken(b2, Token.of([branch: 'b']), 1)

        when: 'consumption runs out of correlated tokens in the weighted place'
        List<Token> consumed = ForkJoinSupport.consumeJoinInputTokens(net, descriptor, marking, [b1, b2], 1)

        then: 'the present tokens are consumed and the missing second weighted token is skipped'
        consumed*.payload == [[branch: 'a'], [branch: 'b']]
        marking.getTokenCount(b1) == 0
        marking.getTokenCount(b2) == 0
    }

    def 'engine tolerates a structure whose collections are null'() {
        given: 'a minimal structure returning null collections'
        PetriNetStructure stub = new PetriNetStructure() {
            @Override
            List<Place> getPlaces() { null }

            @Override
            List<Transition> getTransitions() { null }

            @Override
            Place getStartPlace() { null }

            @Override
            Place getEndPlace() { null }

            @Override
            IncidenceMatrix getIncidenceMatrix() { null }

            @Override
            List<ForkDescriptor> getForkDescriptors() { null }

            @Override
            List<JoinDescriptor> getJoinDescriptors() { null }
        }
        Transition transition = new Transition(0, 't')
        def reg = registry()
        def ctx = context()
        Marking marking = marking(0)

        expect: 'all engine entry points tolerate the null collections'
        engine.getEnabledTransitions(stub, marking, reg, ctx).isEmpty()
        engine.isEnabled(stub, transition, marking, reg, ctx)
        engine.fire(stub, transition, marking, reg, ctx)
    }

    def 'the private constructor is invocable via reflection'() {
        when:
        def ctor = ForkJoinSupport.getDeclaredConstructor()
        ctor.accessible = true
        def instance = ctor.newInstance()

        then:
        instance != null
    }
}
