package eu.describeit.plantflow.parser

import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class SequencePumlParserSpec extends Specification {

    def 'should parse single action linear activity diagram into PetriNetStructure'() {
        when:
        def net = parse(linearPuml('process order'))

        then:
        validateNetStructure(net, 2, 1)
        validatePlaceLabels(net, ['start', 'end'])
        validateTransitionLabels(net, ['process order'])
        validateTransitionAction(net, 'process order', 0)

        and:
        verifyTransitionConnection(net, 0, 0, 1)
    }

    def 'should parse multi-action linear activity diagram into PetriNetStructure'() {
        when:
        def net = parse(linearPuml('Hello world', 'groovy goodness'))

        then:
        validateNetStructure(net, 3, 2)
        validatePlaceLabels(net, ['start', 'P_1', 'end'])
        validateTransitionLabels(net, ['Hello world', 'groovy goodness'])

        and:
        // T_0: consumes from P_start (0), produces to P_1 (1)
        verifyTransitionConnection(net, 0, 0, 1)

        // T_1: consumes from P_1 (1), produces to P_end (2)
        verifyTransitionConnection(net, 1, 1, 2)
    }

    def 'should parse sequence.puml from file'() {
        when:
        def net = parse(new File('src/test/data/puml/sequence.puml'))

        then:
        validateNetStructure(net, 3, 2)
        validateTransitionLabels(net, ['Hello world', 'groovy goodness'])
    }

    def 'should parse activity diagram terminated with stop keyword'() {
        when:
        def net = parse(linearPuml('step one', 'step two').replace('end', 'stop'))

        then:
        validateNetStructure(net, 3, 2)
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'
        validateTransitionLabels(net, ['step one', 'step two'])
    }

    def 'should ignore non-action unrecognized lines inside diagram'() {
        given:
        def puml = '''
            @startuml
            start
            :step one;
            non-action unparsed line
            :step two;
            end
            @enduml
        '''

        when:
        def net = parse(puml)

        then:
        validateNetStructure(net, 3, 2)
        validateTransitionLabels(net, ['step one', 'step two'])
    }

    def 'should skip comments, blank lines, and whitespace lines throughout diagram'() {
        given:
        def puml = """
            ' Leading header comment
            @startuml
            ' Pre-start comment
            
                \t
            start
            ' Mid-diagram comment
            :step one;
            
            ' Another comment
            :step two;
            
            end
            ' Trailing comment
            @enduml
            ' Post enduml comment
        """

        when:
        def net = parse(puml)

        then:
        validateNetStructure(net, 3, 2)
        validateTransitionLabels(net, ['step one', 'step two'])
    }

    def 'should throw IllegalArgumentException when parsing null file'() {
        when:
        new eu.describeit.plantflow.ActivityDiagramParser().parse((File) null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'File cannot be null'
    }

    def 'should throw IllegalArgumentException for invalid diagram input: #scenario'() {
        when:
        new eu.describeit.plantflow.ActivityDiagramParser().parse((String) content)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario                     | content                               | expectedMessage
        'null content'               | null                                  | 'PlantUML content cannot be empty'
        'empty string content'       | ''                                    | 'PlantUML content cannot be empty'
        'whitespace-only content'    | '   \n  \t  '                         | 'PlantUML content cannot be empty'
        'missing start'              | '@startuml\n:step 1;\nend\n@enduml'   | "Diagram must contain 'start'"
        'missing end or stop'        | '@startuml\nstart\n:step 1;\n@enduml' | "Diagram must contain 'end' or 'stop'"
        'missing action transitions' | '@startuml\nstart\nend\n@enduml'      | 'Diagram must contain at least one action transition'
    }

    def 'should parse diagram with action containing special characters'() {
        given:
        def puml = '''
            @startuml
            start
            :process order #123 [priority: high];
            :send email to user@example.com;
            end
            @enduml
        '''

        when:
        def net = parse(puml)

        then:
        validateNetStructure(net, 3, 2)
        net.transitions[0].actionKey == 'process order #123 [priority: high]'
        net.transitions[1].actionKey == 'send email to user@example.com'
    }

    def 'should parse diagram with single character action'() {
        given:
        def puml = '''
            @startuml
            start
            :a;
            end
            @enduml
        '''

        when:
        def net = parse(puml)

        then:
        validateNetStructure(net, 2, 1)
        net.transitions[0].actionKey == 'a'
    }

    def 'should parse diagram with many actions'() {
        given:
        def actions = (1..10).collect { "step ${it}" }
        def puml = linearPuml(actions as String[])

        when:
        def net = parse(puml)

        then:
        validateNetStructure(net, 11, 10)
        actions.eachWithIndex { action, idx ->
            assert net.transitions[idx].actionKey == action
        }
    }

    def 'should parse diagram with action containing colon'() {
        given:
        def puml = '''
            @startuml
            start
            :action: with colon;
            end
            @enduml
        '''

        when:
        def net = parse(puml)

        then:
        net != null
        net.transitions.size() == 1
    }

    def 'should parse diagram with leading and trailing whitespace on action lines'() {
        given:
        def puml = '''
            @startuml
            start
                :  spaced action  ;
            end
            @enduml
        '''

        when:
        def net = parse(puml)

        then:
        net != null
        net.transitions[0].actionKey == 'spaced action'
    }
}
