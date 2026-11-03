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

        if (!hasForkContinuationAvailable(net, transition, marking, inputPlaces)) {
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
        int[] pendingMergeToken = findPendingMergeToken(net, transition, marking, inputPlaces)
        ConsumedInputs consumed = consumeInputTokens(net, transition.index, marking, pendingMergeToken)

        Token outputToken = getActionOutputToken(consumed.tokens, transition, handlerRegistry, executionContext)
        Integer outputActivationId = resolveOutputActivationId(net, transition, consumed.activations, marking)

        List<Place> outputPlaces = getOutputPlaces(net, transition.index)
        for (Place p : outputPlaces) {
            int produceWeight = net.incidenceMatrix.getOutputWeight(p.index, transition.index)
            for (int w = 0; w < produceWeight; w++) {
                marking.addToken(p, outputToken, outputActivationId)
            }
        }

        updateForkActivationState(net, transition, marking, consumed.activations)

        return true
    }

    private static ConsumedInputs consumeInputTokens(PetriNetStructure net, int transitionIndex, Marking marking, int[] pendingMergeToken) {
        ConsumedInputs consumed = new ConsumedInputs()
        for (Place p : getInputPlaces(net, transitionIndex)) {
            int requiredWeight = net.incidenceMatrix.getInputWeight(p.index, transitionIndex)
            for (int w = 0; w < requiredWeight; w++) {
                int position = 0
                if (pendingMergeToken.length > 0 && pendingMergeToken[0] == p.index && w == 0) {
                    position = pendingMergeToken[1]
                }
                Integer activationId = marking.getTokenActivation(p.index, position)
                Token tokenToConsume = marking.removeTokenAt(p.index, position)
                if (tokenToConsume != null) {
                    consumed.tokens.add(tokenToConsume)
                    consumed.activations.add(activationId)
                }
            }
        }
        return consumed
    }

    private static boolean hasForkContinuationAvailable(PetriNetStructure net, Transition transition, Marking marking, List<Place> inputPlaces) {
        if (findMergeForkIndex(net, transition.index) == null) {
            return true
        }
        return findPendingMergeToken(net, transition, marking, inputPlaces).length > 0
    }

    private static int[] findPendingMergeToken(PetriNetStructure net, Transition transition, Marking marking, List<Place> inputPlaces) {
        Integer forkIndex = findMergeForkIndex(net, transition.index)
        if (forkIndex == null) {
            return new int[0]
        }
        for (Place p : inputPlaces) {
            int tokenCount = marking.getTokenCount(p.index)
            for (int i = 0; i < tokenCount; i++) {
                Integer activationId = marking.getTokenActivation(p.index, i)
                if (activationId != null && marking.isForkActivationPending(forkIndex, activationId)) {
                    return [p.index, i] as int[]
                }
            }
        }
        return new int[0]
    }

    private static class ConsumedInputs {
        List<Token> tokens = []
        List<Integer> activations = []
    }

    private static Integer resolveOutputActivationId(PetriNetStructure net, Transition transition, List<Integer> consumedActivations, Marking marking) {
        for (ForkDescriptor descriptor : net.forkDescriptors) {
            if (descriptor.forkTransitionIndex == transition.index) {
                return marking.registerForkActivation(descriptor.forkTransitionIndex)
            }
        }
        return firstNonNullActivation(consumedActivations)
    }

    private static Integer firstNonNullActivation(List<Integer> consumedActivations) {
        for (Integer activationId : consumedActivations) {
            if (activationId != null) {
                return activationId
            }
        }
        return null
    }

    private static void updateForkActivationState(PetriNetStructure net, Transition transition, Marking marking, List<Integer> consumedActivations) {
        for (ForkDescriptor descriptor : net.forkDescriptors) {
            if (descriptor.isMergeTransition(transition.index)) {
                Integer activationId = firstNonNullActivation(consumedActivations)
                if (activationId != null) {
                    marking.selectForkActivation(descriptor.forkTransitionIndex, activationId)
                }
            }
        }
    }

    private static Integer findMergeForkIndex(PetriNetStructure net, int transitionIndex) {
        for (ForkDescriptor descriptor : net.forkDescriptors) {
            if (descriptor.isMergeTransition(transitionIndex)) {
                return descriptor.forkTransitionIndex
            }
        }
        return null
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

    protected static List<Place> getInputPlaces(PetriNetStructure net, int transitionIndex) {
        List<Place> result = []
        for (Place p : net.places) {
            if (net.incidenceMatrix.getInputWeight(p.index, transitionIndex) > 0) {
                result.add(p)
            }
        }
        return Collections.unmodifiableList(result)
    }

    protected static List<Place> getOutputPlaces(PetriNetStructure net, int transitionIndex) {
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
