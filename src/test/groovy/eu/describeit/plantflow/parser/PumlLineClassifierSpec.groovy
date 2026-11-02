package eu.describeit.plantflow.parser

import spock.lang.Specification
import spock.lang.Unroll

class PumlLineClassifierSpec extends Specification {

    private PumlLineClassifier classifier = new PumlLineClassifier()

    @Unroll
    def 'should classify "#line" as #expectedType'() {
        when:
        def result = classifier.classify(line)

        then:
        result.type == expectedType
        result.text == line

        where:
        line                  | expectedType
        'start'               | LineType.START
        'end'                 | LineType.END
        'stop'                | LineType.END
        ':action;'            | LineType.ACTION
        ':process order;'     | LineType.ACTION
        '  :spaced action;  ' | LineType.ACTION
        'if (cond) then (yes)' | LineType.IF
        'else (no)'           | LineType.ELSE
        'endif'               | LineType.ENDIF
        'fork'                | LineType.FORK
        'fork again'          | LineType.FORK_AGAIN
        'end merge'           | LineType.END_MERGE
        '@startuml'           | LineType.IGNORED
        '@enduml'             | LineType.IGNORED
        "'comment"            | LineType.IGNORED
        ''                    | LineType.IGNORED
        '   '                 | LineType.IGNORED
        'random text'         | LineType.IGNORED
    }

    @Unroll
    def 'should extract action "#expectedAction" from action line'() {
        when:
        def result = classifier.classify(line)

        then:
        result.type == LineType.ACTION
        result.action == expectedAction

        where:
        line                        | expectedAction
        ':process order;'           | 'process order'
        ':  spaced action  ;'       | 'spaced action'
        ':action: with colon;'      | 'action: with colon'
        ':process order #123 [priority: high];' | 'process order #123 [priority: high]'
    }

    @Unroll
    def 'should extract guard "#expectedGuard" from if line'() {
        when:
        def result = classifier.classify(line)

        then:
        result.type == LineType.IF
        result.guard == expectedGuard

        where:
        line                                    | expectedGuard
        'if (cond) then (yes)'                  | 'cond'
        'if ( x > 0 ) then (yes)'               | 'x > 0'
        "if (actions['process all']) then (yes)" | "actions['process all']"
        'if (actions[\'process all\'] == true) then (yes)' | "actions['process all'] == true"
    }

    @Unroll
    def 'should extract branch label "#expectedLabel" from else line'() {
        when:
        def result = classifier.classify(line)

        then:
        result.type == LineType.ELSE
        result.branchLabel == expectedLabel

        where:
        line             | expectedLabel
        'else (no)'      | 'no'
        'else (non-positive)' | 'non-positive'
    }

    def 'should classify invalid if line as IF with null guard'() {
        when:
        def result = classifier.classify('if (invalid format without then clause')

        then:
        result.type == LineType.IF
        result.guard == null
    }

    def 'should classify line starting with if keyword as IF type'() {
        when:
        def result = classifier.classify('if ( condition )')

        then:
        result.type == LineType.IF
        result.guard == null
    }

    def 'should classify fork again before fork to avoid misclassification'() {
        when:
        def forkAgain = classifier.classify('fork again')
        def fork = classifier.classify('fork')

        then:
        forkAgain.type == LineType.FORK_AGAIN
        fork.type == LineType.FORK
    }

    def 'should classify end merge as END_MERGE not END'() {
        when:
        def result = classifier.classify('end merge')

        then:
        result.type == LineType.END_MERGE
    }

    def 'should classify all lines with classifyAll'() {
        given:
        def lines = ['@startuml', 'start', ':action 1;', ':action 2;', 'end', '@enduml']

        when:
        def results = classifier.classifyAll(lines)

        then:
        results.size() == 6
        results[0].type == LineType.IGNORED
        results[1].type == LineType.START
        results[2].type == LineType.ACTION
        results[2].action == 'action 1'
        results[3].type == LineType.ACTION
        results[3].action == 'action 2'
        results[4].type == LineType.END
        results[5].type == LineType.IGNORED
    }

    def 'should preserve raw line text in classified line'() {
        given:
        def rawLine = '  :action with spaces;  '

        when:
        def result = classifier.classify(rawLine)

        then:
        result.text == rawLine
        result.action == 'action with spaces'
    }

    def 'should handle whitespace-only lines as IGNORED'() {
        expect:
        classifier.classify('   ').type == LineType.IGNORED
        classifier.classify('\t').type == LineType.IGNORED
        classifier.classify('').type == LineType.IGNORED
    }
}
