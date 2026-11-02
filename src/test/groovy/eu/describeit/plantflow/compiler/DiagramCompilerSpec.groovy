package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ForkNode
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

class DiagramCompilerSpec extends Specification {

    def compiler = new DiagramCompiler()

    def 'should dispatch linear diagram to LinearNetCompiler'() {
        given:
        def diagram = diagram(true, true, [action('action 1'), action('action 2')])

        when:
        def net = compiler.compile(diagram)

        then:
        validateNetStructure(net, 3, 2)
        verifyLinearChain(net, ['action 1', 'action 2'])
    }

    def 'should dispatch conditional diagram to ConditionalNetCompiler'() {
        given:
        def condNode = conditional('isValid', actions('then action'), actions('else action'))
        def diagram = diagram(true, true, [condNode]).withIf().withElse().withEndif()

        when:
        def net = compiler.compile(diagram)

        then:
        validateNetStructure(net, 6, 6)
        verifyConditionalStructure(net, 'isValid')
    }

    def 'should dispatch fork diagram to ForkNetCompiler'() {
        given:
        def branch1 = [action('branch 1')]
        def branch2 = [action('branch 2')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode])

        when:
        def net = compiler.compile(diagram)

        then:
        net != null
        verifyForkStructure(net, 2)
        findTransitionByLabel(net, 'T_fork_1_to_end') != null
    }

    def 'should dispatch fork diagram with post-merge actions'() {
        given:
        def branch1 = [action('action 1')]
        def branch2 = [action('action 2')]
        def forkNode = new ForkNode([branch1, branch2] as List<List<ActivityNode>>)
        def postAction = action('action 5')
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode, postAction])

        when:
        def net = compiler.compile(diagram)

        then:
        net != null
        findTransitionByAction(net, 'action 5') != null

        and:
        def mergePlace = findPlaceByLabel(net, 'P_fork_1_merge')
        def action5Transition = findTransitionByAction(net, 'action 5')
        mergePlace != null
        net.incidenceMatrix.getInputWeight(mergePlace.index, action5Transition.index) == 1
        net.incidenceMatrix.getOutputWeight(net.endPlace.index, action5Transition.index) == 1
    }

    def 'should extract post-fork actions from diagram'() {
        given:
        def forkNode = new ForkNode([
            [action('branch 1')],
            [action('branch 2')]
        ] as List<List<ActivityNode>>)
        def postAction1 = action('post 1')
        def postAction2 = action('post 2')
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode, postAction1, postAction2])

        when:
        def net = compiler.compile(diagram)

        then:
        findTransitionByAction(net, 'post 1') != null
        findTransitionByAction(net, 'post 2') != null
    }

    def 'should handle fork followed by another fork (second fork ignored in post-fork extraction)'() {
        given:
        def forkNode1 = new ForkNode([
            [action('branch 1')],
            [action('branch 2')]
        ] as List<List<ActivityNode>>)
        def forkNode2 = new ForkNode([
            [action('branch 3')],
            [action('branch 4')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode1, forkNode2])

        when:
        def net = compiler.compile(diagram)

        then:
        net != null
        findTransitionByAction(net, 'branch 1') != null
        findTransitionByAction(net, 'branch 2') != null
    }

    def 'should handle ActionNode before fork node'() {
        given:
        def preAction = action('pre action')
        def forkNode = new ForkNode([
            [action('branch 1')],
            [action('branch 2')]
        ] as List<List<ActivityNode>>)
        def postAction = action('post action')
        def diagram = new ActivityDiagram(true, true, true, true, [preAction, forkNode, postAction])

        when:
        def net = compiler.compile(diagram)

        then:
        net != null
        findTransitionByAction(net, 'branch 1') != null
        findTransitionByAction(net, 'branch 2') != null
        findTransitionByAction(net, 'post action') != null
    }

    def 'should handle diagram with only fork node'() {
        given:
        def forkNode = new ForkNode([
            [action('branch 1')],
            [action('branch 2')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode])

        when:
        def net = compiler.compile(diagram)

        then:
        net != null
        findTransitionByLabel(net, 'T_fork_1_to_end') != null
    }

    def 'should handle multiple non-ActionNodes after fork'() {
        given:
        def forkNode1 = new ForkNode([
            [action('branch 1')],
            [action('branch 2')]
        ] as List<List<ActivityNode>>)
        def forkNode2 = new ForkNode([
            [action('branch 3')],
            [action('branch 4')]
        ] as List<List<ActivityNode>>)
        def forkNode3 = new ForkNode([
            [action('branch 5')],
            [action('branch 6')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode1, forkNode2, forkNode3])

        when:
        def net = compiler.compile(diagram)

        then:
        net != null
        findTransitionByAction(net, 'branch 1') != null
        findTransitionByAction(net, 'branch 2') != null
    }

    def 'should compile single-action linear diagram'() {
        given:
        def diagram = diagram(true, true, [action('single action')])

        when:
        def net = compiler.compile(diagram)

        then:
        validateNetStructure(net, 2, 1)
        validateTransitionAction(net, 'single action', 0)
    }

    def 'should compile multi-action linear diagram'() {
        given:
        def actionList = (1..5).collect { "action ${it}" }
        def nodes = actionList.collect { action(it) }
        def diagram = diagram(true, true, nodes)

        when:
        def net = compiler.compile(diagram)

        then:
        validateNetStructure(net, 6, 5)
        verifyLinearChain(net, actionList)
    }

    def 'should compile diagram with no conditional or fork nodes as linear'() {
        given:
        def diagram = diagram(true, true, [action('action 1'), action('action 2')])

        when:
        def net = compiler.compile(diagram)

        then:
        validateNetStructure(net, 3, 2)
    }

    @Unroll
    def 'should throw IllegalArgumentException for #scenario'() {
        when:
        compiler.compile(diagram)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario                                        | diagram                                              | expectedMessage
        'null diagram'                                  | null                                                 | 'Diagram cannot be null'
        'empty linear diagram'                          | diagram(true, true, [])                              | 'Diagram must contain at least one action transition'
        'conditional diagram missing conditional node'  | diagram(true, true, []).withIf()                     | 'Then block must contain at least one action'
    }
}
