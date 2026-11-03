package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Token
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Behavior-focused tests for resuming in-progress synchronizing Fork/Join
 * activations (ticket #31): a Marking that is copied or serialized while a
 * Join is waiting must resume without losing Fork activation correlation or
 * mixing branch results of overlapping activations.
 */
class PlantFlowResumeJoinSpec extends Specification {

    private static final List<Map> EXPECTED_PAYLOADS = [
        [run: 1, a: true, b: true, winner: 'b', after_join: true],
        [run: 2, a: true, b: true, winner: 'b', after_join: true],
    ]

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

    private static List<Marking> snapshotsOf(Marking original, String mode) {
        switch (mode) {
            case 'a copy':
                return [original.copy()]
            case 'a JSON round-trip':
                return [Marshaller.fromJson(Marshaller.toJson(original), Marking)]
            default:
                return [original.copy(), Marshaller.fromJson(Marshaller.toJson(original), Marking)]
        }
    }

    private static void assertResumedToExpectedEndings(PlantFlow engine, Marking marking, ExecutionContext context) {
        assert marking.getTokenCount(engine.petriNet.endPlace) == 2
        assert marking.getTokens(engine.petriNet.endPlace)*.payload.toSet() == EXPECTED_PAYLOADS.toSet()
        assert !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), marking, context)
    }

    @Unroll
    def 'an in-progress join marking resumed via #mode keeps exactly one correlated continuation per activation'() {
        given: 'a two-activation fork/join in progress with interleaved branch tokens'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def finalizeLog = []
        def engine = PlantFlow.from(puml, joinRegistry(finalizeLog))
        def context = new ExecutionContext()
        def original = inProgressMarking(engine, context)

        when: 'the marking is snapshotted via the given mode'
        def snapshots = snapshotsOf(original, mode)

        then: 'each snapshot sees the first activation complete but not the second'
        snapshots.each { Marking snapshot ->
            assert engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), snapshot, context)
            assert snapshot.getTokenCount(engine.petriNet.endPlace) == 0
        }
        original.getTokenCount(engine.petriNet.endPlace) == 0

        when: 'each snapshot resumes to the end'
        snapshots.each { Marking snapshot -> resumeToEnd(engine, snapshot, context) }

        then: 'each snapshot produced exactly one correctly correlated continuation per activation'
        snapshots.each { Marking snapshot -> assertResumedToExpectedEndings(engine, snapshot, context) }
        finalizeLog.toSet() == EXPECTED_PAYLOADS.toSet()

        and: 'the original marking was not advanced by the snapshots and still resumes independently'
        original.getTokenCount(engine.petriNet.endPlace) == 0
        engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), original, context)
        resumeToEnd(engine, original, context)
        assertResumedToExpectedEndings(engine, original, context)

        where:
        mode << ['a copy', 'a JSON round-trip', 'a copy and a JSON round-trip']
    }

    def 'a marking serialized in the legacy count-based format resumes an in-progress fork/join with correlation intact'() {
        given: 'an in-progress two-activation fork/join serialized in the legacy format without per-activation fields'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def engine = PlantFlow.from(puml, joinRegistry())
        def context = new ExecutionContext()
        def marking = inProgressMarking(engine, context)
        def legacyJson = legacyCountBasedJson(marking)

        when: 'the legacy marking is deserialized'
        def restored = Marshaller.fromJson(legacyJson, Marking)

        then: 'the count-based forkActivations field rebuilt the pending activations'
        legacyJson.contains('"forkActivations"')
        restored.getPendingForkActivations(findTransitionByLabel(engine, 'T_fork_1').index) == 2
        engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'), restored, context)
        restored.getTokenCount(engine.petriNet.endPlace) == 0

        when: 'the legacy marking resumes through the public API'
        resumeToEnd(engine, restored, context)

        then: 'exactly one correctly correlated continuation per activation reached the end with diagram-order merged payloads'
        assertResumedToExpectedEndings(engine, restored, context)
    }

    private static String legacyCountBasedJson(Marking marking) {
        def mapper = new ObjectMapper()
        def root = mapper.readTree(Marshaller.toJson(marking)) as ObjectNode
        root.remove(['pendingForkActivationIds', 'selectedForkActivationIds', 'tokenActivations', 'nextForkActivationId'])
        return mapper.writeValueAsString(root)
    }
}
