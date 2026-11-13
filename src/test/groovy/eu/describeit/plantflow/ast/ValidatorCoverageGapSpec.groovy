package eu.describeit.plantflow.ast

import eu.describeit.plantflow.DiagramValidator
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Gap tests for DiagramValidator repeat/fork structural rules and for the
 * AST node predicates used to detect composed structures.
 */
class ValidatorCoverageGapSpec extends Specification {

    def 'validate rejects a repeat while flag without a repeat statement'() {
        given: 'a linear diagram that only carries the repeat while flag'
        def diagram = diagram(true, true, [action('a')]).withRepeatWhile(true)

        when:
        DiagramValidator.validate(diagram)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == DiagramValidator.ERR_NO_REPEAT
    }

    def 'validate rejects a repeat flag without a repeat while line'() {
        given: 'a linear diagram that only carries the repeat flag'
        def diagram = diagram(true, true, [action('a')]).withRepeat(true)

        when:
        DiagramValidator.validate(diagram)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == DiagramValidator.ERR_NO_REPEAT_WHILE
    }

    def 'validate rejects detach without a start statement'() {
        given: 'a repeat diagram without start but with detach'
        def diagram = diagram(false, true, [action('a')])
            .withRepeat(true)
            .withRepeatWhile(true)
            .withDetach(true)

        when:
        DiagramValidator.validate(diagram)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == DiagramValidator.ERR_MUST_CONTAIN_START
    }

    def 'validate rejects fork terminator flags without a fork statement'() {
        given: 'a linear diagram that only carries the end merge flag'
        def diagram = diagram(true, true, [action('a')]).withEndMerge(true)

        when:
        DiagramValidator.validate(diagram)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == DiagramValidator.ERR_NO_FORK
    }

    def 'validate rejects a detach flag without a repeat statement'() {
        given: 'a linear diagram that only carries the detach flag'
        def diagram = diagram(true, true, [action('a')]).withDetach(true)

        when:
        DiagramValidator.validate(diagram)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == DiagramValidator.ERR_NO_REPEAT
    }

    def 'validate rejects a repeat node with a null loop condition'() {
        given: 'a diagram containing a repeat node without a condition'
        def diagram = diagram(true, true, [new RepeatNode(null, [action('a')])])

        when:
        DiagramValidator.validate(diagram)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == DiagramValidator.ERR_LOOP_CONDITION_EMPTY
    }

    def 'validate rejects a repeat node with a blank loop condition'() {
        given: 'a diagram containing a repeat node with a whitespace condition'
        def diagram = diagram(true, true, [new RepeatNode('   ', [action('a')])])

        when:
        DiagramValidator.validate(diagram)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == DiagramValidator.ERR_LOOP_CONDITION_EMPTY
    }

    def 'validate rejects a repeat node with an empty body'() {
        given: 'a diagram containing a repeat node without body actions'
        def diagram = diagram(true, true, [new RepeatNode('cond', [])])

        when:
        DiagramValidator.validate(diagram)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == DiagramValidator.ERR_LOOP_BODY_EMPTY
    }

    def 'validate recurses into composed children of a repeat body'() {
        given: 'a repeat node whose body contains a conditional and a plain action'
        def conditional = conditional('g', ['t'] as String[], ['e'] as String[])
        def diagram = diagram(true, true, [new RepeatNode('c', [action('a'), conditional])])

        when:
        DiagramValidator.validate(diagram)

        then: 'validation passes because the nested conditional has both branches'
        noExceptionThrown()
    }

    def 'validate rejects fork nodes with too few or empty branches'() {
        given: 'a single-branch fork node'
        def singleBranch = diagram(true, true, [new ForkNode([[action('a')]])])

        when:
        DiagramValidator.validate(singleBranch)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == DiagramValidator.ERR_MIN_BRANCHES

        when: 'a fork node with an empty branch is validated'
        def emptyBranchFork = new ForkNode([[action('a')], []] as List<List<ActivityNode>>)
        DiagramValidator.validate(diagram(true, true, [emptyBranchFork] as List<ActivityNode>))

        then:
        def e2 = thrown(IllegalArgumentException)
        e2.message == DiagramValidator.ERR_BRANCH_EMPTY
    }

    def 'RepeatNode with a null body uses an empty body'() {
        expect:
        new RepeatNode('cond', null).body.isEmpty()
        new RepeatNode('cond', null).loopCondition == 'cond'
        !new RepeatNode('cond', null).detach
    }

    def 'ActivityDiagram structure predicates detect composed nodes and flags'() {
        expect: 'a fork node alone marks the diagram as fork-structured'
        diagram { d -> d.addNode(new ForkNode([[action('a')], [action('b')]])) }.hasForkStructure()

        and: 'a repeat node alone marks the diagram as repeat-structured'
        diagram { d -> d.addNode(new RepeatNode('c', [action('a')])) }.hasRepeatStructure()

        and: 'flags alone are detected too'
        new ActivityDiagram().withEndFork(true).hasForkStructure()
        new ActivityDiagram().withDetach(true).hasRepeatStructure()

        and: 'a bare diagram has neither structure'
        !new ActivityDiagram().hasForkStructure()
        !new ActivityDiagram().hasRepeatStructure()
    }
}
