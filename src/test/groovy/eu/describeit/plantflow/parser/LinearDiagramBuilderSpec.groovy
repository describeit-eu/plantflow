package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import spock.lang.Specification

class LinearDiagramBuilderSpec extends Specification {

    private LinearDiagramBuilder builder = new LinearDiagramBuilder()
    private PumlLineClassifier classifier = new PumlLineClassifier()

    private List<ClassifiedLine> classify(String puml) {
        return classifier.classifyAll(puml.readLines())
    }

    def 'should build linear diagram with start, actions, and end'() {
        given:
        def lines = classify('''@startuml
start
:action 1;
:action 2;
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasStart
        diagram.hasEnd
        !diagram.hasIf
        !diagram.hasElse
        !diagram.hasEndif
        !diagram.hasFork
        !diagram.hasEndMerge
        diagram.nodes.size() == 2
        diagram.nodes.every { it instanceof ActionNode }
        (diagram.nodes[0] as ActionNode).action == 'action 1'
        (diagram.nodes[1] as ActionNode).action == 'action 2'
    }

    def 'should build diagram with stop keyword as end'() {
        given:
        def lines = classify('''@startuml
start
:action;
stop
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.nodes.size() == 1
    }

    def 'should ignore comments, blank lines, and unrecognized lines'() {
        given:
        def lines = classify('''@startuml
'comment
start

:action 1;
random text
:action 2;
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.nodes.size() == 2
        (diagram.nodes[0] as ActionNode).action == 'action 1'
        (diagram.nodes[1] as ActionNode).action == 'action 2'
    }

    def 'should build diagram with single action'() {
        given:
        def lines = classify('''@startuml
start
:only action;
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.nodes.size() == 1
        (diagram.nodes[0] as ActionNode).action == 'only action'
    }

    def 'should build diagram with many actions preserving order'() {
        given:
        def puml = '@startuml\nstart\n' + (1..5).collect { ":step $it;" }.join('\n') + '\nend\n@enduml'
        def lines = classify(puml)

        when:
        def diagram = builder.build(lines)

        then:
        diagram.nodes.size() == 5
        (0..4).each { idx ->
            assert (diagram.nodes[idx] as ActionNode).action == "step ${idx + 1}"
        }
    }

    def 'should not set conditional or fork flags'() {
        given:
        def lines = classify('''@startuml
start
:action;
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        !diagram.isConditional()
        !diagram.hasForkStructure()
    }
}
