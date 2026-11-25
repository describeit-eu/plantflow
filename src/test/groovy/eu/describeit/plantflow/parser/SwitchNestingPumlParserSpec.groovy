package eu.describeit.plantflow.parser

import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Ticket #58: verifies that a switch/case/endswitch construct nests inside
 * if/fork/repeat and that if/fork/repeat nest inside switch case bodies, at
 * any depth, with all place/transition labels staying unique via the shared
 * NamingContext.
 */
class SwitchNestingPumlParserSpec extends Specification {

    def 'switch nested inside an if then-branch compiles with unique labels'() {
        given:
        def puml = '''@startuml
start
if ( route? ) then (yes)
  switch ( mode? )
  case ( modeA )
    :thenA;
  case ( modeB )
    :thenB;
  endswitch
else (no)
  :elseAct;
endif
:after;
end
@enduml
'''

        when:
        def net = parse(puml)

        then: 'the if is construct 1 and the nested switch is construct 2'
        validateNetStructure(net, 11, 12)
        assertUniqueLabels(net)

        and: 'the switch structural labels are nested under the then branch'
        def thenPlace = findPlaceByLabel(net, 'P_if_1_then')
        def toDecision = findTransitionByLabel(net, 'T_switch_2_to_decision')
        verifyTransitionConnection(net, toDecision, thenPlace, findPlaceByLabel(net, 'P_switch_2_decision'))
        findTransitionByLabel(net, 'T_switch_2_case_1') != null
        findTransitionByLabel(net, 'T_switch_2_case_2') != null
        findPlaceByLabel(net, 'P_switch_2_endswitch') != null
        findTransitionByLabel(net, 'T_switch_2_to_end') != null

        and: 'the switch exit routes into the if merge place'
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_switch_2_to_end'),
            findPlaceByLabel(net, 'P_switch_2_endswitch'), findPlaceByLabel(net, 'P_if_1_endif'))

        and: 'case guards stay verbatim'
        findTransitionByLabel(net, 'T_switch_2_case_1').guardKey == 'modeA'
        findTransitionByLabel(net, 'T_switch_2_case_2').guardKey == 'modeB'
    }

    def 'switch nested inside an if else-branch compiles with unique labels'() {
        given:
        def puml = '''@startuml
start
if ( route? ) then (yes)
  :thenAct;
else (no)
  switch ( mode? )
  case ( modeA )
    :elseA;
  case ( modeB )
    :elseB;
  endswitch
endif
:after;
end
@enduml
'''

        when:
        def net = parse(puml)

        then: 'the if is construct 1 and the nested switch is construct 2'
        validateNetStructure(net, 11, 12)
        assertUniqueLabels(net)

        and: 'the switch structural labels are nested under the else branch'
        def elsePlace = findPlaceByLabel(net, 'P_if_1_else')
        def toDecision = findTransitionByLabel(net, 'T_switch_2_to_decision')
        verifyTransitionConnection(net, toDecision, elsePlace, findPlaceByLabel(net, 'P_switch_2_decision'))
        findPlaceByLabel(net, 'P_switch_2_endswitch') != null

        and: 'the switch exit routes into the if merge place'
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_switch_2_to_end'),
            findPlaceByLabel(net, 'P_switch_2_endswitch'), findPlaceByLabel(net, 'P_if_1_endif'))
    }

    def 'switch nested inside a fork branch compiles with unique labels'() {
        given:
        def puml = '''@startuml
start
fork
  switch ( mode? )
  case ( modeA )
    :forkA;
  case ( modeB )
    :forkB;
  endswitch
fork again
  :other;
end fork
:after;
end
@enduml
'''

        when:
        def net = parse(puml)

        then: 'the fork is construct 1 and the nested switch is construct 2'
        validateNetStructure(net, 11, 10)
        assertUniqueLabels(net)

        and: 'the switch starts from the first fork branch place'
        def branchPlace = findPlaceByLabel(net, 'P_fork_1_branch_1')
        def toDecision = findTransitionByLabel(net, 'T_switch_2_to_decision')
        verifyTransitionConnection(net, toDecision, branchPlace, findPlaceByLabel(net, 'P_switch_2_decision'))

        and: 'the switch exit routes into the fork branch end place'
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_switch_2_to_end'),
            findPlaceByLabel(net, 'P_switch_2_endswitch'), findPlaceByLabel(net, 'P_fork_1_branch_1_end'))

        and: 'the fork join still collects both branch ends'
        def join = findTransitionByLabel(net, 'T_fork_1_join')
        verifyTransitionConnection(net, join, findPlaceByLabel(net, 'P_fork_1_branch_1_end'), findPlaceByLabel(net, 'P_fork_1_join'))
        verifyTransitionConnection(net, join, findPlaceByLabel(net, 'P_fork_1_branch_2_end'), findPlaceByLabel(net, 'P_fork_1_join'))
    }

    def 'switch nested inside a repeat loop body compiles with unique labels'() {
        given:
        def puml = '''@startuml
start
repeat
  switch ( mode? )
  case ( modeA )
    :loopA;
  case ( modeB )
    :loopB;
  endswitch
repeat while ( more? ) is (yes) not (no)
:after;
end
@enduml
'''

        when:
        def net = parse(puml)

        then: 'the repeat is construct 1 and the nested switch is construct 2'
        validateNetStructure(net, 8, 9)
        assertUniqueLabels(net)

        and: 'the switch starts from the loop start place'
        def toDecision = findTransitionByLabel(net, 'T_switch_2_to_decision')
        verifyTransitionConnection(net, toDecision, net.startPlace, findPlaceByLabel(net, 'P_switch_2_decision'))

        and: 'the switch exit routes into the repeat decision place, keeping the loop cycle intact'
        def decision = findPlaceByLabel(net, 'P_repeat_1_decision')
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_switch_2_to_end'),
            findPlaceByLabel(net, 'P_switch_2_endswitch'), decision)
        verifyRepeatLoopStructure(net, 'more?', 1, 'start')
    }

    def 'if nested inside a switch case body compiles with unique labels'() {
        given:
        def puml = '''@startuml
start
switch ( mode? )
case ( modeA )
  if ( check? ) then (yes)
    :caseThen;
  else (no)
    :caseElse;
  endif
case ( modeB )
  :caseB;
endswitch
:after;
end
@enduml
'''

        when:
        def net = parse(puml)

        then: 'the switch is construct 1 and the nested if is construct 2'
        validateNetStructure(net, 11, 12)
        assertUniqueLabels(net)

        and: 'the if starts from the first case place'
        def casePlace = findPlaceByLabel(net, 'P_switch_1_case_1')
        def toDecision = findTransitionByLabel(net, 'T_if_2_to_decision')
        verifyTransitionConnection(net, toDecision, casePlace, findPlaceByLabel(net, 'P_if_2_decision'))

        and: 'the if exit routes into the switch endswitch place'
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_if_2_to_end'),
            findPlaceByLabel(net, 'P_if_2_endif'), findPlaceByLabel(net, 'P_switch_1_endswitch'))

        and: 'the if branch guards stay verbatim'
        verifyConditionalStructure(net, 'check?')
    }

    def 'fork nested inside a switch case body compiles with unique labels'() {
        given:
        def puml = '''@startuml
start
switch ( mode? )
case ( modeA )
  fork
    :parA;
  fork again
    :parB;
  end fork
case ( modeB )
  :caseB;
endswitch
:after;
end
@enduml
'''

        when:
        def net = parse(puml)

        then: 'the switch is construct 1 and the nested fork is construct 2'
        validateNetStructure(net, 12, 11)
        assertUniqueLabels(net)

        and: 'the fork transition consumes from the first case place'
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_fork_2'),
            findPlaceByLabel(net, 'P_switch_1_case_1'), findPlaceByLabel(net, 'P_fork_2_branch_1'))
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_fork_2'),
            findPlaceByLabel(net, 'P_switch_1_case_1'), findPlaceByLabel(net, 'P_fork_2_branch_2'))

        and: 'the fork join and to-end route back into the switch endswitch place'
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_fork_2_to_end'),
            findPlaceByLabel(net, 'P_fork_2_join'), findPlaceByLabel(net, 'P_switch_1_endswitch'))
    }

    def 'repeat nested inside a switch case body compiles with unique labels'() {
        given:
        def puml = '''@startuml
start
switch ( mode? )
case ( modeA )
  repeat
    :iter;
  repeat while ( more? ) is (yes) not (no)
case ( modeB )
  :caseB;
endswitch
:after;
end
@enduml
'''

        when:
        def net = parse(puml)

        then: 'the switch is construct 1 and the nested repeat is construct 2'
        validateNetStructure(net, 8, 9)
        assertUniqueLabels(net)

        and: 'the loop body starts from the first case place and loops back to it'
        def casePlace = findPlaceByLabel(net, 'P_switch_1_case_1')
        verifyRepeatLoopStructure(net, 'more?', 2, 'P_switch_1_case_1')

        and: 'the loop exit routes into the switch endswitch place'
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_repeat_2_exit'),
            findPlaceByLabel(net, 'P_repeat_2_decision'), findPlaceByLabel(net, 'P_switch_1_endswitch'))

        and: 'the case action consumes from the case place'
        verifyTransitionConnection(net, findTransitionByAction(net, 'iter'), casePlace, findPlaceByLabel(net, 'P_repeat_2_decision'))
    }

    def 'switch nests inside if inside switch at three levels of depth with unique labels'() {
        given:
        def puml = '''@startuml
start
switch ( outer? )
case ( o1 )
  if ( mid? ) then (yes)
    switch ( inner? )
    case ( i1 )
      :deep;
    case ( i2 )
      :deeper;
    endswitch
  else (no)
    :midNo;
  endif
case ( o2 )
  :plain;
endswitch
end
@enduml
'''

        when:
        def net = parse(puml)

        then: 'outer switch, middle if, and inner switch get constructs 1, 2, and 3'
        validateNetStructure(net, 14, 16)
        assertUniqueLabels(net)

        and: 'each nesting level is wired from its enclosing construct'
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_if_2_to_decision'),
            findPlaceByLabel(net, 'P_switch_1_case_1'), findPlaceByLabel(net, 'P_if_2_decision'))
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_switch_3_to_decision'),
            findPlaceByLabel(net, 'P_if_2_then'), findPlaceByLabel(net, 'P_switch_3_decision'))
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_switch_3_to_end'),
            findPlaceByLabel(net, 'P_switch_3_endswitch'), findPlaceByLabel(net, 'P_if_2_endif'))
        verifyTransitionConnection(net, findTransitionByLabel(net, 'T_if_2_to_end'),
            findPlaceByLabel(net, 'P_if_2_endif'), findPlaceByLabel(net, 'P_switch_1_endswitch'))
    }

}
