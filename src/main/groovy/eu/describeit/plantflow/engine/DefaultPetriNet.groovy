package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ActionHandler
import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.GuardPredicate
import eu.describeit.plantflow.HandlerRegistry
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
    final Marking marking

    DefaultPetriNet(List<Place> places, List<Transition> transitions, IncidenceMatrix incidenceMatrix, Place startPlace, Place endPlace) {
        this.places = Collections.unmodifiableList(new ArrayList<>(places))
        this.transitions = Collections.unmodifiableList(new ArrayList<>(transitions))
        this.incidenceMatrix = incidenceMatrix
        this.startPlace = startPlace
        this.endPlace = endPlace
        this.marking = new Marking(this.places)
    }

    @Override
    boolean isEnabled(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        if (transition == null) {
            return false
        }

        List<Place> inputPlaces = incidenceMatrix.getInputPlaces(transition.index)
        if (!hasSufficientTokens(transition, marking, inputPlaces)) {
            return false
        }

        if (!isGuardSatisfied(transition, marking, handlerRegistry, executionContext, inputPlaces)) {
            return false
        }

        validateAction(transition, handlerRegistry)

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

        List<Place> inputPlaces = incidenceMatrix.getInputPlaces(transition.index)
        List<Token> consumedTokens = []

        for (Place p : inputPlaces) {
            int requiredWeight = incidenceMatrix.getInputWeight(p.index, transition.index)
            for (int w = 0; w < requiredWeight; w++) {
                List<Token> available = marking.getTokens(p)
                if (!available.isEmpty()) {
                    Token tokenToConsume = available.get(0)
                    marking.removeToken(p, tokenToConsume)
                    consumedTokens.add(tokenToConsume)
                }
            }
        }

        Token outputToken = getActionOutputToken(consumedTokens, transition, handlerRegistry, executionContext)

        List<Place> outputPlaces = incidenceMatrix.getOutputPlaces(transition.index)
        for (Place p : outputPlaces) {
            int produceWeight = incidenceMatrix.getOutputWeight(p.index, transition.index)
            for (int w = 0; w < produceWeight; w++) {
                marking.addToken(p, outputToken)
            }
        }

        return true
    }

    private boolean hasSufficientTokens(Transition transition, Marking marking, List<Place> inputPlaces) {
        for (Place p : inputPlaces) {
            int requiredWeight = incidenceMatrix.getInputWeight(p.index, transition.index)
            if (marking.getTokenCount(p) < requiredWeight) {
                return false
            }
        }
        return true
    }

    private boolean isGuardSatisfied(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext, List<Place> inputPlaces) {
        if (transition.guardKey) {
            GuardPredicate guard = handlerRegistry.getGuard(transition.guardKey)
            Token tokenForGuard = getGuardToken(marking, inputPlaces)
            return guard.evaluate(executionContext, tokenForGuard)
        }
        return true
    }

    private Token getGuardToken(Marking marking, List<Place> inputPlaces) {
        if (!inputPlaces.isEmpty()) {
            List<Token> tokens = marking.getTokens(inputPlaces.get(0))
            if (!tokens.isEmpty()) {
                return tokens.get(0)
            }
        }
        return null
    }

    private void validateAction(Transition transition, HandlerRegistry handlerRegistry) {
        if (transition.actionKey) {
            handlerRegistry.getAction(transition.actionKey)
        }
    }

    private Token getActionOutputToken(List<Token> consumedTokens, Transition transition, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        Token emptyToken = consumedTokens.isEmpty() ? Token.of() : consumedTokens.get(0)

        if (transition.actionKey) {
            ActionHandler actionHandler = handlerRegistry.getAction(transition.actionKey)
            Object result = actionHandler.execute(executionContext, emptyToken)

            if (result instanceof Token tokenResult) {
                return tokenResult
            } else if (result instanceof Map mapResult) {
                return emptyToken.withPayload(mapResult)
            }
        }
        return emptyToken
    }
}
