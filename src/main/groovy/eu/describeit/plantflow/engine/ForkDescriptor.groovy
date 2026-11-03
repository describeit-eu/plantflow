package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

/**
 * Describes one compiled Fork structure in a Petri net: the fork transition
 * that activates the parallel branches and the per-branch merge transitions
 * that route the first completed branch onward.
 */
@CompileStatic
class ForkDescriptor {
    final int forkTransitionIndex
    final List<Integer> mergeTransitionIndexes

    ForkDescriptor(int forkTransitionIndex, List<Integer> mergeTransitionIndexes) {
        this.forkTransitionIndex = forkTransitionIndex
        this.mergeTransitionIndexes = Collections.unmodifiableList(new ArrayList<>(mergeTransitionIndexes))
    }

    boolean isMergeTransition(int transitionIndex) {
        return mergeTransitionIndexes.contains(transitionIndex)
    }
}
