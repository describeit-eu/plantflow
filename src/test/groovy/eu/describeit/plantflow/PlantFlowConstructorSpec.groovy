package eu.describeit.plantflow


import eu.describeit.plantflow.engine.PetriNet
import spock.lang.Specification

class PlantFlowConstructorSpec extends Specification {

    def 'should instantiate PlantFlow using various constructor and factory overloads: #scenario'() {
        expect:
        flowInstance.petriNet != null
        flowInstance.handlerRegistry != null
        flowInstance.executionContext != null

        where:
        scenario                        | flowInstance
        'PetriNet only'                 | new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'))
        'PetriNet + registry'           | new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'), new HandlerRegistry())
        'PetriNet + registry + context' | new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'), new HandlerRegistry(), new ExecutionContext([k: 'v']))
        'String only'                   | new PlantFlow('@startuml\nstart\n:a;\nend\n@enduml')
        'String + registry'             | new PlantFlow('@startuml\nstart\n:a;\nend\n@enduml', new HandlerRegistry())
        'String + registry + context'   | new PlantFlow('@startuml\nstart\n:a;\nend\n@enduml', new HandlerRegistry(), new ExecutionContext([k: 'v']))
        'File only'                     | new PlantFlow(new File('src/test/data/puml/sequence.puml'))
        'File + registry'               | new PlantFlow(new File('src/test/data/puml/sequence.puml'), new HandlerRegistry())
        'File + registry + context'     | new PlantFlow(new File('src/test/data/puml/sequence.puml'), new HandlerRegistry(), new ExecutionContext([k: 'v']))
        'from(File) overload'           | PlantFlow.from(new File('src/test/data/puml/sequence.puml'))
        'from(String) overload'         | PlantFlow.from('@startuml\nstart\n:a;\nend\n@enduml')
    }

    def 'should throw IllegalArgumentException when constructor receives null argument: #scenario'() {
        when:
        constructorCall.call()

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedParam)

        where:
        scenario                  | constructorCall                                                                                                                             | expectedParam
        'null PetriNet'           | { new PlantFlow((PetriNet) null) }                                                                                                          | 'petriNet'
        'null HandlerRegistry'    | { new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'), (HandlerRegistry) null) }                         | 'handlerRegistry'
        'null ExecutionContext'   | { new PlantFlow(new ActivityDiagramParser().parse('@startuml\nstart\n:a;\nend\n@enduml'), new HandlerRegistry(), (ExecutionContext) null) } | 'executionContext'
    }

}
