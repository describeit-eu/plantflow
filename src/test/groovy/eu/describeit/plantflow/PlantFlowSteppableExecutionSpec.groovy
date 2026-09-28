package eu.describeit.plantflow

import eu.describeit.plantflow.engine.*
import spock.lang.Specification

import java.time.Instant

class PlantFlowSteppableExecutionSpec extends Specification {

    def 'getEnabledTransitions(marking, context) returns transitions enabled for custom marking'() {
        given: 'a simple linear workflow'
        def puml = '''
            @startuml
            start
            :process step;
            end
            @enduml
        '''

        and: 'a handler registry with registered action'
        def registry = new HandlerRegistry()
        registry.registerAction('process step') { ExecutionContext ctx, Token tok ->
            return tok.withPayload([processed: true])
        }

        and: 'a plantflow engine'
        def engine = PlantFlow.from(puml, registry)

        and: 'a custom marking with token in start place'
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([init: true]))

        and: 'a custom context'
        def context = new ExecutionContext([test: 'value'])

        when: 'getting enabled transitions for the custom marking and context'
        def enabled = engine.getEnabledTransitions(marking, context)

        then: 'the start transition is enabled'
        enabled.size() == 1
        enabled[0].label == 'process step'

        and: 'the marking remains unchanged'
        marking.getTokenCount(engine.petriNet.startPlace) == 1
        marking.getTokens(engine.petriNet.startPlace)[0].payload == [init: true]
    }

    def 'fire(transition, marking, context) returns new Marking without modifying original'() {
        given: 'a simple linear workflow'
        def puml = '''
            @startuml
            start
            :process step;
            end
            @enduml
        '''

        and: 'a handler registry with registered action'
        def registry = new HandlerRegistry()
        registry.registerAction('process step') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [processed: true])
        }

        and: 'a plantflow engine'
        def engine = PlantFlow.from(puml, registry)

        and: 'a custom marking with token in start place'
        def marking = new Marking(engine.petriNet.places.size())
        def originalToken = Token.of([init: true])
        marking.addToken(engine.petriNet.startPlace, originalToken)

        and: 'a custom context'
        def context = new ExecutionContext([test: 'value'])

        and: 'the process step transition'
        def transition = engine.petriNet.transitions.find { it.label == 'process step' }

        when: 'firing the transition with custom marking and context'
        def newMarking = engine.fire(transition, marking, context)

        then: 'a new marking is returned'
        newMarking != null
        newMarking !== marking

        and: 'the original marking is unchanged'
        marking.getTokenCount(engine.petriNet.startPlace) == 1
        marking.getTokens(engine.petriNet.startPlace)[0] == originalToken

        and: 'the new marking has token consumed from start and produced at end'
        newMarking.getTokenCount(engine.petriNet.startPlace) == 0
        newMarking.getTokenCount(engine.petriNet.endPlace) == 1

        and: 'the token in end place has the processed payload'
        def endToken = newMarking.getTokens(engine.petriNet.endPlace)[0]
        endToken.payload == [init: true, processed: true]
    }

    def 'step-by-step execution on ifThenElseEndif.puml verifies intermediate markings'() {
        given: 'ifThenElseEndif.puml workflow'
        def pumlFile = new File('src/test/data/puml/ifThenElseEndif.puml')

        and: 'a handler registry with guard and actions'
        def registry = new HandlerRegistry()
        registry.registerGuard("actions['process all']") { ExecutionContext ctx, Token tok ->
            return ctx['processAll'] == true
        }
        registry.registerAction('process all') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [thenBranch: true])
        }
        registry.registerAction('process none') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [elseBranch: true])
        }

        and: 'register the negation guard'
        registry.registerGuard("!(actions['process all'])") { ExecutionContext ctx, Token tok ->
            return ctx['processAll'] != true
        }

        and: 'a plantflow engine'
        def engine = PlantFlow.from(pumlFile, registry)

        and: 'initial marking with token in start place'
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([initial: true]))

        and: 'context with processAll = true to take then branch'
        def context = new ExecutionContext([processAll: true])

        when: 'getting enabled transitions from start'
        def enabled = engine.getEnabledTransitions(marking, context)

        then: 'only start_to_decision transition is enabled'
        enabled.size() == 1
        enabled[0].label == 'T_start_to_decision'

        when: 'firing start_to_decision transition'
        def markingAfterDecision = engine.fire(enabled[0], marking, context)

        then: 'token moved from start (0) to P_if_decision (1)'
        markingAfterDecision.getTokenCount(0) == 0
        markingAfterDecision.getTokenCount(1) == 1
        markingAfterDecision.getTokenCount(2) == 0
        markingAfterDecision.getTokenCount(3) == 0
        markingAfterDecision.getTokenCount(4) == 0
        markingAfterDecision.getTokenCount(5) == 0

        when: 'getting enabled transitions at decision point with processAll=true'
        def enabledAtDecision = engine.getEnabledTransitions(markingAfterDecision, context)

        then: 'only the yes branch transition is enabled (guard is true)'
        enabledAtDecision.size() == 1
        enabledAtDecision[0].label == 'T_branch_yes'

        when: 'firing the then branch (T_branch_yes)'
        def branchYesTransition = enabledAtDecision[0]
        def markingAfterThenBranch = engine.fire(branchYesTransition, markingAfterDecision, context)

        then: 'token moved from P_if_decision (1) to P_then (2)'
        markingAfterThenBranch.getTokenCount(1) == 0
        markingAfterThenBranch.getTokenCount(2) == 1
        markingAfterThenBranch.getTokenCount(3) == 0

        when: 'firing process all action transition'
        def processAllTransition = engine.petriNet.transitions.find { it.label == 'process all' }
        def markingAfterThenAction = engine.fire(processAllTransition, markingAfterThenBranch, context)

        then: 'token moved from P_then (2) to P_endif (4)'
        markingAfterThenAction.getTokenCount(2) == 0
        markingAfterThenAction.getTokenCount(4) == 1
        markingAfterThenAction.getTokenCount(3) == 0

        and: 'token has thenBranch payload'
        def tokenAtEndif = markingAfterThenAction.getTokens(4)[0]
        tokenAtEndif.payload.thenBranch == true

        when: 'firing endif to end transition'
        def endifToEndTransition = engine.petriNet.transitions.find { it.label == 'T_endif_to_end' }
        def markingAfterEnd = engine.fire(endifToEndTransition, markingAfterThenAction, context)

        then: 'token moved from P_endif (4) to end (5)'
        markingAfterEnd.getTokenCount(4) == 0
        markingAfterEnd.getTokenCount(5) == 1

        and: 'workflow completed'
        def finalEnabled = engine.getEnabledTransitions(markingAfterEnd, context)
        finalEnabled.isEmpty()
    }

    def 'step-by-step execution on ifThenElseEndif.puml with else branch'() {
        given: 'ifThenElseEndif.puml workflow'
        def pumlFile = new File('src/test/data/puml/ifThenElseEndif.puml')

        and: 'a handler registry with guard and actions'
        def registry = new HandlerRegistry()
        registry.registerGuard("actions['process all']") { ExecutionContext ctx, Token tok ->
            return ctx['processAll'] == true
        }
        registry.registerGuard("!(actions['process all'])") { ExecutionContext ctx, Token tok ->
            return ctx['processAll'] != true
        }
        registry.registerAction('process all') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [thenBranch: true])
        }
        registry.registerAction('process none') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [elseBranch: true])
        }

        and: 'a plantflow engine'
        def engine = PlantFlow.from(pumlFile, registry)

        and: 'initial marking with token in start place'
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([initial: true]))

        and: 'context with processAll = false to take else branch'
        def context = new ExecutionContext([processAll: false])

        when: 'stepping through to decision point'
        def markingAtDecision = engine.fire(
            engine.petriNet.transitions.find { it.label == 'T_start_to_decision' },
            marking,
            context
        )

        then: 'token at P_if_decision (1)'
        markingAtDecision.getTokenCount(1) == 1

        when: 'getting enabled transitions at decision point with processAll=false'
        def enabledAtDecision = engine.getEnabledTransitions(markingAtDecision, context)

        then: 'only the no branch transition is enabled (guard is true)'
        enabledAtDecision.size() == 1
        enabledAtDecision[0].label == 'T_branch_no'

        when: 'firing the else branch (T_branch_no)'
        def branchNoTransition = enabledAtDecision[0]
        def markingAfterElseBranch = engine.fire(branchNoTransition, markingAtDecision, context)

        then: 'token moved from P_if_decision (1) to P_else (3)'
        markingAfterElseBranch.getTokenCount(1) == 0
        markingAfterElseBranch.getTokenCount(3) == 1
        markingAfterElseBranch.getTokenCount(2) == 0

        when: 'firing process none action transition'
        def processNoneTransition = engine.petriNet.transitions.find { it.label == 'process none' }
        def markingAfterElseAction = engine.fire(processNoneTransition, markingAfterElseBranch, context)

        then: 'token moved from P_else (3) to P_endif (4)'
        markingAfterElseAction.getTokenCount(3) == 0
        markingAfterElseAction.getTokenCount(4) == 1

        and: 'token has elseBranch payload'
        def tokenAtEndif = markingAfterElseAction.getTokens(4)[0]
        tokenAtEndif.payload.elseBranch == true

        when: 'firing endif to end transition'
        def endifToEndTransition = engine.petriNet.transitions.find { it.label == 'T_endif_to_end' }
        def markingAfterEnd = engine.fire(endifToEndTransition, markingAfterElseAction, context)

        then: 'token moved from P_endif (4) to end (5)'
        markingAfterEnd.getTokenCount(4) == 0
        markingAfterEnd.getTokenCount(5) == 1

        and: 'workflow completed'
        def finalEnabled = engine.getEnabledTransitions(markingAfterEnd, context)
        finalEnabled.isEmpty()
    }

    def 'runUntilEnd can be verified using steppable engine primitives'() {
        given: 'a simple sequential workflow'
        def puml = '''
            @startuml
            start
            :step one;
            :step two;
            end
            @enduml
        '''

        and: 'a handler registry with registered actions'
        def registry = new HandlerRegistry()
        registry.registerAction('step one') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [step1: true])
        }
        registry.registerAction('step two') { ExecutionContext ctx, Token tok ->
            return tok.withPayload(tok.payload + [step2: true])
        }

        and: 'a plantflow engine'
        def engine = PlantFlow.from(puml, registry)

        and: 'initial marking with token in start place'
        def marking = new Marking(engine.petriNet.places.size())
        marking.addToken(engine.petriNet.startPlace, Token.of([init: true]))

        and: 'a context'
        def context = new ExecutionContext()

        when: 'manually stepping through using steppable primitives'
        def currentMarking = marking
        def stepCount = 0

        while (true) {
            def enabled = engine.getEnabledTransitions(currentMarking, context)
            if (enabled.isEmpty()) {
                break
            }
            def transition = enabled[0]
            currentMarking = engine.fire(transition, currentMarking, context)
            stepCount++
        }

        then: 'workflow completed in 2 steps'
        stepCount == 2

        and: 'final marking has token in end place'
        currentMarking.getTokenCount(engine.petriNet.endPlace) == 1

        and: 'token has all payloads'
        def endToken = currentMarking.getTokens(engine.petriNet.endPlace)[0]
        endToken.payload == [init: true, step1: true, step2: true]

        when: 'comparing with runUntilEnd'
        def engine2 = PlantFlow.from(puml, registry)
        engine2.runUntilEnd(Token.of([init: true]), context)

        then: 'both produce same result'
        engine2.getEndToken().payload == [init: true, step1: true, step2: true]
        engine2.isCompleted()
    }
}
