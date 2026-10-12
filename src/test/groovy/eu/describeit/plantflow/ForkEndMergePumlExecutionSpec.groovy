package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import spock.lang.Specification
import spock.lang.Unroll

class ForkEndMergePumlExecutionSpec extends Specification {

    def 'should execute forkEndMerge.puml with runUntilEnd and complete successfully'() {
        given:
        def file = new File('src/test/data/puml/forkEndMerge.puml')
        def executionLog = []
        def registry = new HandlerRegistry()
        ['action 1', 'action 2', 'action 3', 'action 4', 'action 5'].each { action ->
            registry.registerAction(action) { ExecutionContext ctx, Token tok ->
                executionLog.add(action)
                return tok.withPayload(tok.payload + [(action.replace(' ', '_')): true])
            }
        }

        def engine = PlantFlow.from(file, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()
        engine.petriNet.isEmpty(engine.petriNet.startPlace)
        !engine.petriNet.isEmpty(engine.petriNet.endPlace)

        and:
        executionLog.size() == 5
        executionLog.containsAll(['action 1', 'action 2', 'action 3', 'action 4', 'action 5'])
    }

    def 'should execute forkEndMerge.puml step by step and verify intermediate states'() {
        given:
        def file = new File('src/test/data/puml/forkEndMerge.puml')
        def registry = new HandlerRegistry()
        ['action 1', 'action 2', 'action 3', 'action 4', 'action 5'].each { action ->
            registry.registerAction(action) { ExecutionContext ctx, Token tok ->
                return tok.withPayload(tok.payload + [(action.replace(' ', '_')): true])
            }
        }

        def engine = PlantFlow.from(file, registry)
        engine.seedToken(Token.of([initial: true]))

        when: 'step 1: fire fork transition'
        def stepped1 = engine.step()

        then:
        stepped1
        engine.petriNet.isEmpty(engine.petriNet.startPlace)

        and: 'tokens are in all three branch start places'
        def branch1Place = engine.petriNet.places.find { it.label == 'P_fork_1_branch_1' }
        def branch2Place = engine.petriNet.places.find { it.label == 'P_fork_1_branch_2' }
        def branch3Place = engine.petriNet.places.find { it.label == 'P_fork_1_branch_3' }
        engine.petriNet.getTokenCount(branch1Place) == 1
        engine.petriNet.getTokenCount(branch2Place) == 1
        engine.petriNet.getTokenCount(branch3Place) == 1

        when: 'step 2: fire action 1 (branch 1)'
        engine.step()

        then: 'branch 1 end place has token'
        def branch1EndPlace = engine.petriNet.places.find { it.label == 'P_fork_1_branch_1_end' }
        engine.petriNet.getTokenCount(branch1EndPlace) == 1

        when: 'step 3: fire action 2 (branch 2)'
        engine.step()

        then: 'branch 2 end place has token'
        def branch2EndPlace = engine.petriNet.places.find { it.label == 'P_fork_1_branch_2_end' }
        engine.petriNet.getTokenCount(branch2EndPlace) == 1

        when: 'step 4: fire action 3 (branch 3, first action)'
        engine.step()

        then: 'branch 3 intermediate place has token'
        def branch3Mid = engine.petriNet.places.find { it.label == 'P_fork_1_branch_3_1' }
        engine.petriNet.getTokenCount(branch3Mid) == 1

        when: 'step 5: fire action 4 (branch 3, second action)'
        engine.step()

        then: 'branch 3 end place has token'
        def branch3EndPlace = engine.petriNet.places.find { it.label == 'P_fork_1_branch_3_end' }
        engine.petriNet.getTokenCount(branch3EndPlace) == 1

        when: 'step 6: fire merge sync transition'
        engine.step()

        then: 'merge place has token'
        def mergePlace = engine.petriNet.places.find { it.label == 'P_fork_1_merge' }
        engine.petriNet.getTokenCount(mergePlace) == 1

        when: 'step 7: fire action 5 (post-merge)'
        def stepped7 = engine.step()

        then:
        stepped7
        engine.isCompleted()
        !engine.petriNet.isEmpty(engine.petriNet.endPlace)

        when: 'step 8: no more transitions enabled'
        def stepped8 = engine.step()

        then:
        !stepped8
    }

    def 'should deep-merge token payloads from all branches at merge point'() {
        given: 'a fork with two branches that produce different payload keys'
        def puml = '''
            @startuml
            start
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            :after merge;
            end
            @enduml
        '''

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [fromA: 'value-a', shared: 'from-a'])
        }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [fromB: 'value-b', shared: 'from-b'])
        }
        registry.registerAction('after merge') { ExecutionContext ctx, Token tok ->
            return tok
        }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()

        and: 'the end token has merged payload from both branches'
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload.initial == true
        endToken.payload.fromA == 'value-a'
        endToken.payload.fromB == 'value-b'
        endToken.payload.shared == 'from-b'
    }

    def 'should deep-merge nested map payloads from branches at merge point'() {
        given: 'a fork with branches that produce nested map payloads'
        def puml = '''
            @startuml
            start
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            end
            @enduml
        '''

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [
                config: [host: 'localhost', port: 8080],
                branchA: true
            ])
        }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [
                config: [port: 9090, ssl: true],
                branchB: true
            ])
        }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()

        and: 'nested maps are deep-merged correctly'
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload.initial == true
        endToken.payload.branchA == true
        endToken.payload.branchB == true

        and: 'nested config map is merged with overlay winning on conflicts'
        def config = endToken.payload.config as Map
        config.host == 'localhost'
        config.port == 9090
        config.ssl == true
    }

    def 'should execute fork with three branches and verify all run in parallel'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :task 1;
            fork again
              :task 2;
            fork again
              :task 3;
            end merge
            :finalize;
            end
            @enduml
        '''

        def executionOrder = []
        def registry = new HandlerRegistry()
        ['task 1', 'task 2', 'task 3', 'finalize'].each { action ->
            registry.registerAction(action) { ExecutionContext ctx, Token tok ->
                executionOrder.add(action)
                return tok.withPayload(tok.payload + [(action.replace(' ', '_')): true])
            }
        }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of())

        then:
        engine.isCompleted()

        and: 'all three tasks executed before finalize'
        executionOrder.size() == 4
        executionOrder.containsAll(['task 1', 'task 2', 'task 3'])
        executionOrder.last() == 'finalize'

        and: 'end token has all task payloads merged'
        def endToken = engine.getEndToken()
        endToken.payload.task_1 == true
        endToken.payload.task_2 == true
        endToken.payload.task_3 == true
    }

    def 'should execute fork with multi-action branches and verify sequential execution within branches'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :a1;
              :a2;
            fork again
              :b1;
              :b2;
              :b3;
            end merge
            end
            @enduml
        '''

        def executionLog = []
        def registry = new HandlerRegistry()
        ['a1', 'a2', 'b1', 'b2', 'b3'].each { action ->
            registry.registerAction(action) { ExecutionContext ctx, Token tok ->
                executionLog.add(action)
                return tok.withPayload(tok.payload + [(action): true])
            }
        }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([start: true]))

        then:
        engine.isCompleted()

        and: 'all actions executed'
        executionLog.size() == 5
        executionLog.containsAll(['a1', 'a2', 'b1', 'b2', 'b3'])

        and: 'end token has all payloads merged'
        def endToken = engine.getEndToken()
        endToken.payload.start == true
        endToken.payload.a1 == true
        endToken.payload.a2 == true
        endToken.payload.b1 == true
        endToken.payload.b2 == true
        endToken.payload.b3 == true
    }

    def 'should execute fork without post-merge actions and reach end directly'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            end
            @enduml
        '''

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [branchA: true])
        }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [branchB: true])
        }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()

        and:
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload.initial == true
        endToken.payload.branchA == true
        endToken.payload.branchB == true
    }

    def 'should preserve token id through fork and merge execution'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            end
            @enduml
        '''

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        def seedToken = Token.of([original: true])

        when:
        engine.runUntilEnd(seedToken)

        then:
        engine.isCompleted()

        and:
        def endToken = engine.getEndToken()
        endToken != null
        endToken.id == seedToken.id
        endToken.payload.original == true
    }

    @Unroll
    def 'should execute fork with #branchCount branches and verify completion'() {
        given:
        def branches = (1..branchCount).collect { ["action ${it}"] }
        def sb = new StringBuilder()
        sb << '@startuml\nstart\nfork\n'
        branches.eachWithIndex { branchActions, index ->
            if (index > 0) sb << 'fork again\n'
            branchActions.each { sb << "  :${it};\n" }
        }
        sb << 'end merge\nend\n@enduml\n'
        def puml = sb.toString()

        def registry = new HandlerRegistry()
        (1..branchCount).each { i ->
            registry.registerAction("action ${i}") { ExecutionContext ctx, Token tok ->
                return tok.withPayload(tok.payload + [("action_${i}" as String): true])
            }
        }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()

        and:
        def endToken = engine.getEndToken()
        endToken != null
        endToken.payload.initial == true
        (1..branchCount).each { i ->
            assert endToken.payload["action_${i}"] == true
        }

        where:
        branchCount << [2, 3, 5]
    }

    def 'should execute nested fork structure correctly'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :action 1;
              fork
                :nested action 1;
              fork again
                :nested action 2;
              end merge
            fork again
              :action 2;
            end merge
            end
            @enduml
        '''

        def executionLog = []
        def registry = new HandlerRegistry()
        ['action 1', 'action 2', 'nested action 1', 'nested action 2'].each { action ->
            registry.registerAction(action) { ExecutionContext ctx, Token tok ->
                executionLog.add(action)
                return tok.withPayload(tok.payload + [(action.replace(' ', '_')): true])
            }
        }

        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()

        and:
        executionLog.size() == 4
        executionLog.containsAll(['action 1', 'action 2', 'nested action 1', 'nested action 2'])

        and:
        def endToken = engine.getEndToken()
        endToken.payload.initial == true
        endToken.payload.action_1 == true
        endToken.payload.action_2 == true
        endToken.payload.nested_action_1 == true
        endToken.payload.nested_action_2 == true
    }

    def 'should update execution context from parallel branch actions'() {
        given:
        def puml = '''
            @startuml
            start
            fork
              :branch a;
            fork again
              :branch b;
            end merge
            end
            @enduml
        '''

        def registry = new HandlerRegistry()
        registry.registerAction('branch a') { ExecutionContext ctx, Token tok ->
            ctx['branchAExecuted'] = true
            return tok.withPayload(tok.payload + [a: true])
        }
        registry.registerAction('branch b') { ExecutionContext ctx, Token tok ->
            ctx['branchBExecuted'] = true
            return tok.withPayload(tok.payload + [b: true])
        }

        def context = new ExecutionContext()
        def engine = PlantFlow.from(puml, registry)

        when:
        engine.runUntilEnd(Token.of(), context)

        then:
        engine.isCompleted()
        context['branchAExecuted'] == true
        context['branchBExecuted'] == true
    }

    def 'regression: all supported puml files should parse and compile without errors'() {
        given: 'puml files that the parser supports (linear, if-then-else-endif, fork-end merge)'
        def pumlFiles = [
            'sequence.puml',
            'ifThenElseEndif.puml',
            'forkEndMerge.puml'
        ]

        expect:
        pumlFiles.each { filename ->
            def file = new File("src/test/data/puml/${filename}")
            def net = new ActivityDiagramParser().parse(file)
            assert net != null : "Failed to parse ${filename}"
            assert net.places.size() > 0 : "No places in ${filename}"
            assert net.transitions.size() > 0 : "No transitions in ${filename}"
            assert net.startPlace != null : "No start place in ${filename}"
            assert net.endPlace != null : "No end place in ${filename}"
        }
    }

    def 'regression: sequence.puml should execute correctly'() {
        given:
        def file = new File('src/test/data/puml/sequence.puml')
        def calls = []
        def engine = PlantFlow.from(file)
            .registerAction('Hello world') { ExecutionContext ctx, Token tok ->
                calls.add('hello')
                return [greeting: 'Hello, World!']
            }
            .registerAction('groovy goodness') { ExecutionContext ctx, Token tok ->
                calls.add('groovy')
                return tok.payload + [goodness: true]
            }

        when:
        engine.runUntilEnd()

        then:
        calls == ['hello', 'groovy']
        engine.isCompleted()
        engine.getEndToken().payload == [greeting: 'Hello, World!', goodness: true]
    }

    def 'regression: ifThenElseEndif.puml should execute with then branch'() {
        given:
        def file = new File('src/test/data/puml/ifThenElseEndif.puml')
        def registry = new HandlerRegistry()
        registry.registerGuard("actions['process all']") { ExecutionContext ctx, Token tok -> true }
        registry.registerGuard("!(actions['process all'])") { ExecutionContext ctx, Token tok -> false }
        registry.registerAction('process all') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [thenBranch: true])
        }
        registry.registerAction('process none') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [elseBranch: true])
        }

        def engine = PlantFlow.from(file, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()
        engine.getEndToken().payload.thenBranch == true
    }

    def 'regression: ifThenElseEndif.puml should execute with else branch'() {
        given:
        def file = new File('src/test/data/puml/ifThenElseEndif.puml')
        def registry = new HandlerRegistry()
        registry.registerGuard("actions['process all']") { ExecutionContext ctx, Token tok -> false }
        registry.registerGuard("!(actions['process all'])") { ExecutionContext ctx, Token tok -> true }
        registry.registerAction('process all') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [thenBranch: true])
        }
        registry.registerAction('process none') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [elseBranch: true])
        }

        def engine = PlantFlow.from(file, registry)

        when:
        engine.runUntilEnd(Token.of([initial: true]))

        then:
        engine.isCompleted()
        engine.getEndToken().payload.elseBranch == true
    }
}
