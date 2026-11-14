package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class ConditionalLoopNetCompilerSpec extends Specification {

    def 'should compile a repeat loop inside the then branch with loop exit wired to the merge point'() {
        when:
        PetriNetStructure net = parse(loopInBranchPuml())

        then:
        verifyConditionalStructure(net, 'run loop?')
        verifyRepeatLoopStructure(net, 'more?', 1, 'P_then')

        and: 'the loop exit transition leads to the conditional merge place'
        def endif = findPlaceByLabel(net, 'P_endif')
        def exit = findTransitionByLabel(net, 'T_repeat_1_exit')
        exit != null
        net.incidenceMatrix.getOutputWeight(endif.index, exit.index) == 1

        and: 'the else branch still chains to the merge place'
        def skip = findTransitionByAction(net, 'skip')
        net.incidenceMatrix.getOutputWeight(endif.index, skip.index) == 1
    }

    def 'should compile a repeat loop inside the else branch with loop exit wired to the merge point'() {
        when:
        PetriNetStructure net = parse(loopInElseBranchPuml())

        then:
        verifyRepeatLoopStructure(net, 'more?', 1, 'P_else')

        and:
        def endif = findPlaceByLabel(net, 'P_endif')
        def exit = findTransitionByLabel(net, 'T_repeat_1_exit')
        exit != null
        net.incidenceMatrix.getOutputWeight(endif.index, exit.index) == 1
    }

    def 'should execute a loop inside the else branch when its guard holds'() {
        given:
        def bodyLog = []
        def engine = PlantFlow.from(loopInElseBranchPuml(), composedLoopRegistry(bodyLog))

        when:
        engine.runUntilEnd(Token.of(), new ExecutionContext([skipLoop: false, remaining: 2]))

        then:
        engine.isCompleted()
        engine.executionContext['iterations'] == 2
        bodyLog == ['iterate', 'iterate']
        verifyEndTokenPayload(engine, [iterations: 2])
    }

    def 'should keep separate loop state for two loops in different branches of one conditional'() {
        given:
        def puml = '''@startuml
start
if (branch?) then (yes)
  repeat
    :iterate;
  repeat while (first more?) is (yes) not (no)
else (no)
  repeat
    :iterate;
  repeat while (second more?) is (yes) not (no)
endif
end
@enduml
'''
        def bodyLog = []
        def registry = composedLoopRegistry(bodyLog)
        registry.registerAction('iterate') { ExecutionContext ctx, Token tok ->
            bodyLog << 'iterate'
            ['remaining', 'first', 'second'].each { key ->
                ctx[key] = ((ctx.get(key) ?: 0) as int) - 1
            }
            int iterations = ((ctx.get('iterations') ?: 0) as int) + 1
            ctx['iterations'] = iterations
            return tok.withPayload(tok.payload + [iterations: iterations])
        }
        registry.registerGuard('branch?') { ExecutionContext ctx, Token tok -> ctx.get('branch') as boolean }
        registry.registerGuard('!(branch?)') { ExecutionContext ctx, Token tok -> !(ctx.get('branch') as boolean) }
        registry.registerGuard('first more?') { ExecutionContext ctx, Token tok -> (ctx.get('first') ?: 0) as int > 0 }
        registry.registerGuard('!(first more?)') { ExecutionContext ctx, Token tok -> (ctx.get('first') ?: 0) as int <= 0 }
        registry.registerGuard('second more?') { ExecutionContext ctx, Token tok -> (ctx.get('second') ?: 0) as int > 0 }
        registry.registerGuard('!(second more?)') { ExecutionContext ctx, Token tok -> (ctx.get('second') ?: 0) as int <= 0 }

        when: 'the else branch runs its own loop with its own condition'
        def net = parse(puml)
        def loopBacks = net.transitions.findAll { it.label ==~ /T_repeat_\d+_loop_back/ }
        loopBacks*.guardKey.sort() == ['first more?', 'second more?']

        and:
        def engine = PlantFlow.from(puml, registry)
        engine.runUntilEnd(Token.of(), new ExecutionContext([branch: false, first: 5, second: 2]))

        then:
        engine.isCompleted()
        engine.executionContext['iterations'] == 2
        bodyLog == ['iterate', 'iterate']
    }
}
