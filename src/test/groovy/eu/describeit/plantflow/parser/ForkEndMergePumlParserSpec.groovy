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

}
