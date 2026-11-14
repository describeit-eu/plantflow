package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class PlantFlowNestedRepeatLoopSpec extends Specification {

    private static final String NESTED_REPEAT_LOOP = '''@startuml
start
repeat
  :outer step;
  repeat
    :inner step;
  repeat while (inner more?) is (yes) not (no)
repeat while (outer more?) is (yes) not (no)
:done;
end
@enduml
'''

    private static final int OUTER_ITERATIONS = 2
    private static final int INNER_ITERATIONS = 3

    private static HandlerRegistry nestedLoopRegistry() {
        def registry = new HandlerRegistry()
        registry.registerAction('outer step') { ExecutionContext ctx, Token tok ->
            int outerCount = ((ctx.get('outerCount') ?: 0) as int) + 1
            ctx['outerCount'] = outerCount
            ctx['outerRemaining'] = ((ctx.get('outerRemaining') ?: 0) as int) - 1
            ctx['innerRemaining'] = INNER_ITERATIONS
            return tok.withPayload(tok.payload + [outerCount: outerCount])
        }
        registry.registerAction('inner step') { ExecutionContext ctx, Token tok ->
            int innerCount = ((ctx.get('innerCount') ?: 0) as int) + 1
            ctx['innerCount'] = innerCount
            ctx['innerRemaining'] = ((ctx.get('innerRemaining') ?: 0) as int) - 1
            return tok.withPayload(tok.payload + [innerCount: innerCount])
        }
        registry.registerAction('done') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [done: true])
        }
        registry.registerGuard('inner more?') { ExecutionContext ctx, Token tok ->
            ((ctx.get('innerRemaining') ?: 0) as int) > 0
        }
        registry.registerGuard('!(inner more?)') { ExecutionContext ctx, Token tok ->
            ((ctx.get('innerRemaining') ?: 0) as int) <= 0
        }
        registry.registerGuard('outer more?') { ExecutionContext ctx, Token tok ->
            ((ctx.get('outerRemaining') ?: 0) as int) > 0
        }
        registry.registerGuard('!(outer more?)') { ExecutionContext ctx, Token tok ->
            ((ctx.get('outerRemaining') ?: 0) as int) <= 0
        }
        return registry
    }

    private static ExecutionContext nestedLoopContext() {
        return new ExecutionContext([outerRemaining: OUTER_ITERATIONS])
    }

    private static Marking runToEnd(PlantFlow engine, Marking marking, ExecutionContext context) {
        def current = marking
        while (true) {
            def enabled = engine.getEnabledTransitions(current, context)
            if (enabled.isEmpty()) {
                break
            }
            current = engine.fire(enabled[0], current, context)
        }
        return current
    }

    def 'nested repeat-in-repeat runs inner loop fully on each outer iteration'() {
        given: 'a PlantFlow engine for the nested repeat diagram'
        def engine = PlantFlow.from(NESTED_REPEAT_LOOP, nestedLoopRegistry())

        when: 'running until end via the public API'
        engine.runUntilEnd(Token.of(), nestedLoopContext())

        then: 'the workflow completes with correct nested iteration counts'
        engine.isCompleted()
        engine.executionContext['outerCount'] == OUTER_ITERATIONS
        engine.executionContext['innerCount'] == OUTER_ITERATIONS * INNER_ITERATIONS

        and: 'the end token payload reflects all iterations'
        verifyEndTokenPayload(engine, [outerCount: OUTER_ITERATIONS, innerCount: OUTER_ITERATIONS * INNER_ITERATIONS, done: true])
    }

    def 'serialized in-progress marking of a nested loop resumes to completion with correct iteration counts'() {
        given: 'an engine and a marking advanced mid-loop (after the first outer and inner steps)'
        def engine = PlantFlow.from(NESTED_REPEAT_LOOP, nestedLoopRegistry())
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of())
        def context = nestedLoopContext()
        engine.fire(findTransitionByAction(engine, 'outer step'), marking, context)
        engine.fire(findTransitionByAction(engine, 'inner step'), marking, context)

        when: 'the in-progress marking is serialized, deserialized, and resumed to completion'
        def json = Marshaller.toJson(marking)
        def resumed = Marshaller.fromJson(json, Marking)
        def finalMarking = runToEnd(engine, resumed, context)

        then: 'the deserialized marking reaches the end place with a single token'
        finalMarking.getTokenCount(engine.petriNet.endPlace) == 1
        engine.getEnabledTransitions(finalMarking, context).isEmpty()

        and: 'iteration counts account for the steps fired before serialization'
        context['outerCount'] == OUTER_ITERATIONS
        context['innerCount'] == OUTER_ITERATIONS * INNER_ITERATIONS

        and: 'the end token payload is correct'
        finalMarking.getTokens(engine.petriNet.endPlace)[0].payload == [
            outerCount: OUTER_ITERATIONS,
            innerCount: OUTER_ITERATIONS * INNER_ITERATIONS,
            done      : true
        ]

        and: 'the original marking is unchanged by the resumed execution'
        marking.getTokenCount(engine.petriNet.endPlace) == 0
    }

    def 'a copied in-progress marking of a nested loop completes independently while the original stays untouched'() {
        given: 'an engine and a marking captured mid-loop (after the first outer step)'
        def engine = PlantFlow.from(NESTED_REPEAT_LOOP, nestedLoopRegistry())
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of())
        def context = nestedLoopContext()
        engine.fire(findTransitionByAction(engine, 'outer step'), marking, context)
        def capturePlace = engine.petriNet.places.find { it.label == 'P_repeat_1_body_1' }
        assert marking.getTokenCount(capturePlace) == 1

        when: 'the marking is copied and only the copy is run to completion'
        def copy = marking.copy()
        def finalMarking = runToEnd(engine, copy, context)

        then: 'the copy completes with correct iteration counts and payload'
        finalMarking.getTokenCount(engine.petriNet.endPlace) == 1
        engine.getEnabledTransitions(finalMarking, context).isEmpty()
        context['outerCount'] == OUTER_ITERATIONS
        context['innerCount'] == OUTER_ITERATIONS * INNER_ITERATIONS
        finalMarking.getTokens(engine.petriNet.endPlace)[0].payload == [
            outerCount: OUTER_ITERATIONS,
            innerCount: OUTER_ITERATIONS * INNER_ITERATIONS,
            done      : true
        ]

        and: 'the original marking is untouched'
        marking.getTokenCount(capturePlace) == 1
        marking.getTokenCount(engine.petriNet.endPlace) == 0
    }
}
