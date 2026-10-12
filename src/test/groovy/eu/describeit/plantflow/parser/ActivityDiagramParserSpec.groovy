package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ActivityDiagramParser
import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import spock.lang.Specification

/**
 * Tests for ActivityDiagramParser edge cases targeting mutation testing survivors.
 */
class ActivityDiagramParserSpec extends Specification {

    private ActivityDiagramParser parser = new ActivityDiagramParser()

    // --- checkControl tests (lines 223-225) ---

    def 'should recognize start keyword as control line'() {
        given:
        def puml = '''
            @startuml
            start
            :action;
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then: 'diagram has start place'
        net != null
        net.startPlace.label == 'start'
    }

    def 'should recognize end keyword as control line'() {
        given:
        def puml = '''
            @startuml
            start
            :action;
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then: 'diagram has end place'
        net != null
        net.endPlace.label == 'end'
    }

    def 'should recognize stop keyword as control line (equivalent to end)'() {
        given:
        def puml = '''
            @startuml
            start
            :action;
            stop
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then: 'diagram has end place'
        net != null
        net.endPlace.label == 'end'
    }

    def 'should ignore empty lines'() {
        given:
        def puml = '''
            @startuml

            start

            :action;

            end

            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.size() == 1
    }

    def 'should ignore comment lines starting with single quote'() {
        given:
        def puml = '''
            @startuml
            start
            'this is a comment
            :action;
            'another comment
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.size() == 1
        net.transitions[0].actionKey == 'action'
    }

    def 'should ignore @startuml and @enduml lines'() {
        given:
        def puml = '''
            @startuml
            start
            :action;
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.size() == 1
    }

    def 'should not treat unrecognized non-control line as control line'() {
        given: 'a diagram with a random unrecognized line that is not a control keyword'
        def puml = '''
            @startuml
            start
            :action 1;
            random text here
            :action 2;
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then: 'both actions are parsed, random line is ignored'
        net != null
        net.transitions.size() == 2
    }

    // --- updateBranch tests (line 204) ---

    def 'should reset branch to NONE after endif so subsequent actions are not recorded in branches'() {
        given: 'a conditional with actions after endif (should not be part of any branch)'
        def puml = '''
            @startuml
            start
            if (cond) then (yes)
              :then action;
            else (no)
              :else action;
            endif
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then: 'only then and else actions are in the net, no extra actions'
        net != null
        net.transitions.find { it.actionKey == 'then action' } != null
        net.transitions.find { it.actionKey == 'else action' } != null
    }

    def 'should throw when endif present without else (updateBranch returns NONE for endif)'() {
        given: 'a diagram with endif but no else clause'
        def puml = '''
            @startuml
            start
            if (cond) then (yes)
              :then action;
            endif
            :after endif;
            end
            @enduml
        '''

        when:
        parser.parse(puml)

        then: 'validator rejects if-then-endif without else'
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('else')
    }

    // --- finalizeConditional tests (line 219) ---

    def 'should set hasEndif when guard is null and neither else nor endif present'() {
        given: 'a diagram with if keyword but no else and no endif'
        def puml = '''
            @startuml
            start
            if (cond) then (yes)
              :action;
            end
            @enduml
        '''

        when:
        parser.parse(puml)

        then:
        thrown(IllegalArgumentException)
    }

    def 'should not set hasEndif when else is present but endif is missing'() {
        given: 'a diagram with if, else but no endif'
        def puml = '''
            @startuml
            start
            if (cond) then (yes)
              :then action;
            else (no)
              :else action;
            end
            @enduml
        '''

        when:
        parser.parse(puml)

        then:
        thrown(IllegalArgumentException)
    }

    def 'should compile conditional with else and endif correctly'() {
        given:
        def puml = '''
            @startuml
            start
            if (cond) then (yes)
              :then action;
            else (no)
              :else action;
            endif
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.find { it.actionKey == 'then action' } != null
        net.transitions.find { it.actionKey == 'else action' } != null
    }

    def 'should compile conditional with endif but no else'() {
        given:
        def puml = '''
            @startuml
            start
            if (cond) then (yes)
              :then action;
            else (no)
              :else action;
            endif
            :post action;
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.find { it.actionKey == 'then action' } != null
        net.transitions.find { it.actionKey == 'else action' } != null
    }

    // --- handleActionRecording boundary test (line 183) ---

    def 'should handle action at branch boundary index (currentBranchIndex == branches.size() - 1)'() {
        given: 'a fork with exactly one branch (boundary: currentBranchIndex = 0, branches.size() = 1)'
        def puml = '''
            @startuml
            start
            fork
              :single branch action;
            end merge
            end
            @enduml
        '''

        when:
        parser.parse(puml)

        then:
        thrown(IllegalArgumentException) // single branch fork is invalid
    }

    def 'should handle action in last branch at boundary index'() {
        given: 'a fork with two branches, action in the last branch'
        def puml = '''
            @startuml
            start
            fork
              :branch 1 action;
            fork again
              :branch 2 action;
            end merge
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then: 'both branch actions are parsed'
        net != null
        net.transitions.find { it.actionKey == 'branch 1 action' } != null
        net.transitions.find { it.actionKey == 'branch 2 action' } != null
    }

    def 'should handle action after fork complete as post-fork action'() {
        given: 'a fork with post-merge action (currentBranchIndex = -1, forkComplete = true)'
        def puml = '''
            @startuml
            start
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            :post merge action;
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then: 'post-merge action is parsed as a separate action node'
        net != null
        net.transitions.find { it.actionKey == 'post merge action' } != null
    }

    def 'should handle action before any fork as non-branch action (ignored in fork parsing)'() {
        given: 'an action before fork keyword in a fork diagram'
        def puml = '''
            @startuml
            start
            :before fork;
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then: 'before-fork action is not in any branch (currentBranchIndex = -1, forkComplete = false)'
        net != null
        net.transitions.find { it.actionKey == 'branch a' } != null
        net.transitions.find { it.actionKey == 'branch b' } != null
    }

    // --- Fork parsing with nested structures ---

    def 'should handle deeply nested fork with actions at various branch indices'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :outer 1;
              fork
                :inner 1;
              fork again
                :inner 2;
              end merge
            fork again
              :outer 2;
            end merge
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.find { it.actionKey == 'outer 1' } != null
        net.transitions.find { it.actionKey == 'outer 2' } != null
        net.transitions.find { it.actionKey == 'inner 1' } != null
        net.transitions.find { it.actionKey == 'inner 2' } != null
    }

    def 'should handle multiple actions in single branch at boundary'() {
        given: 'a fork with multiple actions in one branch'
        def puml = '''
            @startuml
            start
            fork
              :action 1;
              :action 2;
              :action 3;
            fork again
              :action 4;
            end merge
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.find { it.actionKey == 'action 1' } != null
        net.transitions.find { it.actionKey == 'action 2' } != null
        net.transitions.find { it.actionKey == 'action 3' } != null
        net.transitions.find { it.actionKey == 'action 4' } != null
    }

    def 'should handle multiple post-merge actions after fork complete'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            :post 1;
            :post 2;
            :post 3;
            end
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.find { it.actionKey == 'post 1' } != null
        net.transitions.find { it.actionKey == 'post 2' } != null
        net.transitions.find { it.actionKey == 'post 3' } != null
    }

    // --- Linear parsing with control keywords ---

    def 'should parse linear diagram with stop keyword'() {
        given:
        def puml = '''
            @startuml
            start
            :step 1;
            :step 2;
            stop
            @enduml
        '''

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.size() == 2
    }

    def 'should parse linear diagram with comments and blank lines interspersed'() {
        given:
        def puml = """
            'header comment
            @startuml
            start
            'comment before action
            :action 1;
            'comment between actions

            :action 2;
            'comment before end
            end
            @enduml
        """

        when:
        def net = parser.parse(puml)

        then:
        net != null
        net.transitions.size() == 2
    }
}
