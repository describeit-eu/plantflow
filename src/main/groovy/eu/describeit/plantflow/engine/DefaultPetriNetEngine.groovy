package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ActionHandler
import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.GuardPredicate
import eu.describeit.plantflow.HandlerRegistry
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class DefaultPetriNetEngine implements PetriNetEngine {

    @Override
    boolean isEnabled(PetriNetStructure net, Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        if (transition == null) {
            return false
        }

        List<Place> inputPlaces = getInputPlaces(net, transition.index)
        if (!hasSufficientTokens(net, transition, marking, inputPlaces)) {
            return false
        }

        if (!isGuardSatisfied(transition, marking, handlerRegistry, executionContext, inputPlaces)) {
            return false
        }

        validateAction(transition, handlerRegistry)

        return true
    }

    @Override
    List<Transition> getEnabledTransitions(PetriNetStructure net, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        List<Transition> enabled = []
        for (Transition t : net.transitions) {
            if (isEnabled(net, t, marking, handlerRegistry, executionContext)) {
                enabled.add(t)
            }
        }
        return Collections.unmodifiableList(enabled)
    }

    @Override
    boolean fire(PetriNetStructure net, Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        if (!isEnabled(net, transition, marking, handlerRegistry, executionContext)) {
            return false
        }

        log.info('fire() - {}', transition)

        List<Place> inputPlaces = getInputPlaces(net, transition.index)
        List<Token> consumedTokens = []

        for (Place p : inputPlaces) {
            int requiredWeight = net.incidenceMatrix.getInputWeight(p.index, transition.index)
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

        List<Place> outputPlaces = getOutputPlaces(net, transition.index)
        for (Place p : outputPlaces) {
            int produceWeight = net.incidenceMatrix.getOutputWeight(p.index, transition.index)
            for (int w = 0; w < produceWeight; w++) {
                marking.addToken(p, outputToken)
            }
        }

        return true
    }

    private static Token mergeConsumedTokens(List<Token> consumedTokens) {
        if (consumedTokens.isEmpty()) {
            return Token.of()
        }
        Token merged = consumedTokens.get(0)
        for (int i = 1; i < consumedTokens.size(); i++) {
            merged = merged.deepMerge(consumedTokens.get(i))
        }
        return merged
    }

    protected List<Place> getInputPlaces(PetriNetStructure net, int transitionIndex) {
        List<Place> result = []
        for (Place p : net.places) {
            if (net.incidenceMatrix.getInputWeight(p.index, transitionIndex) > 0) {
                result.add(p)
            }
        }
        return Collections.unmodifiableList(result)
    }

    protected List<Place> getOutputPlaces(PetriNetStructure net, int transitionIndex) {
        List<Place> result = []
        for (Place p : net.places) {
            if (net.incidenceMatrix.getOutputWeight(p.index, transitionIndex) > 0) {
                result.add(p)
            }
        }
        return Collections.unmodifiableList(result)
    }

    private boolean hasSufficientTokens(PetriNetStructure net, Transition transition, Marking marking, List<Place> inputPlaces) {
        for (Place p : inputPlaces) {
            int requiredWeight = net.incidenceMatrix.getInputWeight(p.index, transition.index)
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
        Token baseToken = mergeConsumedTokens(consumedTokens)

        if (transition.actionKey) {
            ActionHandler actionHandler = handlerRegistry.getAction(transition.actionKey)
            Object result = actionHandler.execute(executionContext, baseToken)

            if (result instanceof Token tokenResult) {
                return tokenResult
            } else if (result instanceof Map mapResult) {
                return baseToken.withPayload(mapResult)
            }
        }
        return baseToken
    }
}
