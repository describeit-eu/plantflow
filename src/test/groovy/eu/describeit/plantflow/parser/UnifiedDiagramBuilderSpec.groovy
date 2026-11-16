package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ActivityDiagramParser
import eu.describeit.plantflow.DiagramValidator
import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.ast.RepeatNode
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

class UnifiedDiagramBuilderSpec extends Specification {

    private static ActivityDiagram parseAst(String puml) {
        List<ClassifiedLine> lines = new PumlLineClassifier().classifyAll(puml.readLines())
        return new UnifiedDiagramBuilder().build(lines)
    }

    def 'should parse conditional inside repeat loop body'() {
        given:
        def puml = '''@startuml
start
repeat
  :fetch;
  if (valid?) then (yes)
    :process;
  else (no)
    :log;
  endif
repeat while (more?) is (yes) not (no)
end
@enduml
'''

        when:
        ActivityDiagram diagram = parseAst(puml)

        then:
        def loop = diagram.nodes.find { it instanceof RepeatNode } as RepeatNode
        loop != null
        loop.loopCondition == 'more?'
        loop.body[0] instanceof ActionNode
        def cond = loop.body[1] as ConditionalNode
        cond.guardCondition == 'valid?'
        cond.thenActions*.action == ['process']
        cond.elseActions*.action == ['log']
        DiagramValidator.validate(diagram)
    }

    def 'should parse batchProcessing.puml into a valid AST with conditional inside loop'() {
        when:
        ActivityDiagram diagram = parseAst(new File('src/test/data/puml/batchProcessing.puml').text)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasRepeat
        diagram.hasRepeatWhile

        and:
        def loop = diagram.nodes.find { it instanceof RepeatNode } as RepeatNode
        loop != null
        loop.loopCondition == 'More Records Available?'
        loop.body[0].action == 'Fetch Next Record'
        def cond = loop.body[1] as ConditionalNode
        cond.guardCondition == 'Record is valid?'
        cond.thenActions*.action == ['Process Record Data', 'Calculate Metrics', 'Write Metrics to Buffer']
        cond.elseActions*.action == ['Log Validation Error']

        and:
        diagram.nodes[0].action == 'Initialize Data Batch'
        diagram.nodes.findAll { it instanceof ActionNode }*.action == [
            'Initialize Data Batch', 'Aggregate Final Results', 'Commit to Data Store']

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'should parse repeat loop inside then branch of a conditional'() {
        given:
        def puml = '''@startuml
start
if (run loop?) then (yes)
  repeat
    :iterate;
  repeat while (more?) is (yes) not (no)
else (no)
  :skip;
endif
end
@enduml
'''

        when:
        ActivityDiagram diagram = parseAst(puml)

        then:
        def cond = diagram.nodes.find { it instanceof ConditionalNode } as ConditionalNode
        cond != null
        cond.guardCondition == 'run loop?'
        def loop = cond.thenActions.find { it instanceof RepeatNode } as RepeatNode
        loop != null
        loop.loopCondition == 'more?'
        loop.body*.action == ['iterate']
        cond.elseActions*.action == ['skip']
        DiagramValidator.validate(diagram)
    }

    def 'should parse repeat loop inside else branch of a conditional'() {
        given:
        def puml = '''@startuml
start
if (skip loop?) then (yes)
  :plain;
else (no)
  repeat
    :iterate;
  repeat while (more?) is (yes) not (no)
endif
end
@enduml
'''

        when:
        ActivityDiagram diagram = parseAst(puml)

        then:
        def cond = diagram.nodes.find { it instanceof ConditionalNode } as ConditionalNode
        cond.thenActions*.action == ['plain']
        def loop = cond.elseActions.find { it instanceof RepeatNode } as RepeatNode
        loop != null
        loop.loopCondition == 'more?'
        loop.body*.action == ['iterate']
        DiagramValidator.validate(diagram)
    }

    def 'should parse nested loops with a conditional inside the inner loop at any nesting level'() {
        given:
        def puml = '''@startuml
start
repeat
  :outer;
  repeat
    :inner;
    if (check?) then (yes)
      :inner then;
    else (no)
      :inner else;
    endif
  repeat while (inner done?) is (yes) not (no)
repeat while (outer done?) is (yes) not (no)
end
@enduml
'''

        when:
        ActivityDiagram diagram = parseAst(puml)

        then:
        def outer = diagram.nodes.find { it instanceof RepeatNode } as RepeatNode
        outer.loopCondition == 'outer done?'
        outer.body[0].action == 'outer'
        def inner = outer.body[1] as RepeatNode
        inner.loopCondition == 'inner done?'
        inner.body[0].action == 'inner'
        def cond = inner.body[1] as ConditionalNode
        cond.thenActions*.action == ['inner then']
        cond.elseActions*.action == ['inner else']
        DiagramValidator.validate(diagram)
    }

    def 'should parse nested conditional inside conditional inside repeat'() {
        given:
        def puml = '''@startuml
start
repeat
  if (a?) then (yes)
    if (b?) then (yes)
      :deep;
    else (no)
      :shallow;
    endif
  else (no)
    :outer else;
  endif
repeat while (more?) is (yes) not (no)
end
@enduml
'''

        when:
        ActivityDiagram diagram = parseAst(puml)

        then:
        def loop = diagram.nodes.find { it instanceof RepeatNode } as RepeatNode
        def outerCond = loop.body[0] as ConditionalNode
        outerCond.guardCondition == 'a?'
        def innerCond = outerCond.thenActions[0] as ConditionalNode
        innerCond.guardCondition == 'b?'
        innerCond.thenActions*.action == ['deep']
        innerCond.elseActions*.action == ['shallow']
        outerCond.elseActions*.action == ['outer else']
        DiagramValidator.validate(diagram)
    }

    def 'public parser should accept mixed repeat and conditional diagrams without mix rejection'() {
        when:
        def net = new ActivityDiagramParser().parse(new File('src/test/data/puml/batchProcessing.puml'))

        then:
        net != null
        net.startPlace != null
        net.endPlace != null

        and: 'a repeat loop inside a conditional branch also parses through the public parser'
        def loopInBranch = new ActivityDiagramParser().parse(loopInBranchPuml())
        loopInBranch != null
        loopInBranch.transitions.size() > 0
    }

    def 'public parser should accept fork mixed with repeat and conditional without mix rejection'() {
        when:
        def net = parse('@startuml\nstart\nrepeat\n  :a;\nrepeat while (x?) is (yes) not (no)\nfork\n  :b;\nfork again\n  :c;\nend merge\nend\n@enduml')

        then:
        net != null
        net.transitions.find { it.actionKey == 'a' } != null
        net.transitions.find { it.actionKey == 'b' } != null
        net.transitions.find { it.actionKey == 'c' } != null

        and: 'a fork inside a conditional branch also parses through the public parser'
        def forkInIf = parse('''@startuml
start
if (g?) then (yes)
  fork
    :b;
  fork again
    :c;
  end fork
else (no)
  :d;
endif
end
@enduml
''')
        forkInIf.transitions.find { it.actionKey == 'b' } != null
        forkInIf.transitions.find { it.actionKey == 'c' } != null
        forkInIf.transitions.find { it.actionKey == 'd' } != null
    }

    def 'should parse a nested fork inside a fork branch as a real nested ForkNode'() {
        given:
        def puml = '''@startuml
start
fork
  :outer 1;
  fork
    :inner 1;
  fork again
    :inner 2;
  end fork
  :outer 1 tail;
fork again
  :outer 2;
end fork
end
@enduml
'''

        when:
        ActivityDiagram diagram = parseAst(puml)

        then:
        diagram.hasFork
        diagram.hasEndFork
        diagram.nodes.size() == 1
        def outer = diagram.nodes[0] as ForkNode
        outer.synchronizingJoin
        outer.branches.size() == 2
        outer.branches[1]*.action == ['outer 2']

        and: 'the first branch keeps the action before the inner fork, the nested ForkNode, and the action after the inner join'
        def branch = outer.branches[0]
        branch.size() == 3
        branch[0].action == 'outer 1'
        def inner = branch[1] as ForkNode
        inner.synchronizingJoin
        inner.branches*.action == [['inner 1'], ['inner 2']]
        branch[2].action == 'outer 1 tail'
        DiagramValidator.validate(diagram)
    }

    def 'should parse a fork inside a conditional branch and a repeat loop body'() {
        given:
        def puml = '''@startuml
start
if (g?) then (yes)
  fork
    :fb;
  fork again
    :fc;
  end merge
else (no)
  repeat
    :lb;
  repeat while (more?) is (yes) not (no)
endif
end
@enduml
'''

        when:
        ActivityDiagram diagram = parseAst(puml)

        then:
        def cond = diagram.nodes[0] as ConditionalNode
        def fork = cond.thenActions[0] as ForkNode
        !fork.synchronizingJoin
        fork.branches*.action == [['fb'], ['fc']]
        def loop = cond.elseActions[0] as RepeatNode
        loop.body*.action == ['lb']
        DiagramValidator.validate(diagram)
    }

    def 'should parse a conditional and a repeat loop inside a fork branch'() {
        given:
        def puml = '''@startuml
start
fork
  if (g?) then (yes)
    :t;
  else (no)
    :e;
  endif
  repeat
    :l;
  repeat while (more?) is (yes) not (no)
fork again
  :b;
end fork
end
@enduml
'''

        when:
        ActivityDiagram diagram = parseAst(puml)

        then:
        def fork = diagram.nodes[0] as ForkNode
        def branch = fork.branches[0]
        branch.size() == 2
        def cond = branch[0] as ConditionalNode
        cond.thenActions*.action == ['t']
        cond.elseActions*.action == ['e']
        def loop = branch[1] as RepeatNode
        loop.body*.action == ['l']
        DiagramValidator.validate(diagram)
    }

    @Unroll
    def 'should reject invalid fork nesting: #scenario'() {
        when:
        parseAst(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedMessage)

        where:
        scenario                          | puml                                                                                                          | expectedMessage
        'fork again without active fork'  | '@startuml\nstart\nfork again\n  :a;\nend\n@enduml'                                                        | 'fork again without active fork context'
        'end merge without active fork'   | '@startuml\nstart\nend merge\nend\n@enduml'                                                                   | 'end merge without active fork context'
        'end fork without active fork'    | '@startuml\nstart\nend fork\nend\n@enduml'                                                                    | 'end fork without active fork context'
        'unclosed fork'                   | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nend\n@enduml'                                              | 'Unclosed fork'
        'empty fork branch'               | '@startuml\nstart\nfork\n  :a;\nfork again\nend fork\nend\n@enduml'                                          | 'Fork branch must contain at least one action'
        'fork again inside a conditional' | '@startuml\nstart\nif (g?) then (yes)\n  fork again\n  :a;\nelse (no)\n  :b;\nendif\nend\n@enduml'          | 'fork again without active fork context'
        'repeat while closing a fork'     | '@startuml\nstart\nfork\n  :a;\n  repeat while (c?) is (yes) not (no)\nfork again\n  :b;\nend fork\nend\n@enduml' | 'repeat while without active repeat'
        'else inside a fork branch'       | '@startuml\nstart\nfork\n  else (no)\n  :a;\nfork again\n  :b;\nend fork\nend\n@enduml'                        | 'else without active if'
        'endif inside a fork branch'      | '@startuml\nstart\nfork\n  endif\n  :a;\nfork again\n  :b;\nend fork\nend\n@enduml'                           | 'endif without active if'
    }
}
