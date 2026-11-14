package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ActivityDiagramParser
import eu.describeit.plantflow.DiagramValidator
import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.RepeatNode
import spock.lang.Specification

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

    def 'public parser should still reject fork mixed with repeat'() {
        when:
        parse('@startuml\nstart\nrepeat\n  :a;\nrepeat while (x?) is (yes) not (no)\nfork\n  :b;\nfork again\n  :c;\nend merge\nend\n@enduml')

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('mixes fork and repeat')
    }
}
