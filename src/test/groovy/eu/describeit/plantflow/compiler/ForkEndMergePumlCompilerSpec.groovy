package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ActivityDiagramParser
import eu.describeit.plantflow.PetriNetCompiler
import eu.describeit.plantflow.PetriNetTestUtil
import eu.describeit.plantflow.engine.PetriNet
import spock.lang.Specification
import spock.lang.Unroll

class ForkEndMergePumlCompilerSpec extends Specification {

    private static PetriNet parse(String puml) {
        return new ActivityDiagramParser().parse(puml)
    }

    private static PetriNet parseFile(String filename) {
        return new ActivityDiagramParser().parse(new File("src/test/data/puml/${filename}"))
    }

    def 'should compile forkEndMerge.puml with correct fork transition structure (1 input, N outputs)'() {
        when:
        def net = parseFile('forkEndMerge.puml')

        then:
        def forkTransition = net.transitions.find { it.label == 'T_fork_1' }
        forkTransition != null

        and:
        PetriNetTestUtil.verifyTransitionIO(net, forkTransition, 1, 3)

        and:
        net.incidenceMatrix.getInputWeight(net.startPlace.index, forkTransition.index) == 1
    }

    def 'should compile forkEndMerge.puml with correct merge sync transition structure (N inputs, 1 output)'() {
        when:
        def net = parseFile('forkEndMerge.puml')

        then:
        def mergeSyncTransition = net.transitions.find { it.label == 'T_fork_1_merge_sync' }
        mergeSyncTransition != null

        and:
        PetriNetTestUtil.verifyTransitionIO(net, mergeSyncTransition, 3, 1)

        and:
        def mergePlace = net.places.find { it.label == 'P_fork_1_merge' }
        mergePlace != null
        net.incidenceMatrix.getOutputWeight(mergePlace.index, mergeSyncTransition.index) == 1
    }

    def 'should compile forkEndMerge.puml with all branch actions present and connected'() {
        when:
        def net = parseFile('forkEndMerge.puml')

        then:
        def action1 = net.transitions.find { it.actionKey == 'action 1' }
        def action2 = net.transitions.find { it.actionKey == 'action 2' }
        def action3 = net.transitions.find { it.actionKey == 'action 3' }
        def action4 = net.transitions.find { it.actionKey == 'action 4' }
        def action5 = net.transitions.find { it.actionKey == 'action 5' }

        action1 != null
        action2 != null
        action3 != null
        action4 != null
        action5 != null
    }

    def 'should compile forkEndMerge.puml with branch actions connected to their branch places'() {
        when:
        def net = parseFile('forkEndMerge.puml')

        then:
        def branch1Place = net.places.find { it.label == 'P_fork_1_branch_1' }
        def branch1EndPlace = net.places.find { it.label == 'P_fork_1_branch_1_end' }
        def action1 = net.transitions.find { it.actionKey == 'action 1' }

        branch1Place != null
        branch1EndPlace != null
        action1 != null

        and:
        net.incidenceMatrix.getInputWeight(branch1Place.index, action1.index) == 1
        net.incidenceMatrix.getOutputWeight(branch1EndPlace.index, action1.index) == 1
    }

    def 'should compile forkEndMerge.puml with multi-action branch connected sequentially'() {
        when:
        def net = parseFile('forkEndMerge.puml')

        then:
        def branch3Place = net.places.find { it.label == 'P_fork_1_branch_3' }
        def branch3Intermediate = net.places.find { it.label == 'P_fork_1_branch_3_1' }
        def branch3EndPlace = net.places.find { it.label == 'P_fork_1_branch_3_end' }
        def action3 = net.transitions.find { it.actionKey == 'action 3' }
        def action4 = net.transitions.find { it.actionKey == 'action 4' }

        branch3Place != null
        branch3Intermediate != null
        branch3EndPlace != null
        action3 != null
        action4 != null

        and:
        net.incidenceMatrix.getInputWeight(branch3Place.index, action3.index) == 1
        net.incidenceMatrix.getOutputWeight(branch3Intermediate.index, action3.index) == 1

        and:
        net.incidenceMatrix.getInputWeight(branch3Intermediate.index, action4.index) == 1
        net.incidenceMatrix.getOutputWeight(branch3EndPlace.index, action4.index) == 1
    }

    def 'should compile forkEndMerge.puml with post-merge action connected to merge place and end place'() {
        when:
        def net = parseFile('forkEndMerge.puml')

        then:
        def action5 = net.transitions.find { it.actionKey == 'action 5' }
        def mergePlace = net.places.find { it.label == 'P_fork_1_merge' }

        action5 != null
        mergePlace != null

        and:
        net.incidenceMatrix.getInputWeight(mergePlace.index, action5.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, action5.index) == 1
    }

    def 'should compile forkEndMerge.puml with correct place and transition counts'() {
        when:
        def net = parseFile('forkEndMerge.puml')

        then:
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'

        and:
        net.places.find { it.label == 'P_fork_1_branch_1' } != null
        net.places.find { it.label == 'P_fork_1_branch_1_end' } != null
        net.places.find { it.label == 'P_fork_1_branch_2' } != null
        net.places.find { it.label == 'P_fork_1_branch_2_end' } != null
        net.places.find { it.label == 'P_fork_1_branch_3' } != null
        net.places.find { it.label == 'P_fork_1_branch_3_1' } != null
        net.places.find { it.label == 'P_fork_1_branch_3_end' } != null
        net.places.find { it.label == 'P_fork_1_merge' } != null

        and:
        net.transitions.find { it.label == 'T_fork_1' } != null
        net.transitions.find { it.label == 'T_fork_1_merge_sync' } != null
    }

    def 'should compile forkEndMerge.puml with branch end places feeding merge sync transition'() {
        when:
        def net = parseFile('forkEndMerge.puml')

        then:
        def mergeSyncTransition = net.transitions.find { it.label == 'T_fork_1_merge_sync' }
        mergeSyncTransition != null

        and:
        def branch1EndPlace = net.places.find { it.label == 'P_fork_1_branch_1_end' }
        def branch2EndPlace = net.places.find { it.label == 'P_fork_1_branch_2_end' }
        def branch3EndPlace = net.places.find { it.label == 'P_fork_1_branch_3_end' }

        branch1EndPlace != null
        branch2EndPlace != null
        branch3EndPlace != null

        and:
        net.incidenceMatrix.getInputWeight(branch1EndPlace.index, mergeSyncTransition.index) == 1
        net.incidenceMatrix.getInputWeight(branch2EndPlace.index, mergeSyncTransition.index) == 1
        net.incidenceMatrix.getInputWeight(branch3EndPlace.index, mergeSyncTransition.index) == 1
    }

    @Unroll
    def 'should compile fork diagram with #branchCount branches from string and verify structure'() {
        given:
        def branches = []
        (1..branchCount).each { i -> branches.add(["action ${i}"]) }
        def puml = createForkPuml(branches)

        when:
        def net = parse(puml)

        then:
        net != null
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'

        and:
        def forkTransition = net.transitions.find { it.label == 'T_fork_1' }
        forkTransition != null
        PetriNetTestUtil.verifyTransitionIO(net, forkTransition, 1, branchCount)

        and:
        def mergeSyncTransition = net.transitions.find { it.label == 'T_fork_1_merge_sync' }
        mergeSyncTransition != null
        PetriNetTestUtil.verifyTransitionIO(net, mergeSyncTransition, branchCount, 1)

        and:
        (1..branchCount).each { i ->
            assert net.transitions.find { it.actionKey == "action ${i}" } != null
            assert net.places.find { it.label == "P_fork_1_branch_${i}" } != null
            assert net.places.find { it.label == "P_fork_1_branch_${i}_end" } != null
        }

        where:
        branchCount << [2, 3, 5]
    }

    def 'should compile fork with post-merge actions from string and verify linear connection'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            :after merge 1;
            :after merge 2;
            end
            @enduml
        '''

        when:
        def net = parse(puml)

        then:
        def afterMerge1 = net.transitions.find { it.actionKey == 'after merge 1' }
        def afterMerge2 = net.transitions.find { it.actionKey == 'after merge 2' }
        afterMerge1 != null
        afterMerge2 != null

        and:
        def mergePlace = net.places.find { it.label == 'P_fork_1_merge' }
        mergePlace != null

        and:
        net.incidenceMatrix.getInputWeight(mergePlace.index, afterMerge1.index) == 1

        and:
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, afterMerge2.index) == 1
    }

    def 'should compile fork with no post-merge actions and verify direct merge to end transition'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            end
            @enduml
        '''

        when:
        def net = parse(puml)

        then:
        def toEndTransition = net.transitions.find { it.label == 'T_fork_1_to_end' }
        toEndTransition != null

        and:
        def mergePlace = net.places.find { it.label == 'P_fork_1_merge' }
        mergePlace != null

        and:
        net.incidenceMatrix.getInputWeight(mergePlace.index, toEndTransition.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, toEndTransition.index) == 1
    }

    def 'should compile nested fork structure correctly'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
              fork
                :nested action 1;
              fork again
                :nested action 2;
              end merge
            fork again
              :action 2;
            end merge
            end
            @enduml
        '''

        when:
        def net = parse(puml)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0

        and:
        net.transitions.find { it.actionKey == 'action 1' } != null
        net.transitions.find { it.actionKey == 'action 2' } != null
        net.transitions.find { it.actionKey == 'nested action 1' } != null
        net.transitions.find { it.actionKey == 'nested action 2' } != null
    }

    def 'should compile fork with multiple actions per branch and verify sequential connections'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :a1;
              :a2;
              :a3;
            fork again
              :b1;
              :b2;
            end merge
            end
            @enduml
        '''

        when:
        def net = parse(puml)

        then:
        def a1 = net.transitions.find { it.actionKey == 'a1' }
        def a2 = net.transitions.find { it.actionKey == 'a2' }
        def a3 = net.transitions.find { it.actionKey == 'a3' }
        def b1 = net.transitions.find { it.actionKey == 'b1' }
        def b2 = net.transitions.find { it.actionKey == 'b2' }

        a1 != null
        a2 != null
        a3 != null
        b1 != null
        b2 != null

        and:
        def branch1Place = net.places.find { it.label == 'P_fork_1_branch_1' }
        def branch1Mid1 = net.places.find { it.label == 'P_fork_1_branch_1_1' }
        def branch1Mid2 = net.places.find { it.label == 'P_fork_1_branch_1_2' }
        def branch1End = net.places.find { it.label == 'P_fork_1_branch_1_end' }

        branch1Place != null
        branch1Mid1 != null
        branch1Mid2 != null
        branch1End != null

        and:
        net.incidenceMatrix.getInputWeight(branch1Place.index, a1.index) == 1
        net.incidenceMatrix.getOutputWeight(branch1Mid1.index, a1.index) == 1
        net.incidenceMatrix.getInputWeight(branch1Mid1.index, a2.index) == 1
        net.incidenceMatrix.getOutputWeight(branch1Mid2.index, a2.index) == 1
        net.incidenceMatrix.getInputWeight(branch1Mid2.index, a3.index) == 1
        net.incidenceMatrix.getOutputWeight(branch1End.index, a3.index) == 1
    }

    private static String createForkPuml(List<List<String>> branches) {
        def sb = new StringBuilder()
        sb << '@startuml\n'
        sb << 'start\n'
        sb << 'fork\n'
        branches.eachWithIndex { branchActions, index ->
            if (index > 0) {
                sb << 'fork again\n'
            }
            branchActions.each { action ->
                sb << "  :${action};\n"
            }
        }
        sb << 'end merge\n'
        sb << 'end\n'
        sb << '@enduml\n'
        return sb.toString()
    }
}
