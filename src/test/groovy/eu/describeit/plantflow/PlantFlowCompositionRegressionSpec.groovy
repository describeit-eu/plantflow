package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import groovy.transform.CompileStatic
import spock.lang.Specification
import spock.lang.Unroll

class PlantFlowCompositionRegressionSpec extends Specification {

    @Unroll
    def 'should execute sequential compositions without dropping or merging nodes: #scenario'() {
        given:
        def result = execute(puml, guards)

        expect:
        result.endTokenCount == 1
        result.executionLog == expectedTrace

        where:
        scenario                        | puml                                                                                                                 | guards                                                                 | expectedTrace
        'nested conditionals'           | diagram('''if (g1) then (yes)\n  if (g2) then (yes)\n    :A;\n  else (no)\n    :B;\n  endif\nelse (no)\n  :C;\nendif''') | [g1: { List trace -> true }, g2: { List trace -> false }] | ['B']
        'sequential conditionals'       | diagram('''if (g1) then (yes)\n  :A;\nelse (no)\n  :B;\nendif\nif (g2) then (yes)\n  :C;\nelse (no)\n  :D;\nendif''') | [g1: { List trace -> true }, g2: { List trace -> false }] | ['A', 'D']
        'actions surrounding conditional' | diagram(''':pre;\nif (g1) then (yes)\n  :A;\nelse (no)\n  :B;\nendif\n:post;''')                                          | [g1: { List trace -> true }]                                  | ['pre', 'A', 'post']
        'sequential repeats'            | diagram('''repeat\n  :L1;\nrepeat while (c1) is (yes)\nrepeat\n  :L2;\nrepeat while (c2) is (yes)''')                                           | [c1: { List trace -> false }, c2: { List trace -> false }]     | ['L1', 'L2']
        'conditional followed by repeat'| diagram('''if (g1) then (yes)\n  :A;\nelse (no)\n  :B;\nendif\nrepeat\n  :L;\nrepeat while (c1) is (yes)''')                            | [g1: { List trace -> true }, c1: { List trace -> false }]      | ['A', 'L']
    }

    def 'should retain actions around a fork and join only after its branches complete'() {
        given:
        def result = execute(diagram('''
            :pre;
            fork
              :A;
            fork again
              :B;
            end fork
            :after;
        '''), [:])

        expect:
        result.endTokenCount == 1
        result.executionLog.count('pre') == 1
        result.executionLog.count('A') == 1
        result.executionLog.count('B') == 1
        result.executionLog.count('after') == 1
        result.executionLog.indexOf('pre') < result.executionLog.indexOf('A')
        result.executionLog.indexOf('pre') < result.executionLog.indexOf('B')
        result.executionLog.indexOf('after') > result.executionLog.indexOf('A')
        result.executionLog.indexOf('after') > result.executionLog.indexOf('B')
    }

    def 'should execute every branch in sequential forks'() {
        given:
        def result = execute(diagram('''
            fork
              :A;
            fork again
              :B;
            end fork
            fork
              :C;
            fork again
              :D;
            end fork
        '''), [:])

        expect:
        result.endTokenCount == 1
        result.executionLog.count('A') == 1
        result.executionLog.count('B') == 1
        result.executionLog.count('C') == 1
        result.executionLog.count('D') == 1
    }

    def 'should preserve nested fork structure and actions after the inner join'() {
        given:
        def result = execute(diagram('''
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
        '''), [:])

        expect:
        result.endTokenCount == 1
        result.executionLog.count('A') == 1
        result.executionLog.count('A1') == 1
        result.executionLog.count('A2') == 1
        result.executionLog.count('A3') == 1
        result.executionLog.count('B') == 1
        result.executionLog.count('after') == 1
        result.executionLog.indexOf('A') < result.executionLog.indexOf('A1')
        result.executionLog.indexOf('A') < result.executionLog.indexOf('A2')
        result.executionLog.indexOf('A1') < result.executionLog.indexOf('A3')
        result.executionLog.indexOf('A2') < result.executionLog.indexOf('A3')
        result.executionLog.indexOf('A3') < result.executionLog.indexOf('after')
    }

    @Unroll
    def 'should reject unsupported PlantUML control syntax: #syntax'() {
        when:
        PlantFlow.from(diagram(puml), new HandlerRegistry())

        then:
        thrown(IllegalArgumentException)

        where:
        syntax      | puml
        'elseif'    | '''if (g1) then (yes)
                           :A;
                         elseif (g2) then (yes)
                           :B;
                         else (no)
                           :C;
                         endif'''
        'while'     | '''while (c1) is (yes)
                           :read;
                         endwhile (no)'''
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
//        assert steps < 500: 'workflow exceeded the bounded execution limit (500)'

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
