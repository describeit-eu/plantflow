package eu.describeit.plantflow.ast

import spock.lang.Specification

class SwitchNodeSpec extends Specification {

    def 'SwitchCase should store condition and defensive-copy actions'() {
        given:
        def actions = [new ActionNode('a'), new ActionNode('b')]

        when:
        def switchCase = new SwitchCase('cond', actions)

        then:
        switchCase.condition == 'cond'
        switchCase.actions == actions
        !switchCase.actions.is(actions)

        and: 'mutating the original list does not affect the stored copy'
        actions.clear()
        switchCase.actions*.actionKey == ['a', 'b']
    }

    def 'SwitchCase should use an empty list when actions are null'() {
        when:
        def switchCase = new SwitchCase('cond', null)

        then:
        switchCase.condition == 'cond'
        switchCase.actions.isEmpty()
    }

    def 'SwitchCase actions list should be unmodifiable'() {
        given:
        def switchCase = new SwitchCase('cond', [new ActionNode('a')])

        when:
        switchCase.actions.add(new ActionNode('b'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'SwitchNode should store switch expression and defensive-copy cases'() {
        given:
        def cases = [new SwitchCase('condA', [new ActionNode('a')])]

        when:
        def switchNode = new SwitchNode('expr?', cases)

        then:
        switchNode.switchExpression == 'expr?'
        switchNode.cases == cases
        !switchNode.cases.is(cases)

        and: 'mutating the original list does not affect the stored copy'
        cases.clear()
        switchNode.cases.size() == 1
        switchNode.cases[0].condition == 'condA'
    }

    def 'SwitchNode should use an empty list when cases are null'() {
        when:
        def switchNode = new SwitchNode('expr?', null)

        then:
        switchNode.switchExpression == 'expr?'
        switchNode.cases.isEmpty()
    }

    def 'SwitchNode cases list should be unmodifiable'() {
        given:
        def switchNode = new SwitchNode('expr?', [new SwitchCase('cond', [])])

        when:
        switchNode.cases.add(new SwitchCase('other', []))

        then:
        thrown(UnsupportedOperationException)
    }
}
