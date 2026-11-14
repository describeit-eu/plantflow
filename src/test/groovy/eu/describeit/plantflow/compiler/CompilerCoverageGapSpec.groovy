package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.ast.RepeatNode
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.PetriNetStructure
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Gap tests for the net compilers: pre/post action extraction, synchronizing
 * joins, composed nodes inside loops and conditionals, and validation errors.
 */
class CompilerCoverageGapSpec extends Specification {

    def 'DiagramCompiler extracts pre and post actions around a repeat loop'() {
        given: 'a diagram with actions before and after a repeat node'
        def repeat = new RepeatNode('c', [action('in')])
        def diagram = diagram(true, true, [action('pre'), repeat, action('post')])

        when:
        PetriNetStructure net = new DiagramCompiler().compile(diagram)

        then: 'all three actions are compiled into the net'
        net.transitions*.actionKey.containsAll(['pre', 'in', 'post'])
    }

    def 'DiagramCompiler extracts post actions after a fork node'() {
        given: 'a diagram with an action after a fork node'
        def fork = new ForkNode([[action('b1')], [action('b2')]])
        def diagram = diagram(true, true, [action('pre'), fork, action('post')])

        when:
        PetriNetStructure net = new DiagramCompiler().compile(diagram)

        then: 'branch and post-fork actions are compiled'
        net.transitions*.actionKey.containsAll(['b1', 'b2', 'post'])
    }

    def 'DiagramCompiler treats a second repeat node after the first as a non-action post node'() {
        given: 'a diagram with two repeat nodes'
        def diagram = diagram(true, true, [new RepeatNode('c1', [action('a')]), new RepeatNode('c2', [action('b')])])

        when:
        PetriNetStructure net = new DiagramCompiler().compile(diagram)

        then: 'the first loop is compiled with its body action'
        net.transitions*.actionKey.containsAll(['a'])
        net.transitions*.label.any { it == 'T_repeat_1_loop_back' }
    }

    def 'ForkNetCompiler validates branch lists and compiles a synchronizing join'() {
        given:
        def compiler = new ForkNetCompiler()

        when: 'the fork node has no branches'
        compiler.compile(new ForkNode([]))

        then:
        thrown(IllegalArgumentException)

        when: 'the fork node has a null branch list'
        compiler.compile(new ForkNode(null))

        then:
        thrown(IllegalArgumentException)

        when: 'one branch is empty'
        compiler.compile(new ForkNode([[action('a')], []]))

        then:
        thrown(IllegalArgumentException)

        when: 'a synchronizing fork node is compiled'
        PetriNetStructure net = compiler.compile(new ForkNode([[action('a')], [action('b')]], true))

        then: 'a join descriptor with both branch ends in diagram order is registered'
        net.joinDescriptors.size() == 1
        net.joinDescriptors[0].branchEndPlaceIndexes.size() == 2
        net.forkDescriptors.isEmpty()
    }

    def 'ConditionalNetCompiler supports a repeat node inside a branch'() {
        given: 'a conditional whose then branch contains a repeat loop'
        def conditional = new ConditionalNode('g', [new RepeatNode('c', [action('a')])], [action('b')])

        when:
        PetriNetStructure net = new ConditionalNetCompiler().compile(conditional)

        then: 'the net compiles with the loop back and exit transitions'
        net.transitions*.label.any { it == 'T_repeat_1_loop_back' }
        net.transitions*.label.any { it == 'T_repeat_1_exit' }
    }

    def 'ConditionalNetCompiler rejects a fork node inside a branch'() {
        given: 'a conditional whose then branch contains a fork node'
        def conditional = new ConditionalNode('g', [new ForkNode([[action('a')], [action('b')]])], [action('e')])

        when:
        new ConditionalNetCompiler().compile(conditional)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == 'Conditional branch contains a composed node that is not supported here yet'
    }

    def 'RepeatNetCompiler validates compileLoopInto arguments'() {
        given:
        PetriNetBuilder builder = new PetriNetBuilder()
        def p1 = builder.addPlace('p1')
        def p2 = builder.addPlace('p2')
        def compiler = new RepeatNetCompiler()

        when: 'the node is null'
        compiler.compileLoopInto(builder, null, p1, p2)

        then:
        thrown(IllegalArgumentException)

        when: 'the loop body is empty'
        compiler.compileLoopInto(builder, new RepeatNode('c', []), p1, p2)

        then:
        thrown(IllegalArgumentException)
    }

    def 'RepeatNetCompiler compiles nested repeats, conditionals and multiple pre actions'() {
        given: 'a loop body with a nested repeat and a conditional, plus two pre actions'
        def body = [
            action('first'),
            new RepeatNode('inner', [action('nested')]),
            new ConditionalNode('g', [action('t')], [action('e')]),
        ]
        def node = new RepeatNode('c', body)

        when:
        PetriNetStructure net = new RepeatNetCompiler().compile(node, [action('p1'), action('p2')], [])

        then: 'the outer and inner loops and the embedded conditional are all compiled'
        net.transitions*.label.any { it == 'T_repeat_1_loop_back' }
        net.transitions*.label.any { it == 'T_repeat_2_loop_back' }
        net.transitions*.label.any { it.contains('_if_') && it.endsWith('_yes') }
        net.transitions*.actionKey.containsAll(['p1', 'p2', 'first', 'nested', 't', 'e'])
    }

    def 'RepeatNetCompiler rejects unsupported composed nodes in the loop body'() {
        given: 'a loop body containing a fork node'
        def node = new RepeatNode('c', [new ForkNode([[action('a')], [action('b')]])])

        when:
        new RepeatNetCompiler().compile(node)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.startsWith('Unsupported node type in repeat chain: ')
    }

    def 'NetLabeling repeat branch place labels include the branch index only when positive'() {
        expect:
        NetLabeling.repeatThenPlace(2, 3, 0) == 'P_repeat_2_if_3_then'
        NetLabeling.repeatThenPlace(2, 3, 2) == 'P_repeat_2_if_3_then_2'
        NetLabeling.repeatElsePlace(2, 3, 0) == 'P_repeat_2_if_3_else'
        NetLabeling.repeatElsePlace(2, 3, 1) == 'P_repeat_2_if_3_else_1'
    }
}
