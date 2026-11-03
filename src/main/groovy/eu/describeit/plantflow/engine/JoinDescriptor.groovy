package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

/**
 * Describes one compiled synchronizing Join (`end fork`) in a Petri net:
 * the fork transition that activates the parallel branches and the single
 * multi-input join transition that waits for all branches of one activation.
 */
@CompileStatic
class JoinDescriptor {
    final int forkTransitionIndex
    final int joinTransitionIndex

    JoinDescriptor(int forkTransitionIndex, int joinTransitionIndex) {
        this.forkTransitionIndex = forkTransitionIndex
        this.joinTransitionIndex = joinTransitionIndex
    }
}
