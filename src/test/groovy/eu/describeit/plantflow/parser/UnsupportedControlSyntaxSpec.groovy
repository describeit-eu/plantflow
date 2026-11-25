package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ActivityDiagramParser
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unsupported PlantUML control syntax (elseif, while/endwhile)
 * must be rejected explicitly as an unimplemented feature, never silently ignored.
 */
class UnsupportedControlSyntaxSpec extends Specification {

    private ActivityDiagramParser parser = new ActivityDiagramParser()

    @Unroll
    def 'should reject unsupported syntax #construct as unimplemented feature: #scenario'() {
        when:
        parser.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('unimplemented feature')
        ex.message.contains("'$construct'")
        ex.message.contains("'$line'")
        !ex.message.contains('Malformed')

        where:
        scenario                    | construct   | line                     | puml
        'elseif inside an if'       | 'elseif'    | 'elseif (g2) then (yes)' | '@startuml\nstart\nif (g1) then (yes)\n  :A;\nelseif (g2) then (yes)\n  :B;\nelse (no)\n  :C;\nendif\nend\n@enduml'
        'while loop'                | 'while'     | 'while (c1) is (yes)'    | '@startuml\nstart\nwhile (c1) is (yes)\n  :read;\nendwhile (no)\nend\n@enduml'
        'endwhile without while'    | 'endwhile'  | 'endwhile (no)'          | '@startuml\nstart\n  :A;\nendwhile (no)\nend\n@enduml'
    }

    def 'should list all offending lines of the same unsupported construct'() {
        given:
        def puml = '@startuml\nstart\nif (g1) then (yes)\n  :A;\nelseif (g2) then (yes)\n  :B;\nelseif (g3) then (yes)\n  :C;\nelse (no)\n  :D;\nendif\nend\n@enduml'

        when:
        parser.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('unimplemented feature')
        ex.message.contains("'elseif (g2) then (yes)'")
        ex.message.contains("'elseif (g3) then (yes)'")
        ex.message.contains("'elseif'")
    }

    def 'should reject unsupported syntax even when no start/end present'() {
        when:
        parser.parse('while (c1) is (yes)\n  :read;\nendwhile (no)')

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('unimplemented feature')
        ex.message.contains("'while'")
    }

    def 'should still parse a supported diagram without unsupported constructs'() {
        given:
        def puml = '@startuml\nstart\nif (g) then (yes)\n  :A;\nelse (no)\n  :B;\nendif\nend\n@enduml'

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.find { it.actionKey == 'A' } != null
        net.transitions.find { it.actionKey == 'B' } != null
    }
}
