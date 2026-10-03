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
        // Check that we have action transitions
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
        ex.message.contains('end merge') || ex.message.contains('fork')
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
        ex.message.contains('end merge') || ex.message.contains('fork')
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
}