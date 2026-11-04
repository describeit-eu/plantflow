package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import eu.describeit.plantflow.PlantFlow
import eu.describeit.plantflow.ast.RepeatNode
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class RepeatLoopNetCompilerSpec extends Specification {

    // ==================== repeatWhile.puml / repeatInfinite.puml compile ====================

    def 'should compile repeatWhile.puml into a net with a loop back edge'() {
        when:
        PetriNetStructure net = parse(new File('src/test/data/puml/repeatWhile.puml'))

        then:
        net != null
        verifyRepeatLoopStructure(net, 'more data?')

        and: 'loop-back branch transition carries the loop condition guard'
        def loopBack = findTransitionByLabel(net, 'T_repeat_1_loop_back')
        loopBack != null
        loopBack.guardKey == 'more data?'

        and: 'exit branch transition carries the negated guard'
        def exit = findTransitionByLabel(net, 'T_repeat_1_exit')
        exit != null
        exit.guardKey == '!(more data?)'

        and: 'the net contains a cycle from the decision place back to the loop start place'
        def decision = findPlaceByLabel(net, 'P_repeat_1_decision')
        def loopStart = net.startPlace
        net.incidenceMatrix.getInputWeight(decision.index, loopBack.index) == 1
        net.incidenceMatrix.getOutputWeight(loopStart.index, loopBack.index) == 1

        and: 'no detach transition is present'
        findTransitionByLabel(net, 'T_repeat_1_detach') == null
    }

    def 'should compile repeatWhile.puml body actions in diagram order'() {
        when:
        PetriNetStructure net = parse(new File('src/test/data/puml/repeatWhile.puml'))

        then:
        def bodyTransitions = net.transitions.findAll { it.actionKey != null }
        bodyTransitions*.actionKey == ['read data', 'generate diagrams']
    }

    def 'should compile repeatInfinite.puml with constant-true loop-back guard and detach transition'() {
        when:
        PetriNetStructure net = parse(new File('src/test/data/puml/repeatInfinite.puml'))

        then:
        def loopBack = findTransitionByLabel(net, 'T_repeat_1_loop_back')
        loopBack != null
        loopBack.guardKey == 'true'

        and: 'detach is a structural transition with no output place'
        def detach = findTransitionByLabel(net, 'T_repeat_1_detach')
        detach != null
        detach.actionKey == null
        detach.guardKey == null
        verifyTransitionIO(net, detach, 1, 0)

        and: 'no regular exit branch transition for a detached loop'
        findTransitionByLabel(net, 'T_repeat_1_exit') == null

        and: 'detach consumes from the decision place'
        def decision = findPlaceByLabel(net, 'P_repeat_1_decision')
        net.incidenceMatrix.getInputWeight(decision.index, detach.index) == 1
    }

    // ==================== Structural variants ====================

    def 'should compile loop directly followed by end with exit routing to the end place'() {
        when:
        PetriNetStructure net = parse(repeatPuml('more data?', ['read data']))

        then:
        verifyRepeatLoopStructure(net, 'more data?')
        def exit = findTransitionByLabel(net, 'T_repeat_1_exit')
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, exit.index) == 1
    }

    def 'should compile post-loop actions after the loop exit'() {
        when:
        PetriNetStructure net = parse(repeatPuml('more data?', ['read data'], ['finalize']))

        then:
        def exit = findTransitionByLabel(net, 'T_repeat_1_exit')
        def afterPlace = findPlaceByLabel(net, 'P_repeat_1_after')
        afterPlace != null
        net.incidenceMatrix.getOutputWeight(afterPlace.index, exit.index) == 1

        and: 'the post-loop action chains from the after place to the end place'
        def finalize = findTransitionByAction(net, 'finalize')
        finalize != null
        net.incidenceMatrix.getInputWeight(afterPlace.index, finalize.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, finalize.index) == 1
    }

    def 'should compile repeat loop mixed with plain action nodes before and after the loop'() {
        given:
        def puml = '''@startuml
start
:pre action;
repeat
  :loop action;
repeat while (more data?) is (yes) not (no)
:post action;
end
@enduml
'''

        when:
        PetriNetStructure net = parse(puml)

        then:
        findTransitionByAction(net, 'pre action') != null
        findTransitionByAction(net, 'loop action') != null
        findTransitionByAction(net, 'post action') != null

        and: 'the pre action feeds into the loop start place'
        def preTransition = findTransitionByAction(net, 'pre action')
        def loopStart = findPlaceByLabel(net, 'P_repeat_1_start')
        loopStart != null
        net.incidenceMatrix.getOutputWeight(loopStart.index, preTransition.index) == 1

        and: 'the loop-back routes to the loop start place'
        def loopBack = findTransitionByLabel(net, 'T_repeat_1_loop_back')
        net.incidenceMatrix.getOutputWeight(loopStart.index, loopBack.index) == 1

        and: 'the exit feeds the post-loop action chain'
        def exit = findTransitionByLabel(net, 'T_repeat_1_exit')
        def afterPlace = findPlaceByLabel(net, 'P_repeat_1_after')
        net.incidenceMatrix.getOutputWeight(afterPlace.index, exit.index) == 1
        def postTransition = findTransitionByAction(net, 'post action')
        net.incidenceMatrix.getInputWeight(afterPlace.index, postTransition.index) == 1
    }

    def 'should compile nested repeat loops with distinct decision places and loop-back transitions per loop'() {
        given:
        def puml = '''@startuml
start
repeat
  :outer start;
  repeat
    :inner action;
  repeat while (inner done?) is (yes) not (no)
  :outer end;
repeat while (outer done?) is (yes) not (no)
end
@enduml
'''

        when:
        PetriNetStructure net = parse(puml)

        then: 'distinct decision places per loop instance'
        findPlaceByLabel(net, 'P_repeat_1_decision') != null
        findPlaceByLabel(net, 'P_repeat_2_decision') != null

        and: 'distinct loop-back transitions with their own loop condition guards'
        def outerLoopBack = findTransitionByLabel(net, 'T_repeat_1_loop_back')
        def innerLoopBack = findTransitionByLabel(net, 'T_repeat_2_loop_back')
        outerLoopBack != null
        innerLoopBack != null
        outerLoopBack.guardKey == 'outer done?'
        innerLoopBack.guardKey == 'inner done?'

        and: 'inner loop-back routes back to its own loop start (a body place of the outer loop)'
        def innerDecision = findPlaceByLabel(net, 'P_repeat_2_decision')
        def innerStart = findPlaceByLabel(net, 'P_repeat_1_body_1')
        innerStart != null
        net.incidenceMatrix.getInputWeight(innerDecision.index, innerLoopBack.index) == 1
        net.incidenceMatrix.getOutputWeight(innerStart.index, innerLoopBack.index) == 1

        and: 'inner exit carries the negated inner guard and continues the outer body'
        def innerExit = findTransitionByLabel(net, 'T_repeat_2_exit')
        innerExit.guardKey == '!(inner done?)'
        def outerBodyNext = findPlaceByLabel(net, 'P_repeat_1_body_2')
        net.incidenceMatrix.getOutputWeight(outerBodyNext.index, innerExit.index) == 1
        def outerEnd = findTransitionByAction(net, 'outer end')
        net.incidenceMatrix.getInputWeight(outerBodyNext.index, outerEnd.index) == 1

        and: 'outer exit carries the negated outer guard and routes to the end place'
        def outerExit = findTransitionByLabel(net, 'T_repeat_1_exit')
        outerExit.guardKey == '!(outer done?)'
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, outerExit.index) == 1
    }

    // ==================== Guard resolution through the Handler Registry ====================

    def 'should resolve loop condition guards through the Handler Registry during execution'() {
        given:
        def registry = new HandlerRegistry()
        registry.registerAction('read data') { ExecutionContext ctx, Token tok ->
            int count = (ctx['iterations'] ?: 0) + 1
            ctx['iterations'] = count
            return tok.withPayload(tok.payload + [readCount: count])
        }
        registry.registerAction('finalize') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [finalized: true])
        }
        registry.registerGuard('more data?') { ExecutionContext ctx, Token tok ->
            return (ctx['iterations'] ?: 0) < 2
        }
        registry.registerGuard('!(more data?)') { ExecutionContext ctx, Token tok ->
            return (ctx['iterations'] ?: 0) >= 2
        }
        def engine = PlantFlow.from(repeatPuml('more data?', ['read data'], ['finalize']), registry)

        when:
        engine.runUntilEnd(Token.of())

        then: 'the loop body executed twice before the negated guard routed the token onward'
        engine.isCompleted()
        engine.executionContext['iterations'] == 2
        verifyEndTokenPayload(engine, [readCount: 2, finalized: true])
    }

    // ==================== Validation ====================

    def 'should reject repeat node with empty body'() {
        given:
        def compiler = new eu.describeit.plantflow.compiler.DiagramCompiler()
        def diagram = diagram(true, true, [new RepeatNode('x?', [])])

        when:
        compiler.compile(diagram)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Repeat loop body must contain at least one action'
    }

    def 'should reject null repeat node'() {
        given:
        def repeatCompiler = new eu.describeit.plantflow.compiler.RepeatNetCompiler()

        when:
        repeatCompiler.compile(null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'RepeatNode cannot be null'
    }
}
