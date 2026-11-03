package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Token
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Behavior-focused tests for non-synchronizing Fork/Merge semantics:
 * for each Fork activation the first completed Parallel Branch continues
 * through Merge once with its own Token, while remaining branches finish
 * independently without triggering another continuation.
 */
class PlantFlowForkFirstCompletionSpec extends Specification {

    @Unroll
    def 'either branch can complete first: #winnerLabel branch continues immediately while the other is unfinished'() {
        given: 'a fork with two single-action branches and a post-merge action'
        def puml = forkPuml([['branch a'], ['branch b']], ['after merge'])

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'a']) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'b']) }
        registry.registerAction('after merge') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of([initial: true]))

        when: 'the fork transition fires'
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'))

        then: 'no merge transition is enabled before any branch completes'
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_merge_1'))
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_merge_2'))

        when: "the ${winnerLabel} branch action fires and completes first"
        engine.fire(findTransitionByAction(engine, winnerAction))

        then: 'only the completed branch merge transition is enabled, even though the other branch is unfinished'
        engine.isEnabled(findTransitionByLabel(engine, winnerMerge))
        !engine.isEnabled(findTransitionByLabel(engine, loserMerge))
        !engine.isEnabled(findTransitionByAction(engine, 'after merge'))

        when: 'the winning merge transition fires'
        def fired = engine.fire(findTransitionByLabel(engine, winnerMerge))

        then: 'the outgoing flow is immediately enabled without waiting for the other branch'
        fired
        engine.isEnabled(findTransitionByAction(engine, 'after merge'))

        when: 'the post-merge action fires and the losing branch finishes afterwards'
        engine.fire(findTransitionByAction(engine, 'after merge'))
        engine.fire(findTransitionByAction(engine, loserAction))

        then: 'exactly one continuation reached the end with the winner branch token'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [initial: true, winner: expectedWinner]

        and: 'the losing branch merge transition can no longer trigger a second continuation'
        !engine.isEnabled(findTransitionByLabel(engine, loserMerge))
        !engine.step()

        where:
        winnerLabel | winnerAction | winnerMerge          | loserAction  | loserMerge           | expectedWinner
        'branch a'  | 'branch a'   | 'T_fork_1_merge_1'   | 'branch b'   | 'T_fork_1_merge_2'   | 'a'
        'branch b'  | 'branch b'   | 'T_fork_1_merge_2'   | 'branch a'   | 'T_fork_1_merge_1'   | 'b'
    }

    def 'a multi-action branch cannot reach merge before its final action and its resulting token continues'() {
        given: 'a fork with a two-action branch and a single-action branch, proceeding directly to end'
        def puml = forkPuml([['first a', 'final a'], ['branch b']])

        def registry = new HandlerRegistry()
        registry.registerAction('first a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [step: 'a1']) }
        registry.registerAction('final a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [step: 'a2']) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [step: 'b']) }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of([initial: true]))
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'))

        when: 'the first action of the multi-action branch fires'
        engine.fire(findTransitionByAction(engine, 'first a'))

        then: 'the multi-action branch merge transition is not enabled before the final action'
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_merge_1'))

        when: 'the final action of the multi-action branch fires and wins the merge'
        engine.fire(findTransitionByAction(engine, 'final a'))
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_merge_1'))
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_to_end'))

        then: 'the merge led directly to the end place with the multi-action branch resulting token'
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [initial: true, step: 'a2']

        when: 'the other branch finishes afterwards'
        engine.fire(findTransitionByAction(engine, 'branch b'))

        then: 'no second continuation is triggered and the workflow is completed'
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_merge_2'))
        engine.getEndTokens().size() == 1
        engine.isCompleted()
    }

    def 'later branches remain executable and finish without a second outgoing continuation'() {
        given: 'a fork with three branches and a post-merge action'
        def puml = forkPuml([['task 1'], ['task 2'], ['task 3']], ['finalize'])
        def executionLog = []
        def registry = payloadRegistry(['task 1', 'task 2', 'task 3', 'finalize'], executionLog)

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of())
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'))
        engine.fire(findTransitionByAction(engine, 'task 1'))
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_merge_1'))

        when: 'the post-merge action runs while other branches are unfinished, then the remaining branches run'
        engine.fire(findTransitionByAction(engine, 'finalize'))
        engine.fire(findTransitionByAction(engine, 'task 2'))
        engine.fire(findTransitionByAction(engine, 'task 3'))

        then: 'all branch actions executed exactly once and finalize ran exactly once'
        executionLog.sort() == ['finalize', 'task 1', 'task 2', 'task 3']

        and: 'both later branch merge transitions are disabled and completion holds with a single end token'
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_merge_2'))
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_merge_3'))
        engine.isCompleted()
        engine.getEndTokens().size() == 1
    }

    def 'merge leading directly to the end place continues once from the first completed branch'() {
        given: 'a fork without post-merge actions'
        def puml = forkPuml([['branch a'], ['branch b']])

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'a']) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'b']) }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then: 'the workflow completed with exactly one end token from the first completed branch'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [initial: true, winner: 'a']
    }

    def 'a handler returning a replacement token keeps the fork activation and routes the replacement onward'() {
        given: 'a fork where the winning branch handler returns a replacement token'
        def puml = forkPuml([['branch a'], ['branch b']], ['after merge'])

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> Token.of([replaced: 'a']) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> Token.of([replaced: 'b']) }
        registry.registerAction('after merge') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of([initial: true]))
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'))

        when: 'branch a completes first and its replacement token wins the merge'
        engine.fire(findTransitionByAction(engine, 'branch a'))
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_merge_1'))
        engine.fire(findTransitionByAction(engine, 'after merge'))

        then: 'the replacement token reached the end'
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [replaced: 'a']

        when: 'branch b finishes afterwards'
        engine.fire(findTransitionByAction(engine, 'branch b'))

        then: 'branch b cannot trigger another continuation and the workflow is completed'
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_merge_2'))
        engine.getEndTokens().size() == 1
        engine.isCompleted()
    }

    def 'overlapping fork activations each produce exactly one continuation'() {
        given: 'a fork workflow seeded with two tokens, creating two overlapping activations'
        def puml = forkPuml([['branch a'], ['branch b']], ['after merge'])

        def finalizeCount = 0
        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [branch: 'a']) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [branch: 'b']) }
        registry.registerAction('after merge') { ExecutionContext ctx, Token tok ->
            finalizeCount++
            return tok
        }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of([run: 1]))
        engine.seedToken(Token.of([run: 2]))

        when:
        engine.runUntilEnd()

        then: 'each activation continued exactly once, so two end tokens exist and finalize ran twice'
        engine.isCompleted()
        engine.getEndTokens().size() == 2
        finalizeCount == 2
    }

    def 'custom-Marking execution applies the same first-completed-branch semantics'() {
        given: 'a fork with post-merge action executed via the steppable custom-Marking API'
        def puml = forkPuml([['branch a'], ['branch b']], ['after merge'])

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'a']) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'b']) }
        registry.registerAction('after merge') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([initial: true]))
        def context = new ExecutionContext()

        when: 'driving the fork with custom-marking primitives, branch b completing first'
        def enabled = engine.getEnabledTransitions(marking, context)
        engine.fire(enabled[0], marking, context)
        engine.fire(findTransitionByAction(engine, 'branch b'), marking, context)

        then: 'only the completed branch b merge transition can continue for the custom marking'
        def enabledAfterB = engine.getEnabledTransitions(marking, context)
        enabledAfterB*.label.contains('T_fork_1_merge_2')
        !enabledAfterB*.label.contains('T_fork_1_merge_1')

        when: 'branch b merge fires and the flow continues'
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_merge_2'), marking, context)
        engine.fire(findTransitionByAction(engine, 'after merge'), marking, context)

        then: 'the end place of the custom marking holds exactly the winner token'
        marking.getTokenCount(engine.petriNet.endPlace) == 1
        marking.getTokens(engine.petriNet.endPlace)[0].payload == [initial: true, winner: 'b']

        when: 'branch a finishes afterwards on the same custom marking'
        engine.fire(findTransitionByAction(engine, 'branch a'), marking, context)

        then: 'no second continuation is enabled'
        engine.getEnabledTransitions(marking, context).isEmpty()
        marking.getTokenCount(engine.petriNet.endPlace) == 1
    }

    def 'a copied marking preserves in-progress fork activation state'() {
        given: 'a fork activation in progress, copied before any branch completes'
        def puml = forkPuml([['branch a'], ['branch b']])

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'a']) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'b']) }

        def engine = PlantFlow.from(puml, registry)
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([initial: true]))
        def context = new ExecutionContext()
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'), marking, context)

        when: 'the marking is copied and the copy continues the fork to the end'
        def copied = marking.copy()
        engine.fire(findTransitionByAction(engine, 'branch a'), copied, context)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_merge_1'), copied, context)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_to_end'), copied, context)

        then: 'the copy produces exactly one continuation with the winner token'
        copied.getTokenCount(engine.petriNet.endPlace) == 1
        copied.getTokens(engine.petriNet.endPlace)[0].payload == [initial: true, winner: 'a']

        and: 'the original marking still has its pending fork activation and finishes independently'
        engine.fire(findTransitionByAction(engine, 'branch b'), marking, context)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_merge_2'), marking, context)
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_to_end'), marking, context)
        marking.getTokenCount(engine.petriNet.endPlace) == 1
        marking.getTokens(engine.petriNet.endPlace)[0].payload == [initial: true, winner: 'b']
    }
}
