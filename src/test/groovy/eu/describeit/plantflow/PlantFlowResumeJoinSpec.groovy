package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Behavior-focused tests for resuming in-progress synchronizing Fork/Join
 * activations (ticket #31): a Marking that is copied or serialized while a
 * Join is waiting must resume without losing Fork activation correlation or
 * mixing branch results of overlapping activations.
 */
class PlantFlowResumeJoinSpec extends Specification {

    private static HandlerRegistry joinRegistry(List<String> finalizeLog = null) {
        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok ->
            tok.withPayload(tok.payload + [a: true, winner: 'a'])
        }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok ->
            tok.withPayload(tok.payload + [b: true, winner: 'b'])
        }
        registry.registerAction('after join') { ExecutionContext ctx, Token tok ->
            def result = tok.withPayload(tok.payload + [after_join: true])
            finalizeLog?.add(result.payload)
            return result
        }
        return registry
    }

    private static Marking inProgressMarking(PlantFlow engine, ExecutionContext context) {
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([run: 1]))
        marking.addToken(engine.petriNet.startPlace, Token.of([run: 2]))
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'), marking, context)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'), marking, context)
        engine.fire(findTransitionByAction(engine, 'branch a'), marking, context)
        engine.fire(findTransitionByAction(engine, 'branch a'), marking, context)
        engine.fire(findTransitionByAction(engine, 'branch b'), marking, context)
        return marking
    }

    private static void resumeToEnd(PlantFlow engine, Marking marking, ExecutionContext context) {
        engine.fire(findTransitionByAction(engine, 'branch b'), marking, context)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_join'), marking, context)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_join'), marking, context)
        engine.fire(findTransitionByAction(engine, 'after join'), marking, context)
        engine.fire(findTransitionByAction(engine, 'after join'), marking, context)
    }

    private static final List<Map> EXPECTED_PAYLOADS = [
        [run: 1, a: true, b: true, winner: 'b', after_join: true],
        [run: 2, a: true, b: true, winner: 'b', after_join: true],
    ]

    def 'a copied in-progress marking with interleaved overlapping activations resumes with one correlated continuation per activation'() {
        given: 'a two-activation fork/join in progress with interleaved branch tokens, then copied'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def finalizeLog = []
        def engine = PlantFlow.from(puml, joinRegistry(finalizeLog))
        def context = new ExecutionContext()
        def marking = inProgressMarking(engine, context)

        when: 'the marking is copied mid-flight'
        def copied = marking.copy()

        then: 'the copy sees the first activation complete but not the second'
        engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), copied, context)
        copied.getTokenCount(engine.petriNet.endPlace) == 0

        when: 'the copy resumes: join for the first activation, second branch completes, join again'
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_join'), copied, context)
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), copied, context)
        engine.fire(findTransitionByAction(engine, 'branch b'), copied, context)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_join'), copied, context)
        engine.fire(findTransitionByAction(engine, 'after join'), copied, context)
        engine.fire(findTransitionByAction(engine, 'after join'), copied, context)

        then: 'the copy produced exactly one correctly correlated continuation per activation'
        copied.getTokenCount(engine.petriNet.endPlace) == 2
        copied.getTokens(engine.petriNet.endPlace)*.payload.toSet() == EXPECTED_PAYLOADS.toSet()
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), copied, context)

        and: 'the original marking was not advanced by the copy and can still resume independently'
        marking.getTokenCount(engine.petriNet.endPlace) == 0
        engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), marking, context)
        resumeToEnd(engine, marking, context)
        marking.getTokenCount(engine.petriNet.endPlace) == 2
        marking.getTokens(engine.petriNet.endPlace)*.payload.toSet() == EXPECTED_PAYLOADS.toSet()
    }

    def 'a serialized and deserialized in-progress marking resumes with correct join behavior and deterministic payloads'() {
        given: 'a two-activation fork/join in progress, serialized mid-flight'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def finalizeLog = []
        def engine = PlantFlow.from(puml, joinRegistry(finalizeLog))
        def context = new ExecutionContext()
        def marking = inProgressMarking(engine, context)

        when: 'the marking round-trips through JSON and resumes with branches completing out of order'
        def restored = Marshaller.fromJson(Marshaller.toJson(marking), Marking)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_join'), restored, context)
        engine.fire(findTransitionByAction(engine, 'branch b'), restored, context)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_join'), restored, context)
        engine.fire(findTransitionByAction(engine, 'after join'), restored, context)
        engine.fire(findTransitionByAction(engine, 'after join'), restored, context)

        then: 'each activation continued exactly once with deterministic diagram-order merged payloads'
        restored.getTokenCount(engine.petriNet.endPlace) == 2
        restored.getTokens(engine.petriNet.endPlace)*.payload.toSet() == EXPECTED_PAYLOADS.toSet()
        finalizeLog.toSet() == EXPECTED_PAYLOADS.toSet()

        and: 'the original marking is untouched by the restored one'
        marking.getTokenCount(engine.petriNet.endPlace) == 0
    }

    def 'copied, deserialized, and original markings resume independently without interfering'() {
        given: 'one in-progress marking from which a copy and a serialization round-trip are made'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def engine = PlantFlow.from(puml, joinRegistry())
        def context = new ExecutionContext()
        def original = inProgressMarking(engine, context)
        def copied = original.copy()
        def restored = Marshaller.fromJson(Marshaller.toJson(original), Marking)

        when: 'the copy resumes fully while the others stay in progress'
        resumeToEnd(engine, copied, context)

        then: 'the original and the restored marking still wait for their second branch'
        copied.getTokenCount(engine.petriNet.endPlace) == 2
        original.getTokenCount(engine.petriNet.endPlace) == 0
        restored.getTokenCount(engine.petriNet.endPlace) == 0
        engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), original, context)
        engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), restored, context)

        when: 'the restored marking resumes fully'
        resumeToEnd(engine, restored, context)

        then: 'the original is still untouched'
        restored.getTokenCount(engine.petriNet.endPlace) == 2
        original.getTokenCount(engine.petriNet.endPlace) == 0

        when: 'the original resumes fully'
        resumeToEnd(engine, original, context)

        then: 'all three markings produced the same two correctly correlated continuations'
        [copied, restored, original].each { Marking m ->
            assert m.getTokenCount(engine.petriNet.endPlace) == 2
            assert m.getTokens(engine.petriNet.endPlace)*.payload.toSet() == EXPECTED_PAYLOADS.toSet()
        }
    }

    def 'a marking serialized in the legacy count-based format still deserializes and executes'() {
        given: 'a legacy Marking JSON without the per-activation correlation fields'
        def puml = linearPuml('action 1')
        def engine = PlantFlow.from(puml, payloadRegistry(['action 1']))
        def context = new ExecutionContext()
        def json = '{"tokens":[[{"id":"t1","timestamp":"2026-01-01T00:00:00Z","payload":{"initial":true}}],[]],"forkActivations":{"0":1}}'

        when: 'the legacy marking is deserialized and the workflow runs on it'
        def restored = Marshaller.fromJson(json, Marking)
        engine.fire(findTransitionByAction(engine, 'action 1'), restored, context)

        then: 'the legacy marking still executes ordinary transitions to the end'
        restored.getTokenCount(engine.petriNet.endPlace) == 1
        restored.getTokens(engine.petriNet.endPlace)[0].payload == [initial: true, action_1: true]
    }
}
