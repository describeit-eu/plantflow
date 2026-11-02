package eu.describeit.plantflow.ast

import eu.describeit.plantflow.DiagramValidator
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

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
        'null thenActions'   | null                       | [action('else1')]  | DiagramValidator.ERR_THEN_EMPTY
        'empty thenActions'  | []                         | [action('else1')]  | DiagramValidator.ERR_THEN_EMPTY
        'null elseActions'   | [action('then1')]  | null                       | DiagramValidator.ERR_ELSE_EMPTY
        'empty elseActions'  | [action('then1')]  | []                         | DiagramValidator.ERR_ELSE_EMPTY
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
        DiagramValidator.validateBranchActions(
            [action('a')], 
            [action('b')]
        )

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
        scenario          | hasStart | hasEnd | actions             | expectedMessage
        'missing start'   | false    | true   | ['action1']         | DiagramValidator.ERR_MUST_CONTAIN_START
        'missing end'     | true     | false  | ['action1']         | DiagramValidator.ERR_MUST_CONTAIN_END
        'null actions'    | true     | true   | null                | DiagramValidator.ERR_MUST_CONTAIN_ACTION
        'empty actions'   | true     | true   | []                  | DiagramValidator.ERR_MUST_CONTAIN_ACTION
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
        def diagram = diagram(hasStart, hasEnd, nodes)

        when:
        DiagramValidator.validate(diagram)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario                     | hasStart | hasEnd | nodes                              | expectedMessage
        'missing start'              | false    | true   | [action('a1')]   | DiagramValidator.ERR_MUST_CONTAIN_START
        'missing end'                | true     | false  | [action('a1')]   | DiagramValidator.ERR_MUST_CONTAIN_END
        'empty action transitions'   | true     | true   | []                                   | DiagramValidator.ERR_MUST_CONTAIN_ACTION
    }

    def 'should pass validation for valid linear ActivityDiagram'() {
        given:
        def diagram = diagram(true, true, [action('process order')])

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
        scenario          | hasIf | hasElse | hasEndif | hasStart | hasEnd | nodes                                                                                     | expectedMessage
        'missing if'      | false | true    | true     | true     | true   | [conditional('c', ['a'], ['b'])]                              | DiagramValidator.ERR_NO_IF
        'missing else'    | true  | false   | true     | true     | true   | [conditional('c', ['a'], ['b'])]                              | DiagramValidator.ERR_NO_ELSE
        'missing endif'   | true  | true    | false    | true     | true   | [conditional('c', ['a'], ['b'])]                              | DiagramValidator.ERR_NO_ENDIF
        'missing start'   | true  | true    | true     | false    | true   | [conditional('c', ['a'], ['b'])]                              | DiagramValidator.ERR_MUST_CONTAIN_START
        'missing end'     | true  | true    | true     | true     | false  | [conditional('c', ['a'], ['b'])]                              | DiagramValidator.ERR_MUST_CONTAIN_END
        'empty then block'| true  | true    | true     | true     | true   | [conditional('c', [], ['b'])]                                 | DiagramValidator.ERR_THEN_EMPTY
        'empty else block'| true  | true    | true     | true     | true   | [conditional('c', ['a'], [])]                                 | DiagramValidator.ERR_ELSE_EMPTY
        'empty nodes'     | true  | true    | true     | true     | true   | []                                                                                        | DiagramValidator.ERR_THEN_EMPTY
    }

    def 'should pass validation for valid conditional ActivityDiagram'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withIf(true)
            .withElse(true)
            .withEndif(true)
            .addNode(conditional("actions['check']", ['then action'], ['else action']))

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'should pass validation for valid conditional ActivityDiagram constructed without flags'() {
        given:
        def diagram = diagram(true, true, [
            conditional("actions['check']", ['then action'], ['else action'])
        ])

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'should pass validation for conditional diagram with ActionNode alongside ConditionalNode'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withIf(true)
            .withElse(true)
            .withEndif(true)
            .addNode(action('extra action'))
            .addNode(conditional("actions['check']", ['then action'], ['else action']))

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'ActionNode should store trimmed action and support equals, hashCode, and toString'() {
        given:
        def node1 = action('  do something  ')
        def node2 = action('do something')
        def node3 = action('other')

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
        def thenList = [action('then1')]
        def elseList = [action('else1')]
        def node = conditional('x > 0', thenList, elseList)

        when:
        thenList.add(action('then2'))

        then:
        node.thenActions.size() == 1
        node.thenActions[0].action == 'then1'
        node.elseActions.size() == 1
        node.elseActions[0].action == 'else1'
        node.guardCondition == 'x > 0'

        when:
        node.thenActions.add(action('fail'))

        then:
        thrown(UnsupportedOperationException)

        when:
        node.elseActions.add(action('fail'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'ConditionalNode should safely handle null action lists'() {
        when:
        def node = conditional('guard', null, null)

        then:
        node.thenActions != null
        node.thenActions.isEmpty()
        node.elseActions != null
        node.elseActions.isEmpty()
    }

    def 'ConditionalNode should implement equals, hashCode, and toString'() {
        given:
        def node1 = conditional('c', ['t'], ['e'])
        def node2 = conditional('c', ['t'], ['e'])
        def node3 = conditional('other', ['t'], ['e'])

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
        def actionNode = action('step 1')
        diagram.addNode(actionNode)

        expect:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasIf
        diagram.hasElse
        diagram.hasEndif
        diagram.isConditional()
        diagram.nodes.size() == 1
        diagram.nodes[0] == actionNode

        when:
        diagram.nodes.add(action('step 2'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'ActivityDiagram isConditional should return true if any conditional flag or ConditionalNode exists'() {
        expect:
        !new ActivityDiagram().isConditional()
        new ActivityDiagram().withIf().isConditional()
        new ActivityDiagram().withElse().isConditional()
        new ActivityDiagram().withEndif().isConditional()
        new ActivityDiagram(true, true, [conditional('c', [], [])]).isConditional()
        !diagram(true, true, [action('a')]).isConditional()
    }

    def 'ForkNode should store safe unmodifiable copies of branches'() {
        given:
        def branchList = [action('branch1'), action('branch2')]
        def node = fork(branchList)

        when:
        branchList.add(action('branch3'))

        then:
        node.branches.size() == 2
        node.branches[0].action == 'branch1'
        node.branches[1].action == 'branch2'

        when:
        node.branches.add(action('fail'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'ForkNode should safely handle null branches list'() {
        when:
        def node = fork(null)

        then:
        node.branches != null
        node.branches.isEmpty()
    }

    def 'ForkNode should implement equals, hashCode, and toString'() {
        given:
        def node1 = fork([action('a'), action('b')])
        def node2 = fork([action('a'), action('b')])
        def node3 = fork([action('c')])

        expect:
        node1 == node2
        node1 != node3
        node1.hashCode() == node2.hashCode()
        node1.toString().contains('ForkNode')
    }

    def 'ActivityDiagram should support hasFork and hasEndMerge fields'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withFork(true)
            .withEndMerge(true)

        expect:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasFork
        diagram.hasEndMerge
    }

    def 'ActivityDiagram constructor should support hasFork and hasEndMerge'() {
        given:
        def diagram = new ActivityDiagram(true, true, true, true)

        expect:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasFork
        diagram.hasEndMerge
    }

    def 'ActivityDiagram withFork and withEndMerge should be fluent'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart()
            .withEnd()
            .withFork()
            .withEndMerge()

        expect:
        diagram instanceof ActivityDiagram
        diagram.hasStart
        diagram.hasEnd
        diagram.hasFork
        diagram.hasEndMerge
    }

    def 'ActivityDiagram withFork should allow setting to false'() {
        given:
        def diagram = new ActivityDiagram()
            .withFork(true)
            .withFork(false)

        expect:
        !diagram.hasFork
    }

    def 'ActivityDiagram withEndMerge should allow setting to false'() {
        given:
        def diagram = new ActivityDiagram()
            .withEndMerge(true)
            .withEndMerge(false)

        expect:
        !diagram.hasEndMerge
    }

    def 'ActivityDiagram constructor should handle null nodes list'() {
        given:
        def diagram = new ActivityDiagram(true, true, null)

        expect:
        diagram.hasStart
        diagram.hasEnd
        diagram.nodes.isEmpty()
    }

    def 'ActivityDiagram constructor with hasFork and hasEndMerge should handle null nodes list'() {
        given:
        def diagram = new ActivityDiagram(true, true, true, true, null)

        expect:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasFork
        diagram.hasEndMerge
        diagram.nodes.isEmpty()
    }

    @Unroll
    def 'should throw IllegalArgumentException for invalid fork structure: #scenario'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(hasStart)
            .withEnd(hasEnd)
            .withFork(hasFork)
            .withEndMerge(hasEndMerge)
        nodes.each { diagram.addNode(it) }

        when:
        DiagramValidator.validate(diagram)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario | hasStart | hasEnd | hasFork | hasEndMerge | nodes | expectedMessage
        'fork without end merge' | true | true | true | false | [fork([['a'], ['b']])] | DiagramValidator.ERR_NO_END_MERGE
        'end merge without fork' | true | true | false | true | [fork([['a'], ['b']])] | DiagramValidator.ERR_NO_FORK
        'fork without start' | false | true | true | true | [fork([['a'], ['b']])] | DiagramValidator.ERR_MUST_CONTAIN_START
        'fork without end' | true | false | true | true | [fork([['a'], ['b']])] | DiagramValidator.ERR_MUST_CONTAIN_END
        'fork with single branch' | true | true | true | true | [fork([['a']])] | DiagramValidator.ERR_MIN_BRANCHES
        'fork with empty branches' | true | true | true | true | [fork([])] | DiagramValidator.ERR_BRANCH_EMPTY
         'fork with null branches' | true | true | true | true | [new ForkNode(null)] | DiagramValidator.ERR_BRANCH_EMPTY
         'fork with empty nodes and no fork node' | true | true | true | true | [] | DiagramValidator.ERR_BRANCH_EMPTY
    }

    def 'should pass validation for valid fork ActivityDiagram'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withFork(true)
            .withEndMerge(true)
            .addNode(fork([['a'], ['b']]))

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'should pass validation for valid fork ActivityDiagram constructed without flags'() {
        given:
        def diagram = diagram(true, true, [
            fork([['a'], ['b']])
        ])

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'should pass validation for valid fork with multiple actions per branch'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withFork(true)
            .withEndMerge(true)
            .addNode(fork([
                [action('branch1 action1'), action('branch1 action2')],
                [action('branch2 action1'), action('branch2 action2')]
            ]))

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    @Unroll
    def 'should throw IllegalArgumentException for fork with invalid branches: #scenario'() {
        when:
        DiagramValidator.validateForkBranches(branches)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario | branches | expectedMessage
        'null branches' | null | DiagramValidator.ERR_BRANCH_EMPTY
        'empty branches' | [] | DiagramValidator.ERR_BRANCH_EMPTY
        'single branch' | [['a']] | DiagramValidator.ERR_MIN_BRANCHES
        'branch with null actions' | [['a'], null] | DiagramValidator.ERR_BRANCH_EMPTY
        'branch with empty actions' | [['a'], []] | DiagramValidator.ERR_BRANCH_EMPTY
    }

    @Unroll
    def 'should pass validation for valid fork branches with #scenario'() {
        when:
        DiagramValidator.validateForkBranches(branches)

        then:
        noExceptionThrown()

        where:
        scenario | branches
        'two branches' | [['a'], ['b']]
        'branches with multiple actions' | [['a1', 'a2'], ['b1', 'b2', 'b3']]
    }

    def 'should pass validation for fork diagram with post-merge actions'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withFork(true)
            .withEndMerge(true)
            .addNode(fork([['a'], ['b']]))
            .addNode(action('post merge action'))

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'should pass validation for fork diagram with ActionNode before ForkNode'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withFork(true)
            .withEndMerge(true)
            .addNode(action('before fork'))
            .addNode(fork([['a'], ['b']]))

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    def 'should pass validation for fork with three branches and varying action counts'() {
        given:
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withFork(true)
            .withEndMerge(true)
            .addNode(fork([
                [action('a1')],
                [action('a2')],
                [action('a3'), action('a4')]
            ]))

        when:
        DiagramValidator.validate(diagram)

        then:
        noExceptionThrown()
    }

    @Unroll
    def 'should throw IllegalArgumentException for fork branch with empty action list at position #position'() {
        given:
        def branches = [[action('a')], [action('b')]]
        branches[position] = []
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withFork(true)
            .withEndMerge(true)
            .addNode(new ForkNode(branches as List<List<ActivityNode>>))

        when:
        DiagramValidator.validate(diagram)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == DiagramValidator.ERR_BRANCH_EMPTY

        where:
        position << [0, 1]
    }

    def 'ActivityDiagram addNode should handle null node'() {
        given:
        def diagram = new ActivityDiagram()

        when:
        diagram.addNode(null)

        then:
        noExceptionThrown()
        diagram.nodes.isEmpty()
    }

    def 'ActivityDiagram with forks and endMerge should work with ForkNode'() {
        given:
        def forkNode = fork([
            action('branch1'),
            action('branch2')
        ])
        def diagram = new ActivityDiagram()
            .withStart(true)
            .withEnd(true)
            .withFork(true)
            .withEndMerge(true)
            .addNode(forkNode)

        expect:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasFork
        diagram.hasEndMerge
        diagram.nodes.size() == 1
        diagram.nodes[0] == forkNode
    }
}
