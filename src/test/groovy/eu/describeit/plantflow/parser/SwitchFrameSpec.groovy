package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.SwitchNode
import spock.lang.Specification

class SwitchFrameSpec extends Specification {

    def 'should store the switch expression'() {
        when:
        def frame = new SwitchFrame('test?')

        then:
        frame.switchExpression == 'test?'
        frame.caseConditions.isEmpty()
        frame.caseBodies.isEmpty()
    }

    def 'currentCaseBody should throw when no case has been added yet'() {
        given:
        def frame = new SwitchFrame('test?')

        when:
        frame.currentCaseBody()

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == SwitchFrame.ERR_ACTION_OUTSIDE_CASE
    }

    def 'addCase should append condition and an initially empty mutable body'() {
        given:
        def frame = new SwitchFrame('test?')

        when:
        frame.addCase('conditionA')
        frame.addCase('conditionB')

        then:
        frame.caseConditions == ['conditionA', 'conditionB']
        frame.caseBodies.size() == 2
        frame.caseBodies.every { it.isEmpty() }

        and: 'the current case body is the last added body and accepts actions'
        frame.currentCaseBody().add(new ActionNode('a'))
        frame.caseBodies[1]*.actionKey == ['a']
        frame.caseBodies[0].isEmpty()
    }

    def 'toNode should materialize a SwitchNode with conditions and bodies in order'() {
        given:
        def frame = new SwitchFrame('test?')
        frame.addCase('conditionA')
        frame.currentCaseBody().add(new ActionNode('Action 1'))
        frame.addCase('conditionB')
        frame.currentCaseBody().add(new ActionNode('Action 2'))
        frame.currentCaseBody().add(new ActionNode('Action 3'))

        when:
        def node = frame.toNode()

        then:
        node instanceof SwitchNode
        node.switchExpression == 'test?'
        node.cases.size() == 2
        node.cases[0].condition == 'conditionA'
        node.cases[0].actions*.actionKey == ['Action 1']
        node.cases[1].condition == 'conditionB'
        node.cases[1].actions*.actionKey == ['Action 2', 'Action 3']
    }

    def 'toNode should throw when no case was added'() {
        given:
        def frame = new SwitchFrame('test?')

        when:
        frame.toNode()

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == SwitchFrame.ERR_SWITCH_NO_CASES
    }
}
