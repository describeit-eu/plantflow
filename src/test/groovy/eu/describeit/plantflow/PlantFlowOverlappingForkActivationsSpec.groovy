package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Token
import eu.describeit.plantflow.engine.Transition
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Behavior-focused tests for independent overlapping Fork activations:
 * each activation selects its own first completed Parallel Branch and
 * continues exactly once with that branch's Token, even when branch
 * handlers return replacement Tokens, when Markings are copied or
 * serialized, and when separate custom Markings execute side by side.
 */
class PlantFlowOverlappingForkActivationsSpec extends Specification {

    private static Transition transitionByLabel(PlantFlow engine, String label) {
        return engine.petriNet.transitions.find { it.label == label }
    }

    private static Transition transitionByAction(PlantFlow engine, String actionKey) {
        return engine.petriNet.transitions.find { it.actionKey == actionKey }
    }

    private static HandlerRegistry branchRegistry(List<String> finalizeLog = null) {
        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [branch: 'a']) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [branch: 'b']) }
        registry.registerAction('after merge') { ExecutionContext ctx, Token tok ->
            finalizeLog?.add(tok.payload)
            return tok
        }
        return registry
    }

    def 'interleaved overlapping activations each continue exactly once with their own first completed branch token'() {
        given: 'a fork workflow seeded with two tokens, creating two overlapping activations'
        def puml = forkPuml([['branch a'], ['branch b']], ['after merge'])
        def finalizeLog = []
        def engine = PlantFlow.from(puml, branchRegistry(finalizeLog))
        engine.seedToken(Token.of([run: 1]))
        engine.seedToken(Token.of([run: 2]))
        engine.fire(transitionByLabel(engine, 'T_fork_1'))
        engine.fire(transitionByLabel(engine, 'T_fork_1'))

        when: 'the first activation completes branch a and wins the merge'
        engine.fire(transitionByAction(engine, 'branch a'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_1'))

        and: 'the same activation later completes branch b, a late arrival for it'
        engine.fire(transitionByAction(engine, 'branch b'))

        then: 'the late arrival does not repeat or steal a continuation, the other activation is still pending'
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'))
        engine.petriNet.marking.getPendingForkActivations(transitionByLabel(engine, 'T_fork_1').index) == 1

        when: 'the second activation completes its own first branch and wins'
        engine.fire(transitionByAction(engine, 'branch a'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_1'))

        and: 'the second activation later completes branch b'
        engine.fire(transitionByAction(engine, 'branch b'))

        then: 'no further merge continuation is available for either activation'
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_1'))
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'))

        when: 'the post-merge action runs once per continuation'
        engine.fire(transitionByAction(engine, 'after merge'))
        engine.fire(transitionByAction(engine, 'after merge'))

        then: 'each activation continued exactly once with its own first completed branch token'
        engine.isCompleted()
        engine.getEndTokens().size() == 2
        engine.getEndTokens()*.payload.toSet() == [[run: 1, branch: 'a'], [run: 2, branch: 'a']].toSet()
        finalizeLog.toSet() == [[run: 1, branch: 'a'], [run: 2, branch: 'a']].toSet()
    }

    def 'handler-returned replacement tokens keep activation correlation across overlapping activations'() {
        given: 'branch handlers that replace the token entirely, keyed by the run payload'
        def puml = forkPuml([['branch a'], ['branch b']], ['after merge'])
        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> Token.of([run: tok.payload.run, replaced: 'a']) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> Token.of([run: tok.payload.run, replaced: 'b']) }
        registry.registerAction('after merge') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of([run: 1]))
        engine.seedToken(Token.of([run: 2]))
        engine.fire(transitionByLabel(engine, 'T_fork_1'))
        engine.fire(transitionByLabel(engine, 'T_fork_1'))

        when: 'the first activation wins via branch a with a replacement token, then completes branch b late'
        engine.fire(transitionByAction(engine, 'branch a'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_1'))
        engine.fire(transitionByAction(engine, 'branch b'))

        then: 'the replacement token did not lose the activation, so the late branch b cannot continue'
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'))

        when: 'the second activation completes its own first branch with its own replacement token'
        engine.fire(transitionByAction(engine, 'branch a'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_1'))
        engine.fire(transitionByAction(engine, 'branch b'))
        engine.fire(transitionByAction(engine, 'after merge'))
        engine.fire(transitionByAction(engine, 'after merge'))

        then: 'each activation continued once with its own replacement token'
        engine.isCompleted()
        engine.getEndTokens().size() == 2
        engine.getEndTokens()*.payload.toSet() == [[run: 1, replaced: 'a'], [run: 2, replaced: 'a']].toSet()
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'))
    }

    def 'separate custom Markings do not share activation state'() {
        given: 'two independent custom markings on the same fork workflow'
        def puml = forkPuml([['branch a'], ['branch b']])
        def engine = PlantFlow.from(puml, branchRegistry())
        def context = new ExecutionContext()
        def marking1 = new Marking(engine.petriNet.places.size())
        marking1.addToken(engine.petriNet.startPlace, Token.of([run: 1]))
        def marking2 = new Marking(engine.petriNet.places.size())
        marking2.addToken(engine.petriNet.startPlace, Token.of([run: 2]))

        when: 'each marking activates the fork and completes a different branch first'
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking1, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking2, context)
        engine.fire(transitionByAction(engine, 'branch a'), marking1, context)
        engine.fire(transitionByAction(engine, 'branch b'), marking2, context)

        then: 'each marking enables only its own winner merge transition'
        engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_1'), marking1, context)
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'), marking1, context)
        engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'), marking2, context)
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_1'), marking2, context)

        when: 'both markings continue and finish'
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_1'), marking1, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_2'), marking2, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), marking1, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), marking2, context)
        engine.fire(transitionByAction(engine, 'branch b'), marking1, context)
        engine.fire(transitionByAction(engine, 'branch a'), marking2, context)

        then: 'each marking continued exactly once with its own winner and late arrivals stay suppressed'
        marking1.getTokenCount(engine.petriNet.endPlace) == 1
        marking1.getTokens(engine.petriNet.endPlace)[0].payload == [run: 1, branch: 'a']
        marking2.getTokenCount(engine.petriNet.endPlace) == 1
        marking2.getTokens(engine.petriNet.endPlace)[0].payload == [run: 2, branch: 'b']
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'), marking1, context)
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_1'), marking2, context)
    }

    def 'a copied marking preserves already-selected activation state and continues independently'() {
        given: 'a marking whose activation already selected its winner, then copied'
        def puml = forkPuml([['branch a'], ['branch b']])
        def engine = PlantFlow.from(puml, branchRegistry())
        def context = new ExecutionContext()
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([run: 1]))
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking, context)
        engine.fire(transitionByAction(engine, 'branch a'), marking, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_1'), marking, context)

        when: 'the marking is copied after the winner was selected'
        def copied = marking.copy()

        then: 'the copy preserves both the selected and the exhausted pending state'
        def forkIndex = transitionByLabel(engine, 'T_fork_1').index
        copied.getPendingForkActivations(forkIndex) == 0
        copied.getSelectedForkActivations(forkIndex).size() == 1
        marking.getSelectedForkActivations(forkIndex).size() == 1

        when: 'the late branch completes on the copy and the copy finishes'
        engine.fire(transitionByAction(engine, 'branch b'), copied, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), copied, context)

        then: 'the late arrival cannot trigger a second continuation on the copy'
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'), copied, context)
        copied.getTokenCount(engine.petriNet.endPlace) == 1
        copied.getTokens(engine.petriNet.endPlace)[0].payload == [run: 1, branch: 'a']

        when: 'the original marking finishes independently with the same semantics'
        engine.fire(transitionByAction(engine, 'branch b'), marking, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), marking, context)

        then:
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'), marking, context)
        marking.getTokenCount(engine.petriNet.endPlace) == 1
        marking.getTokens(engine.petriNet.endPlace)[0].payload == [run: 1, branch: 'a']
    }

    def 'serialization round-trip preserves in-progress overlapping activation state'() {
        given: 'two overlapping activations serialized mid-flight'
        def puml = forkPuml([['branch a'], ['branch b']])
        def engine = PlantFlow.from(puml, branchRegistry())
        def context = new ExecutionContext()
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([run: 1]))
        marking.addToken(engine.petriNet.startPlace, Token.of([run: 2]))
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking, context)

        when: 'the marking is serialized and restored'
        def json = Marshaller.toJson(marking)
        def restored = Marshaller.fromJson(json, Marking)

        then: 'both pending activations and token correlations survive the round-trip'
        def forkIndex = transitionByLabel(engine, 'T_fork_1').index
        restored.getPendingForkActivations(forkIndex) == 2

        when: 'the interleaved scenario runs on the restored marking'
        engine.fire(transitionByAction(engine, 'branch a'), restored, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_1'), restored, context)
        engine.fire(transitionByAction(engine, 'branch b'), restored, context)

        then: 'the first activation selected its winner and its late branch b cannot continue'
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'), restored, context)
        restored.getPendingForkActivations(forkIndex) == 1

        when: 'the second activation completes its own branch and both finish'
        engine.fire(transitionByAction(engine, 'branch a'), restored, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_1'), restored, context)
        engine.fire(transitionByAction(engine, 'branch b'), restored, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), restored, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), restored, context)

        then: 'each restored activation continued exactly once with its own winner token'
        restored.getTokenCount(engine.petriNet.endPlace) == 2
        restored.getTokens(engine.petriNet.endPlace)*.payload.toSet() == [[run: 1, branch: 'a'], [run: 2, branch: 'a']].toSet()
        restored.getPendingForkActivations(forkIndex) == 0
        restored.getSelectedForkActivations(forkIndex).size() == 2
    }

    def 'serialization round-trip preserves already-selected activation state'() {
        given: 'a marking serialized after its activation selected the winner'
        def puml = forkPuml([['branch a'], ['branch b']])
        def engine = PlantFlow.from(puml, branchRegistry())
        def context = new ExecutionContext()
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([run: 1]))
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking, context)
        engine.fire(transitionByAction(engine, 'branch a'), marking, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_merge_1'), marking, context)

        when: 'the marking round-trips through JSON'
        def json = Marshaller.toJson(marking)
        def restored = Marshaller.fromJson(json, Marking)

        then: 'the selected state and the pending exhaustion survive'
        def forkIndex = transitionByLabel(engine, 'T_fork_1').index
        restored.getPendingForkActivations(forkIndex) == 0
        restored.getSelectedForkActivations(forkIndex).size() == 1

        when: 'the late branch completes on the restored marking and it finishes'
        engine.fire(transitionByAction(engine, 'branch b'), restored, context)

        then: 'no second continuation is possible after deserialization'
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_merge_2'), restored, context)

        when:
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), restored, context)

        then:
        restored.getTokenCount(engine.petriNet.endPlace) == 1
        restored.getTokens(engine.petriNet.endPlace)[0].payload == [run: 1, branch: 'a']
    }

    def 'stateful and custom-Marking execution APIs behave consistently for overlapping activations'() {
        given: 'the same two-seed fork workflow executed through both APIs'
        def puml = forkPuml([['branch a'], ['branch b']], ['after merge'])

        def stateful = PlantFlow.from(puml, branchRegistry())
        stateful.seedToken(Token.of([run: 1]))
        stateful.seedToken(Token.of([run: 2]))

        def custom = PlantFlow.from(puml, branchRegistry())
        def context = new ExecutionContext()
        def marking = new Marking(custom.petriNet.places.size())
        marking.addToken(custom.petriNet.startPlace, Token.of([run: 1]))
        marking.addToken(custom.petriNet.startPlace, Token.of([run: 2]))

        when: 'the stateful API runs to completion'
        stateful.runUntilEnd()

        and: 'the custom-Marking API is driven with the same interleaving'
        custom.fire(transitionByLabel(custom, 'T_fork_1'), marking, context)
        custom.fire(transitionByLabel(custom, 'T_fork_1'), marking, context)
        custom.fire(transitionByAction(custom, 'branch a'), marking, context)
        custom.fire(transitionByLabel(custom, 'T_fork_1_merge_1'), marking, context)
        custom.fire(transitionByAction(custom, 'branch b'), marking, context)
        custom.fire(transitionByAction(custom, 'branch a'), marking, context)
        custom.fire(transitionByLabel(custom, 'T_fork_1_merge_1'), marking, context)
        custom.fire(transitionByAction(custom, 'branch b'), marking, context)
        custom.fire(transitionByAction(custom, 'after merge'), marking, context)
        custom.fire(transitionByAction(custom, 'after merge'), marking, context)

        then: 'both APIs produce the same two independent continuations'
        stateful.isCompleted()
        stateful.getEndTokens()*.payload.toSet() == [[run: 1, branch: 'a'], [run: 2, branch: 'a']].toSet()
        marking.getTokenCount(custom.petriNet.endPlace) == 2
        marking.getTokens(custom.petriNet.endPlace)*.payload.toSet() == stateful.getEndTokens()*.payload.toSet()
    }
}
