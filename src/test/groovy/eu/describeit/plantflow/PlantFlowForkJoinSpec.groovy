package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Marking
import eu.describeit.plantflow.engine.Token
import eu.describeit.plantflow.engine.Transition
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Behavior-focused tests for the synchronizing Fork/Join (`fork ... end fork`):
 * each Fork activation waits for ALL its Parallel Branches, then continues
 * exactly once with branch Token payloads deep-merged in diagram order.
 */
class PlantFlowForkJoinSpec extends Specification {

    private static Transition transitionByLabel(PlantFlow engine, String label) {
        return engine.petriNet.transitions.find { it.label == label }
    }

    private static Transition transitionByAction(PlantFlow engine, String actionKey) {
        return engine.petriNet.transitions.find { it.actionKey == actionKey }
    }

    private static HandlerRegistry taggingRegistry(List<String> actions, List<String> executionLog = null) {
        def registry = new HandlerRegistry()
        actions.each { String action ->
            registry.registerAction(action) { ExecutionContext ctx, Token tok ->
                executionLog?.add(action)
                def key = action.replace(' ', '_')
                return tok.withPayload(tok.payload + [(key): true])
            }
        }
        return registry
    }

    def 'the three-branch forkEndFork sample executes with a synchronizing join and a post-join action'() {
        given: 'the sample diagram with a multi-action branch and a post-join action'
        def executionLog = []
        def registry = taggingRegistry(['action 1', 'action 2', 'action 3', 'action 4', 'action 5'], executionLog)
        def engine = PlantFlow.from(new File('src/test/data/puml/forkEndFork.puml'), registry)

        when: 'the workflow runs to completion'
        engine.runUntilEnd(Token.of([initial: true]))

        then: 'exactly one continuation reached the end with all branch payloads merged'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [
            initial   : true,
            action_1  : true,
            action_2  : true,
            action_3  : true,
            action_4  : true,
            action_5  : true,
        ]

        and: 'every action executed exactly once'
        executionLog.sort() == ['action 1', 'action 2', 'action 3', 'action 4', 'action 5']
    }

    def 'the join waits until every branch of the activation completes before continuing'() {
        given: 'a two-branch fork/join with a post-join action'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def registry = taggingRegistry(['branch a', 'branch b', 'after join'])
        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of([initial: true]))

        when: 'the fork transition fires'
        engine.fire(transitionByLabel(engine, 'T_fork_1'))

        then: 'the join is not enabled before any branch completes'
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))
        !engine.isEnabled(transitionByAction(engine, 'after join'))

        when: 'only the first branch completes'
        engine.fire(transitionByAction(engine, 'branch a'))

        then: 'the join still waits for the remaining branch without any timeout'
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))
        !engine.isEnabled(transitionByAction(engine, 'after join'))

        when: 'the second branch completes as well'
        engine.fire(transitionByAction(engine, 'branch b'))

        then: 'the join becomes enabled exactly now'
        engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))
        !engine.isEnabled(transitionByAction(engine, 'after join'))

        when: 'the join fires and the post-join action runs'
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'))
        engine.isEnabled(transitionByAction(engine, 'after join'))
        engine.fire(transitionByAction(engine, 'after join'))

        then: 'exactly one merged continuation reached the end'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [initial: true, branch_a: true, branch_b: true, after_join: true]
    }

    @Unroll
    def 'any branch completion order #order still continues exactly once with diagram-order merge'() {
        given: 'a three-branch fork/join where every branch sets the same conflicting key'
        def puml = forkJoinPuml([['branch a'], ['branch b'], ['branch c']])
        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'a', a: true]) }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'b', b: true]) }
        registry.registerAction('branch c') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [winner: 'c', c: true]) }
        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of([initial: true]))
        engine.fire(transitionByLabel(engine, 'T_fork_1'))

        when: 'the branches complete in the given order'
        order.each { String action ->
            engine.fire(transitionByAction(engine, action))
            if (action != order.last()) {
                assert !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))
            }
        }

        then: 'the join is enabled only after the last branch'
        engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))

        when: 'the join fires and flows directly to the end'
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'))

        then: 'exactly one end token exists, later diagram branches win conflicts regardless of completion order'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [initial: true, winner: 'c', a: true, b: true, c: true]

        where:
        order                        << [['branch a', 'branch b', 'branch c'],
                                        ['branch c', 'branch b', 'branch a'],
                                        ['branch b', 'branch c', 'branch a'],
                                        ['branch b', 'branch a', 'branch c'],
                                        ['branch c', 'branch a', 'branch b'],
                                        ['branch a', 'branch c', 'branch b']]
    }

    def 'a multi-action branch counts as complete only after its final action'() {
        given: 'a fork/join with a two-action branch and a single-action branch'
        def puml = forkJoinPuml([['first a', 'final a'], ['branch b']])
        def registry = taggingRegistry(['first a', 'final a', 'branch b'])
        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of())
        engine.fire(transitionByLabel(engine, 'T_fork_1'))

        when: 'the first action of the multi-action branch fires'
        engine.fire(transitionByAction(engine, 'first a'))

        then: 'the join is not enabled even though the other branch could complete'
        engine.fire(transitionByAction(engine, 'branch b'))
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))

        when: 'the final action of the multi-action branch fires'
        engine.fire(transitionByAction(engine, 'final a'))

        then: 'the join is now enabled and continues exactly once to the end'
        engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'))
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [first_a: true, final_a: true, branch_b: true]
    }

    def 'a join directly followed by the diagram end works without a post-join action'() {
        given: 'a two-branch fork/join with no post-join action'
        def puml = forkJoinPuml([['branch a'], ['branch b']])
        def registry = taggingRegistry(['branch a', 'branch b'])
        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then: 'the workflow completed with exactly one merged end token'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [initial: true, branch_a: true, branch_b: true]
    }

    def 'the join continues exactly once per activation and cannot fire again'() {
        given: 'a two-branch fork/join'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def registry = taggingRegistry(['branch a', 'branch b', 'after join'])
        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of())
        engine.fire(transitionByLabel(engine, 'T_fork_1'))
        engine.fire(transitionByAction(engine, 'branch a'))
        engine.fire(transitionByAction(engine, 'branch b'))

        when: 'the join fires'
        def fired = engine.fire(transitionByLabel(engine, 'T_fork_1_join'))

        then: 'it cannot fire a second time'
        fired
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))

        when: 'the post-join action runs'
        engine.fire(transitionByAction(engine, 'after join'))

        then: 'no further step is possible and exactly one end token exists'
        !engine.step()
        engine.isCompleted()
        engine.getEndTokens().size() == 1
    }

    def 'nested map payloads from all branches are deep-merged recursively'() {
        given: 'branches producing nested map payloads with independent and conflicting keys'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok ->
            tok.withPayload(tok.payload + [result: [x: 1, y: 1, shared: 'a'], onlyA: true])
        }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok ->
            tok.withPayload(tok.payload + [result: [y: 2, z: 2, shared: 'b'], onlyB: true])
        }
        registry.registerAction('after join') { ExecutionContext ctx, Token tok -> tok }
        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of([initial: true]))

        when: 'branch b completes before branch a, then the join fires'
        engine.fire(transitionByLabel(engine, 'T_fork_1'))
        engine.fire(transitionByAction(engine, 'branch b'))
        engine.fire(transitionByAction(engine, 'branch a'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'))
        engine.fire(transitionByAction(engine, 'after join'))

        then: 'nested maps merged recursively and the later diagram branch won the conflict'
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [
            initial: true,
            result : [x: 1, y: 2, z: 2, shared: 'b'],
            onlyA  : true,
            onlyB  : true,
        ]
    }

    @Unroll
    def 'overlapping activations with #mode never mix tokens or duplicate continuations'() {
        given: 'a two-branch fork/join seeded with two tokens for two overlapping activations'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def finalizeLog = []
        def registry = new HandlerRegistry()
        if (replacementTokens) {
            registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> Token.of([run: tok.payload.run, replaced: 'a']) }
            registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> Token.of([run: tok.payload.run, replaced: 'b']) }
        } else {
            registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [a: true]) }
            registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok.withPayload(tok.payload + [b: true]) }
        }
        registry.registerAction('after join') { ExecutionContext ctx, Token tok ->
            finalizeLog.add(tok.payload)
            return tok
        }
        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of([run: 1]))
        engine.seedToken(Token.of([run: 2]))

        when: 'both activations start and branches complete interleaved and out of order'
        engine.fire(transitionByLabel(engine, 'T_fork_1'))
        engine.fire(transitionByLabel(engine, 'T_fork_1'))
        engine.fire(transitionByAction(engine, 'branch a'))
        engine.fire(transitionByAction(engine, 'branch a'))
        assert !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))
        engine.fire(transitionByAction(engine, 'branch b'))

        then: 'the join is enabled once the first activation has a complete set'
        engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'))

        when: 'the second activation completes its branch b and both activations join and finish'
        engine.fire(transitionByAction(engine, 'branch b'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'))
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'))
        engine.fire(transitionByAction(engine, 'after join'))
        engine.fire(transitionByAction(engine, 'after join'))

        then: 'each activation continued exactly once with its own merged branch tokens'
        engine.isCompleted()
        engine.getEndTokens().size() == 2
        engine.getEndTokens()*.payload.toSet() == expectedPayloads.toSet()
        finalizeLog.toSet() == expectedPayloads.toSet()

        where:
        mode                       | replacementTokens | expectedPayloads
        'replacement tokens'       | true              | [[run: 1, replaced: 'b'], [run: 2, replaced: 'b']]
        'in-place payload updates' | false             | [[run: 1, a: true, b: true], [run: 2, a: true, b: true]]
    }

    def 'independent custom Markings keep join correlation isolated'() {
        given: 'two independent custom markings on the same fork/join workflow'
        def puml = forkJoinPuml([['branch a'], ['branch b']])
        def registry = taggingRegistry(['branch a', 'branch b'])
        def engine = PlantFlow.from(puml, registry)
        def context = new ExecutionContext()
        def marking1 = new Marking(engine.petriNet.places.size())
        marking1.addToken(engine.petriNet.startPlace, Token.of([run: 1]))
        def marking2 = new Marking(engine.petriNet.places.size())
        marking2.addToken(engine.petriNet.startPlace, Token.of([run: 2]))

        when: 'each marking activates the fork and completes only one branch'
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking1, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking2, context)
        engine.fire(transitionByAction(engine, 'branch a'), marking1, context)
        engine.fire(transitionByAction(engine, 'branch b'), marking2, context)

        then: 'neither marking can join yet'
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'), marking1, context)
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'), marking2, context)

        when: 'each marking completes its second branch and both join to the end'
        engine.fire(transitionByAction(engine, 'branch b'), marking1, context)
        engine.fire(transitionByAction(engine, 'branch a'), marking2, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'), marking1, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'), marking2, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), marking1, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), marking2, context)

        then: 'each marking produced exactly one end token with its own merged payload'
        marking1.getTokenCount(engine.petriNet.endPlace) == 1
        marking1.getTokens(engine.petriNet.endPlace)[0].payload == [run: 1, branch_a: true, branch_b: true]
        marking2.getTokenCount(engine.petriNet.endPlace) == 1
        marking2.getTokens(engine.petriNet.endPlace)[0].payload == [run: 2, branch_a: true, branch_b: true]
    }

    def 'a copied marking preserves in-progress join correlation'() {
        given: 'a fork/join activation in progress with one completed branch, then copied'
        def puml = forkJoinPuml([['branch a'], ['branch b']])
        def registry = taggingRegistry(['branch a', 'branch b'])
        def engine = PlantFlow.from(puml, registry)
        def context = new ExecutionContext()
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([run: 1]))
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking, context)
        engine.fire(transitionByAction(engine, 'branch a'), marking, context)

        when: 'the marking is copied and the copy completes the join'
        def copied = marking.copy()
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'), copied, context)
        engine.fire(transitionByAction(engine, 'branch b'), copied, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'), copied, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), copied, context)

        then: 'the copy continued exactly once with the merged payload'
        copied.getTokenCount(engine.petriNet.endPlace) == 1
        copied.getTokens(engine.petriNet.endPlace)[0].payload == [run: 1, branch_a: true, branch_b: true]

        and: 'the original marking can still complete its own join independently'
        engine.fire(transitionByAction(engine, 'branch b'), marking, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'), marking, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), marking, context)
        marking.getTokenCount(engine.petriNet.endPlace) == 1
        marking.getTokens(engine.petriNet.endPlace)[0].payload == [run: 1, branch_a: true, branch_b: true]
    }

    def 'serialization round-trip preserves in-progress join correlation'() {
        given: 'an in-progress fork/join marking serialized mid-flight'
        def puml = forkJoinPuml([['branch a'], ['branch b']])
        def registry = taggingRegistry(['branch a', 'branch b'])
        def engine = PlantFlow.from(puml, registry)
        def context = new ExecutionContext()
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([run: 1]))
        engine.fire(transitionByLabel(engine, 'T_fork_1'), marking, context)
        engine.fire(transitionByAction(engine, 'branch a'), marking, context)

        when: 'the marking round-trips through JSON and completes the join'
        def restored = Marshaller.fromJson(Marshaller.toJson(marking), Marking)
        !engine.isEnabled(transitionByLabel(engine, 'T_fork_1_join'), restored, context)
        engine.fire(transitionByAction(engine, 'branch b'), restored, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_join'), restored, context)
        engine.fire(transitionByLabel(engine, 'T_fork_1_to_end'), restored, context)

        then: 'the restored marking continued exactly once with the merged payload'
        restored.getTokenCount(engine.petriNet.endPlace) == 1
        restored.getTokens(engine.petriNet.endPlace)[0].payload == [run: 1, branch_a: true, branch_b: true]
    }

    @Unroll
    def 'malformed fork terminators are rejected: #scenario'() {
        when:
        PlantFlow.from(puml, new HandlerRegistry())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedMessage)

        where:
        scenario                          | puml                                                                            | expectedMessage
        'end fork without active fork'    | '@startuml\nstart\nend fork\nend\n@enduml'                                     | 'end fork without active fork context'
        'unclosed fork'                   | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nend\n@enduml'                | 'Unclosed fork'
        'end fork with a single branch'   | '@startuml\nstart\nfork\n  :a;\nend fork\nend\n@enduml'                        | 'branch'
        'end fork with empty branches'    | '@startuml\nstart\nfork\nfork again\nend fork\nend\n@enduml'                   | 'branch'
        'mixed end merge and end fork'    | '@startuml\nstart\nfork\n  :a;\n  fork\n    :b;\n  end merge\nfork again\n  :c;\nend fork\nend\n@enduml' | 'Mismatched fork terminators'
        'mixed end fork and end merge'    | '@startuml\nstart\nfork\n  :a;\n  fork\n    :b;\n  end fork\nfork again\n  :c;\nend merge\nend\n@enduml' | 'Mismatched fork terminators'
    }

    def 'end merge keeps first-completion semantics while end fork synchronizes'() {
        given: 'the same two-branch shape compiled with each terminator'
        def mergePuml = forkPuml([['branch a'], ['branch b']], ['after'])
        def joinPuml = forkJoinPuml([['branch a'], ['branch b']], ['after'])
        def mergeEngine = PlantFlow.from(mergePuml, taggingRegistry(['branch a', 'branch b', 'after']))
        def joinEngine = PlantFlow.from(joinPuml, taggingRegistry(['branch a', 'branch b', 'after']))
        mergeEngine.seedToken(Token.of([initial: true]))
        joinEngine.seedToken(Token.of([initial: true]))

        when: 'only branch a completes on both engines'
        mergeEngine.fire(transitionByLabel(mergeEngine, 'T_fork_1'))
        joinEngine.fire(transitionByLabel(joinEngine, 'T_fork_1'))
        mergeEngine.fire(transitionByAction(mergeEngine, 'branch a'))
        joinEngine.fire(transitionByAction(joinEngine, 'branch a'))

        then: 'the merge continues immediately while the join still waits'
        mergeEngine.isEnabled(transitionByLabel(mergeEngine, 'T_fork_1_merge_1'))
        !joinEngine.isEnabled(transitionByLabel(joinEngine, 'T_fork_1_join'))

        when: 'the remaining branch completes only on the join engine'
        joinEngine.fire(transitionByAction(joinEngine, 'branch b'))

        then: 'the join now continues with both branch payloads merged'
        joinEngine.isEnabled(transitionByLabel(joinEngine, 'T_fork_1_join'))
        joinEngine.fire(transitionByLabel(joinEngine, 'T_fork_1_join'))
        joinEngine.fire(transitionByAction(joinEngine, 'after'))
        joinEngine.isCompleted()
        joinEngine.getEndTokens().size() == 1
        joinEngine.getEndToken().payload == [initial: true, branch_a: true, branch_b: true, after: true]
    }
}
