package eu.describeit.plantflow

import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic
import groovy.transform.NullCheck
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class PlantFlow {

    final PetriNet petriNet
    final HandlerRegistry handlerRegistry
    ExecutionContext executionContext

    @NullCheck
    PlantFlow(PetriNet petriNet, HandlerRegistry handlerRegistry = new HandlerRegistry(), ExecutionContext executionContext = new ExecutionContext()) {
        this.petriNet = petriNet
        this.handlerRegistry = handlerRegistry
        this.executionContext = executionContext
    }

    PlantFlow(String pumlContent, HandlerRegistry handlerRegistry = new HandlerRegistry(), ExecutionContext executionContext = new ExecutionContext()) {
        this(new ActivityDiagramParser().parse(pumlContent), handlerRegistry, executionContext)
    }

    PlantFlow(File pumlFile, HandlerRegistry handlerRegistry = new HandlerRegistry(), ExecutionContext executionContext = new ExecutionContext()) {
        this(new ActivityDiagramParser().parse(pumlFile), handlerRegistry, executionContext)
    }

    static PlantFlow from(String pumlContent, HandlerRegistry handlerRegistry = new HandlerRegistry()) {
        return new PlantFlow(pumlContent, handlerRegistry)
    }

    static PlantFlow from(File pumlFile, HandlerRegistry handlerRegistry = new HandlerRegistry()) {
        return new PlantFlow(pumlFile, handlerRegistry)
    }

    PlantFlow registerAction(String label, Closure handler) {
        handlerRegistry.registerAction(label, handler)
        return this
    }

    void seedToken(Token token = null) {
        petriNet.seedToken(token)
    }

    boolean isEnabled(Transition transition) {
        return petriNet.isEnabled(transition, handlerRegistry, executionContext)
    }

    List<Transition> getEnabledTransitions() {
        return petriNet.getEnabledTransitions(handlerRegistry, executionContext)
    }

    boolean fire(Transition transition) {
        return petriNet.fire(transition, handlerRegistry, executionContext)
    }

    boolean step() {
        List<Transition> enabledTransitions = getEnabledTransitions()

        log.info('step() - enabledTransitions:{}', enabledTransitions)
        
        if (enabledTransitions) return fire(enabledTransitions.get(0))

        return false
    }

    PlantFlow runUntilEnd(Token token = null, ExecutionContext context = null) {
        if (context) {
            this.executionContext = context
        }
        petriNet.runUntilEnd(handlerRegistry, executionContext, token)
        return this
    }

    List<Token> getEndTokens() {
        return petriNet.getTokens(petriNet.endPlace)
    }

    Token getEndToken() {
        List<Token> tokens = getEndTokens()
        return tokens.isEmpty() ? null : tokens[0]
    }

    boolean isCompleted() {
        return !petriNet.isEmpty(petriNet.endPlace) && getEnabledTransitions().isEmpty()
    }
}
