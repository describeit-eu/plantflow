package eu.describeit.plantflow.compiler

import groovy.transform.CompileStatic

/**
 * Single naming context threaded through the recursive chain compiler.
 * One counter source feeds the indices of every Repeat Loop, Conditional,
 * and Fork label, so all Place and Transition labels stay unique at any
 * nesting depth. This is the single place where the uniform label scheme
 * is defined.
 */
@CompileStatic
class NamingContext {

    private static final String PLACE_IF_PREFIX = 'P_if_'
    private static final String TRANSITION_IF_PREFIX = 'T_if_'

    private int constructCounter = 0

    int nextConstructId() {
        return ++constructCounter
    }

    String ifDecisionPlace(int conditionalId) {
        PLACE_IF_PREFIX + conditionalId + '_decision'
    }

    String ifThenPlace(int conditionalId) {
        PLACE_IF_PREFIX + conditionalId + '_then'
    }

    String ifElsePlace(int conditionalId) {
        PLACE_IF_PREFIX + conditionalId + '_else'
    }

    String ifEndifPlace(int conditionalId) {
        PLACE_IF_PREFIX + conditionalId + '_endif'
    }

    String ifToDecisionTransition(int conditionalId) {
        TRANSITION_IF_PREFIX + conditionalId + '_to_decision'
    }

    String ifBranchYesTransition(int conditionalId) {
        TRANSITION_IF_PREFIX + conditionalId + '_yes'
    }

    String ifBranchNoTransition(int conditionalId) {
        TRANSITION_IF_PREFIX + conditionalId + '_no'
    }

    String ifExitTransition(int conditionalId) {
        TRANSITION_IF_PREFIX + conditionalId + '_to_end'
    }

    String forkTransition(int forkId) {
        NetLabeling.forkTransition(forkId)
    }

    String forkBranchPlace(int forkId, int branchNumber) {
        NetLabeling.forkBranchPlace(forkId, branchNumber)
    }

    String forkBranchEndPlace(int forkId, int branchNumber) {
        NetLabeling.forkBranchPlace(forkId, branchNumber) + '_end'
    }

    String forkMergePlace(int forkId) {
        NetLabeling.forkMergePlace(forkId)
    }

    String forkJoinPlace(int forkId) {
        NetLabeling.forkJoinPlace(forkId)
    }

    String forkMergeTransition(int forkId, int branchNumber) {
        NetLabeling.forkMergeTransition(forkId, branchNumber)
    }

    String forkJoinTransition(int forkId) {
        NetLabeling.forkJoinTransition(forkId)
    }

    String forkToEndTransition(int forkId) {
        NetLabeling.forkToEndTransition(forkId)
    }
}
