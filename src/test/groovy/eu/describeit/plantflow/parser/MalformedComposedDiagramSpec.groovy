package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ActivityDiagramParser
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

class MalformedComposedDiagramSpec extends Specification {

    private ActivityDiagramParser parser = new ActivityDiagramParser()

    @Unroll
    def 'should reject malformed composed construct: #scenario'() {
        when:
        parser.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedMessage)

        where:
        scenario                                              | puml                                                                                                                                                                                                          | expectedMessage
        'unclosed repeat inside a conditional branch'         | '@startuml\nstart\nif (x?) then (yes)\n  repeat\n    :a;\nelse (no)\n  :b;\nendif\nend\n@enduml'                                                                                                            | 'Unclosed repeat'
        'unclosed repeat inside else branch closed by endif'  | '@startuml\nstart\nif (x?) then (yes)\n  :a;\nelse (no)\n  repeat\n    :b;\nendif\nend\n@enduml'                                                                                                              | 'Unclosed repeat'
        'repeat while without matching repeat inside branch'  | '@startuml\nstart\nif (x?) then (yes)\n  repeat while (y?) is (yes) not (no)\nelse (no)\n  :b;\nendif\nend\n@enduml'                                                                                          | 'Unclosed if'
        'repeat while without any repeat'                    | '@startuml\nstart\nrepeat while (y?) is (yes) not (no)\nend\n@enduml'                                                                                                                                        | 'repeat while without active repeat'
        'detach not following a repeat while inside branch'   | '@startuml\nstart\nif (x?) then (yes)\n  repeat\n    :a;\n  repeat while (y?) is (yes) not (no)\n  :other;\n  -[hidden]-> detach\nelse (no)\n  :b;\nendif\nend\n@enduml'                                   | 'detach'
        'detach inside a branch without any loop'             | '@startuml\nstart\nif (x?) then (yes)\n  :a;\n  detach\nelse (no)\n  :b;\nendif\nend\n@enduml'                                                                                                              | 'detach'
        'empty loop body containing only an unclosed if'      | '@startuml\nstart\nrepeat\n  if (x?) then (yes)\n    :a;\nrepeat while (y?) is (yes) not (no)\nend\n@enduml'                                                                                                | 'Unclosed if'
        'loop body with only an empty conditional'            | '@startuml\nstart\nrepeat\n  if (x?) then (yes)\n  else (no)\n    :b;\n  endif\nrepeat while (y?) is (yes) not (no)\nend\n@enduml'                                                                          | 'Then block must contain at least one action'
        'unclosed if inside a repeat loop'                    | '@startuml\nstart\nrepeat\n  :a;\n  if (x?) then (yes)\n    :b;\nrepeat while (y?) is (yes) not (no)\nend\n@enduml'                                                                                         | 'Unclosed if'
        'else inside a loop body without an if'               | '@startuml\nstart\nrepeat\n  :a;\n  else (no)\nrepeat while (y?) is (yes) not (no)\nend\n@enduml'                                                                                                          | 'Unclosed repeat'
        'endif inside a loop body without an if'              | '@startuml\nstart\nrepeat\n  :a;\n  endif\nrepeat while (y?) is (yes) not (no)\nend\n@enduml'                                                                                                              | 'Unclosed repeat'
        'malformed repeat while line in composed diagram'     | '@startuml\nstart\nrepeat\n  if (x?) then (yes)\n    :a;\n  else (no)\n    :b;\n  endif\nrepeat while (broken\nend\n@enduml'                                                                               | 'Malformed'
    }

    def 'should accept well-formed composed diagrams through the public parser'() {
        expect:
        parse(new File('src/test/data/puml/batchProcessing.puml')) != null
        parse('@startuml\nstart\nif (x?) then (yes)\n  repeat\n    :a;\n  repeat while (y?) is (yes) not (no)\nelse (no)\n  :b;\nendif\nend\n@enduml') != null
    }
}
