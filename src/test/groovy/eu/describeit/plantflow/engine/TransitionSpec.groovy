package eu.describeit.plantflow.engine

import spock.lang.Specification

class TransitionSpec extends Specification {

    def 'should construct Transition with 3 arguments'() {
        when:
        def transition = new Transition(0, 't1', 'action1')

        then:
        transition.index == 0
        transition.label == 't1'
        transition.actionKey == 'action1'
        transition.guardKey == null
    }

    def 'should construct Transition with 4 arguments'() {
        when:
        def transition = new Transition(1, 't2', 'action2', 'guard2')

        then:
        transition.index == 1
        transition.label == 't2'
        transition.actionKey == 'action2'
        transition.guardKey == 'guard2'
    }

    def 'should throw IllegalArgumentException when required parameters are null: #scenario'() {
        when:
        new Transition(index, label, actionKey)

        then:
        thrown(IllegalArgumentException)

        where:
        scenario          | index | label  | actionKey
        'null index'      | null  | 't1'   | 'action1'
        'null label'      | 0     | null   | 'action1'
        'null actionKey'  | 0     | 't1'   | null
    }

    def 'should throw IllegalArgumentException when label or actionKey is blank or whitespace: #scenario'() {
        when:
        new Transition(0, label, actionKey)

        then:
        thrown(IllegalArgumentException)

        where:
        scenario               | label   | actionKey
        'empty label'          | ''      | 'action1'
        'whitespace label'     | '   '   | 'action1'
        'empty actionKey'      | 't1'    | ''
        'whitespace actionKey' | 't1'    | '   '
    }

    def 'should satisfy equals, hashCode, and toString contracts'() {
        given:
        def t1 = new Transition(0, 't1', 'act1', 'guard1')
        def t2 = new Transition(0, 't1', 'act1', 'guard1')
        def t3 = new Transition(1, 't1', 'act1', 'guard1')
        def t4 = new Transition(0, 't2', 'act1', 'guard1')
        def t5 = new Transition(0, 't1', 'act2', 'guard1')
        def t6 = new Transition(0, 't1', 'act1', 'guard2')

        expect:
        t1 == t2
        t1.hashCode() == t2.hashCode()
        t1 != t3
        t1 != t4
        t1 != t5
        t1 != t6
        t1.toString().contains('label')
    }
}
