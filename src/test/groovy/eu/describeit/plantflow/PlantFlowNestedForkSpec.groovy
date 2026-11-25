package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * End-to-end tests for a Fork nested inside another Fork's Parallel Branch
 * (spec #48 stories 8-10): the nested Fork registers its own
 * ForkDescriptor/JoinDescriptor keyed by its own fork transition, so its
 * Join/Merge semantics are tracked per activation at any depth.
 */
class PlantFlowNestedForkSpec extends Specification {

    private static final String INNER_JOIN_PUML = '''@startuml
start
fork
  :A;
  fork
    :A1;
  fork again
    :A2;
  end fork
  :A3;
fork again
  :B;
end merge
:after;
end
@enduml
'''

    private static final String INNER_MERGE_PUML = '''@startuml
start
fork
  :A;
  fork
    :A1;
  fork again
    :A2;
  end merge
  :A3;
fork again
  :B;
end merge
:after;
end
@enduml
'''

    def 'a fork nested in a fork branch with end fork waits for ALL inner branches before continuing the outer branch'() {
        given: 'a two-branch outer fork whose first branch contains a nested fork/join'
        def executionLog = []
        def actions = ['A', 'A1', 'A2', 'A3', 'B', 'after']
        def engine = PlantFlow.from(INNER_JOIN_PUML, payloadRegistry(actions, executionLog))
        engine.seedToken(Token.of([initial: true]))

        when: 'the outer fork fires and the first inner branch completes'
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'))
        engine.fire(findTransitionByAction(engine, 'A'))
        engine.fire(findTransitionByLabel(engine, 'T_fork_2'))
        engine.fire(findTransitionByAction(engine, 'A1'))

        then: 'the nested join is NOT enabled yet: it waits for every inner branch of its own activation'
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_2_join'))
        !engine.isEnabled(findTransitionByAction(engine, 'A3'))

        when: 'the second inner branch completes as well'
        engine.fire(findTransitionByAction(engine, 'A2'))

        then: 'the nested join is enabled now and fires, continuing the outer branch'
        engine.isEnabled(findTransitionByLabel(engine, 'T_fork_2_join'))

        when: 'the workflow runs to completion'
        engine.runUntilEnd()

        then: 'the outer branch continued only after ALL inner branches, and the workflow completed once'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        executionLog.count('A1') == 1
        executionLog.count('A2') == 1
        executionLog.count('A3') == 1
        executionLog.indexOf('A3') > executionLog.indexOf('A1')
        executionLog.indexOf('A3') > executionLog.indexOf('A2')
        executionLog.last() == 'after'
    }

    def 'a fork nested in a fork branch with end merge continues the outer branch exactly once on first inner branch completion'() {
        given: 'a two-branch outer fork whose first branch contains a nested fork/merge'
        def executionLog = []
        def actions = ['A', 'A1', 'A2', 'A3', 'B', 'after']
        def engine = PlantFlow.from(INNER_MERGE_PUML, payloadRegistry(actions, executionLog))
        engine.seedToken(Token.of([initial: true]))

        when: 'the outer fork fires and the first inner branch completes'
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'))
        engine.fire(findTransitionByAction(engine, 'A'))
        engine.fire(findTransitionByLabel(engine, 'T_fork_2'))
        engine.fire(findTransitionByAction(engine, 'A1'))

        then: 'the nested merge continues immediately on the first completed inner branch'
        engine.isEnabled(findTransitionByLabel(engine, 'T_fork_2_merge_1'))
        engine.fire(findTransitionByLabel(engine, 'T_fork_2_merge_1'))

        when: 'the late inner branch finishes and the workflow runs to completion'
        engine.runUntilEnd()

        then: 'the outer branch continued exactly once: the late inner branch did not trigger a second continuation'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        executionLog.count('A1') == 1
        executionLog.count('A2') == 1
        executionLog.count('A3') == 1
        executionLog.count('after') == 1
        executionLog.last() == 'after'
    }

    def 'two interleaved outer activations do not cross-contaminate their nested fork activations'() {
        given: 'the nested fork/join workflow seeded with two tokens, creating two overlapping outer activations'
        def executionLog = []
        def actions = ['A', 'A1', 'A2', 'A3', 'B', 'after']
        def engine = PlantFlow.from(INNER_JOIN_PUML, payloadRegistry(actions, executionLog))
        engine.seedToken(Token.of([run: 1]))
        engine.seedToken(Token.of([run: 2]))

        when: 'the workflow runs to completion'
        engine.runUntilEnd()

        then: 'each outer activation ran its own nested activation independently: two continuations, each inner branch pair joined per activation'
        engine.isCompleted()
        engine.getEndTokens().size() == 2
        executionLog.count('A') == 2
        executionLog.count('A1') == 2
        executionLog.count('A2') == 2
        executionLog.count('A3') == 2
        executionLog.count('B') == 2
        executionLog.count('after') == 2
    }
}
