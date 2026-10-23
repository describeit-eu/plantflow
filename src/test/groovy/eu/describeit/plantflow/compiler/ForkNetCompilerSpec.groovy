package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ForkNode
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

class ForkNetCompilerSpec extends Specification {

    def compiler = new ForkNetCompiler()

    def 'should compile fork with two branches and single actions'() {
        given:
        def forkNode = new ForkNode([[action('a1')], [action('a2')]] as List<List<ActivityNode>>)

        when:
        def net = compiler.compile(forkNode)

        then:
        net != null
        net.startPlace != null
        net.endPlace != null

        and:
        verifyForkStructure(net, 2)

        and:
        findTransitionByLabel(net, 'T_fork_1') != null
        findTransitionByLabel(net, 'T_fork_1_merge_sync') != null
        findTransitionByLabel(net, 'T_fork_1_to_end') != null

        and:
        findTransitionByAction(net, 'a1') != null
        findTransitionByAction(net, 'a2') != null
    }

    @Unroll
    def 'should compile fork with #branchCount branches'() {
        given:
        def branches = (1..branchCount).collect { [action("action ${it}")] }
        def forkNode = new ForkNode(branches as List<List<ActivityNode>>)

        when:
        def net = compiler.compile(forkNode)

        then:
        net != null
        verifyForkStructure(net, branchCount)

        and:
        (1..branchCount).each { i ->
            assert findTransitionByAction(net, "action ${i}") != null
            assert findPlaceByLabel(net, "P_fork_1_branch_${i}") != null
            assert findPlaceByLabel(net, "P_fork_1_branch_${i}_end") != null
            assert findTransitionByLabel(net, "T_fork_1_branch_${i}") != null
        }

        where:
        branchCount << [2, 3, 5]
    }

    def 'should compile fork with multiple actions per branch'() {
        given:
        def branch1 = [action('a1'), action('a2')]
        def branch2 = [action('b1'), action('b2'), action('b3')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = compiler.compile(forkNode)

        then:
        net != null

        and:
        ['a1', 'a2', 'b1', 'b2', 'b3'].every { findTransitionByAction(net, it) != null }

        and:
        net.places.findAll { it.label.contains('P_fork_1_branch_1') }.size() >= 2
        net.places.findAll { it.label.contains('P_fork_1_branch_2') }.size() >= 3
        findPlaceByLabel(net, 'P_fork_1_branch_1_end') != null
        findPlaceByLabel(net, 'P_fork_1_branch_2_end') != null
    }

    def 'should compile fork with post-merge actions'() {
        given:
        def branch1 = [action('branch a')]
        def branch2 = [action('branch b')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)
        def postActions = actions('post 1', 'post 2')

        when:
        def net = compiler.compile(forkNode, postActions)

        then:
        net != null

        and:
        def post1 = findTransitionByAction(net, 'post 1')
        def post2 = findTransitionByAction(net, 'post 2')
        post1 != null
        post2 != null

        and:
        def mergePlace = findPlaceByLabel(net, 'P_fork_1_merge')
        mergePlace != null
        net.incidenceMatrix.getInputWeight(mergePlace.index, post1.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, post2.index) == 1
    }

    def 'should compile fork without post-merge actions using direct to_end transition'() {
        given:
        def branch1 = [action('a1')]
        def branch2 = [action('a2')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = compiler.compile(forkNode, [])

        then:
        findTransitionByLabel(net, 'T_fork_1_to_end') != null

        and:
        def mergePlace = findPlaceByLabel(net, 'P_fork_1_merge')
        def toEndTransition = findTransitionByLabel(net, 'T_fork_1_to_end')
        net.incidenceMatrix.getInputWeight(mergePlace.index, toEndTransition.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, toEndTransition.index) == 1
    }

    def 'should verify fork transition has 1 input and N outputs'() {
        given:
        def branch1 = [action('a1')]
        def branch2 = [action('a2')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = compiler.compile(forkNode)

        then:
        def forkTransition = findTransitionByLabel(net, 'T_fork_1')
        forkTransition != null
        verifyTransitionIO(net, forkTransition, 1, 2)
        net.incidenceMatrix.getInputWeight(net.startPlace.index, forkTransition.index) == 1
    }

    def 'should verify merge sync transition has N inputs and 1 output'() {
        given:
        def branches = (1..3).collect { [action("a${it}")] }
        def forkNode = new ForkNode(branches as List<List<ActivityNode>>)

        when:
        def net = compiler.compile(forkNode)

        then:
        def mergeSyncTransition = findTransitionByLabel(net, 'T_fork_1_merge_sync')
        mergeSyncTransition != null
        verifyTransitionIO(net, mergeSyncTransition, 3, 1)

        and:
        def mergePlace = findPlaceByLabel(net, 'P_fork_1_merge')
        mergePlace != null
        net.incidenceMatrix.getOutputWeight(mergePlace.index, mergeSyncTransition.index) == 1
    }

    def 'should compile fork with sequential actions in branch'() {
        given:
        def branch1 = [action('a1'), action('a2'), action('a3')]
        def branch2 = [action('b1'), action('b2')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = compiler.compile(forkNode)

        then:
        def a1 = findTransitionByAction(net, 'a1')
        def a2 = findTransitionByAction(net, 'a2')
        def a3 = findTransitionByAction(net, 'a3')

        def branch1Place = findPlaceByLabel(net, 'P_fork_1_branch_1')
        def branch1Mid1 = findPlaceByLabel(net, 'P_fork_1_branch_1_1')
        def branch1Mid2 = findPlaceByLabel(net, 'P_fork_1_branch_1_2')
        def branch1End = findPlaceByLabel(net, 'P_fork_1_branch_1_end')

        branch1Place != null
        branch1Mid1 != null
        branch1Mid2 != null
        branch1End != null

        and:
        net.incidenceMatrix.getInputWeight(branch1Place.index, a1.index) == 1
        net.incidenceMatrix.getOutputWeight(branch1Mid1.index, a1.index) == 1
        net.incidenceMatrix.getInputWeight(branch1Mid1.index, a2.index) == 1
        net.incidenceMatrix.getOutputWeight(branch1Mid2.index, a2.index) == 1
        net.incidenceMatrix.getInputWeight(branch1Mid2.index, a3.index) == 1
        net.incidenceMatrix.getOutputWeight(branch1End.index, a3.index) == 1
    }

    def 'should compile fork with single action per branch'() {
        given:
        def branch1 = [action('only a')]
        def branch2 = [action('only b')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = compiler.compile(forkNode)

        then:
        net != null
        findTransitionByAction(net, 'only a') != null
        findTransitionByAction(net, 'only b') != null
    }

    def 'should compile fork with many actions in branches'() {
        given:
        def branch1 = (1..5).collect { action("a${it}") }
        def branch2 = (1..3).collect { action("b${it}") }
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)

        when:
        def net = compiler.compile(forkNode)

        then:
        net != null
        (1..5).each { i -> assert findTransitionByAction(net, "a${i}") != null }
        (1..3).each { i -> assert findTransitionByAction(net, "b${i}") != null }
    }

    @Unroll
    def 'should throw IllegalArgumentException for #scenario'() {
        when:
        compiler.compile(forkNode)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario                    | forkNode                                                               | expectedMessage
        'null fork node'            | null                                                                   | 'ForkNode cannot be null'
        'null branches'             | new ForkNode(null)                                                     | 'Fork branch must contain at least one action'
        'empty branches'            | new ForkNode([])                                                       | 'Fork branch must contain at least one action'
        'empty branch'              | new ForkNode([[action('a1')], []] as List<List<ActivityNode>>)         | 'Fork branch must contain at least one action'
        'null branch'               | new ForkNode([[action('a1')], null] as List<List<ActivityNode>>)       | 'Fork branch must contain at least one action'
    }
}
