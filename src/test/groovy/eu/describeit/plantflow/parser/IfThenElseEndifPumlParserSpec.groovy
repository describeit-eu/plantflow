package eu.describeit.plantflow.parser

import spock.lang.Specification

class IfThenElseEndifPumlParserSpec extends Specification {

    def 'should parse ifThenElseEndif.puml from file'() {
        when:
        def net = ParserTestBase.parse(new File('src/test/data/puml/ifThenElseEndif.puml'))

        then:
        ParserTestBase.validateNetStructure(net, 6, 6)

        and:
        net.startPlace.index == 0
        net.startPlace.label == 'start'
        net.endPlace.index == 5
        net.endPlace.label == 'end'

        and:
        // Check decision place
        def decisionPlace = net.places.find { it.label == 'P_if_decision' }
        decisionPlace != null
        decisionPlace.index == 1

        // Check then branch place
        def thenPlace = net.places.find { it.label == 'P_then' }
        thenPlace != null
        thenPlace.index == 2

        // Check else branch place
        def elsePlace = net.places.find { it.label == 'P_else' }
        elsePlace != null
        elsePlace.index == 3

        // Check endif merge place
        def endifPlace = net.places.find { it.label == 'P_endif' }
        endifPlace != null
        endifPlace.index == 4

        and:
        // Check transitions
        def startToDecision = net.transitions.find { it.label == 'T_start_to_decision' }
        def branchYesTransition = net.transitions.find { it.label == 'T_branch_yes' }
        def branchNoTransition = net.transitions.find { it.label == 'T_branch_no' }
        def actionThenTransition = net.transitions.find { it.label == 'process all' }
        def actionElseTransition = net.transitions.find { it.label == 'process none' }
        def endifToEnd = net.transitions.find { it.label == 'T_endif_to_end' }

        startToDecision != null
        branchYesTransition != null
        branchNoTransition != null
        actionThenTransition != null
        actionElseTransition != null
        endifToEnd != null

        // Verify guard keys on branch transitions
        ParserTestBase.validateTransitionGuard(net, "actions['process all']", branchYesTransition.index)
        branchNoTransition.guardKey == "!(actions['process all'])"

        // Verify action keys on action transitions
        ParserTestBase.validateTransitionAction(net, 'process all', actionThenTransition.index)
        ParserTestBase.validateTransitionAction(net, 'process none', actionElseTransition.index)

        // Verify structural transitions have null actionKey
        startToDecision.actionKey == null
        branchYesTransition.actionKey == null
        branchNoTransition.actionKey == null
        endifToEnd.actionKey == null

        and:
        // Verify incidence matrix connections using verifyTransitionConnection
        ParserTestBase.verifyTransitionConnection(net, startToDecision.index, 0, 1)
        ParserTestBase.verifyTransitionConnection(net, branchYesTransition.index, 1, 2)
        ParserTestBase.verifyTransitionConnection(net, branchNoTransition.index, 1, 3)
        ParserTestBase.verifyTransitionConnection(net, actionThenTransition.index, 2, 4)
        ParserTestBase.verifyTransitionConnection(net, actionElseTransition.index, 3, 4)
        ParserTestBase.verifyTransitionConnection(net, endifToEnd.index, 4, 5)
    }

    def 'should parse if-then-else-endif diagram from string'() {
        when:
        def net = ParserTestBase.parse(ParserTestBase.createConditionalPuml("actions['process all']", 'process all', 'process none'))

        then:
        ParserTestBase.validateNetStructure(net, 6, 6)
    }

    def 'should throw IllegalArgumentException when if statement has no matching endif'() {
        given:
        def puml = '''
            @startuml
            start
            if ( condition ) then (yes)
              :action;
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('else') || ex.message.contains('endif')
    }

    def 'should throw IllegalArgumentException when then or else is missing'() {
        given:
        def puml = '''
            @startuml
            start
            if ( condition )
              :action;
            endif
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

         then:
         def ex = thrown(IllegalArgumentException)
         ex.message.contains('then') || ex.message.contains('else')
     }

    def 'should handle conditional with else block correctly'() {
        given:
        def puml = '''
            @startuml
            start
            if (condition) then (yes)
              :then action;
            else (no)
              :else action;
            endif
            end
            @enduml
        '''

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
        
        and:
        def thenTransition = net.transitions.find { it.actionKey == 'then action' }
        def elseTransition = net.transitions.find { it.actionKey == 'else action' }
        
        thenTransition != null
        elseTransition != null
    }

    def 'should handle conditional with explicit else block'() {
        given:
        def puml = '''
            @startuml
            start
            if (x > 0) then (positive)
              :process positive;
            else (non-positive)
              :process non-positive;
            endif
            end
            @enduml
        '''

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
        
        and:
        def positiveTransition = net.transitions.find { it.actionKey == 'process positive' }
        def nonPositiveTransition = net.transitions.find { it.actionKey == 'process non-positive' }
        
        positiveTransition != null
        nonPositiveTransition != null
    }

    def 'should handle conditional with guard containing spaces'() {
        given:
        def puml = '''
            @startuml
            start
            if ( x > 0 ) then (yes)
              :then action;
            else (no)
              :else action;
            endif
            end
            @enduml
        '''

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
    }

    def 'should handle conditional with multiple actions in branches'() {
        given:
        def puml = '''
            @startuml
            start
            if (condition) then (yes)
              :action 1;
              :action 2;
            else (no)
              :action 3;
              :action 4;
            endif
            end
            @enduml
        '''

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
        
        and:
        def action1Transition = net.transitions.find { it.actionKey == 'action 1' }
        def action2Transition = net.transitions.find { it.actionKey == 'action 2' }
        def action3Transition = net.transitions.find { it.actionKey == 'action 3' }
        def action4Transition = net.transitions.find { it.actionKey == 'action 4' }
        
        action1Transition != null
        action2Transition != null
        action3Transition != null
        action4Transition != null
    }

    def 'should throw when if keyword present but if pattern does not match'() {
        given: 'a diagram with if keyword but invalid if format (no then clause)'
        def puml = '''
            @startuml
            start
            if (invalid format without then clause
            :some action;
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('if')
    }

    def 'should handle conditional with stop keyword instead of end'() {
        given:
        def puml = '''
            @startuml
            start
            if (condition) then (yes)
              :then action;
            else (no)
              :else action;
            endif
            stop
            @enduml
        '''

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.places.size() > 0
        net.transitions.size() > 0
    }

    def 'should handle conditional with extra whitespace around keywords'() {
        given:
        def puml = '''
            @startuml
              start
            if ( condition ) then ( yes )
              :then action;
                else ( no )
              :else action;
                endif
              end
            @enduml
        '''

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        net.transitions.find { it.actionKey == 'then action' } != null
        net.transitions.find { it.actionKey == 'else action' } != null
    }

    def 'should handle conditional with guard containing special characters'() {
        given:
        def puml = '''
            @startuml
            start
            if (actions['process all'] == true) then (yes)
              :process all;
            else (no)
              :skip;
            endif
            end
            @enduml
        '''

        when:
        def net = ParserTestBase.parse(puml)

        then:
        net != null
        def branchYes = net.transitions.find { it.label == 'T_branch_yes' }
        branchYes != null
        branchYes.guardKey == "actions['process all'] == true"
    }

    def 'should throw when invalid if format has else but no matching if pattern'() {
        given: 'a diagram with if keyword, invalid if format, and else clause'
        def puml = '''
            @startuml
            start
            if (invalid format without then
            :some action;
            else (no)
            :else action;
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('if')
    }

    def 'should throw when invalid if format has endif but no matching if pattern'() {
        given: 'a diagram with if keyword, invalid if format, and endif'
        def puml = '''
            @startuml
            start
            if (invalid format without then
            :some action;
            endif
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('if')
    }

    def 'should throw when invalid if format has both else and endif but no matching if pattern'() {
        given: 'a diagram with if keyword, invalid if format, else, and endif'
        def puml = '''
            @startuml
            start
            if (invalid format without then
            :some action;
            else (no)
            :else action;
            endif
            end
            @enduml
        '''

        when:
        ParserTestBase.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('if')
    }
}
