package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ActivityDiagramParser
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unsupported PlantUML control syntax (elseif, while/endwhile, switch/case/endswitch)
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
        'switch with cases'         | 'switch'    | 'switch (s?)'            | '@startuml\nstart\nswitch (s?)\ncase (a)\n  :A;\ncase (b)\n  :B;\nendswitch\nend\n@enduml'
        'case without switch'       | 'case'      | 'case (a)'               | '@startuml\nstart\n  :A;\ncase (a)\n  :B;\nend\n@enduml'
        'endswitch without switch'  | 'endswitch' | 'endswitch'             | '@startuml\nstart\n  :A;\nendswitch\nend\n@enduml'
    }

    def 'should list all offending lines of the same unsupported construct'() {
        given:
        def puml = '@startuml\nstart\nswitch (s?)\ncase (a)\n  :A;\ncase (b)\n  :B;\nendswitch\nend\n@enduml'

        when:
        parser.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('unimplemented feature')
        ex.message.contains("'switch (s?)'")
        ex.message.contains("'case (a)'")
        ex.message.contains("'case (b)'")
        ex.message.contains("'endswitch'")
        ex.message.contains("'switch'")
        ex.message.contains("'endswitch'")
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
