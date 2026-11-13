package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.ast.RepeatNode
import spock.lang.Specification

/**
 * Gap tests driving each DiagramBuilder directly with ClassifiedLine inputs,
 * including the error paths of the unified builder.
 */
class DiagramBuilderCoverageGapSpec extends Specification {

    private static ClassifiedLine line(LineType type, String text, String action = null, String guard = null) {
        return new ClassifiedLine(type, text, action, guard)
    }

    def 'PumlLineClassifier ignores startuml/enduml with trailing text and comments'() {
        given:
        def classifier = new PumlLineClassifier()

        expect:
        classifier.classify('@startuml foo').type == LineType.IGNORED
        classifier.classify('@enduml bar').type == LineType.IGNORED
        classifier.classify("' a comment").type == LineType.IGNORED
        classifier.classify('   ').type == LineType.IGNORED
    }

    def 'PumlLineClassifier accepts an if line without a space before the parenthesis'() {
        given:
        def classifier = new PumlLineClassifier()

        when:
        ClassifiedLine classified = classifier.classify('if(x) then (y)')

        then:
        classified.type == LineType.IF
        classified.guard == 'x'
    }

    def 'LinearDiagramBuilder handles all line types and empty input'() {
        given:
        def builder = new LinearDiagramBuilder()

        expect: 'empty input produces an empty diagram'
        builder.build([]).nodes.isEmpty()

        and: 'null input is tolerated as an empty diagram'
        builder.build(null).nodes.isEmpty()

        when: 'start, action and end lines are processed'
        def diagram = builder.build([
            line(LineType.START, 'start'),
            line(LineType.ACTION, ':a;', 'a'),
            line(LineType.END, 'end'),
        ])

        then:
        diagram.hasStart
        diagram.hasEnd
        diagram.nodes*.action == ['a']
    }

    def 'ConditionalDiagramBuilder processes if/else/endif and skips actions outside branches'() {
        given:
        def builder = new ConditionalDiagramBuilder()

        expect: 'empty and null input are tolerated'
        builder.build([]) != null
        builder.build(null) != null

        when: 'a full conditional plus a trailing action outside any branch is built'
        def diagram = builder.build([
            line(LineType.START, 'start'),
            line(LineType.IF, 'if (g) then (y)', null, 'g'),
            line(LineType.ACTION, ':a;', 'a'),
            line(LineType.ELSE, 'else (n)'),
            line(LineType.ACTION, ':b;', 'b'),
            line(LineType.ENDIF, 'endif'),
            line(LineType.ACTION, ':c;', 'c'),
            line(LineType.END, 'end'),
        ])

        then:
        diagram.hasStart && diagram.hasEnd && diagram.hasIf && diagram.hasElse && diagram.hasEndif
        diagram.nodes.size() == 1
        def cond = diagram.nodes[0] as ConditionalNode
        cond.guardCondition == 'g'
        cond.thenActions*.action == ['a']
        cond.elseActions*.action == ['b']
    }

    def 'ConditionalDiagramBuilder tolerates an if line without a guard'() {
        given:
        def builder = new ConditionalDiagramBuilder()

        when:
        def diagram = builder.build([
            line(LineType.IF, 'if'),
            line(LineType.ACTION, ':a;', 'a'),
        ])

        then: 'no guard means no conditional node and no hasIf flag'
        !diagram.hasIf
        diagram.nodes.isEmpty()
    }

    def 'RepeatDiagramBuilder builds a loop with detach and tolerates empty input'() {
        given:
        def builder = new RepeatDiagramBuilder()

        expect:
        builder.build([]).nodes.isEmpty()
        builder.build(null).nodes.isEmpty()

        when:
        def diagram = builder.build([
            line(LineType.START, 'start'),
            line(LineType.REPEAT, 'repeat'),
            line(LineType.ACTION, ':a;', 'a'),
            line(LineType.REPEAT_WHILE, 'repeat while (c) is (yes)', null, 'c'),
            line(LineType.DETACH, 'detach'),
            line(LineType.END, 'end'),
        ])

        then:
        diagram.hasStart && diagram.hasEnd && diagram.hasRepeat && diagram.hasRepeatWhile && diagram.hasDetach
        diagram.nodes.size() == 1
        def loop = diagram.nodes[0] as RepeatNode
        loop.loopCondition == 'c'
        loop.detach
        loop.body*.action == ['a']
    }

    def 'RepeatDiagramBuilder rejects repeat while without context, empty bodies, unclosed loops and misplaced detach'() {
        given:
        def builder = new RepeatDiagramBuilder()

        when: 'repeat while appears without an open repeat'
        builder.build([line(LineType.REPEAT_WHILE, 'x', null, 'c')])

        then:
        thrown(IllegalArgumentException)

        when: 'the loop body is empty'
        builder.build([line(LineType.REPEAT, 'repeat'), line(LineType.REPEAT_WHILE, 'x', null, 'c')])

        then:
        thrown(IllegalArgumentException)

        when: 'the repeat is never closed'
        builder.build([line(LineType.REPEAT, 'repeat'), line(LineType.ACTION, ':a;', 'a')])

        then:
        thrown(IllegalArgumentException)

        when: 'detach appears without a preceding loop'
        builder.build([line(LineType.DETACH, 'detach')])

        then:
        thrown(IllegalArgumentException)

        when: 'detach does not directly follow the loop'
        builder.build([
            line(LineType.REPEAT, 'repeat'),
            line(LineType.ACTION, ':a;', 'a'),
            line(LineType.REPEAT_WHILE, 'x', null, 'c'),
            line(LineType.ACTION, ':b;', 'b'),
            line(LineType.DETACH, 'detach'),
        ])

        then:
        thrown(IllegalArgumentException)
    }

    def 'ForkDiagramBuilder builds branches, collects post-fork actions and tolerates empty input'() {
        given:
        def builder = new ForkDiagramBuilder()

        expect:
        builder.build([]).nodes.isEmpty()
        builder.build(null).nodes.isEmpty()

        when: 'a two-branch fork with a post-merge action is built'
        def diagram = builder.build([
            line(LineType.START, 'start'),
            line(LineType.FORK, 'fork'),
            line(LineType.ACTION, ':a;', 'a'),
            line(LineType.FORK_AGAIN, 'fork again'),
            line(LineType.ACTION, ':b;', 'b'),
            line(LineType.END_MERGE, 'end merge'),
            line(LineType.ACTION, ':c;', 'c'),
            line(LineType.END, 'end'),
        ])

        then:
        diagram.hasFork && diagram.hasEndMerge && !diagram.hasEndFork
        diagram.nodes.size() == 2
        def fork = diagram.nodes[0] as ForkNode
        fork.branches*.size() == [1, 1]
        fork.branches[0][0].action == 'a'
        fork.branches[1][0].action == 'b'
        diagram.nodes[1].action == 'c'
    }

    def 'ForkDiagramBuilder drops actions outside any fork and rejects malformed constructs'() {
        given:
        def builder = new ForkDiagramBuilder()

        expect: 'an action before any fork is not attached anywhere'
        builder.build([line(LineType.ACTION, ':x;', 'x')]).nodes.isEmpty()

        when: 'fork again appears without an open fork'
        builder.build([line(LineType.FORK_AGAIN, 'fork again')])

        then:
        thrown(IllegalArgumentException)

        when: 'end merge appears without an open fork'
        builder.build([line(LineType.END_MERGE, 'end merge')])

        then:
        thrown(IllegalArgumentException)

        when: 'end fork appears without an open fork'
        builder.build([line(LineType.END_FORK, 'end fork')])

        then:
        thrown(IllegalArgumentException)

        when: 'the fork is never closed'
        builder.build([line(LineType.FORK, 'fork'), line(LineType.ACTION, ':a;', 'a')])

        then:
        thrown(IllegalArgumentException)

        when: 'end merge and end fork are mixed within one nested fork construct'
        builder.build([
            line(LineType.FORK, 'fork'),
            line(LineType.FORK, 'fork'),
            line(LineType.END_MERGE, 'end merge'),
            line(LineType.END_FORK, 'end fork'),
        ])

        then:
        thrown(IllegalArgumentException)
    }

    def 'UnifiedDiagramBuilder builds nested repeat and conditional structures'() {
        given:
        def builder = new UnifiedDiagramBuilder()

        expect: 'empty and null input are tolerated'
        builder.build([]).nodes.isEmpty()
        builder.build(null).nodes.isEmpty()

        when: 'a repeat loop containing a conditional is built'
        def diagram = builder.build([
            line(LineType.START, 'start'),
            line(LineType.REPEAT, 'repeat'),
            line(LineType.IF, 'if (g) then (y)', null, 'g'),
            line(LineType.ACTION, ':a;', 'a'),
            line(LineType.ELSE, 'else (n)'),
            line(LineType.ACTION, ':b;', 'b'),
            line(LineType.ENDIF, 'endif'),
            line(LineType.REPEAT_WHILE, 'repeat while (c) is (yes)', null, 'c'),
            line(LineType.DETACH, 'detach'),
            line(LineType.END, 'end'),
        ])

        then:
        diagram.hasStart && diagram.hasEnd && diagram.hasRepeat && diagram.hasRepeatWhile && diagram.hasDetach
        diagram.hasIf && diagram.hasElse && diagram.hasEndif
        diagram.nodes.size() == 1
        def loop = diagram.nodes[0] as RepeatNode
        loop.detach
        loop.body.size() == 1
        (loop.body[0] as ConditionalNode).thenActions*.action == ['a']
        (loop.body[0] as ConditionalNode).elseActions*.action == ['b']
    }

    def 'UnifiedDiagramBuilder rejects unclosed constructs and misplaced terminators'() {
        given:
        def builder = new UnifiedDiagramBuilder()

        when: 'a repeat is never closed'
        builder.build([line(LineType.REPEAT, 'repeat'), line(LineType.ACTION, ':a;', 'a')])

        then: 'unclosed repeat error'
        def e1 = thrown(IllegalArgumentException)
        e1.message == 'Unclosed repeat - missing repeat while'

        when: 'an if is never closed'
        builder.build([line(LineType.IF, 'if (g) then (y)', null, 'g'), line(LineType.ACTION, ':a;', 'a')])

        then: 'unclosed if error'
        def e2 = thrown(IllegalArgumentException)
        e2.message == 'Unclosed if - missing endif'

        when: 'repeat while appears without an open loop'
        builder.build([line(LineType.REPEAT_WHILE, 'x', null, 'c')])

        then:
        thrown(IllegalArgumentException)

        when: 'repeat while appears while an if is open'
        builder.build([line(LineType.IF, 'if (g) then (y)', null, 'g'), line(LineType.REPEAT_WHILE, 'x', null, 'c')])

        then:
        thrown(IllegalArgumentException)

        when: 'the loop body is empty'
        builder.build([line(LineType.REPEAT, 'repeat'), line(LineType.REPEAT_WHILE, 'x', null, 'c')])

        then:
        thrown(IllegalArgumentException)

        when: 'else appears without an open if'
        builder.build([line(LineType.ELSE, 'else (n)')])

        then:
        thrown(IllegalArgumentException)

        when: 'else appears while a loop is open'
        builder.build([line(LineType.REPEAT, 'repeat'), line(LineType.ELSE, 'else (n)')])

        then:
        thrown(IllegalArgumentException)

        when: 'endif appears without an open if'
        builder.build([line(LineType.ENDIF, 'endif')])

        then:
        thrown(IllegalArgumentException)

        when: 'endif appears while a loop is open'
        builder.build([line(LineType.REPEAT, 'repeat'), line(LineType.ENDIF, 'endif')])

        then:
        thrown(IllegalArgumentException)

        when: 'detach appears without a preceding loop'
        builder.build([line(LineType.DETACH, 'detach')])

        then:
        thrown(IllegalArgumentException)

        when: 'detach does not directly follow the loop'
        builder.build([
            line(LineType.REPEAT, 'repeat'),
            line(LineType.ACTION, ':a;', 'a'),
            line(LineType.REPEAT_WHILE, 'x', null, 'c'),
            line(LineType.ACTION, ':b;', 'b'),
            line(LineType.DETACH, 'detach'),
        ])

        then:
        thrown(IllegalArgumentException)
    }

    def 'UnifiedDiagramBuilder tolerates an if line without a guard'() {
        given:
        def builder = new UnifiedDiagramBuilder()

        when:
        def diagram = builder.build([
            line(LineType.IF, 'if'),
            line(LineType.ACTION, ':a;', 'a'),
            line(LineType.ENDIF, 'endif'),
        ])

        then: 'no guard means the hasIf flag stays false but the node is still built'
        !diagram.hasIf
        diagram.nodes.size() == 1
        (diagram.nodes[0] as ConditionalNode).guardCondition == null
    }
}
