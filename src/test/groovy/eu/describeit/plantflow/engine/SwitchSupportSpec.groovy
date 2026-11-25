package eu.describeit.plantflow.engine

import spock.lang.Specification

/**
 * Ticket #56 coverage gap: focused tests for {@link SwitchSupport} covering
 * the empty-descriptor path, the no-token path, the enabled-case path, the
 * non-case-enabled path, and the stalled-token failure path.
 */
class SwitchSupportSpec extends Specification {

    private PetriNetStructure buildSwitchNet(List<Integer> caseTransitionIndexes = [1, 2]) {
        PetriNetBuilder builder = new PetriNetBuilder()
        Place start = builder.addPlace('start')
        Transition toDecision = builder.addTransition('T_to_decision')
        builder.connect(start, toDecision)
        Place decision = builder.addPlace('P_switch_decision')
        builder.connect(toDecision, decision)
        Transition caseA = builder.addTransition('T_case_a')
        Transition caseB = builder.addTransition('T_case_b')
        Place end = builder.addPlace('end')
        builder.connect(decision, caseA)
        builder.connect(decision, caseB)
        builder.connect(caseA, end)
        builder.connect(caseB, end)
        builder.setStartPlace(start)
        builder.setEndPlace(end)
        builder.addSwitchDescriptor(new SwitchDescriptor(decision.index, 'mode?', caseTransitionIndexes))
        return builder.build()
    }

    private Marking markingWithTokenAt(PetriNetStructure net, String placeLabel, Token token) {
        Marking marking = new Marking(net.places.size())
        Place place = net.places.find { it.label == placeLabel }
        marking.addToken(place, token)
        return marking
    }

    def 'getSwitchDescriptors returns the descriptors of the given net'() {
        given:
        PetriNetStructure net = buildSwitchNet()

        expect:
        SwitchSupport.getSwitchDescriptors(net) == net.switchDescriptors
        SwitchSupport.getSwitchDescriptors(net).size() == 1
    }

    def 'assertNoStalledSwitchToken passes when the net has no switch descriptors'() {
        given: 'a net without any switch descriptor but with a token somewhere'
        PetriNetBuilder builder = new PetriNetBuilder()
        Place start = builder.addPlace('start')
        Place end = builder.addPlace('end')
        Transition t = builder.addTransition('T_start_to_end')
        builder.connect(start, t)
        builder.connect(t, end)
        builder.setStartPlace(start)
        builder.setEndPlace(end)
        PetriNetStructure net = builder.build()
        Marking marking = markingWithTokenAt(net, 'start', new Token('token-1', null, [:]))

        when: 'the assertion runs with an empty enabled list'
        SwitchSupport.assertNoStalledSwitchToken(net, marking, [])

        then: 'no exception is thrown'
        notThrown(IllegalStateException)
    }

    def 'assertNoStalledSwitchToken ignores a switch decision place without tokens'() {
        given: 'a switch net where the decision place holds no token'
        PetriNetStructure net = buildSwitchNet()
        Marking marking = new Marking(net.places.size())

        when:
        SwitchSupport.assertNoStalledSwitchToken(net, marking, [])

        then:
        notThrown(IllegalStateException)
    }

    def 'assertNoStalledSwitchToken passes when a case transition of the stalled switch is enabled'() {
        given: 'a token at the decision place and its case transition enabled'
        PetriNetStructure net = buildSwitchNet([1, 2])
        Marking marking = markingWithTokenAt(net, 'P_switch_decision', new Token('token-2', null, [:]))
        Transition caseA = net.transitions.find { it.label == 'T_case_a' }
        Transition other = net.transitions.find { it.label == 'T_to_decision' }

        when: 'the enabled list contains a non-case transition before the case transition'
        SwitchSupport.assertNoStalledSwitchToken(net, marking, [other, caseA])

        then: 'no exception is thrown because a case transition is enabled'
        notThrown(IllegalStateException)
    }

    def 'assertNoStalledSwitchToken throws when only non-case transitions are enabled'() {
        given: 'a token at the decision place but only unrelated transitions enabled'
        PetriNetStructure net = buildSwitchNet([1, 2])
        Marking marking = markingWithTokenAt(net, 'P_switch_decision', new Token('token-3', null, [:]))
        Transition toDecision = net.transitions.find { it.label == 'T_to_decision' }

        when:
        SwitchSupport.assertNoStalledSwitchToken(net, marking, [toDecision])

        then: 'the stall is reported naming the switch expression and the token'
        def ex = thrown(IllegalStateException)
        ex.message.contains("No matching case in switch 'mode?'")
        ex.message.contains('token-3')
    }

    def 'assertNoStalledSwitchToken throws when the enabled list is empty'() {
        given: 'a token stalled at the decision place with no enabled transition at all'
        PetriNetStructure net = buildSwitchNet()
        Marking marking = markingWithTokenAt(net, 'P_switch_decision', new Token('token-4', null, [:]))

        when:
        SwitchSupport.assertNoStalledSwitchToken(net, marking, [])

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains("No matching case in switch 'mode?'")
        ex.message.contains('token-4')
    }

    def 'assertNoStalledSwitchToken checks every descriptor and reports the first stalled one'() {
        given: 'a net with two switches where only the second has a stalled token'
        PetriNetBuilder builder = new PetriNetBuilder()
        Place start = builder.addPlace('start')
        Place decision1 = builder.addPlace('P_switch_decision_1')
        Transition toDecision1 = builder.addTransition('T_to_decision_1')
        builder.connect(start, toDecision1)
        builder.connect(toDecision1, decision1)
        Transition case1 = builder.addTransition('T_case_1')
        Place end = builder.addPlace('end')
        builder.connect(decision1, case1)
        builder.connect(case1, end)
        Place decision2 = builder.addPlace('P_switch_decision_2')
        Transition case2 = builder.addTransition('T_case_2')
        builder.connect(decision2, case2)
        builder.connect(case2, end)
        builder.setStartPlace(start)
        builder.setEndPlace(end)
        builder.addSwitchDescriptor(new SwitchDescriptor(decision1.index, 'first?', [case1.index]))
        builder.addSwitchDescriptor(new SwitchDescriptor(decision2.index, 'second?', [case2.index]))
        PetriNetStructure twoSwitchNet = builder.build()
        Marking marking = new Marking(twoSwitchNet.places.size())
        marking.addToken(decision2, new Token('token-5', null, [:]))

        when:
        SwitchSupport.assertNoStalledSwitchToken(twoSwitchNet, marking, [])

        then: 'the first descriptor has no token and the second one is reported'
        def ex = thrown(IllegalStateException)
        ex.message.contains("No matching case in switch 'second?'")
        ex.message.contains('token-5')
    }

    def 'assertNoStalledSwitchToken tolerates a structure whose switch descriptor list is null'() {
        given: 'a structure reporting null switch descriptors'
        PetriNetStructure net = Stub(PetriNetStructure) {
            getSwitchDescriptors() >> null
        }

        when: 'the assertion runs'
        SwitchSupport.assertNoStalledSwitchToken(net, new Marking(0), [])

        then: 'no exception is thrown'
        notThrown(IllegalStateException)
    }

    def 'assertNoStalledSwitchToken throws when the enabled transition list is null and a token waits at the decision place'() {
        given: 'a token stalled at the decision place and a null enabled list'
        PetriNetStructure net = buildSwitchNet()
        Marking marking = markingWithTokenAt(net, 'P_switch_decision', new Token('token-6', null, [:]))

        when:
        SwitchSupport.assertNoStalledSwitchToken(net, marking, null)

        then: 'the stall is still reported instead of causing a NullPointerException'
        def ex = thrown(IllegalStateException)
        ex.message.contains("No matching case in switch 'mode?'")
        ex.message.contains('token-6')
    }

    def 'private constructor is invocable via reflection so the utility class stays non-instantiable'() {
        when:
        def ctor = SwitchSupport.getDeclaredConstructor()
        ctor.accessible = true
        def instance = ctor.newInstance()

        then:
        instance != null
    }
}
