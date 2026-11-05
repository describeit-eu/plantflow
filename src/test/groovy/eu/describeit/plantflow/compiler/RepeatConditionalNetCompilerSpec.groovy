package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class RepeatConditionalNetCompilerSpec extends Specification {

    private static final String COMPOSED_PUML = '''@startuml
start
repeat
  :fetch;
  if (valid?) then (yes)
    :process;
  else (no)
    :log;
  endif
repeat while (more?) is (yes) not (no)
:done;
end
@enduml
'''

    def 'should compile conditional inside loop body with a decision place and two guarded branch transitions'() {
        when:
        PetriNetStructure net = parse(COMPOSED_PUML)

        then:
        def decision = findPlaceByLabel(net, 'P_repeat_1_if_1_decision')
        decision != null

        and: 'the preceding body action feeds the if decision place through a structural transition'
        def toDecision = findTransitionByLabel(net, 'T_repeat_1_if_1_to_decision')
        verifyTransitionConnection(net, toDecision, findPlaceByLabel(net, 'P_repeat_1_body_1'), decision)

        and:
        def branchYes = findTransitionByLabel(net, 'T_repeat_1_if_1_yes')
        branchYes != null
        branchYes.guardKey == 'valid?'
        verifyTransitionConnection(net, branchYes, decision, findPlaceByLabel(net, 'P_repeat_1_if_1_then'))

        and:
        def branchNo = findTransitionByLabel(net, 'T_repeat_1_if_1_no')
        branchNo != null
        branchNo.guardKey == '!(valid?)'
        verifyTransitionConnection(net, branchNo, decision, findPlaceByLabel(net, 'P_repeat_1_if_1_else'))
    }

    def 'should converge both conditional branches at the loop decision place'() {
        when:
        PetriNetStructure net = parse(COMPOSED_PUML)

        then:
        def loopDecision = findPlaceByLabel(net, 'P_repeat_1_decision')
        loopDecision != null

        and: 'the then branch action leads into the loop decision place'
        def process = findTransitionByAction(net, 'process')
        net.incidenceMatrix.getInputWeight(findPlaceByLabel(net, 'P_repeat_1_if_1_then').index, process.index) == 1
        net.incidenceMatrix.getOutputWeight(loopDecision.index, process.index) == 1

        and: 'the else branch action leads into the loop decision place'
        def log = findTransitionByAction(net, 'log')
        net.incidenceMatrix.getInputWeight(findPlaceByLabel(net, 'P_repeat_1_if_1_else').index, log.index) == 1
        net.incidenceMatrix.getOutputWeight(loopDecision.index, log.index) == 1

        and: 'the loop structure is intact'
        verifyRepeatLoopStructure(net, 'more?')
    }

    def 'should compile batchProcessing.puml into a valid PetriNetStructure'() {
        when:
        PetriNetStructure net = parse(new File('src/test/data/puml/batchProcessing.puml'))

        then:
        net != null
        net.startPlace != null
        net.endPlace != null

        and: 'the loop and its inner conditional compiled'
        findPlaceByLabel(net, 'P_repeat_1_decision') != null
        findPlaceByLabel(net, 'P_repeat_1_if_1_decision') != null
        findTransitionByLabel(net, 'T_repeat_1_if_1_yes')?.guardKey == 'Record is valid?'
        findTransitionByLabel(net, 'T_repeat_1_if_1_no')?.guardKey == '!(Record is valid?)'
        findTransitionByLabel(net, 'T_repeat_1_loop_back')?.guardKey == 'More Records Available?'

        and: 'all diagram actions are present'
        ['Initialize Data Batch', 'Fetch Next Record', 'Process Record Data', 'Calculate Metrics',
         'Write Metrics to Buffer', 'Log Validation Error', 'Aggregate Final Results', 'Commit to Data Store'].each { action ->
            assert findTransitionByAction(net, action) != null : "missing action ${action}"
        }
    }

    def 'should execute a conditional inside a loop body with correct iteration count and token payload'() {
        given:
        def bodyLog = []
        def registry = new HandlerRegistry()
        registry.registerAction('fetch') { ExecutionContext ctx, Token tok ->
            bodyLog << 'fetch'
            int remaining = (ctx.get('remaining') ?: 0) as int
            ctx['remaining'] = remaining - 1
            int iterations = ((ctx.get('iterations') ?: 0) as int) + 1
            ctx['iterations'] = iterations
            return tok.withPayload(tok.payload + [fetchCount: iterations])
        }
        registry.registerAction('process') { ExecutionContext ctx, Token tok ->
            bodyLog << 'process'
            return tok.withPayload(tok.payload + [processed: true])
        }
        registry.registerAction('log') { ExecutionContext ctx, Token tok ->
            bodyLog << 'log'
            return tok.withPayload(tok.payload + [logged: true])
        }
        registry.registerAction('done') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [done: true])
        }
        registry.registerGuard('valid?') { ExecutionContext ctx, Token tok -> ctx.get('valid') as boolean }
        registry.registerGuard('!(valid?)') { ExecutionContext ctx, Token tok -> !(ctx.get('valid') as boolean) }
        registry.registerGuard('more?') { ExecutionContext ctx, Token tok -> (ctx.get('remaining') ?: 0) as int > 0 }
        registry.registerGuard('!(more?)') { ExecutionContext ctx, Token tok -> (ctx.get('remaining') ?: 0) as int <= 0 }

        when: 'running two iterations where every record is valid'
        def engine = PlantFlow.from(COMPOSED_PUML, registry)
        engine.runUntilEnd(Token.of(), new ExecutionContext([remaining: 2, valid: true]))

        then:
        engine.isCompleted()
        engine.executionContext['iterations'] == 2
        bodyLog == ['fetch', 'process', 'fetch', 'process']
        verifyEndTokenPayload(engine, [fetchCount: 2, processed: true, done: true])

        when: 'running two iterations where no record is valid'
        bodyLog.clear()
        def engine2 = PlantFlow.from(COMPOSED_PUML, registry)
        engine2.runUntilEnd(Token.of(), new ExecutionContext([remaining: 2, valid: false]))

        then:
        engine2.isCompleted()
        engine2.executionContext['iterations'] == 2
        bodyLog == ['fetch', 'log', 'fetch', 'log']
        verifyEndTokenPayload(engine2, [fetchCount: 2, logged: true, done: true])
    }
}
