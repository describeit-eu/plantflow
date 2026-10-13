package eu.describeit.plantflow.ast

import spock.lang.Specification

class ActivityDiagramSpec extends Specification {

    def 'hasForkStructure should return true when hasFork flag is set'() {
        given:
        def diagram = new ActivityDiagram().withFork()

        expect:
        diagram.hasForkStructure()
    }

    def 'hasForkStructure should return true when hasEndMerge flag is set'() {
        given:
        def diagram = new ActivityDiagram().withEndMerge()

        expect:
        diagram.hasForkStructure()
    }

    def 'hasForkStructure should return true when nodes contain a ForkNode'() {
        given:
        def forkNode = new ForkNode([
            [new ActionNode('a')],
            [new ActionNode('b')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, [forkNode])

        expect:
        diagram.hasForkStructure()
    }

    def 'hasForkStructure should return false when no fork indicators are present'() {
        given:
        def diagram = new ActivityDiagram(true, true, [new ActionNode('action')])

        expect:
        !diagram.hasForkStructure()
    }

    def 'hasForkStructure should return false for empty diagram'() {
        given:
        def diagram = new ActivityDiagram()

        expect:
        !diagram.hasForkStructure()
    }

    def 'hasForkStructure should return true when nodes contain ForkNode alongside other nodes'() {
        given:
        def forkNode = new ForkNode([
            [new ActionNode('a')],
            [new ActionNode('b')]
        ] as List<List<ActivityNode>>)
        def diagram = new ActivityDiagram(true, true, [new ActionNode('before'), forkNode])

        expect:
        diagram.hasForkStructure()
    }

    def 'isConditional should return true when hasIf flag is set'() {
        given:
        def diagram = new ActivityDiagram().withIf()

        expect:
        diagram.isConditional()
    }

    def 'isConditional should return true when hasElse flag is set'() {
        given:
        def diagram = new ActivityDiagram().withElse()

        expect:
        diagram.isConditional()
    }

    def 'isConditional should return true when hasEndif flag is set'() {
        given:
        def diagram = new ActivityDiagram().withEndif()

        expect:
        diagram.isConditional()
    }

    def 'isConditional should return true when nodes contain a ConditionalNode'() {
        given:
        def condNode = new ConditionalNode('guard', [new ActionNode('then')], [new ActionNode('else')])
        def diagram = new ActivityDiagram(true, true, [condNode])

        expect:
        diagram.isConditional()
    }

    def 'isConditional should return false when no conditional indicators are present'() {
        given:
        def diagram = new ActivityDiagram(true, true, [new ActionNode('action')])

        expect:
        !diagram.isConditional()
    }

    def 'addNode should ignore null node'() {
        given:
        def diagram = new ActivityDiagram()

        when:
        diagram.addNode(null)

        then:
        diagram.nodes.isEmpty()
    }

    def 'addNode should add non-null node and return this for chaining'() {
        given:
        def diagram = new ActivityDiagram()
        def node = new ActionNode('test')

        when:
        def result = diagram.addNode(node)

        then:
        result.is(diagram)
        diagram.nodes == [node]
    }

    def 'getNodes should return unmodifiable list'() {
        given:
        def diagram = new ActivityDiagram(true, true, [new ActionNode('a')])

        when:
        diagram.nodes.add(new ActionNode('b'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'withStart/withEnd/withIf/withElse/withEndif should set flags and return this'() {
        given:
        def diagram = new ActivityDiagram()

        expect:
        diagram.withStart().is(diagram)
        diagram.hasStart
        diagram.withEnd().is(diagram)
        diagram.hasEnd
        diagram.withIf().is(diagram)
        diagram.hasIf
        diagram.withElse().is(diagram)
        diagram.hasElse
        diagram.withEndif().is(diagram)
        diagram.hasEndif
    }

    def 'withStart(false) should clear the hasStart flag'() {
        given:
        def diagram = new ActivityDiagram().withStart()

        when:
        diagram.withStart(false)

        then:
        !diagram.hasStart
    }

    def 'withEnd(false) should clear the hasEnd flag'() {
        given:
        def diagram = new ActivityDiagram().withEnd()

        when:
        diagram.withEnd(false)

        then:
        !diagram.hasEnd
    }

    def 'withFork(false) should clear the hasFork flag'() {
        given:
        def diagram = new ActivityDiagram().withFork()

        when:
        diagram.withFork(false)

        then:
        !diagram.hasFork
    }

    def 'withEndMerge(false) should clear the hasEndMerge flag'() {
        given:
        def diagram = new ActivityDiagram().withEndMerge()

        when:
        diagram.withEndMerge(false)

        then:
        !diagram.hasEndMerge
    }

    def 'constructor with fork flags should set hasFork and hasEndMerge'() {
        given:
        def forkNode = new ForkNode([
            [new ActionNode('a')],
            [new ActionNode('b')]
        ] as List<List<ActivityNode>>)

        when:
        def diagram = new ActivityDiagram(true, true, true, true, [forkNode])

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.hasFork
        diagram.hasEndMerge
        diagram.nodes == [forkNode]
    }

    def 'constructor with null nodes list should not add any nodes'() {
        when:
        def diagram = new ActivityDiagram(true, true, (List<ActivityNode>) null)

        then:
        diagram.nodes.isEmpty()
    }
}
