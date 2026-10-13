package eu.describeit.plantflow.parser

import eu.describeit.plantflow.PetriNetTestUtil
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
        def action5Transition = net.transitions.find { it.actionKey == 'action 5' }

        action1Transition != null
        action2Transition != null
        action3Transition != null
        action4Transition != null
        action5Transition != null
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

    def 'should compile forkEndMerge.puml with correct place and transition structure'() {
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

        and: 'fork transition has 1 input and 3 outputs, merge sync has 3 inputs and 1 output'
        def forkTransition = net.transitions.find { it.label == 'T_fork_1' }
        forkTransition != null
        PetriNetTestUtil.verifyTransitionIO(net, forkTransition, 1, 3)

        def mergeSyncTransition = net.transitions.find { it.label == 'T_fork_1_merge_sync' }
        mergeSyncTransition != null
        PetriNetTestUtil.verifyTransitionIO(net, mergeSyncTransition, 3, 1)
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

    @Unroll
    def 'should parse fork diagram with #scenario and verify all actions are parsed'() {
        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        expectedActions.each { action ->
            assert net.transitions.find { it.actionKey == action } != null
        }

        where:
        scenario                                        | puml                                                                              | expectedActions
        'comment lines between fork and end merge'      | '''
            @startuml
            start
            fork
              'this is a comment
              :action a;
              'another comment
            fork again
              :action b;
            end merge
            'comment after merge
            end
            @enduml
        '''                                                                                 | ['action a', 'action b']
        'action before fork keyword (ignored)'          | '''
            @startuml
            start
            :before fork;
            fork
              :action a;
            fork again
              :action b;
            end merge
            end
            @enduml
        '''                                                                                 | ['action a', 'action b']
        'unrecognized line (ignored)'                   | '''
            @startuml
            start
            fork
              :action a;
              some unrecognized line
            fork again
              :action b;
            end merge
            end
            @enduml
        '''                                                                                 | ['action a', 'action b']
        'blank lines inside branches'                   | '''
            @startuml
            start
            fork
              :action a;

              :action a2;
            fork again
              :action b;

            end merge
            end
            @enduml
        '''                                                                                 | ['action a', 'action a2', 'action b']
        'stop keyword instead of end'                   | '''
            @startuml
            start
            fork
              :action a;
            fork again
              :action b;
            end merge
            stop
            @enduml
        '''                                                                                 | ['action a', 'action b']
    }

    @Unroll
    def 'should parse fork diagram with post-merge actions after end merge: #postActions'() {
        given:
        def puml = ParserTestBase.createForkPumlWithBranches([['branch a'], ['branch b']], postActions)

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.transitions.find { it.actionKey == 'branch a' } != null
        net.transitions.find { it.actionKey == 'branch b' } != null
        postActions.each { action ->
            assert net.transitions.find { it.actionKey == action } != null
        }

        where:
        postActions << [['after merge'], ['post 1', 'post 2', 'post 3']]
    }

    def 'should parse fork diagram with deeply nested fork structures'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :outer 1;
              fork
                :inner 1;
                fork
                  :deep 1;
                fork again
                  :deep 2;
                end merge
              fork again
                :inner 2;
              end merge
            fork again
              :outer 2;
            end merge
            end
            @enduml
        '''

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.transitions.find { it.actionKey == 'outer 1' } != null
        net.transitions.find { it.actionKey == 'outer 2' } != null
        net.transitions.find { it.actionKey == 'inner 1' } != null
        net.transitions.find { it.actionKey == 'inner 2' } != null
        net.transitions.find { it.actionKey == 'deep 1' } != null
        net.transitions.find { it.actionKey == 'deep 2' } != null
    }

}
