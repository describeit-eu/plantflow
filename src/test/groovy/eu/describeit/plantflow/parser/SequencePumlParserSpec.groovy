package eu.describeit.plantflow.parser

import spock.lang.Specification

class SequencePumlParserSpec extends Specification {

    def 'should parse single action linear activity diagram into PetriNet'() {
        when:
        def net = ParserTestBase.parse(ParserTestBase.createLinearPuml('process order'))

        then:
        ParserTestBase.validateNetStructure(net, 2, 1)
        ParserTestBase.validatePlaceLabels(net, ['start', 'end'])
        ParserTestBase.validateTransitionLabels(net, ['process order'])
        ParserTestBase.validateTransitionAction(net, 'process order', 0)

        and:
        ParserTestBase.verifyTransitionConnection(net, 0, 0, 1)
    }

    def 'should parse multi-action linear activity diagram into PetriNet'() {
        when:
        def net = ParserTestBase.parse(ParserTestBase.createLinearPuml('Hello world', 'groovy goodness'))

        then:
        ParserTestBase.validateNetStructure(net, 3, 2)
        ParserTestBase.validatePlaceLabels(net, ['start', 'P_1', 'end'])
        ParserTestBase.validateTransitionLabels(net, ['Hello world', 'groovy goodness'])

        and:
        // T_0: consumes from P_start (0), produces to P_1 (1)
        ParserTestBase.verifyTransitionConnection(net, 0, 0, 1)

        // T_1: consumes from P_1 (1), produces to P_end (2)
        ParserTestBase.verifyTransitionConnection(net, 1, 1, 2)
    }

    def 'should parse sequence.puml from file'() {
        when:
        def net = ParserTestBase.parse(new File('src/test/data/puml/sequence.puml'))

        then:
        ParserTestBase.validateNetStructure(net, 3, 2)
        ParserTestBase.validateTransitionLabels(net, ['Hello world', 'groovy goodness'])
    }

    def 'should parse activity diagram terminated with stop keyword'() {
        when:
        def net = ParserTestBase.parse(ParserTestBase.createLinearPuml('step one', 'step two').replace('end', 'stop'))

        then:
        ParserTestBase.validateNetStructure(net, 3, 2)
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'
        ParserTestBase.validateTransitionLabels(net, ['step one', 'step two'])
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
        def net = ParserTestBase.parse(puml)

        then:
        ParserTestBase.validateNetStructure(net, 3, 2)
        ParserTestBase.validateTransitionLabels(net, ['step one', 'step two'])
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
        def net = ParserTestBase.parse(puml)

        then:
        ParserTestBase.validateNetStructure(net, 3, 2)
        ParserTestBase.validateTransitionLabels(net, ['step one', 'step two'])
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
}
