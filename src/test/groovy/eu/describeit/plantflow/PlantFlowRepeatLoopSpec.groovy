package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * End-to-end execution tests for conditional repeat loops (ticket #38).
 * Exercises the public PlantFlow API: PlantFlow.from(puml, registry).runUntilEnd(...).
 */
class PlantFlowRepeatLoopSpec extends Specification {

    private static int remaining(ExecutionContext ctx, int startRemaining) {
        Object value = ctx.get('remaining')
        return value == null ? startRemaining : (value as int)
    }

    private static HandlerRegistry countdownRegistry(List<String> bodyLog, List<String> guardLog, int startRemaining) {
        def registry = new HandlerRegistry()
        registry.registerAction('read data') { ExecutionContext ctx, Token tok ->
            ctx['remaining'] = remaining(ctx, startRemaining) - 1
            int iterations = ((ctx.get('iterations') ?: 0) as int) + 1
            ctx['iterations'] = iterations
            bodyLog << 'read data'
            return tok.withPayload(tok.payload + [readCount: iterations])
        }
        registry.registerAction('generate diagrams') { ExecutionContext ctx, Token tok ->
            bodyLog << 'generate diagrams'
            return tok.withPayload(tok.payload + [diagrams: true])
        }
        registry.registerAction('finalize') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [finalized: true])
        }
        registry.registerGuard('more data?') { ExecutionContext ctx, Token tok ->
            guardLog << 'more data?'
            return remaining(ctx, startRemaining) > 0
        }
        registry.registerGuard('!(more data?)') { ExecutionContext ctx, Token tok ->
            guardLog << '!(more data?)'
            return remaining(ctx, startRemaining) <= 0
        }
        return registry
    }

    // ==================== Public API end-to-end ====================

    def 'repeatWhile.puml runs to completion via the public workflow API'() {
        given: 'repeatWhile.puml with handlers for its body actions and loop condition'
        def pumlFile = new File('src/test/data/puml/repeatWhile.puml')
        def bodyLog = []
        def guardLog = []
        def registry = countdownRegistry(bodyLog, guardLog, 2)

        when: 'running the workflow through the public API'
        def engine = PlantFlow.from(pumlFile, registry)
        engine.runUntilEnd(Token.of(), new ExecutionContext([remaining: 2]))

        then: 'the workflow is completed with an end token present'
        engine.isCompleted()
        engine.getEndToken() != null

        and: 'the loop body ran twice per action and the guard drove the exit'
        bodyLog == ['read data', 'generate diagrams', 'read data', 'generate diagrams']
        engine.executionContext['iterations'] == 2
        guardLog.count('more data?') >= 2
        guardLog.last() == '!(more data?)'

        and: 'the end token carries the accumulated payload'
        verifyEndTokenPayload(engine, [readCount: 2, diagrams: true])
    }

    def 'loop body executes at least once before the first guard evaluation (post-test)'() {
        given: 'a loop whose condition is false from the start'
        def bodyLog = []
        def guardLog = []
        def registry = countdownRegistry(bodyLog, guardLog, 0)
        def engine = PlantFlow.from(repeatPuml('more data?', ['read data'], ['finalize']), registry)

        when:
        engine.runUntilEnd(Token.of(), new ExecutionContext([remaining: 0]))

        then: 'the body ran exactly once although the condition never held'
        engine.isCompleted()
        bodyLog == ['read data']
        guardLog.contains('more data?')
        guardLog.last() == '!(more data?)'
        verifyEndTokenPayload(engine, [readCount: 1, finalized: true])
    }

    def 'iteration count matches loop condition evaluations driven by a context countdown'() {
        given: 'a countdown of 3 in the execution context'
        def bodyLog = []
        def guardLog = []
        def registry = countdownRegistry(bodyLog, guardLog, 3)
        def engine = PlantFlow.from(repeatPuml('more data?', ['read data'], ['finalize']), registry)

        when:
        engine.runUntilEnd(Token.of(), new ExecutionContext([remaining: 3]))

        then: 'three iterations ran and the condition was evaluated after each iteration'
        engine.isCompleted()
        engine.executionContext['iterations'] == 3
        bodyLog.size() == 3
        guardLog.count('more data?') >= 3
        guardLog.last() == '!(more data?)'
        verifyEndTokenPayload(engine, [readCount: 3, finalized: true])
    }

    def 'exiting token carries the accumulated payload visible to post-loop actions'() {
        given: 'a loop whose body accumulates items in the token payload'
        def registry = new HandlerRegistry()
        registry.registerAction('collect item') { ExecutionContext ctx, Token tok ->
            int remaining = (ctx.get('remaining') ?: 0) as int
            ctx['remaining'] = remaining - 1
            def items = (tok.payload['items'] ?: []) as List
            return tok.withPayload(tok.payload + [items: items + ["item-${items.size() + 1}"]])
        }
        registry.registerAction('summarize') { ExecutionContext ctx, Token tok ->
            def items = (tok.payload['items'] ?: []) as List
            return tok.withPayload(tok.payload + [itemCount: items.size(), summarized: true])
        }
        registry.registerGuard('more data?') { ExecutionContext ctx, Token tok ->
            return ((ctx.get('remaining') ?: 0) as int) > 0
        }
        registry.registerGuard('!(more data?)') { ExecutionContext ctx, Token tok ->
            return ((ctx.get('remaining') ?: 0) as int) <= 0
        }
        def engine = PlantFlow.from(repeatPuml('more data?', ['collect item'], ['summarize']), registry)
        def context = new ExecutionContext([remaining: 3])

        when:
        engine.runUntilEnd(Token.of(), context)

        then: 'the post-loop action saw the accumulated items and the end token carries them'
        engine.isCompleted()
        verifyEndTokenPayload(engine, [items: ['item-1', 'item-2', 'item-3'], itemCount: 3, summarized: true])
    }

    def 'loop condition reads execution context variables mutated by action handlers'() {
        given: 'an action handler that mutates a context flag consumed by the loop condition'
        def registry = new HandlerRegistry()
        registry.registerAction('flip flag') { ExecutionContext ctx, Token tok ->
            ctx['done'] = true
            return tok
        }
        registry.registerGuard('working?') { ExecutionContext ctx, Token tok ->
            return ctx.get('done') != true
        }
        registry.registerGuard('!(working?)') { ExecutionContext ctx, Token tok ->
            return ctx.get('done') == true
        }
        def engine = PlantFlow.from(repeatPuml('working?', ['flip flag']), registry)

        when:
        engine.runUntilEnd(Token.of())

        then: 'the first iteration flipped the flag and the negated exit fired on the second guard evaluation'
        engine.isCompleted()
        engine.executionContext['done'] == true
        engine.getEndToken() != null
    }

    // ==================== Per-marking isolation ====================

    def 'separate runs do not share loop state'() {
        given: 'two independent runs with different countdowns over the same puml'
        def puml = repeatPuml('more data?', ['read data'], ['finalize'])

        def registry1 = countdownRegistry([], [], 1)
        def registry2 = countdownRegistry([], [], 4)
        def engine1 = PlantFlow.from(puml, registry1)
        def engine2 = PlantFlow.from(puml, registry2)

        when:
        engine1.runUntilEnd(Token.of(), new ExecutionContext([remaining: 1]))
        engine2.runUntilEnd(Token.of(), new ExecutionContext([remaining: 4]))

        then: 'each run looped according to its own context countdown'
        engine1.isCompleted()
        engine2.isCompleted()
        engine1.executionContext['iterations'] == 1
        engine2.executionContext['iterations'] == 4
        engine1.getEndToken().payload['readCount'] == 1
        engine2.getEndToken().payload['readCount'] == 4
    }

    def 'Marking.copy() of an in-progress loop resumes correctly'() {
        given: 'a repeat loop stepped to the decision place after the first iteration'
        def registry = countdownRegistry([], [], 2)
        def puml = repeatPuml('more data?', ['read data'], ['finalize'])
        def engine = PlantFlow.from(puml, registry)
        def context = new ExecutionContext([remaining: 2])
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of())
        def decision = engine.petriNet.places.find { it.label == 'P_repeat_1_decision' }

        while (marking.getTokenCount(decision) == 0) {
            def enabled = engine.getEnabledTransitions(marking, context)
            assert !enabled.isEmpty()
            engine.fire(enabled[0], marking, context)
        }

        when: 'the in-progress marking is copied and the copy is resumed on a fresh engine'
        def copy = marking.copy()
        def resumed = new PlantFlow(engine.petriNet.structure, registry, context)
        while (true) {
            def enabled = resumed.getEnabledTransitions(copy, context)
            if (enabled.isEmpty()) break
            resumed.fire(enabled[0], copy, context)
        }

        then: 'the copy completes the remaining iteration and reaches the end place'
        copy.getTokenCount(resumed.petriNet.endPlace) == 1
        context['iterations'] == 2
        copy.getTokens(resumed.petriNet.endPlace)[0].payload == [readCount: 2, finalized: true]

        and: 'the original in-progress marking is untouched by the resumed run'
        marking.getTokenCount(decision) == 1
        marking.getTokenCount(resumed.petriNet.endPlace) == 0
    }

    def 'Marking serialization round-trip of an in-progress loop resumes correctly'() {
        given: 'a repeat loop stepped to the decision place after the first iteration'
        def registry = countdownRegistry([], [], 2)
        def puml = repeatPuml('more data?', ['read data'], ['finalize'])
        def engine = PlantFlow.from(puml, registry)
        def context = new ExecutionContext([remaining: 2])
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of())
        def decision = engine.petriNet.places.find { it.label == 'P_repeat_1_decision' }

        while (marking.getTokenCount(decision) == 0) {
            def enabled = engine.getEnabledTransitions(marking, context)
            assert !enabled.isEmpty()
            engine.fire(enabled[0], marking, context)
        }

        when: 'the marking is serialized and deserialized, then resumed'
        def json = Marshaller.toJson(marking)
        def restored = Marshaller.fromJson(json, Marking)
        def resumed = new PlantFlow(engine.petriNet.structure, registry, context)
        while (true) {
            def enabled = resumed.getEnabledTransitions(restored, context)
            if (enabled.isEmpty()) break
            resumed.fire(enabled[0], restored, context)
        }

        then: 'the restored marking resumes the loop and completes'
        restored.getTokenCount(resumed.petriNet.endPlace) == 1
        context['iterations'] == 2
        restored.getTokens(resumed.petriNet.endPlace)[0].payload == [readCount: 2, finalized: true]

        and: 'the original marking still holds the in-progress token at the decision place'
        marking.getTokenCount(decision) == 1
        marking.getTokenCount(resumed.petriNet.endPlace) == 0
    }

    // ==================== Regression: malformed terminator rejection ====================

    def 'malformed repeat terminator is still rejected through the public API'() {
        given:
        def puml = '@startuml\nstart\nrepeat\n  :a;\nrepeat while (broken\nend\n@enduml'

        when:
        PlantFlow.from(puml, new HandlerRegistry())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('Malformed')
    }
}
