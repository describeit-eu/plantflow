package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

/**
 * Describes one compiled synchronizing Join (`end fork`) in a Petri net:
 * the fork transition that activates the parallel branches, the single
 * multi-input join transition that waits for all branches of one activation,
 * and the branch-end input place indexes of that join transition in diagram
 * (branch) order, which defines the deep-merge order of branch Tokens.
 */
@CompileStatic
class JoinDescriptor {
    final int forkTransitionIndex
    final int joinTransitionIndex
    final List<Integer> branchEndPlaceIndexes

    JoinDescriptor(int forkTransitionIndex, int joinTransitionIndex) {
        this(forkTransitionIndex, joinTransitionIndex, Collections.<Integer>emptyList())
    }

    JoinDescriptor(int forkTransitionIndex, int joinTransitionIndex, List<Integer> branchEndPlaceIndexes) {
        this.forkTransitionIndex = forkTransitionIndex
        this.joinTransitionIndex = joinTransitionIndex
        this.branchEndPlaceIndexes = Collections.unmodifiableList(new ArrayList<>(branchEndPlaceIndexes))
    }
}
