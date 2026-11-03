package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

/**
 * Support for synchronizing Joins (`end fork`): a join transition is enabled
 * only when one pending Fork activation has a Token in every branch-end input
 * place; firing consumes exactly that correlated set and the Tokens are
 * deep-merged in the diagram branch order carried by the JoinDescriptor.
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

    static List<Token> consumeJoinInputTokens(PetriNetStructure net, JoinDescriptor joinDescriptor, Marking marking, List<Place> inputPlaces, Integer activationId) {
        List<Place> orderedInputs = orderBranchEndInputs(joinDescriptor, inputPlaces)
        List<Integer> positions = []
        for (Place p : orderedInputs) {
            int requiredWeight = net.incidenceMatrix.getInputWeight(p.index, joinDescriptor.joinTransitionIndex)
            for (int w = 0; w < requiredWeight; w++) {
                int position = findTokenPositionWithActivation(marking, p.index, activationId)
                if (position < 0) {
                    throw new IllegalStateException(
                        "No token correlated with fork activation ${activationId} in place '${p.label}' " +
                            "for join transition ${joinDescriptor.joinTransitionIndex}")
                }
                positions.add(position)
            }
        }
        List<Token> consumedTokens = []
        for (int i = 0; i < orderedInputs.size(); i++) {
            Place p = orderedInputs.get(i)
            int requiredWeight = net.incidenceMatrix.getInputWeight(p.index, joinDescriptor.joinTransitionIndex)
            for (int w = 0; w < requiredWeight; w++) {
                Token tokenToConsume = marking.removeTokenAt(p.index, positions.remove(0))
                if (tokenToConsume != null) {
                    consumedTokens.add(tokenToConsume)
                }
            }
        }
        return consumedTokens
    }

    private static List<Place> orderBranchEndInputs(JoinDescriptor joinDescriptor, List<Place> inputPlaces) {
        List<Integer> diagramOrder = joinDescriptor.branchEndPlaceIndexes
        if (diagramOrder == null || diagramOrder.isEmpty()) {
            return inputPlaces
        }
        List<Place> remaining = new ArrayList<>(inputPlaces)
        List<Place> ordered = []
        for (Integer placeIndex : diagramOrder) {
            Place match = remaining.find { Place p -> p.index == placeIndex }
            if (match != null) {
                ordered.add(match)
                remaining.remove(match)
            }
        }
        ordered.addAll(remaining)
        return ordered
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
        // Markings deserialized from the legacy count-based format carry untagged
        // tokens; adopt the first untagged token in place order for this activation.
        for (int i = 0; i < tokenCount; i++) {
            if (marking.getTokenActivation(placeIndex, i) == null) {
                return i
            }
        }
        return -1
    }
}
