package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

/**
 * Support for synchronizing Joins (`end fork`): a join transition is enabled
 * only when one pending Fork activation has a Token in every branch-end input
 * place; firing consumes exactly that correlated set and deep-merges the Tokens
 * in input-place (diagram branch) order.
 */
@CompileStatic
class ForkJoinSupport {

    private ForkJoinSupport() {
    }

    static JoinDescriptor findJoinDescriptor(PetriNetStructure net, int transitionIndex) {
        for (JoinDescriptor descriptor : net.joinDescriptors) {
            if (descriptor.joinTransitionIndex == transitionIndex) {
                return descriptor
            }
        }
        return null
    }

    static boolean hasCompleteJoinSet(PetriNetStructure net, Transition transition, Marking marking, List<Place> inputPlaces) {
        JoinDescriptor joinDescriptor = findJoinDescriptor(net, transition.index)
        if (joinDescriptor == null) {
            return true
        }
        return findCompleteJoinActivation(marking, inputPlaces, joinDescriptor) != null
    }

    static Integer findCompleteJoinActivation(Marking marking, List<Place> inputPlaces, JoinDescriptor joinDescriptor) {
        List<Integer> pending = marking.getPendingForkActivationIds().get(joinDescriptor.forkTransitionIndex)
        if (pending == null) {
            return null
        }
        for (Integer activationId : pending) {
            boolean complete = true
            for (Place p : inputPlaces) {
                if (findTokenPositionWithActivation(marking, p.index, activationId) < 0) {
                    complete = false
                    break
                }
            }
            if (complete) {
                return activationId
            }
        }
        return null
    }

    static List<Token> consumeJoinInputTokens(PetriNetStructure net, int transitionIndex, Marking marking, List<Place> inputPlaces, Integer activationId) {
        List<Token> consumedTokens = []
        for (Place p : inputPlaces) {
            int requiredWeight = net.incidenceMatrix.getInputWeight(p.index, transitionIndex)
            for (int w = 0; w < requiredWeight; w++) {
                int position = findTokenPositionWithActivation(marking, p.index, activationId)
                if (position < 0) {
                    position = 0
                }
                Token tokenToConsume = marking.removeTokenAt(p.index, position)
                if (tokenToConsume != null) {
                    consumedTokens.add(tokenToConsume)
                }
            }
        }
        return consumedTokens
    }

    private static int findTokenPositionWithActivation(Marking marking, int placeIndex, Integer activationId) {
        if (activationId == null) {
            return -1
        }
        int tokenCount = marking.getTokenCount(placeIndex)
        for (int i = 0; i < tokenCount; i++) {
            if (activationId == marking.getTokenActivation(placeIndex, i)) {
                return i
            }
        }
        return -1
    }
}
