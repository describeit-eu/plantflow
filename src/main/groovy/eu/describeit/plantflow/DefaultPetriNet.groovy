package eu.describeit.plantflow

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class DefaultPetriNet implements PetriNet {
    final List<Place> places
    final List<Transition> transitions
    final IncidenceMatrix incidenceMatrix
    final Place startPlace
    final Place endPlace

    DefaultPetriNet(List<Place> places, List<Transition> transitions, IncidenceMatrix incidenceMatrix, Place startPlace, Place endPlace) {
        this.places = Collections.unmodifiableList(new ArrayList<>(places))
        this.transitions = Collections.unmodifiableList(new ArrayList<>(transitions))
        this.incidenceMatrix = incidenceMatrix
        this.startPlace = startPlace
        this.endPlace = endPlace
    }

    @Override
    List<Place> getPlaces() {
        return places
    }

    @Override
    List<Transition> getTransitions() {
        return transitions
    }

    @Override
    Place getStartPlace() {
        return startPlace
    }

    @Override
    Place getEndPlace() {
        return endPlace
    }

    @Override
    boolean isEnabled(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        if (transition == null) return false

        List<Place> inputPlaces = incidenceMatrix.getInputPlaces(transition.index)
        for (Place p : inputPlaces) {
            int requiredWeight = incidenceMatrix.getInputWeight(p.index, transition.index)
            if (marking.getTokenCount(p) < requiredWeight) {
                return false
            }
        }

        if (transition.guardKey != null && !transition.guardKey.isEmpty()) {
            GuardPredicate guard = handlerRegistry.getGuard(transition.guardKey)
            RecordToken tokenForGuard = null
            if (!inputPlaces.isEmpty()) {
                List<RecordToken> tokens = marking.getTokens(inputPlaces.get(0))
                if (!tokens.isEmpty()) {
                    tokenForGuard = tokens.get(0)
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

    @Override
    List<Transition> getEnabledTransitions(Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        List<Transition> enabled = []
        for (Transition t : transitions) {
            if (isEnabled(t, marking, handlerRegistry, executionContext)) {
                enabled.add(t)
            }
        }
        return Collections.unmodifiableList(enabled)
    }

    @Override
    boolean fire(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        if (!isEnabled(transition, marking, handlerRegistry, executionContext)) {
            return false
        }

        log.info('fire() - {}', transition)

        ActionHandler actionHandler = null
        if (transition.actionKey != null && !transition.actionKey.isEmpty()) {
            actionHandler = handlerRegistry.getAction(transition.actionKey)
        }

        List<Place> inputPlaces = incidenceMatrix.getInputPlaces(transition.index)
        List<RecordToken> consumedTokens = []

        for (Place p : inputPlaces) {
            int requiredWeight = incidenceMatrix.getInputWeight(p.index, transition.index)
            for (int w = 0; w < requiredWeight; w++) {
                List<RecordToken> available = marking.getTokens(p)
                if (!available.isEmpty()) {
                    RecordToken tokenToConsume = available.get(0)
                    marking.removeToken(p, tokenToConsume)
                    consumedTokens.add(tokenToConsume)
                }
            }
        }

        RecordToken primaryToken = consumedTokens.isEmpty() ? RecordToken.of() : consumedTokens.get(0)
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

        List<Place> outputPlaces = incidenceMatrix.getOutputPlaces(transition.index)
        for (Place p : outputPlaces) {
            int produceWeight = incidenceMatrix.getOutputWeight(p.index, transition.index)
            for (int w = 0; w < produceWeight; w++) {
                marking.addToken(p, outputToken)
            }
        }

        return true
    }
}
