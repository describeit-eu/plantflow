package eu.describeit.plantflow.parser

import spock.lang.Specification

class ForkEndMergePumlParserSpec extends Specification {

    def 'should parse forkEndMerge.puml from file'() {
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
        // Check that we have fork-related places
        def forkPlace = net.places.find { it.label.contains('fork') }
        forkPlace != null

        def mergePlace = net.places.find { it.label.contains('merge') }
        mergePlace != null

        and:
        // Check that we have action transitions for the fork branches
        def action1Transition = net.transitions.find { it.actionKey == 'action 1' }
        def action2Transition = net.transitions.find { it.actionKey == 'action 2' }
        def action3Transition = net.transitions.find { it.actionKey == 'action 3' }
        def action4Transition = net.transitions.find { it.actionKey == 'action 4' }

        action1Transition != null
        action2Transition != null
        action3Transition != null
        action4Transition != null
    }

    def 'should parse fork diagram from string'() {
        when:
        def net = ParserTestBase.parse(ParserTestBase.createForkPuml(['action 1'], ['action 2'], ['action 3']))

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
    }

    def 'should throw IllegalArgumentException when fork has no matching end merge'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('Unclosed fork')
    }

    def 'should throw IllegalArgumentException with specific message for unclosed fork'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
            fork again
              :action 2;
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Unclosed fork - missing end merge'
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
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('end merge without active fork context')
    }

    def 'should handle multiple nested fork structures correctly'() {
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
        def action1Transition = net.transitions.find { it.actionKey == 'action 1' }
        def action2Transition = net.transitions.find { it.actionKey == 'action 2' }
        def nested1Transition = net.transitions.find { it.actionKey == 'nested action 1' }
        def nested2Transition = net.transitions.find { it.actionKey == 'nested action 2' }
        
        action1Transition != null
        action2Transition != null
        nested1Transition != null
        nested2Transition != null
    }

    def 'should handle multiple branches in fork'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
            fork again
              :action 2;
            fork again
              :action 3;
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
    }

    def 'should throw IllegalArgumentException when fork has single branch'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
            end merge
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('2 branches') || ex.message.contains('branch')
    }

    def 'should throw IllegalArgumentException when fork has no branches at all'() {
        given:
        def puml = '''
            @startuml
            start
            fork
            end merge
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('branch') || ex.message.contains('action')
    }

    def 'should throw IllegalArgumentException when fork has empty branches'() {
        given:
        def puml = '''
            @startuml
            start
            fork
            fork again
            end merge
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('branch') || ex.message.contains('action')
    }

    def 'should handle fork with only whitespace in branches'() {
        given:
        def puml = '''
            @startuml
            start
            fork

            fork again

            end merge
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('branch') || ex.message.contains('action')
    }

    def 'should handle fork with multiple actions in branches'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
              :action 2;
            fork again
              :action 3;
              :action 4;
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
          def action1Transition = net.transitions.find { it.actionKey == 'action 1' }
          def action2Transition = net.transitions.find { it.actionKey == 'action 2' }
          def action3Transition = net.transitions.find { it.actionKey == 'action 3' }
          def action4Transition = net.transitions.find { it.actionKey == 'action 4' }
          
          action1Transition != null
          action2Transition != null
          action3Transition != null
          action4Transition != null
    }

    def 'should handle actions after fork completion'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :branch action 1;
            fork again
              :branch action 2;
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
        def branch1Transition = net.transitions.find { it.actionKey == 'branch action 1' }
        def branch2Transition = net.transitions.find { it.actionKey == 'branch action 2' }
        
        branch1Transition != null
        branch2Transition != null
        
        // Test that the diagram has the expected structure for fork completion
        net.places.any { it.label.contains('merge') || it.label.contains('end') }
    }

    def 'should handle end merge return true when pattern matches'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
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
        // Verify that end merge was processed correctly
        net.places.any { it.label.contains('end merge') || it.label.contains('merge') }
    }

    def 'should handle fork with exactly two branches'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
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
        def action1Transition = net.transitions.find { it.actionKey == 'action 1' }
        def action2Transition = net.transitions.find { it.actionKey == 'action 2' }
        
        action1Transition != null
        action2Transition != null
    }

    def 'should handle fork with many branches to test boundary conditions'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
            fork again
              :action 2;
            fork again
              :action 3;
            fork again
              :action 4;
            fork again
              :action 5;
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
        // Test all actions are parsed correctly
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

    def 'should throw IllegalArgumentException when fork again without active fork context'() {
        given:
        def puml = '''
            @startuml
            start
            fork again
              :action 1;
            end merge
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('fork again') || ex.message.contains('context')
    }

}
