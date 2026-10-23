package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ForkNode
import spock.lang.Specification

class ForkDiagramBuilderSpec extends Specification {

    private ForkDiagramBuilder builder = new ForkDiagramBuilder()
    private PumlLineClassifier classifier = new PumlLineClassifier()

    private List<ClassifiedLine> classify(String puml) {
        return classifier.classifyAll(puml.readLines())
    }

    def 'should build fork diagram with two branches'() {
        given:
        def lines = classify('''@startuml
start
fork
  :branch a;
fork again
  :branch b;
end merge
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasFork
        diagram.hasEndMerge
        diagram.nodes.size() == 1
        diagram.nodes[0] instanceof ForkNode

        and:
        def fork = diagram.nodes[0] as ForkNode
        fork.branches.size() == 2
        fork.branches[0]*.action == ['branch a']
        fork.branches[1]*.action == ['branch b']
    }

    def 'should build fork diagram with three branches'() {
        given:
        def lines = classify('''@startuml
start
fork
  :action 1;
fork again
  :action 2;
fork again
  :action 3;
  :action 4;
end merge
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        def fork = diagram.nodes[0] as ForkNode
        fork.branches.size() == 3
        fork.branches[0]*.action == ['action 1']
        fork.branches[1]*.action == ['action 2']
        fork.branches[2]*.action == ['action 3', 'action 4']
    }

    def 'should build fork diagram with post-merge actions'() {
        given:
        def lines = classify('''@startuml
start
fork
  :branch a;
fork again
  :branch b;
end merge
:post merge 1;
:post merge 2;
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.nodes.size() == 3
        diagram.nodes[0] instanceof ForkNode
        diagram.nodes[1] instanceof ActionNode
        diagram.nodes[2] instanceof ActionNode
        (diagram.nodes[1] as ActionNode).action == 'post merge 1'
        (diagram.nodes[2] as ActionNode).action == 'post merge 2'
    }

    def 'should build fork diagram with nested fork structures'() {
        given:
        def lines = classify('''@startuml
start
fork
  :outer 1;
  fork
    :inner 1;
  fork again
    :inner 2;
  end merge
fork again
  :outer 2;
end merge
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasFork
        diagram.hasEndMerge
        diagram.nodes.size() == 1
        def fork = diagram.nodes[0] as ForkNode
        fork.branches.size() == 4
        fork.branches[0]*.action == ['outer 1']
        fork.branches[1]*.action == ['inner 1']
        fork.branches[2]*.action == ['inner 2']
        fork.branches[3]*.action == ['outer 2']
    }

    def 'should handle stop keyword instead of end'() {
        given:
        def lines = classify('''@startuml
start
fork
  :a;
fork again
  :b;
end merge
stop
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasFork
        diagram.hasEndMerge
    }

    def 'should throw for fork again without active fork context'() {
        given:
        def lines = classify('''@startuml
start
fork again
  :action 1;
end merge
end
@enduml''')

        when:
        builder.build(lines)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('fork again')
    }

    def 'should throw for end merge without active fork context'() {
        given:
        def lines = classify('''@startuml
start
end merge
end
@enduml''')

        when:
        builder.build(lines)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('end merge without active fork context')
    }

    def 'should throw for unclosed fork'() {
        given:
        def lines = classify('''@startuml
start
fork
  :action 1;
end
@enduml''')

        when:
        builder.build(lines)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('Unclosed fork')
    }

    def 'should throw for nested fork without closing end merge'() {
        given:
        def lines = classify('''@startuml
start
fork
  :action 1;
fork again
  :action 2;
end
@enduml''')

        when:
        builder.build(lines)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('Unclosed fork')
    }

    def 'should handle multiple actions in single branch'() {
        given:
        def lines = classify('''@startuml
start
fork
  :action 1;
  :action 2;
  :action 3;
fork again
  :action 4;
end merge
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        def fork = diagram.nodes[0] as ForkNode
        fork.branches[0]*.action == ['action 1', 'action 2', 'action 3']
        fork.branches[1]*.action == ['action 4']
    }

    def 'should ignore actions before fork keyword'() {
        given:
        def lines = classify('''@startuml
start
:before fork;
fork
  :branch a;
fork again
  :branch b;
end merge
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.nodes.size() == 1
        diagram.nodes[0] instanceof ForkNode
        def fork = diagram.nodes[0] as ForkNode
        fork.branches[0]*.action == ['branch a']
        fork.branches[1]*.action == ['branch b']
    }

    def 'should handle deeply nested fork structures'() {
        given:
        def lines = classify('''@startuml
start
fork
  :outer 1;
  fork
    :inner 1;
    fork
      :deep 1;
    fork again
      :deep 2;
    end merge
  fork again
    :inner 2;
  end merge
fork again
  :outer 2;
end merge
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.nodes.size() == 1
        def fork = diagram.nodes[0] as ForkNode
        fork.branches.size() == 6
        fork.branches[0]*.action == ['outer 1']
        fork.branches[1]*.action == ['inner 1']
        fork.branches[2]*.action == ['deep 1']
        fork.branches[3]*.action == ['deep 2']
        fork.branches[4]*.action == ['inner 2']
        fork.branches[5]*.action == ['outer 2']
    }

    def 'should produce diagram that hasForkStructure returns true for'() {
        given:
        def lines = classify('''@startuml
start
fork
  :a;
fork again
  :b;
end merge
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        diagram.hasForkStructure()
    }

    def 'should ignore comments and blank lines in fork'() {
        given:
        def lines = classify('''@startuml
start
fork
  'comment
  :action a;

fork again
  :action b;
end merge
'comment after merge
end
@enduml''')

        when:
        def diagram = builder.build(lines)

        then:
        def fork = diagram.nodes[0] as ForkNode
        fork.branches[0]*.action == ['action a']
        fork.branches[1]*.action == ['action b']
    }
}
