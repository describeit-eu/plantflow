package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * End-to-end tests for src/test/data/puml/crud.puml (ticket #61): after :init;,
 * a fork runs two infinite repeat loops (:update; vs :deactivate;/:activate;)
 * concurrently. Each loop can only exit via detach, and detach is a sink: the
 * fork's branch-end places never receive tokens, so the `end fork` Join never
 * fires and the End Place stays empty. runUntilEnd() would never return on
 * this net (the constant-true loop-back guards keep transitions enabled
 * forever), so these tests drive the engine with bounded step()/fire() calls.
 */
class PlantFlowCrudSpec extends Specification {

    private static final String PUML = 'src/test/data/puml/crud.puml'

    private static HandlerRegistry crudRegistry(List<String> executionLog) {
        return payloadRegistry(['init', 'update', 'deactivate', 'activate'], executionLog)
    }

    def 'crud.puml compiles to a fork whose two infinite repeat loops each end in a detach sink'() {
        given: 'the parsed crud.puml net'
        def net = parse(new File(PUML))

        expect: 'all place and transition labels are unique'
        assertUniqueLabels(net)

        and: 'the chain actions and the fork/join/loop structure exist'
        ['init', 'update', 'deactivate', 'activate',
         'T_fork_1', 'T_fork_1_join', 'T_fork_1_to_end',
         'T_repeat_2_loop_back', 'T_repeat_2_detach',
         'T_repeat_3_loop_back', 'T_repeat_3_detach'].each { String label ->
            assert findTransitionByLabel(net, label) != null : "missing transition ${label}"
        }

        and: 'both loop-back transitions carry the constant-true guard'
        findTransitionByLabel(net, 'T_repeat_2_loop_back').guardKey == 'true'
        findTransitionByLabel(net, 'T_repeat_3_loop_back').guardKey == 'true'

        and: 'both detach transitions are sinks (one input, no outputs)'
        verifyTransitionIO(net, findTransitionByLabel(net, 'T_repeat_2_detach'), 1, 0)
        verifyTransitionIO(net, findTransitionByLabel(net, 'T_repeat_3_detach'), 1, 0)

        and: 'the fork splits into two branches and the join synchronizes on both branch ends'
        verifyTransitionIO(net, findTransitionByLabel(net, 'T_fork_1'), 1, 2)
        verifyTransitionIO(net, findTransitionByLabel(net, 'T_fork_1_join'), 2, 1)

        and: 'no transition ever produces into a branch-end place, so the join can never fire'
        ['P_fork_1_branch_1_end', 'P_fork_1_branch_2_end'].each { String label ->
            def branchEnd = findPlaceByLabel(net, label)
            assert branchEnd != null : "missing place ${label}"
            net.transitions.each { t ->
                assert net.incidenceMatrix.getOutputWeight(branchEnd.index, t.index) == 0 :
                    "${t.label} unexpectedly produces into ${label}"
            }
        }
    }

    def 'after init and the fork, both parallel branches iterate concurrently'() {
        given: 'crud.puml with all four actions registered'
        def executionLog = []
        def engine = PlantFlow.from(new File(PUML), crudRegistry(executionLog))
        engine.seedToken(Token.of())

        when: 'init and the fork transition fire'
        engine.fire(findTransitionByAction(engine, 'init'))
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'))

        then: 'both branch places hold a token'
        engine.petriNet.getTokenCount(findPlaceByLabel(engine.petriNet.structure, 'P_fork_1_branch_1')) == 1
        engine.petriNet.getTokenCount(findPlaceByLabel(engine.petriNet.structure, 'P_fork_1_branch_2')) == 1

        when: 'each branch runs one full loop iteration'
        engine.fire(findTransitionByAction(engine, 'update'))
        engine.fire(findTransitionByLabel(engine, 'T_repeat_2_loop_back'))
        engine.fire(findTransitionByAction(engine, 'deactivate'))
        engine.fire(findTransitionByAction(engine, 'activate'))
        engine.fire(findTransitionByLabel(engine, 'T_repeat_3_loop_back'))

        then: 'both loops executed their bodies and are ready to loop again'
        executionLog == ['init', 'update', 'deactivate', 'activate']
        engine.isEnabled(findTransitionByAction(engine, 'update'))
        engine.isEnabled(findTransitionByAction(engine, 'deactivate'))
    }

    def 'bounded stepping never enables the join: the end place stays empty and the flow never completes'() {
        given: 'crud.puml with a seeded token'
        def executionLog = []
        def engine = PlantFlow.from(new File(PUML), crudRegistry(executionLog))
        engine.seedToken(Token.of())
        def join = findTransitionByLabel(engine, 'T_fork_1_join')

        when: 'the engine is stepped 50 times, nudging the second branch explicitly between steps'
        (1..50).each { int i ->
            assert engine.step()
            assert !engine.isEnabled(join)
            if (i % 2 == 0 && engine.isEnabled(findTransitionByAction(engine, 'deactivate'))) {
                engine.fire(findTransitionByAction(engine, 'deactivate'))
                engine.fire(findTransitionByAction(engine, 'activate'))
                engine.fire(findTransitionByLabel(engine, 'T_repeat_3_loop_back'))
                assert !engine.isEnabled(join)
            }
        }

        then: 'both branches kept circulating'
        executionLog.contains('init')
        executionLog.count('update') >= 20
        executionLog.count('deactivate') >= 10
        executionLog.count('activate') >= 10

        and: 'no continuation ever reached the end'
        engine.getEndTokens().isEmpty()
        engine.petriNet.getTokenCount(engine.petriNet.endPlace) == 0
        !engine.isCompleted()
        !engine.petriNet.isNetEmpty()
    }

    def 'firing one branch detach consumes its token and stops only that loop; the other branch keeps looping'() {
        given: 'both branches running after init and fork, with branch 1 parked at its loop decision'
        def executionLog = []
        def engine = PlantFlow.from(new File(PUML), crudRegistry(executionLog))
        engine.seedToken(Token.of())
        engine.fire(findTransitionByAction(engine, 'init'))
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'))
        engine.fire(findTransitionByAction(engine, 'update'))
        def detach1 = findTransitionByLabel(engine, 'T_repeat_2_detach')

        expect: 'the branch 1 detach transition is enabled'
        engine.isEnabled(detach1)

        when: 'branch 1 detaches'
        engine.fire(detach1)

        then: 'branch 1 is dead: none of its transitions are enabled anymore'
        !engine.isEnabled(findTransitionByAction(engine, 'update'))
        !engine.isEnabled(findTransitionByLabel(engine, 'T_repeat_2_loop_back'))
        !engine.isEnabled(detach1)

        and: 'branch 2 keeps looping'
        engine.isEnabled(findTransitionByAction(engine, 'deactivate'))
        engine.fire(findTransitionByAction(engine, 'deactivate'))
        engine.fire(findTransitionByAction(engine, 'activate'))
        engine.fire(findTransitionByLabel(engine, 'T_repeat_3_loop_back'))
        executionLog.count('deactivate') == 1
        engine.isEnabled(findTransitionByAction(engine, 'deactivate'))

        and: 'still no end token, no join, no completion'
        engine.getEndTokens().isEmpty()
        !engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_join'))
        !engine.isCompleted()
    }
}
