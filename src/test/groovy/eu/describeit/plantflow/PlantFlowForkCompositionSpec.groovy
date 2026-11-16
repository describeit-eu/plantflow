package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import groovy.transform.CompileStatic
import spock.lang.Specification
import spock.lang.Unroll

/**
 * End-to-end tests for Fork composition at any depth (ticket #50): conditionals
 * and Repeat Loops inside Fork branches, Forks inside conditional branches and
 * Repeat Loop bodies, and per-iteration Fork activation isolation.
 */
class PlantFlowForkCompositionSpec extends Specification {

    def 'a conditional inside a fork branch compiles and executes per branch'() {
        given:
        def result = execute(diagram('''
            fork
              if (g1) then (yes)
                :branchA;
              else (no)
                :branchAelse;
              endif
            fork again
              if (g2) then (yes)
                :branchB;
              else (no)
                :branchBelse;
              endif
            end fork
            :after;
        '''), [g1: { List trace -> true }, g2: { List trace -> false }])

        expect:
        result.endTokenCount == 1
        result.executionLog.count('branchA') == 1
        result.executionLog.count('branchAelse') == 0
        result.executionLog.count('branchB') == 0
        result.executionLog.count('branchBelse') == 1
        result.executionLog.count('after') == 1
        result.executionLog.indexOf('after') > result.executionLog.indexOf('branchA')
        result.executionLog.indexOf('after') > result.executionLog.indexOf('branchBelse')
    }

    def 'a repeat loop inside a fork branch executes fully on each branch'() {
        given: 'a two-branch fork where each branch loops while its own counter lasts'
        def puml = diagram('''
            fork
              :a1;
              repeat
                :aLoop;
              repeat while (aMore?) is (yes) not (no)
            fork again
              :b1;
              repeat
                :bLoop;
              repeat while (bMore?) is (yes) not (no)
            end fork
            :after;
        ''')
        def result = execute(puml, [
            'aMore?': { List trace -> trace.count('aLoop') < 2 },
            'bMore?': { List trace -> trace.count('bLoop') < 3 },
        ])

        expect:
        result.endTokenCount == 1
        result.executionLog.count('a1') == 1
        result.executionLog.count('aLoop') == 2
        result.executionLog.count('b1') == 1
        result.executionLog.count('bLoop') == 3
        result.executionLog.count('after') == 1
        result.executionLog.last() == 'after'
    }

    def 'a fork inside a conditional branch executes all its branches'() {
        given:
        def result = execute(diagram('''
            if (g?) then (yes)
              fork
                :A;
              fork again
                :B;
              end fork
              :thenAfter;
            else (no)
              :elseAction;
            endif
            :post;
        '''), ['g?': { List trace -> true }])

        expect:
        result.endTokenCount == 1
        result.executionLog == ['A', 'B', 'thenAfter', 'post'] || result.executionLog == ['B', 'A', 'thenAfter', 'post']
    }

    def 'a fork inside a repeat loop body creates an independent activation per iteration'() {
        given: 'a loop that forks on every iteration, two iterations deep'
        def puml = diagram('''
            repeat
              fork
                :A;
              fork again
                :B;
              end fork
            repeat while (more?) is (yes) not (no)
            :after;
        ''')
        def result = execute(puml, ['more?': { List trace -> trace.count('A') + trace.count('B') < 4 }])

        expect: 'both branches of each of the two iterations ran exactly once and the join held per iteration'
        result.endTokenCount == 1
        result.executionLog.count('A') == 2
        result.executionLog.count('B') == 2
        result.executionLog.count('after') == 1
        result.executionLog.last() == 'after'
    }

    def 'overlapping iterations of a fork inside a repeat body never mix branch tokens'() {
        given: 'branch handlers tag their payload with the iteration, driven stepwise with two iterations'
        def puml = diagram('''
            repeat
              fork
                :A;
              fork again
                :B;
              end fork
            repeat while (more?) is (yes) not (no)
        ''')
        List<String> executionLog = []
        HandlerRegistry registry = new HandlerRegistry()
        registry.registerAction('A') { ExecutionContext ctx, Token tok ->
            executionLog << 'A'
            return tok.withPayload(tok.payload + [a: executionLog.count('A')])
        }
        registry.registerAction('B') { ExecutionContext ctx, Token tok ->
            executionLog << 'B'
            return tok.withPayload(tok.payload + [b: executionLog.count('B')])
        }
        registry.registerGuard('more?') { ExecutionContext ctx, Token tok -> executionLog.count('A') < 2 }
        registry.registerGuard('!(more?)') { ExecutionContext ctx, Token tok -> executionLog.count('A') >= 2 }

        def flow = PlantFlow.from(puml, registry)
        flow.seedToken(Token.of([iteration: 0]))
        int steps = 0
        while (steps < 500 && flow.step()) {
            steps++
        }

        expect: 'each iteration joined independently: two iterations ran and exactly one merged continuation reached the end'
        flow.endTokens.size() == 1
        executionLog.count('A') == 2
        executionLog.count('B') == 2
        flow.endToken.payload.containsKey('a')
        flow.endToken.payload.containsKey('b')
    }

    @Unroll
    def 'invalid fork nesting fails at parse time: #scenario'() {
        when:
        PlantFlow.from(diagram(puml), new HandlerRegistry())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedMessage)

        where:
        scenario                          | puml                                                                    | expectedMessage
        'fork again without active fork'  | 'fork again\n  :a;\nend'                                                | 'fork again without active fork context'
        'end merge without active fork'   | ':x;\nend merge'                                                        | 'end merge without active fork context'
        'end fork without active fork'    | ':x;\nend fork'                                                         | 'end fork without active fork context'
        'unclosed fork'                   | 'fork\n  :a;\nfork again\n  :b;'                                        | 'Unclosed fork'
        'empty fork branch'               | 'fork\n  :a;\nfork again\nend fork'                                    | 'Fork branch must contain at least one action'
        'fork again inside conditional'   | 'if (g?) then (yes)\n  fork again\n  :a;\nelse (no)\n  :b;\nendif'      | 'fork again without active fork context'
        'repeat while closing a fork'     | 'fork\n  :a;\n  repeat while (c?) is (yes) not (no)\nfork again\n  :b;\nend fork' | 'repeat while without active repeat'
        'else inside a repeat'            | 'repeat\n  :a;\n  else (no)\nrepeat while (c?) is (yes) not (no)'       | 'else without active if'
    }

    @CompileStatic
    private static Map execute(String puml, Map<String, Closure<Boolean>> guards) {
        List<String> executionLog = []
        HandlerRegistry registry = new HandlerRegistry()
        actionNames(puml).each { String action ->
            registry.registerAction(action) { ExecutionContext context, Token token ->
                executionLog << action
                return token
            }
        }
        guards.each { String guard, Closure<Boolean> condition ->
            registry.registerGuard(guard) { ExecutionContext context, Token token ->
                return condition.call(executionLog)
            }
            registry.registerGuard("!(${guard})") { ExecutionContext context, Token token ->
                return !condition.call(executionLog)
            }
        }

        PlantFlow flow = PlantFlow.from(puml, registry)
        flow.seedToken()
        int steps = 0
        while (steps < 500 && flow.step()) {
            steps++
        }

        return [executionLog: executionLog, endTokenCount: flow.endTokens.size()]
    }

    @CompileStatic
    private static List<String> actionNames(String puml) {
        return puml.readLines().findResults { String line ->
            def matcher = line =~ /^\s*:(.+);\s*$/
            matcher.matches() ? matcher.group(1).trim() : null
        } as List<String>
    }

    @CompileStatic
    private static String diagram(String body) {
        return """@startuml
            start
            ${body.stripIndent()}
            end
            @enduml"""
    }
}
