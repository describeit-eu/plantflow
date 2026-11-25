package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.SwitchNode
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

class SwitchCaseEndswitchPumlParserSpec extends Specification {

    def 'should parse switchCaseEndswitch.puml from file with full net structure'() {
        when:
        def net = parse(new File('src/test/data/puml/switchCaseEndswitch.puml'))

        then: 'place and transition counts: start, end, decision, endswitch, 5 case places; to_decision, 5 case branches, 5 actions, to_end'
        validateNetStructure(net, 9, 12)
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'

        and: 'decision place and endswitch merge place exist'
        def decisionPlace = net.places.find { it.label == 'P_switch_1_decision' }
        def endswitchPlace = net.places.find { it.label == 'P_switch_1_endswitch' }
        decisionPlace != null
        endswitchPlace != null
        (1..5).each { int n ->
            assert net.places.find { it.label == "P_switch_1_case_${n}" } != null
        }

        and: 'structural transitions exist and carry no action key'
        def toDecision = net.transitions.find { it.label == 'T_switch_1_to_decision' }
        def toEnd = net.transitions.find { it.label == 'T_switch_1_to_end' }
        toDecision != null
        toEnd != null
        toDecision.actionKey == null
        toEnd.actionKey == null

        and: 'one guarded case branch transition per case, condition verbatim as guard key'
        def conditions = ['conditionA', 'conditionB', 'conditionC', 'conditionD', 'conditionE']
        (1..5).each { int n ->
            def caseTransition = net.transitions.find { it.label == "T_switch_1_case_${n}" }
            assert caseTransition != null
            assert caseTransition.guardKey == conditions[n - 1]
            assert caseTransition.actionKey == null
        }

        and: 'action transitions carry their action keys'
        (1..5).each { int n ->
            def actionTransition = net.transitions.find { it.actionKey == "Action ${n}" }
            assert actionTransition != null
            assert actionTransition.guardKey == null
        }

        and: 'incidence matrix connections follow fromPlace -> to_decision -> decision -> case_N -> case place -> action -> endswitch -> to_end -> end'
        verifyTransitionConnection(net, toDecision, net.startPlace, decisionPlace)
        (1..5).each { int n ->
            def caseTransition = net.transitions.find { it.label == "T_switch_1_case_${n}" }
            def casePlace = net.places.find { it.label == "P_switch_1_case_${n}" }
            verifyTransitionConnection(net, caseTransition, decisionPlace, casePlace)
            def actionTransition = net.transitions.find { it.actionKey == "Action ${n}" }
            verifyTransitionConnection(net, actionTransition, casePlace, endswitchPlace)
        }
        verifyTransitionConnection(net, toEnd, endswitchPlace, net.endPlace)
    }

    @Unroll
    def 'should parse a #caseCount-case switch from string with full net structure'() {
        given:
        def conditions = (1..caseCount).collect { "cond${it}".toString() }
        def actions = (1..caseCount).collect { "action ${it}".toString() }

        when:
        def net = parse(switchPuml(conditions, actions))

        then:
        validateNetStructure(net, 4 + caseCount, 2 + 2 * caseCount)

        and:
        def decisionPlace = net.places.find { it.label == 'P_switch_1_decision' }
        def endswitchPlace = net.places.find { it.label == 'P_switch_1_endswitch' }
        decisionPlace != null
        endswitchPlace != null

        and:
        def toDecision = net.transitions.find { it.label == 'T_switch_1_to_decision' }
        verifyTransitionConnection(net, toDecision, net.startPlace, decisionPlace)

        and:
        (1..caseCount).each { int n ->
            def caseTransition = net.transitions.find { it.label == "T_switch_1_case_${n}" }
            assert caseTransition != null
            validateTransitionGuard(net, conditions[n - 1], caseTransition.index)
            def casePlace = net.places.find { it.label == "P_switch_1_case_${n}" }
            verifyTransitionConnection(net, caseTransition, decisionPlace, casePlace)
            def actionTransition = net.transitions.find { it.actionKey == actions[n - 1] }
            assert actionTransition != null
            validateTransitionAction(net, actions[n - 1], actionTransition.index)
            verifyTransitionConnection(net, actionTransition, casePlace, endswitchPlace)
        }

        and:
        def toEnd = net.transitions.find { it.label == 'T_switch_1_to_end' }
        verifyTransitionConnection(net, toEnd, endswitchPlace, net.endPlace)

        where: 'the 5-case scenario is covered by the file-based test above'
        caseCount << [2, 3]
    }

    def 'should build a SwitchNode AST with switch expression, case conditions, and actions'() {
        given:
        def lines = new PumlLineClassifier().classifyAll('''@startuml
start
switch (test?)
case ( conditionA )
  :Action 1;
case ( conditionB )
  :Action 2;
  :Action 3;
endswitch
stop
@enduml'''.readLines())

        when:
        def diagram = new UnifiedDiagramBuilder().build(lines)

        then:
        diagram.hasSwitch
        diagram.hasEndswitch
        diagram.hasSwitchStructure()
        diagram.nodes.size() == 1

        and:
        def switchNode = diagram.nodes[0] as SwitchNode
        switchNode.switchExpression == 'test?'
        switchNode.cases.size() == 2
        switchNode.cases[0].condition == 'conditionA'
        switchNode.cases[0].actions*.actionKey == ['Action 1']
        switchNode.cases[1].condition == 'conditionB'
        switchNode.cases[1].actions*.actionKey == ['Action 2', 'Action 3']
    }

    @Unroll
    def 'should throw IllegalArgumentException for malformed switch construct: #scenario'() {
        given:
        def puml = (['@startuml', 'start'] + bodyLines + ['stop', '@enduml']).join('\n')
        def lines = new PumlLineClassifier().classifyAll(puml.readLines())

        when:
        new UnifiedDiagramBuilder().build(lines)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario                             | bodyLines                                                                            | expectedMessage
        'case without an active switch'      | ['case ( conditionA )', '  :Action 1;', 'endswitch']                                  | SwitchFrame.ERR_CASE_NO_CONTEXT
        'case inside a non-switch construct' | ['if ( valid? ) then (yes)', '  case ( conditionA )', '    :Action 1;', 'endif']      | SwitchFrame.ERR_CASE_NO_CONTEXT
        'endswitch without an active switch' | [':Action 1;', 'endswitch']                                                           | SwitchFrame.ERR_ENDSWITCH_NO_CONTEXT
        'unclosed switch missing endswitch'  | ['switch ( test? )', 'case ( conditionA )', '  :Action 1;']                           | SwitchFrame.ERR_UNCLOSED_SWITCH
        'empty switch with no cases'         | ['switch ( test? )', 'endswitch']                                                     | SwitchFrame.ERR_SWITCH_NO_CASES
    }

    def 'should allow an empty case body when the case has no actions'() {
        given:
        def lines = new PumlLineClassifier().classifyAll('''@startuml
start
switch ( test? )
case ( conditionA )
case ( conditionB )
  :Action 2;
endswitch
stop
@enduml'''.readLines())

        when:
        def diagram = new UnifiedDiagramBuilder().build(lines)

        then: 'no error and the empty case body stays empty'
        noExceptionThrown()
        def switchNode = diagram.nodes[0] as SwitchNode
        switchNode.cases.size() == 2
        switchNode.cases[0].condition == 'conditionA'
        switchNode.cases[0].actions.isEmpty()
        switchNode.cases[1].condition == 'conditionB'
        switchNode.cases[1].actions*.actionKey == ['Action 2']
    }
}
