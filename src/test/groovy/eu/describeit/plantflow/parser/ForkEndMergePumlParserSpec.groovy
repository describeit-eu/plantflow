package eu.describeit.plantflow.parser

import spock.lang.Specification
import spock.lang.Unroll

class ForkEndMergePumlParserSpec extends Specification {

    def 'should parse forkEndMerge.puml from file and verify all acceptance criteria'() {
        when:
        def net = ParserTestBase.parse(new File('src/test/data/puml/forkEndMerge.puml'))

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
        net.startPlace != null
        net.startPlace.label == 'start'
        net.endPlace != null
        net.endPlace.label == 'end'

        and:
        // Acceptance Criteria 1: ActivityDiagramParser detects fork/fork again/end merge as control keywords
        // Acceptance Criteria 3: forkEndMerge.puml parses successfully into ActivityDiagram with ForkNode with 3+ branches
        def forkPlace = net.places.find { it.label.contains('fork') }
        forkPlace != null

        def mergePlace = net.places.find { it.label.contains('merge') }
        mergePlace != null

        and:
        // Verify all actions from the file are parsed
        def action1Transition = net.transitions.find { it.actionKey == 'action 1' }
        def action2Transition = net.transitions.find { it.actionKey == 'action 2' }
        def action3Transition = net.transitions.find { it.actionKey == 'action 3' }
        def action4Transition = net.transitions.find { it.actionKey == 'action 4' }

        action1Transition != null
        action2Transition != null
        action3Transition != null
        action4Transition != null
    }

    @Unroll
    def 'should parse fork diagram with #branchCount branches from string'() {
        given:
        def branches = []
        (1..branchCount).each { i -> branches.add(["action ${i}"]) }
        def puml = ParserTestBase.createForkPumlWithBranches(branches)

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
        
        and:
        // Verify all actions are parsed
        (1..branchCount).each { i ->
            def actionTransition = net.transitions.find { it.actionKey == "action ${i}" }
            actionTransition != null
        }

        where:
        branchCount << [2, 3, 5]
    }

    def 'should handle nested fork structures correctly'() {
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
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
        
        and:
        // Acceptance Criteria 2: Parser uses stack-based tracking to handle nested fork contexts
        def action1Transition = net.transitions.find { it.actionKey == 'action 1' }
        def action2Transition = net.transitions.find { it.actionKey == 'action 2' }
        def nested1Transition = net.transitions.find { it.actionKey == 'nested action 1' }
        def nested2Transition = net.transitions.find { it.actionKey == 'nested action 2' }
        
        action1Transition != null
        action2Transition != null
        nested1Transition != null
        nested2Transition != null
    }

    def 'should throw IllegalArgumentException when end merge without active fork'() {
        given:
        def puml = '''
            @startuml
            start
            end merge
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        // Acceptance Criteria 4: Parser throws exception on end merge without active fork context
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('end merge without active fork context')
    }

    @Unroll
    def 'should throw IllegalArgumentException for invalid fork structure: #scenario'() {
        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedMessage)

        where:
        scenario | puml | expectedMessage
        'single fork without end merge' | '''
            @startuml
            start
            fork
              :action 1;
            end
            @enduml
        ''' | 'Unclosed fork'
        'nested fork without end merge' | '''
            @startuml
            start
            fork
              :action 1;
            fork again
              :action 2;
            end
            @enduml
        ''' | 'Unclosed fork'
        'fork again without active fork context' | '''
            @startuml
            start
            fork again
              :action 1;
            end merge
            end
            @enduml
        ''' | 'fork again'
        'end merge without active fork' | '''
            @startuml
            start
            end merge
            end
            @enduml
        ''' | 'end merge without active fork context'
    }

    @Unroll
    def 'should throw IllegalArgumentException for invalid branch structure: #scenario'() {
        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedMessage)

        where:
        scenario | puml | expectedMessage
        'single branch' | '''
            @startuml
            start
            fork
              :action 1;
            end merge
            end
            @enduml
        ''' | 'branch'
        'no branches' | '''
            @startuml
            start
            fork
            end merge
            end
            @enduml
        ''' | 'branch'
        'empty branches' | '''
            @startuml
            start
            fork
            fork again
            end merge
            end
            @enduml
        ''' | 'branch'
    }

    def 'should compile forkEndMerge.puml with correct fork transition structure (1 input, N outputs)'() {
        when:
        def net = ParserTestBase.parse(new File('src/test/data/puml/forkEndMerge.puml'))

        then:
        def forkTransition = net.transitions.find { it.label == 'T_fork_1' }
        forkTransition != null

        and:
        def io = countTransitionIO(net, forkTransition)
        io[0] == 1
        io[1] == 3

        and:
        net.incidenceMatrix.getInputWeight(net.startPlace.index, forkTransition.index) == 1
    }

    def 'should compile forkEndMerge.puml with correct merge transition structure (N inputs, 1 output)'() {
        when:
        def net = ParserTestBase.parse(new File('src/test/data/puml/forkEndMerge.puml'))

        then:
        def mergeSyncTransition = net.transitions.find { it.label == 'T_fork_1_merge_sync' }
        mergeSyncTransition != null

        and:
        def io = countTransitionIO(net, mergeSyncTransition)
        io[0] == 3
        io[1] == 1

        and:
        def mergePlace = net.places.find { it.label == 'P_fork_1_merge' }
        mergePlace != null
        net.incidenceMatrix.getOutputWeight(mergePlace.index, mergeSyncTransition.index) == 1
    }

    def 'should compile forkEndMerge.puml with post-merge action connected to merge place'() {
        when:
        def net = ParserTestBase.parse(new File('src/test/data/puml/forkEndMerge.puml'))

        then:
        def action5Transition = net.transitions.find { it.actionKey == 'action 5' }
        action5Transition != null

        and:
        def mergePlace = net.places.find { it.label == 'P_fork_1_merge' }
        mergePlace != null

        and:
        net.incidenceMatrix.getInputWeight(mergePlace.index, action5Transition.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, action5Transition.index) == 1
    }

    def 'should compile forkEndMerge.puml with all branch actions present'() {
        when:
        def net = ParserTestBase.parse(new File('src/test/data/puml/forkEndMerge.puml'))

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

    def 'should compile forkEndMerge.puml with correct branch end places feeding merge sync'() {
        when:
        def net = ParserTestBase.parse(new File('src/test/data/puml/forkEndMerge.puml'))

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

    def 'should compile forkEndMerge.puml with correct place and transition counts'() {
        when:
        def net = ParserTestBase.parse(new File('src/test/data/puml/forkEndMerge.puml'))

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

    def 'should compile fork with post-merge actions from string and verify structure'() {
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
        def net = ParserTestBase.parse(puml)

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

    private static int[] countTransitionIO(def net, def transition) {
        int inputCount = 0
        int outputCount = 0
        net.places.each { place ->
            if (net.incidenceMatrix.getInputWeight(place.index, transition.index) > 0) inputCount++
            if (net.incidenceMatrix.getOutputWeight(place.index, transition.index) > 0) outputCount++
        }
        [inputCount, outputCount] as int[]
    }

}
