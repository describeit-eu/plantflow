package eu.describeit.plantflow


import eu.describeit.plantflow.engine.Token
import spock.lang.Specification

class PlantFlowErrorHandlingSpec extends Specification {

    def 'should throw UnregisteredHandlerException when action handler is missing during execution'() {
        given:
        def puml = '''
            @startuml
            start
            :unregistered step;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry())

        when:
        engine.runUntilEnd(Token.of([key: 'val']))

        then:
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered step')
    }

    def 'should throw UnregisteredHandlerException when evaluating isEnabled for unregistered action handler'() {
        given: 'a workflow with an unregistered action step and a seeded token'
        def puml = '''
            @startuml
            start
            :unregistered step;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry())
        engine.seedToken(Token.of())
        def transition = engine.petriNet.transitions[0]

        when: 'checking if the transition is enabled'
        engine.isEnabled(transition)

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered step')
    }

    def 'should throw UnregisteredHandlerException when evaluating getEnabledTransitions with unregistered action handler'() {
        given: 'a workflow with an unregistered action step and a seeded token'
        def puml = '''
            @startuml
            start
            :unregistered step;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry())
        engine.seedToken(Token.of())

        when: 'querying enabled transitions'
        engine.getEnabledTransitions()

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered step')
    }

    def 'should throw UnregisteredHandlerException when evaluating isEnabled for unregistered guard handler'() {
        given: 'a workflow with an unregistered guard condition'
        def puml = '''
            @startuml
            start
            if ( unregistered guard ) then (yes)
              :action then;
            else (no)
              :action else;
            endif
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        // Register the actions but NOT the guard
        registry.registerAction('action then') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action else') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of())
        
        // First, fire the initial transition to get token to decision place
        def startTransition = engine.petriNet.transitions.find { it.label == 'T_start_to_decision' }
        engine.fire(startTransition)
        
        def transition = engine.petriNet.transitions.find { it.guardKey == 'unregistered guard' }

        when: 'checking if the transition is enabled'
        engine.isEnabled(transition)

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered guard')
    }

    def 'should throw UnregisteredHandlerException when evaluating getEnabledTransitions with unregistered guard handler'() {
        given: 'a workflow with an unregistered guard condition'
        def puml = '''
            @startuml
            start
            if ( unregistered guard ) then (yes)
              :action then;
            else (no)
              :action else;
            endif
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        // Register the actions but NOT the guard
        registry.registerAction('action then') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action else') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of())
        
        // First, fire the initial transition to get token to decision place
        def startTransition = engine.petriNet.transitions.find { it.label == 'T_start_to_decision' }
        engine.fire(startTransition)

        when: 'querying enabled transitions'
        engine.getEnabledTransitions()

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered guard')
    }

    def 'should throw UnregisteredHandlerException when firing transition with unregistered guard'() {
        given: 'a workflow with an unregistered guard condition'
        def puml = '''
            @startuml
            start
            if ( unregistered guard ) then (yes)
              :action then;
            else (no)
              :action else;
            endif
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        // Register the actions but NOT the guard
        registry.registerAction('action then') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action else') { ExecutionContext ctx, Token tok -> tok }

        def engine = PlantFlow.from(puml, registry)
        engine.seedToken(Token.of())
        
        // First, fire the initial transition to get token to decision place
        def startTransition = engine.petriNet.transitions.find { it.label == 'T_start_to_decision' }
        engine.fire(startTransition)
        
        def transition = engine.petriNet.transitions.find { it.guardKey == 'unregistered guard' }

        when: 'firing the transition'
        engine.fire(transition)

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered guard')
    }

    def 'should throw UnregisteredHandlerException when firing transition with unregistered action handler'() {
        given: 'a workflow with an unregistered action step and a seeded token'
        def puml = '''
            @startuml
            start
            :unregistered action;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry())
        engine.seedToken(Token.of())
        def transition = engine.petriNet.transitions[0]

        when: 'firing the transition'
        engine.fire(transition)

        then: 'it immediately raises UnregisteredHandlerException'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered action')
    }

    def 'should fail fast with UnregisteredHandlerException during runUntilEnd with unregistered action'() {
        given: 'a workflow with an unregistered action step'
        def puml = '''
            @startuml
            start
            :step one;
            :unregistered step;
            :step three;
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        registry.registerAction('step one') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('step three') { ExecutionContext ctx, Token tok -> tok }
        // step two is NOT registered

        def engine = PlantFlow.from(puml, registry)

        when: 'running until end'
        engine.runUntilEnd(Token.of([init: true]))

        then: 'it immediately raises UnregisteredHandlerException without completing'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered step')
        !engine.isCompleted()
    }

    def 'should fail fast with UnregisteredHandlerException during runUntilEnd with unregistered guard'() {
        given: 'a workflow with an unregistered guard condition'
        def puml = '''
            @startuml
            start
            :step one;
            if ( unregistered guard ) then (yes)
              :action then;
            else (no)
              :action else;
            endif
            :step three;
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        registry.registerAction('step one') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action then') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('action else') { ExecutionContext ctx, Token tok -> tok }
        registry.registerAction('step three') { ExecutionContext ctx, Token tok -> tok }
        // guard is NOT registered

        def engine = PlantFlow.from(puml, registry)

        when: 'running until end'
        engine.runUntilEnd(Token.of([init: true]))

        then: 'it immediately raises UnregisteredHandlerException without completing'
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains('unregistered guard')
        !engine.isCompleted()
    }

}
