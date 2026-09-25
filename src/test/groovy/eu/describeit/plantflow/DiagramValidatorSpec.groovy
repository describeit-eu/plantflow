package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import spock.lang.Specification
import spock.lang.Unroll

class DiagramValidatorSpec extends Specification {

    @Unroll
    def 'should throw IllegalArgumentException for invalid content: "#scenario"'() {
        when:
        DiagramValidator.validateContent(content)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == DiagramValidator.ERR_CONTENT_EMPTY

        where:
        scenario          | content
        'null content'    | null
        'empty content'   | ''
        'spaces only'     | '   '
        'whitespace only' | '\t\n\r '
    }

    def 'should pass validation for non-empty content'() {
        when:
        DiagramValidator.validateContent('start\n:action;\nend')

        then:
        noExceptionThrown()
    }

    def 'should throw IllegalArgumentException when file is null'() {
        when:
        DiagramValidator.validateFile(null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == DiagramValidator.ERR_FILE_NULL
    }

    def 'should pass validation for non-null file'() {
        given:
        def file = new File('dummy.puml')

        when:
        DiagramValidator.validateFile(file)

        then:
        noExceptionThrown()
    }

    @Unroll
    def 'should throw IllegalArgumentException for invalid if-then-else structure: #scenario'() {
        when:
        DiagramValidator.validateIfThenElseStructure(hasIf, hasElse, hasEndif, hasStart, hasEnd)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario          | hasIf | hasElse | hasEndif | hasStart | hasEnd | expectedMessage
        'missing if'      | false | true    | true     | true     | true   | DiagramValidator.ERR_NO_IF
        'missing else'    | true  | false   | true     | true     | true   | DiagramValidator.ERR_NO_ELSE
        'missing endif'   | true  | true    | false    | true     | true   | DiagramValidator.ERR_NO_ENDIF
        'missing start'   | true  | true    | true     | false    | true   | DiagramValidator.ERR_MUST_CONTAIN_START
        'missing end'     | true  | true    | true     | true     | false  | DiagramValidator.ERR_MUST_CONTAIN_END
    }

    def 'should pass validateIfThenElseStructure when all conditions are true'() {
        when:
        DiagramValidator.validateIfThenElseStructure(true, true, true, true, true)

        then:
        noExceptionThrown()
    }

    @Unroll
    def 'should throw IllegalArgumentException for invalid branch actions with ActionNodes: #scenario'() {
        when:
        DiagramValidator.validateBranchActions(thenActions, elseActions)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario             | thenActions                | elseActions                | expectedMessage
        'null thenActions'   | null                       | [new ActionNode('else1')]  | DiagramValidator.ERR_THEN_EMPTY
        'empty thenActions'  | []                         | [new ActionNode('else1')]  | DiagramValidator.ERR_THEN_EMPTY
        'null elseActions'   | [new ActionNode('then1')]  | null                       | DiagramValidator.ERR_ELSE_EMPTY
        'empty elseActions'  | [new ActionNode('then1')]  | []                         | DiagramValidator.ERR_ELSE_EMPTY
    }

    @Unroll
    def 'should throw IllegalArgumentException for invalid branch actions with String lists: #scenario'() {
        when:
        DiagramValidator.validateBranchActions(thenActions, elseActions)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario             | thenActions   | elseActions   | expectedMessage
        'null thenActions'   | null          | ['else1']     | DiagramValidator.ERR_THEN_EMPTY
        'empty thenActions'  | []            | ['else1']     | DiagramValidator.ERR_THEN_EMPTY
        'null elseActions'   | ['then1']     | null          | DiagramValidator.ERR_ELSE_EMPTY
        'empty elseActions'  | ['then1']     | []            | DiagramValidator.ERR_ELSE_EMPTY
    }

    def 'should pass validateBranchActions when both branches have actions'() {
        when:
        DiagramValidator.validateBranchActions([new ActionNode('a')], [new ActionNode('b')])

        then:
        noExceptionThrown()
    }

    @Unroll
    def 'should throw IllegalArgumentException for invalid linear diagram structure: #scenario'() {
        when:
        DiagramValidator.validateDiagramStructure(hasStart, hasEnd, actions)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario          | hasStart | hasEnd | actions     | expectedMessage
        'missing start'   | false    | true   | ['action1'] | DiagramValidator.ERR_MUST_CONTAIN_START
        'missing end'     | true     | false  | ['action1'] | DiagramValidator.ERR_MUST_CONTAIN_END
        'null actions'    | true     | true   | null        | DiagramValidator.ERR_MUST_CONTAIN_ACTION
        'empty actions'   | true     | true   | []          | DiagramValidator.ERR_MUST_CONTAIN_ACTION
    }

    def 'should pass validateDiagramStructure when start, end, and actions are valid'() {
        when:
        DiagramValidator.validateDiagramStructure(true, true, ['action1'])

        then:
        noExceptionThrown()
    }

    def 'should throw IllegalArgumentException when ActivityDiagram is null'() {
        when:
        DiagramValidator.validate(null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == DiagramValidator.ERR_DIAGRAM_NULL
    }

    @Unroll
    def 'should throw IllegalArgumentException when validating linear diagram: #scenario'() {
        given:
        def diagram = new ActivityDiagram(hasStart, hasEnd, nodes)

        when:
        DiagramValidator.validate(diagram)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario                     | hasStart | hasEnd | nodes                    | expectedMessage
        'missing start'              | false    | true   | [new ActionNode('a1')]   | DiagramValidator.ERR_MUST_CONTAIN_START
        'missing end'                | true     | false  | [new ActionNode('a1')]   | DiagramValidator.ERR_MUST_CONTAIN_END
        'empty action transitions'   | true     | true   | []                       | DiagramValidator.ERR_MUST_CONTAIN_ACTION
    }

    def 'should pass validation for valid linear ActivityDiagram'() {
        given:
        def diagram = new ActivityDiagram(true, true, [new ActionNode('process order')])

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    @Unroll
    def 'should throw IllegalArgumentException when validating conditional diagram flags: #scenario'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(hasStart)
            .withEnd(hasEnd)
            .withIf(hasIf)
            .withElse(hasElse)
            .withEndif(hasEndif)
        nodes.each { diagram.addNode(it) }

        when:
        DiagramValidator.validate(diagram)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario          | hasIf | hasElse | hasEndif | hasStart | hasEnd | nodes                                                                             | expectedMessage
        'missing if'      | false | true    | true     | true     | true   | [new ConditionalNode('c', [new ActionNode('a')], [new ActionNode('b')])]          | DiagramValidator.ERR_NO_IF
        'missing else'    | true  | false   | true     | true     | true   | [new ConditionalNode('c', [new ActionNode('a')], [new ActionNode('b')])]          | DiagramValidator.ERR_NO_ELSE
        'missing endif'   | true  | true    | false    | true     | true   | [new ConditionalNode('c', [new ActionNode('a')], [new ActionNode('b')])]          | DiagramValidator.ERR_NO_ENDIF
        'missing start'   | true  | true    | true     | false    | true   | [new ConditionalNode('c', [new ActionNode('a')], [new ActionNode('b')])]          | DiagramValidator.ERR_MUST_CONTAIN_START
        'missing end'     | true  | true    | true     | true     | false  | [new ConditionalNode('c', [new ActionNode('a')], [new ActionNode('b')])]          | DiagramValidator.ERR_MUST_CONTAIN_END
        'empty then block'| true  | true    | true     | true     | true   | [new ConditionalNode('c', [], [new ActionNode('b')])]                             | DiagramValidator.ERR_THEN_EMPTY
        'empty else block'| true  | true    | true     | true     | true   | [new ConditionalNode('c', [new ActionNode('a')], [])]                             | DiagramValidator.ERR_ELSE_EMPTY
        'empty nodes'     | true  | true    | true     | true     | true   | []                                                                                | DiagramValidator.ERR_THEN_EMPTY
    }

    def 'should pass validation for valid conditional ActivityDiagram'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withIf(true)
            .withElse(true)
            .withEndif(true)
            .addNode(new ConditionalNode("actions['check']", [new ActionNode('then action')], [new ActionNode('else action')]))

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'should pass validation for valid conditional ActivityDiagram constructed without flags'() {
        given:
        def diagram = new ActivityDiagram(true, true, [
            new ConditionalNode("actions['check']", [new ActionNode('then action')], [new ActionNode('else action')])
        ])

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'ActionNode should store trimmed action and support equals, hashCode, and toString'() {
        given:
        def node1 = new ActionNode('  do something  ')
        def node2 = new ActionNode('do something')
        def node3 = new ActionNode('other')

        expect:
        node1.action == 'do something'
        node1.actionKey == 'do something'
        node1 == node2
        node1 != node3
        node1.hashCode() == node2.hashCode()
        node1.toString().contains('do something')
    }

    @Unroll
    def 'ActionNode should throw IllegalArgumentException for invalid action: "#invalidAction"'() {
        when:
        new ActionNode(invalidAction)

        then:
        thrown(IllegalArgumentException)

        where:
        invalidAction << [null, '', '   ', '\t\n']
    }

    def 'ConditionalNode should store safe unmodifiable copies of actions'() {
        given:
        def thenList = [new ActionNode('then1')]
        def elseList = [new ActionNode('else1')]
        def node = new ConditionalNode('x > 0', thenList, elseList)

        when:
        thenList.add(new ActionNode('then2'))

        then:
        node.thenActions.size() == 1
        node.thenActions[0].action == 'then1'
        node.elseActions.size() == 1
        node.elseActions[0].action == 'else1'
        node.guardCondition == 'x > 0'

        when:
        node.thenActions.add(new ActionNode('fail'))

        then:
        thrown(UnsupportedOperationException)

        when:
        node.elseActions.add(new ActionNode('fail'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'ConditionalNode should safely handle null action lists'() {
        when:
        def node = new ConditionalNode('guard', null, null)

        then:
        node.thenActions != null
        node.thenActions.isEmpty()
        node.elseActions != null
        node.elseActions.isEmpty()
    }

    def 'ConditionalNode should implement equals, hashCode, and toString'() {
        given:
        def node1 = new ConditionalNode('c', [new ActionNode('t')], [new ActionNode('e')])
        def node2 = new ConditionalNode('c', [new ActionNode('t')], [new ActionNode('e')])
        def node3 = new ConditionalNode('other', [new ActionNode('t')], [new ActionNode('e')])

        expect:
        node1 == node2
        node1 != node3
        node1.hashCode() == node2.hashCode()
        node1.toString().contains('ConditionalNode')
    }

    def 'ActivityDiagram should store unmodifiable node list and support fluent building'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withIf(true)
            .withElse(true)
            .withEndif(true)
        def action = new ActionNode('step 1')
        diagram.addNode(action)

        expect:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasIf
        diagram.hasElse
        diagram.hasEndif
        diagram.isConditional()
        diagram.nodes.size() == 1
        diagram.nodes[0] == action

        when:
        diagram.nodes.add(new ActionNode('step 2'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'ActivityDiagram isConditional should return true if any conditional flag or ConditionalNode exists'() {
        expect:
        !new ActivityDiagram().isConditional()
        new ActivityDiagram().withIf().isConditional()
        new ActivityDiagram().withElse().isConditional()
        new ActivityDiagram().withEndif().isConditional()
        new ActivityDiagram(true, true, [new ConditionalNode('c', [], [])]).isConditional()
        !new ActivityDiagram(true, true, [new ActionNode('a')]).isConditional()
    }
}
