package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.engine.PetriNetStructure
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class RepeatConditionalNetCompilerSpec extends Specification {

    def 'should compile conditional inside loop body with a decision place and two guarded branch transitions'() {
        when:
        PetriNetStructure net = parse(branchInLoopPuml())

        then:
        def decision = findPlaceByLabel(net, 'P_if_2_decision')
        decision != null

        and: 'the preceding body action feeds the if decision place through a structural transition'
        def toDecision = findTransitionByLabel(net, 'T_if_2_to_decision')
        verifyTransitionConnection(net, toDecision, findPlaceByLabel(net, 'P_repeat_1_body_1'), decision)

        and:
        def branchYes = findTransitionByLabel(net, 'T_if_2_yes')
        branchYes != null
        branchYes.guardKey == 'valid?'
        verifyTransitionConnection(net, branchYes, decision, findPlaceByLabel(net, 'P_if_2_then'))

        and:
        def branchNo = findTransitionByLabel(net, 'T_if_2_no')
        branchNo != null
        branchNo.guardKey == '!(valid?)'
        verifyTransitionConnection(net, branchNo, decision, findPlaceByLabel(net, 'P_if_2_else'))
    }

    def 'should converge both conditional branches at the loop decision place'() {
        when:
        PetriNetStructure net = parse(branchInLoopPuml())

        then:
        def loopDecision = findPlaceByLabel(net, 'P_repeat_1_decision')
        loopDecision != null

        and: 'the then branch action leads into the conditional merge place'
        def process = findTransitionByAction(net, 'process')
        def merge = findPlaceByLabel(net, 'P_if_2_endif')
        net.incidenceMatrix.getInputWeight(findPlaceByLabel(net, 'P_if_2_then').index, process.index) == 1
        net.incidenceMatrix.getOutputWeight(merge.index, process.index) == 1

        and: 'the else branch action leads into the conditional merge place'
        def reject = findTransitionByAction(net, 'reject')
        net.incidenceMatrix.getInputWeight(findPlaceByLabel(net, 'P_if_2_else').index, reject.index) == 1
        net.incidenceMatrix.getOutputWeight(merge.index, reject.index) == 1

        and: 'the conditional exit leads from the merge place into the loop decision place'
        def exit = findTransitionByLabel(net, 'T_if_2_to_end')
        net.incidenceMatrix.getInputWeight(merge.index, exit.index) == 1
        net.incidenceMatrix.getOutputWeight(loopDecision.index, exit.index) == 1

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
        findPlaceByLabel(net, 'P_if_2_decision') != null
        findTransitionByLabel(net, 'T_if_2_yes')?.guardKey == 'Record is valid?'
        findTransitionByLabel(net, 'T_if_2_no')?.guardKey == '!(Record is valid?)'
        findTransitionByLabel(net, 'T_repeat_1_loop_back')?.guardKey == 'More Records Available?'

        and: 'all diagram actions are present'
        ['Initialize Data Batch', 'Fetch Next Record', 'Process Record Data', 'Calculate Metrics',
         'Write Metrics to Buffer', 'Log Validation Error', 'Aggregate Final Results', 'Commit to Data Store'].each { action ->
            assert findTransitionByAction(net, action) != null : "missing action ${action}"
        }
    }

}
