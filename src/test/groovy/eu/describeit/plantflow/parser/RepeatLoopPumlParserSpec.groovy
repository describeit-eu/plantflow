package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.RepeatNode
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class RepeatLoopPumlParserSpec extends Specification {

    private static ActivityDiagram parseAst(String puml) {
        List<ClassifiedLine> lines = new PumlLineClassifier().classifyAll(puml.readLines())
        return new RepeatDiagramBuilder().build(lines)
    }

    // ==================== Classifier ====================

    def 'should classify repeat line as REPEAT'() {
        expect:
        new PumlLineClassifier().classify('repeat').type == LineType.REPEAT
    }

    def 'should classify repeat while line as REPEAT_WHILE with guard and yes label'() {
        when:
        def line = new PumlLineClassifier().classify('repeat while (more data?) is (yes) not (no)')

        then:
        line.type == LineType.REPEAT_WHILE
        line.guard == 'more data?'
        line.branchLabel == 'yes'
    }

    def 'should classify repeat while without not label as REPEAT_WHILE'() {
        when:
        def line = new PumlLineClassifier().classify('repeat while (true) is (true)')

        then:
        line.type == LineType.REPEAT_WHILE
        line.guard == 'true'
        line.branchLabel == 'true'
    }

    def 'should classify hidden detach lines as DETACH'() {
        expect:
        new PumlLineClassifier().classify(line).type == LineType.DETACH

        where:
        line << ['-[hidden]-> detach', '-[hidden]->', 'detach']
    }

    def 'should classify malformed repeat lines as MALFORMED'() {
        expect:
        new PumlLineClassifier().classify(line).type == LineType.MALFORMED

        where:
        line << [
            'repeat while (broken',
            'repeat while more data?',
            'repeat now',
            'REPEAT',
            '-[hidden]-> foo',
        ]
    }

    // ==================== AST building ====================

    def 'should parse repeatWhile.puml into AST with a Repeat node'() {
        when:
        ActivityDiagram diagram = parseAst(new File('src/test/data/puml/repeatWhile.puml').text)

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasRepeat
        diagram.hasRepeatWhile
        !diagram.hasDetach

        and:
        def repeat = diagram.nodes.find { it instanceof RepeatNode } as RepeatNode
        repeat != null
        repeat.loopCondition == 'more data?'
        repeat.body*.action == ['read data', 'generate diagrams']
        !repeat.detach
    }

    def 'should parse repeatInfinite.puml into AST with detached infinite loop'() {
        when:
        ActivityDiagram diagram = parseAst(new File('src/test/data/puml/repeatInfinite.puml').text)

        then:
        def repeat = diagram.nodes.find { it instanceof RepeatNode } as RepeatNode
        repeat != null
        repeat.loopCondition == 'true'
        repeat.body*.action == ['read data', 'generate diagrams']
        repeat.detach
        diagram.hasDetach
    }

    def 'should parse actions after the loop as post-loop nodes'() {
        when:
        ActivityDiagram diagram = parseAst(repeatPuml('more data?', ['read data'], ['finalize']))

        then:
        diagram.nodes.size() == 2
        diagram.nodes[0] instanceof RepeatNode
        diagram.nodes[1].action == 'finalize'
    }

    def 'should parse nested repeat loops with inner loop inside outer body'() {
        given:
        def puml = '''@startuml
start
repeat
  :outer start;
  repeat
    :inner action;
  repeat while (inner done?) is (yes) not (no)
  :outer end;
repeat while (outer done?) is (yes) not (no)
end
@enduml
'''

        when:
        ActivityDiagram diagram = parseAst(puml)

        then:
        def outer = diagram.nodes.find { it instanceof RepeatNode } as RepeatNode
        outer.loopCondition == 'outer done?'
        outer.body.findAll { it instanceof eu.describeit.plantflow.ast.ActionNode }*.action == ['outer start', 'outer end']
        def inner = outer.body.find { it instanceof RepeatNode } as RepeatNode
        inner != null
        inner.loopCondition == 'inner done?'
        inner.body*.action == ['inner action']
    }

    // ==================== Malformed rejection (public parse) ====================

    def 'should reject malformed repeat lines through the public parser'() {
        when:
        parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('Malformed')

        where:
        puml << [
            '@startuml\nstart\nrepeat\n  :a;\nrepeat while (broken\nend\n@enduml',
            '@startuml\nstart\nrepeat\n  :a;\nrepeat while (x?) is\nend\n@enduml',
            '@startuml\nstart\nrepeat\n  :a;\nrepeat while (x?) is (yes) not (no)\n-[hidden]-> foo\nend\n@enduml',
        ]
    }

    def 'should reject detach outside a repeat loop through the public parser'() {
        given:
        def puml = '@startuml\nstart\n:action;\ndetach\nend\n@enduml'

        when:
        parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('detach')
    }

    def 'should reject unterminated repeat through the public parser'() {
        given:
        def puml = '@startuml\nstart\nrepeat\n  :a;\nend\n@enduml'

        when:
        parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('repeat while')
    }

    def 'should reject repeat while without repeat through the public parser'() {
        given:
        def puml = '@startuml\nstart\n:action;\nrepeat while (x?) is (yes) not (no)\nend\n@enduml'

        when:
        parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('repeat while without active repeat')
    }

    def 'should reject empty loop body through the public parser'() {
        given:
        def puml = '@startuml\nstart\nrepeat\nrepeat while (x?) is (yes) not (no)\nend\n@enduml'

        when:
        parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('Repeat loop body')
    }

    def 'should reject detach not directly after a repeat while loop'() {
        given:
        def puml = '@startuml\nstart\nrepeat\n  :a;\nrepeat while (x?) is (yes) not (no)\n:other;\n-[hidden]-> detach\nend\n@enduml'

        when:
        parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('detach')
    }

    // ==================== Existing diagrams unchanged ====================

    def 'should still parse linear, conditional and fork diagrams through the public parser'() {
        expect:
        parse(linearPuml('a', 'b')).transitions.size() == 2
        parse(conditionalPuml('x > 0', 'yes action', 'no action')).transitions.size() == 6
        parse(forkPuml(2)).transitions.size() > 0
    }
}
