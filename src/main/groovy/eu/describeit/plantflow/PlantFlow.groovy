package eu.describeit.plantflow

import groovy.transform.CompileStatic
import groovy.transform.NullCheck
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class PlantFlow {

    final PetriNet petriNet
    final HandlerRegistry handlerRegistry
    final Marking marking
    ExecutionContext executionContext

    @NullCheck
    PlantFlow(PetriNet petriNet, HandlerRegistry handlerRegistry = new HandlerRegistry(), ExecutionContext executionContext = new ExecutionContext()) {
        this.petriNet = petriNet
        this.handlerRegistry = handlerRegistry
        this.marking = new Marking(petriNet.places)
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

    void seedToken(RecordToken token) {
        RecordToken tokenToSeed = token ?: RecordToken.of()
        marking.addToken(petriNet.startPlace, tokenToSeed)
    }

    boolean isEnabled(Transition transition) {
        return petriNet.isEnabled(transition, marking, handlerRegistry, executionContext)
    }

    List<Transition> getEnabledTransitions() {
        return petriNet.getEnabledTransitions(marking, handlerRegistry, executionContext)
    }

    boolean fire(Transition transition) {
        return petriNet.fire(transition, marking, handlerRegistry, executionContext)
    }

    boolean step() {
        List<Transition> enabledTransitions = getEnabledTransitions()

        log.info('step() - enabledTransitions:{}', enabledTransitions)
        
        if (enabledTransitions) return fire(enabledTransitions.get(0))

        return false
    }

    Marking runUntilEnd(RecordToken token = null, ExecutionContext context = null) {
        if (context) {
            this.executionContext = context
        }
        return petriNet.runUntilEnd(marking, handlerRegistry, executionContext, token)
    }

    List<RecordToken> getEndTokens() {
        return marking.getTokens(petriNet.endPlace)
    }

    RecordToken getEndToken() {
        List<RecordToken> tokens = getEndTokens()
        return tokens.isEmpty() ? null : tokens[0]
    }

    boolean isCompleted() {
        return !marking.isEmpty(petriNet.endPlace) && getEnabledTransitions().isEmpty()
    }
}
