package eu.describeit.plantflow

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class PlantFlow {

    final PetriNet petriNet
    final HandlerRegistry handlerRegistry
    final Marking marking
    ExecutionContext executionContext

    PlantFlow(PetriNet petriNet, HandlerRegistry handlerRegistry = new HandlerRegistry(), ExecutionContext executionContext = new ExecutionContext()) {
        if (petriNet == null) throw new IllegalArgumentException("PetriNet cannot be null")
        this.petriNet = petriNet
        this.handlerRegistry = handlerRegistry ?: new HandlerRegistry()
        this.marking = new Marking(petriNet.places)
        this.executionContext = executionContext ?: new ExecutionContext()
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
        if (token == null) {
            token = RecordToken.of()
        }
        marking.addToken(petriNet.startPlace, token)
    }

    boolean isEnabled(Transition transition) {
        if (transition == null) return false

        List<Place> inputPlaces = petriNet.incidenceMatrix.getInputPlaces(transition.index)
        for (Place p : inputPlaces) {
            int requiredWeight = petriNet.incidenceMatrix.getInputWeight(p.index, transition.index)
            if (marking.getTokenCount(p) < requiredWeight) {
                return false
            }
        }

        if (transition.guardKey != null && !transition.guardKey.isEmpty()) {
            GuardPredicate guard = handlerRegistry.getGuard(transition.guardKey)
            RecordToken tokenForGuard = null
            if (!inputPlaces.isEmpty()) {
                List<RecordToken> tokens = marking.getTokens(inputPlaces[0])
                if (!tokens.isEmpty()) {
                    tokenForGuard = tokens[0]
                }
            }
            if (!guard.evaluate(executionContext, tokenForGuard)) {
                return false
            }
        }

        if (transition.actionKey != null && !transition.actionKey.isEmpty()) {
            handlerRegistry.getAction(transition.actionKey)
        }

        return true
    }

    List<Transition> getEnabledTransitions() {
        List<Transition> enabled = []
        for (Transition t : petriNet.transitions) {
            if (isEnabled(t)) {
                enabled.add(t)
            }
        }
        return Collections.unmodifiableList(enabled)
    }

    boolean fire(Transition transition) {
        if (!isEnabled(transition)) {
            return false
        }

        log.info("fire() - {}", transition)

        ActionHandler actionHandler = null
        if (transition.actionKey != null && !transition.actionKey.isEmpty()) {
            actionHandler = handlerRegistry.getAction(transition.actionKey)
        }

        List<Place> inputPlaces = petriNet.incidenceMatrix.getInputPlaces(transition.index)
        List<RecordToken> consumedTokens = []

        for (Place p : inputPlaces) {
            int requiredWeight = petriNet.incidenceMatrix.getInputWeight(p.index, transition.index)
            for (int w = 0; w < requiredWeight; w++) {
                List<RecordToken> available = marking.getTokens(p)
                if (!available.isEmpty()) {
                    RecordToken tokenToConsume = available[0]
                    marking.removeToken(p, tokenToConsume)
                    consumedTokens.add(tokenToConsume)
                }
            }
        }

        RecordToken primaryToken = consumedTokens.isEmpty() ? RecordToken.of() : consumedTokens[0]
        RecordToken outputToken = primaryToken

        if (actionHandler != null) {
            Object result = actionHandler.execute(executionContext, primaryToken)
            if (result instanceof RecordToken) {
                outputToken = (RecordToken) result
            } else if (result instanceof Map) {
                @SuppressWarnings('unchecked')
                Map<String, Object> newPayload = (Map<String, Object>) result
                outputToken = primaryToken.withPayload(newPayload)
            }
        }

        List<Place> outputPlaces = petriNet.incidenceMatrix.getOutputPlaces(transition.index)
        for (Place p : outputPlaces) {
            int produceWeight = petriNet.incidenceMatrix.getOutputWeight(p.index, transition.index)
            for (int w = 0; w < produceWeight; w++) {
                marking.addToken(p, outputToken)
            }
        }

        return true
    }

    boolean step() {
        List<Transition> enabledTransitions = getEnabledTransitions()
        log.info("step() - enabledTransitions:{}", enabledTransitions)
        if (enabledTransitions.isEmpty()) {
            return false
        }
        return fire(enabledTransitions[0])
    }

    Marking runUntilEnd(RecordToken token = null, ExecutionContext context = null) {
        if (context != null) {
            this.executionContext = context
        }

        if (token != null) {
            this.seedToken(token)
        } else if (isNetEmpty()) {
            this.seedToken(RecordToken.of())
        }

        while (step()) {
            // keep stepping until no enabled transitions remain
        }

        return marking
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

    private boolean isNetEmpty() {
        for (Place p : petriNet.places) {
            if (!marking.isEmpty(p)) {
                return false
            }
        }
        return true
    }
}
