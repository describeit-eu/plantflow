package eu.describeit.plantflow

import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import spock.lang.Specification

class SequencePumlParserSpec extends Specification {

    def 'should parse single action linear activity diagram into PetriNet'() {
        given:
        def puml = '''
            @startuml
            start
            :process order;
            end
            @enduml
        '''
        def parser = new ActivityDiagramParser()

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.places.size() == 2
        net.transitions.size() == 1

        and:
        net.startPlace.index == 0
        net.startPlace.label == 'start'
        net.endPlace.index == 1
        net.endPlace.label == 'end'

        and:
        def transition = net.transitions[0]
        transition.index == 0
        transition.label == 'process order'
        transition.actionKey == 'process order'

        and:
        net.incidenceMatrix.getInputWeight(0, 0) == 1
        net.incidenceMatrix.getOutputWeight(0, 0) == 0
        net.incidenceMatrix.getInputWeight(1, 0) == 0
        net.incidenceMatrix.getOutputWeight(1, 0) == 1
        net.incidenceMatrix.getIncidence(0, 0) == -1
        net.incidenceMatrix.getIncidence(1, 0) == 1
    }

    def 'should parse multi-action linear activity diagram into PetriNet'() {
        given:
        def puml = '''
            @startuml
            start
            :Hello world;
            :groovy goodness;
            end
            @enduml
        '''
        def parser = new ActivityDiagramParser()

        when:
        def net = parser.parse(puml)

        then:
        net.places.size() == 3
        net.transitions.size() == 2

        and:
        net.places[0].label == 'start'
        net.places[1].label == 'P_1'
        net.places[2].label == 'end'

        and:
        net.transitions[0].label == 'Hello world'
        net.transitions[1].label == 'groovy goodness'

        and:
        // T_0: consumes from P_start (0), produces to P_1 (1)
        net.incidenceMatrix.getInputWeight(0, 0) == 1
        net.incidenceMatrix.getOutputWeight(1, 0) == 1

        // T_1: consumes from P_1 (1), produces to P_end (2)
        net.incidenceMatrix.getInputWeight(1, 1) == 1
        net.incidenceMatrix.getOutputWeight(2, 1) == 1
    }

    def 'should parse sequence.puml from file'() {
        given:
        def parser = new ActivityDiagramParser()
        def file = new File('src/test/data/puml/sequence.puml')

        when:
        def net = parser.parse(file)

        then:
        net.places.size() == 3
        net.transitions.size() == 2
        net.transitions[0].label == 'Hello world'
        net.transitions[1].label == 'groovy goodness'
    }

    def 'should parse activity diagram terminated with stop keyword'() {
        given:
        def puml = '''
            @startuml
            ' This is a comment
            start
            :step one;
            :step two;
            stop
            @enduml
        '''
        def parser = new ActivityDiagramParser()

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.places.size() == 3
        net.transitions.size() == 2
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'
        net.transitions[0].label == 'step one'
        net.transitions[1].label == 'step two'
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
        def parser = new ActivityDiagramParser()

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.places.size() == 3
        net.transitions.size() == 2
        net.transitions[0].label == 'step one'
        net.transitions[1].label == 'step two'
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
        def parser = new ActivityDiagramParser()

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.places.size() == 3
        net.transitions.size() == 2
        net.transitions[0].label == 'step one'
        net.transitions[1].label == 'step two'
    }

    def 'should throw IllegalArgumentException when parsing null file'() {
        given:
        def parser = new ActivityDiagramParser()

        when:
        parser.parse((File) null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'File cannot be null'
    }

    def 'should throw IllegalArgumentException for invalid diagram input: #scenario'() {
        given:
        def parser = new ActivityDiagramParser()

        when:
        parser.parse((String) content)

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
