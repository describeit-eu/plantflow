package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class PlantFlowComposedRepeatConditionalSpec extends Specification {

    private static HandlerRegistry batchRegistry(List<String> executionLog) {
        def registry = new HandlerRegistry()
        registry.registerAction('Initialize Data Batch') { ExecutionContext ctx, Token tok ->
            executionLog << 'Initialize Data Batch'
            return tok.withPayload(tok.payload + [initialized: true])
        }
        registry.registerAction('Fetch Next Record') { ExecutionContext ctx, Token tok ->
            executionLog << 'Fetch Next Record'
            def records = (ctx.get('records') ?: []) as List
            ctx['current'] = records ? records[0] : null
            ctx['records'] = records ? records.drop(1) : []
            int fetched = ((ctx.get('fetched') ?: 0) as int) + 1
            ctx['fetched'] = fetched
            return tok.withPayload(tok.payload + [fetched: fetched])
        }
        registry.registerAction('Process Record Data') { ExecutionContext ctx, Token tok ->
            executionLog << 'Process Record Data'
            int processed = ((ctx.get('processed') ?: 0) as int) + 1
            ctx['processed'] = processed
            return tok.withPayload(tok.payload + [processed: processed])
        }
        registry.registerAction('Calculate Metrics') { ExecutionContext ctx, Token tok ->
            executionLog << 'Calculate Metrics'
            return tok.withPayload(tok.payload + [metrics: true])
        }
        registry.registerAction('Write Metrics to Buffer') { ExecutionContext ctx, Token tok ->
            executionLog << 'Write Metrics to Buffer'
            return tok.withPayload(tok.payload + [buffered: true])
        }
        registry.registerAction('Log Validation Error') { ExecutionContext ctx, Token tok ->
            executionLog << 'Log Validation Error'
            int rejected = ((ctx.get('rejected') ?: 0) as int) + 1
            ctx['rejected'] = rejected
            return tok.withPayload(tok.payload + [rejected: rejected])
        }
        registry.registerAction('Aggregate Final Results') { ExecutionContext ctx, Token tok ->
            executionLog << 'Aggregate Final Results'
            return tok.withPayload(tok.payload + [aggregated: true])
        }
        registry.registerAction('Commit to Data Store') { ExecutionContext ctx, Token tok ->
            executionLog << 'Commit to Data Store'
            return tok.withPayload(tok.payload + [committed: true])
        }
        registry.registerGuard('Record is valid?') { ExecutionContext ctx, Token tok ->
            ctx.get('current') != null && (ctx.get('current') as String).endsWith('ok')
        }
        registry.registerGuard('!(Record is valid?)') { ExecutionContext ctx, Token tok ->
            !(ctx.get('current') != null && (ctx.get('current') as String).endsWith('ok'))
        }
        registry.registerGuard('More Records Available?') { ExecutionContext ctx, Token tok ->
            !((ctx.get('records') ?: []) as List).isEmpty()
        }
        registry.registerGuard('!(More Records Available?)') { ExecutionContext ctx, Token tok ->
            ((ctx.get('records') ?: []) as List).isEmpty()
        }
        return registry
    }

    def 'batchProcessing.puml runs end-to-end via PlantFlow.from() and runUntilEnd()'() {
        given:
        def executionLog = []
        def registry = batchRegistry(executionLog)
        def engine = PlantFlow.from(new File('src/test/data/puml/batchProcessing.puml'), registry)

        when:
        engine.runUntilEnd(Token.of(), new ExecutionContext([records: ['r1-ok', 'r2-bad', 'r3-ok']]))

        then: 'the workflow completes and every record went through exactly one branch per iteration'
        engine.isCompleted()
        engine.getEndToken() != null
        engine.executionContext['fetched'] == 3
        engine.executionContext['processed'] == 2
        engine.executionContext['rejected'] == 1

        and:
        executionLog == [
            'Initialize Data Batch',
            'Fetch Next Record', 'Process Record Data', 'Calculate Metrics', 'Write Metrics to Buffer',
            'Fetch Next Record', 'Log Validation Error',
            'Fetch Next Record', 'Process Record Data', 'Calculate Metrics', 'Write Metrics to Buffer',
            'Aggregate Final Results', 'Commit to Data Store'
        ]

        and: 'the end token carries the accumulated payload'
        verifyEndTokenPayload(engine, [
            initialized: true, fetched: 3, processed: 2, metrics: true, buffered: true,
            rejected: 1, aggregated: true, committed: true
        ])
    }

    def 'branch-inside-loop branches correctly on each iteration'() {
        given: 'alternating validity across four iterations'
        def executionLog = []
        def engine = PlantFlow.from(branchInLoopPuml(), composedLoopRegistry(executionLog))

        when:
        engine.runUntilEnd(Token.of(), new ExecutionContext([remaining: 4, validPattern: [true, false, true, false]]))

        then:
        engine.isCompleted()
        engine.executionContext['fetched'] == 4
        executionLog == ['fetch', 'process', 'fetch', 'reject', 'fetch', 'process', 'fetch', 'reject', 'finalize']
        verifyEndTokenPayload(engine, [fetched: 4, processed: true, rejected: 2, finalized: true])
    }

    def 'loop-inside-branch executes the correct number of iterations'() {
        given:
        def executionLog = []
        def engine = PlantFlow.from(loopInBranchPuml(), composedLoopRegistry(executionLog))

        when:
        engine.runUntilEnd(Token.of(), new ExecutionContext([runLoop: true, remaining: 3]))

        then:
        engine.isCompleted()
        engine.executionContext['iterations'] == 3
        executionLog == ['iterate', 'iterate', 'iterate']
        verifyEndTokenPayload(engine, [iterations: 3])

        when: 'the branch guard is false'
        executionLog.clear()
        def engine2 = PlantFlow.from(loopInBranchPuml(), composedLoopRegistry(executionLog))
        engine2.runUntilEnd(Token.of(), new ExecutionContext([runLoop: false, remaining: 3]))

        then:
        engine2.isCompleted()
        executionLog == ['skip']
        verifyEndTokenPayload(engine2, [skipped: true])
    }

    def 'per-Marking isolation holds for a composed diagram: a copied in-progress marking evolves independently'() {
        given: 'an engine for the branch-inside-loop diagram and a marking inside the first iteration'
        def registry = composedLoopRegistry([])
        def engine = PlantFlow.from(branchInLoopPuml(), registry)
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of())
        def context = new ExecutionContext([remaining: 2, validPattern: [true, true]])

        and: 'advance to the conditional decision place inside the loop body'
        def fetch = engine.petriNet.transitions.find { it.actionKey == 'fetch' }
        engine.fire(fetch, marking, context)
        def toDecision = engine.petriNet.transitions.find { it.label == 'T_repeat_1_if_1_to_decision' }
        engine.fire(toDecision, marking, context)
        def loopDecision = engine.petriNet.places.find { it.label == 'P_repeat_1_decision' }
        def ifDecision = engine.petriNet.places.find { it.label == 'P_repeat_1_if_1_decision' }

        when: 'the marking is copied and both copies continue independently'
        def copy = marking.copy()
        def branchYes = engine.petriNet.transitions.find { it.label == 'T_repeat_1_if_1_yes' }
        engine.fire(branchYes, copy, context)

        then: 'the original marking still has its token at the if decision place and none on the then branch'
        marking.getTokenCount(ifDecision) == 1
        marking.getTokenCount(loopDecision) == 0
        copy.getTokenCount(ifDecision) == 0
        copy.getTokenCount(engine.petriNet.places.find { it.label == 'P_repeat_1_if_1_then' }) == 1
    }
}
