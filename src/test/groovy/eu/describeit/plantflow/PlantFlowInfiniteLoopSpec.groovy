package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * End-to-end execution tests for the infinite loop terminated by detach (ticket #39).
 * Exercises the public PlantFlow API with src/test/data/puml/repeatInfinite.puml:
 * `repeat while (true)` loops with a constant-true loop-back guard and
 * `-[hidden]-> detach` terminates the Token without producing a continuation.
 */
class PlantFlowInfiniteLoopSpec extends Specification {

    private static HandlerRegistry infiniteRegistry(List<String> bodyLog) {
        def registry = new HandlerRegistry()
        registry.registerAction('read data') { ExecutionContext ctx, Token tok ->
            int iterations = ((ctx.get('iterations') ?: 0) as int) + 1
            ctx['iterations'] = iterations
            bodyLog << 'read data'
            return tok.withPayload(tok.payload + [readCount: iterations])
        }
        registry.registerAction('generate diagrams') { ExecutionContext ctx, Token tok ->
            bodyLog << 'generate diagrams'
            return tok.withPayload(tok.payload + [diagrams: true])
        }
        return registry
    }

    private static void stepUntilDetachEnabled(PlantFlow engine, int minIterations) {
        def detach = findTransitionByLabel(engine, 'T_repeat_1_detach')
        int guard = 0
        while ((!engine.isEnabled(detach) || iterations(engine) < minIterations) && guard++ < 1000) {
            assert engine.step()
        }
        assert engine.isEnabled(detach)
    }

    private static int iterations(PlantFlow engine) {
        return (engine.executionContext.get('iterations') ?: 0) as int
    }

    // ==================== Public API end-to-end ====================

    def 'repeatInfinite.puml parses, compiles and starts executing via the public workflow API'() {
        given: 'repeatInfinite.puml with handlers for its body actions'
        def bodyLog = []
        def engine = PlantFlow.from(new File('src/test/data/puml/repeatInfinite.puml'), infiniteRegistry(bodyLog))

        when: 'a token is seeded and the flow is stepped'
        engine.seedToken(Token.of())
        def stepped = engine.step()

        then: 'the first body action executed and a continuation is enabled'
        stepped
        bodyLog == ['read data']
        !engine.getEnabledTransitions().isEmpty()
    }

    def 'loop-back guard for repeat while (true) is constant true'() {
        given: 'the compiled infinite loop net'
        def engine = PlantFlow.from(new File('src/test/data/puml/repeatInfinite.puml'), infiniteRegistry([]))

        when: 'the loop-back transition is inspected'
        def loopBack = findTransitionByLabel(engine, 'T_repeat_1_loop_back')

        then: 'its guard key is the constant true expression'
        loopBack != null
        loopBack.guardKey == 'true'

        and: 'the constant-true guard evaluates without any registered guard predicate'
        engine.seedToken(Token.of())
        stepUntilDetachEnabled(engine, 1)
        engine.isEnabled(loopBack)
    }

    def 'firing the detach transition consumes the token and produces no output token'() {
        given: 'an infinite loop stepped for three iterations'
        def bodyLog = []
        def engine = PlantFlow.from(new File('src/test/data/puml/repeatInfinite.puml'), infiniteRegistry(bodyLog))
        engine.seedToken(Token.of())
        stepUntilDetachEnabled(engine, 3)

        when: 'the detach transition is fired'
        def detach = findTransitionByLabel(engine, 'T_repeat_1_detach')
        def fired = engine.fire(detach)

        then: 'the detach fired and consumed the token without producing a continuation'
        fired
        engine.getEnabledTransitions().isEmpty()
        engine.getEndTokens().isEmpty()
        engine.petriNet.isNetEmpty()

        and: 'the loop body ran exactly three times before detaching'
        iterations(engine) == 3
        bodyLog == ['read data', 'generate diagrams'] * 3
    }

    def 'no continuation is enabled after detach and the flow is done with zero tokens at the end place'() {
        given: 'an infinite loop built via the repeatPuml helper with detach'
        def bodyLog = []
        def registry = infiniteRegistry(bodyLog)
        def registryForHelper = new HandlerRegistry()
        registryForHelper.registerAction('read data') { ExecutionContext ctx, Token tok ->
            int iterations = ((ctx.get('iterations') ?: 0) as int) + 1
            ctx['iterations'] = iterations
            return tok
        }
        def engine = PlantFlow.from(repeatPuml('true', ['read data'], [], true), registryForHelper)
        engine.seedToken(Token.of())
        def detach = findTransitionByLabel(engine, 'T_repeat_1_detach')
        int guard = 0
        while (!engine.isEnabled(detach) && guard++ < 100) {
            assert engine.step()
        }

        when: 'detach is fired'
        engine.fire(detach)

        then: 'nothing remains enabled and no end token was produced'
        engine.getEnabledTransitions().isEmpty()
        engine.getEndToken() == null
        engine.getEndTokens().isEmpty()
        engine.petriNet.getTokenCount(engine.petriNet.endPlace) == 0
        !engine.isCompleted()
        engine.petriNet.isNetEmpty()
    }
}
