package eu.describeit.plantflow.engine

import spock.lang.Specification

class TransitionSpec extends Specification {

    def 'should construct Transition with 2 arguments'() {
        when:
        def transition = new Transition(0, 't1')

        then:
        transition.index == 0
        transition.label == 't1'
        transition.actionKey == null
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
        new Transition(index, label)

        then:
        thrown(IllegalArgumentException)

        where:
        scenario          | index | label
        'null index'      | null  | 't1'
        'null label'      | 0     | null
    }

    def 'should allow null actionKey for structural transitions'() {
        when:
        def transition = new Transition(0, 't1', null, null)

        then:
        transition.index == 0
        transition.label == 't1'
        transition.actionKey == null
        transition.guardKey == null
    }

    def 'should allow empty or whitespace actionKey for structural transitions'() {
        when:
        def transition1 = new Transition(0, 't1', '', null)
        def transition2 = new Transition(0, 't2', '   ', null)

        then:
        transition1.actionKey == ''
        transition2.actionKey == ''
    }

    def 'should throw IllegalArgumentException when label is blank or whitespace: #scenario'() {
        when:
        new Transition(0, label)

        then:
        thrown(IllegalArgumentException)

        where:
        scenario          | label
        'empty label'     | ''
        'whitespace label'| '   '
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
