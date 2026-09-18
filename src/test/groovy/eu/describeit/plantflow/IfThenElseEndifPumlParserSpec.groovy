package eu.describeit.plantflow

import spock.lang.Specification

class IfThenElseEndifPumlParserSpec extends Specification {

    def 'should parse ifThenElseEndif.puml from file'() {
        given:
        def parser = new ActivityDiagramParser()
        def file = new File('src/test/data/puml/ifThenElseEndif.puml')

        when:
        def net = parser.parse(file)

        then:
        net != null
        // Expected places: P_start (0), P_if_decision (1), P_then (2), P_else (3), P_endif (4), P_end (5)
        net.places.size() == 6
        net.transitions.size() == 6 // T_start_to_decision, T_branch_yes, T_branch_no, T_action_then, T_action_else, T_endif_to_end

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
        branchYesTransition.guardKey == "actions['process all']"
        branchNoTransition.guardKey == "!(actions['process all'])"

        // Verify action keys on action transitions
        actionThenTransition.actionKey == 'process all'
        actionElseTransition.actionKey == 'process none'

        // Verify structural transitions have null actionKey
        startToDecision.actionKey == null
        branchYesTransition.actionKey == null
        branchNoTransition.actionKey == null
        endifToEnd.actionKey == null

        and:
        // Verify incidence matrix connections
        // T_start_to_decision: consumes from P_start (0), produces to P_if_decision (1)
        net.incidenceMatrix.getInputWeight(0, startToDecision.index) == 1
        net.incidenceMatrix.getOutputWeight(1, startToDecision.index) == 1

        // T_branch_yes: consumes from P_if_decision (1), produces to P_then (2)
        net.incidenceMatrix.getInputWeight(1, branchYesTransition.index) == 1
        net.incidenceMatrix.getOutputWeight(2, branchYesTransition.index) == 1

        // T_branch_no: consumes from P_if_decision (1), produces to P_else (3)
        net.incidenceMatrix.getInputWeight(1, branchNoTransition.index) == 1
        net.incidenceMatrix.getOutputWeight(3, branchNoTransition.index) == 1

        // T_action_then: consumes from P_then (2), produces to P_endif (4)
        net.incidenceMatrix.getInputWeight(2, actionThenTransition.index) == 1
        net.incidenceMatrix.getOutputWeight(4, actionThenTransition.index) == 1

        // T_action_else: consumes from P_else (3), produces to P_endif (4)
        net.incidenceMatrix.getInputWeight(3, actionElseTransition.index) == 1
        net.incidenceMatrix.getOutputWeight(4, actionElseTransition.index) == 1

        // T_endif_to_end: consumes from P_endif (4), produces to P_end (5)
        net.incidenceMatrix.getInputWeight(4, endifToEnd.index) == 1
        net.incidenceMatrix.getOutputWeight(5, endifToEnd.index) == 1
    }

    def 'should parse if-then-else-endif diagram from string'() {
        given:
        def puml = '''
            @startuml
            start
            if ( actions['process all'] ) then (yes)
              :process all;
            else (no)
              :process none;
            endif
            end
            @enduml
        '''
        def parser = new ActivityDiagramParser()

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.places.size() == 6
        net.transitions.size() == 6
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
        def parser = new ActivityDiagramParser()

        when:
        parser.parse(puml)

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
        def parser = new ActivityDiagramParser()

        when:
        parser.parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('then') || ex.message.contains('else')
    }
}
