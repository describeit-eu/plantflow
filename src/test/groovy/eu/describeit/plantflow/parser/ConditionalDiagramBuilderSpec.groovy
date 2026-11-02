package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ConditionalNode
import spock.lang.Specification

class ConditionalDiagramBuilderSpec extends Specification {

    private ConditionalDiagramBuilder builder = new ConditionalDiagramBuilder()
    private PumlLineClassifier classifier = new PumlLineClassifier()

    private List<ClassifiedLine> classify(String puml) {
        return classifier.classifyAll(puml.readLines())
    }

    def 'should build conditional diagram with if-then-else-endif'() {
        given:
        def lines = classify('''@startuml
start
if (condition) then (yes)
  :then action;
else (no)
  :else action;
endif
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasIf
        diagram.hasElse
        diagram.hasEndif
        diagram.nodes.size() == 1
        diagram.nodes[0] instanceof ConditionalNode

        and:
        def cond = diagram.nodes[0] as ConditionalNode
        cond.guardCondition == 'condition'
        cond.thenActions.size() == 1
        cond.thenActions[0].action == 'then action'
        cond.elseActions.size() == 1
        cond.elseActions[0].action == 'else action'
    }

    def 'should build conditional diagram with multiple actions in branches'() {
        given:
        def lines = classify('''@startuml
start
if (cond) then (yes)
  :action 1;
  :action 2;
else (no)
  :action 3;
  :action 4;
endif
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.nodes.size() == 1
        def cond = diagram.nodes[0] as ConditionalNode
        cond.guardCondition == 'cond'
        cond.thenActions.size() == 2
        cond.thenActions*.action == ['action 1', 'action 2']
        cond.elseActions.size() == 2
        cond.elseActions*.action == ['action 3', 'action 4']
    }

    def 'should handle guard with special characters'() {
        given:
        def lines = classify('''@startuml
start
if (actions['process all'] == true) then (yes)
  :process all;
else (no)
  :skip;
endif
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        def cond = diagram.nodes[0] as ConditionalNode
        cond.guardCondition == "actions['process all'] == true"
    }

    def 'should handle stop keyword instead of end'() {
        given:
        def lines = classify('''@startuml
start
if (cond) then (yes)
  :then;
else (no)
  :else;
endif
stop
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasIf
        diagram.hasElse
        diagram.hasEndif
    }

    def 'should set hasEndif when invalid if with no else and no endif'() {
        given: 'an if-like line that does not match IF_PATTERN, with no else or endif'
        def lines = classify('''@startuml
start
if (invalid format without then clause
  :some action;
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        !diagram.hasIf
        !diagram.hasElse
        diagram.hasEndif
        diagram.nodes.isEmpty()
    }

    def 'should not set hasIf for invalid if format'() {
        given:
        def lines = classify('''@startuml
start
if (invalid format without then
  :some action;
else (no)
  :else action;
endif
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        !diagram.hasIf
        diagram.hasElse
        diagram.hasEndif
        diagram.nodes.isEmpty()
    }

    def 'should handle extra whitespace around keywords'() {
        given:
        def lines = classify('''@startuml
  start
if ( condition ) then ( yes )
  :then action;
    else ( no )
  :else action;
    endif
  end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasIf
        diagram.hasElse
        diagram.hasEndif
        def cond = diagram.nodes[0] as ConditionalNode
        cond.guardCondition == 'condition'
        cond.thenActions[0].action == 'then action'
        cond.elseActions[0].action == 'else action'
    }

    def 'should not include actions outside branches in the diagram'() {
        given: 'actions before if and after endif should not appear in nodes'
        def lines = classify('''@startuml
start
:before if;
if (cond) then (yes)
  :then action;
else (no)
  :else action;
endif
:after endif;
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.nodes.size() == 1
        diagram.nodes[0] instanceof ConditionalNode
        def cond = diagram.nodes[0] as ConditionalNode
        cond.thenActions*.action == ['then action']
        cond.elseActions*.action == ['else action']
    }

    def 'should produce diagram that isConditional returns true for'() {
        given:
        def lines = classify('''@startuml
start
if (cond) then (yes)
  :then;
else (no)
  :else;
endif
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.isConditional()
    }
}
