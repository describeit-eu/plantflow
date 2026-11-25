package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Ticket #56: runtime no-match detection for the switch decision place.
 * Verifies that a token routed by case guards takes exactly one case branch
 * (first match wins) and that a token with no matching case guard fails
 * loudly with an IllegalStateException naming the switch and the token.
 */
class PlantFlowSwitchExecutionSpec extends Specification {

    private static final List<String> CONDITIONS = ['condA', 'condB', 'condC']
    private static final List<String> ACTIONS = ['action A', 'action B', 'action C']

    private static HandlerRegistry switchRegistry(List<String> executionLog, Set<String> matchingConditions) {
        def registry = new HandlerRegistry()
        ACTIONS.each { String action ->
            registry.registerAction(action) { ExecutionContext ctx, Token tok ->
                executionLog << action
                return tok
            }
        }
        registry.registerAction('after') { ExecutionContext ctx, Token tok ->
            executionLog << 'after'
            return tok
        }
        CONDITIONS.each { String condition ->
            registry.registerGuard(condition) { ExecutionContext ctx, Token tok ->
                matchingConditions.contains(condition)
            }
        }
        return registry
    }

    def 'a token whose context matches exactly one case guard fires only that case action'() {
        given: 'a three-case switch where only condB matches'
        def executionLog = []
        def engine = PlantFlow.from(
            switchPuml(CONDITIONS, ACTIONS, ['after'], 'test?'),
            switchRegistry(executionLog, ['condB'] as Set))

        when: 'the flow runs to completion'
        engine.runUntilEnd(token('token-1'), new ExecutionContext())

        then: 'the workflow completed and only the matching case action and the post-switch action fired'
        engine.isCompleted()
        engine.getEndToken() != null
        executionLog == ['action B', 'after']
    }

    def 'first match wins when multiple case guards evaluate to true'() {
        given: 'a three-case switch where condA and condB both match'
        def executionLog = []
        def engine = PlantFlow.from(
            switchPuml(CONDITIONS, ACTIONS, ['after'], 'test?'),
            switchRegistry(executionLog, ['condA', 'condB'] as Set))

        when: 'the flow runs to completion'
        engine.runUntilEnd(token('token-2'), new ExecutionContext())

        then: 'only the first matching case action fired, the later matching case did not'
        engine.isCompleted()
        executionLog == ['action A', 'after']
    }

    def 'a token matching no case guard throws IllegalStateException naming the switch expression and token id'() {
        given: 'a two-case switch where no case guard matches'
        def executionLog = []
        def engine = PlantFlow.from(
            switchPuml(['condA', 'condB'], ['action A', 'action B'], [], 'mode?'),
            switchRegistry(executionLog, [] as Set))

        when: 'the flow runs'
        engine.runUntilEnd(token('token-99'), new ExecutionContext())

        then: 'the engine fails loudly instead of stalling silently'
        def ex = thrown(IllegalStateException)
        ex.message.contains("No matching case in switch 'mode?'")
        ex.message.contains('token-99')

        and: 'no case action fired'
        executionLog.isEmpty()
    }

    def 'single-stepping also detects the no-match stall at the switch decision place'() {
        given: 'a two-case switch where no case guard matches'
        def executionLog = []
        def engine = PlantFlow.from(
            switchPuml(['condA', 'condB'], ['action A', 'action B'], [], 'test?'),
            switchRegistry(executionLog, [] as Set))
        engine.seedToken(token('token-7'))

        when: 'the token is stepped onto the switch decision place'
        engine.step()

        and: 'the next step finds no enabled transition'
        engine.step()

        then: 'the stall is reported as an IllegalStateException naming switch and token'
        def ex = thrown(IllegalStateException)
        ex.message.contains("No matching case in switch 'test?'")
        ex.message.contains('token-7')
    }
}
