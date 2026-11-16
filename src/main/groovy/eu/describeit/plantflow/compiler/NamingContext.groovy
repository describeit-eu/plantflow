package eu.describeit.plantflow.compiler

import groovy.transform.CompileStatic

/**
 * Single naming context threaded through the recursive chain compiler.
 * One counter source feeds the indices of every Conditional, Repeat Loop,
 * and Fork label, so all Place and Transition labels stay unique at any
 * nesting depth. This is the single place where the uniform label scheme
 * is defined.
 */
@CompileStatic
class NamingContext {

    static final String PLACE_START = 'start'
    static final String PLACE_END = 'end'
    static final String PREFIX_INTERMEDIATE_PLACE = 'P_'

    private static final String PLACE_IF_PREFIX = 'P_if_'
    private static final String TRANSITION_IF_PREFIX = 'T_if_'
    private static final String PLACE_REPEAT_PREFIX = 'P_repeat_'
    private static final String TRANSITION_REPEAT_PREFIX = 'T_repeat_'
    private static final String PLACE_FORK_PREFIX = 'P_fork_'
    private static final String TRANSITION_FORK_PREFIX = 'T_fork_'
    private static final String DECISION_SUFFIX = '_decision'
    private static final String THEN_SUFFIX = '_then'
    private static final String ELSE_SUFFIX = '_else'
    private static final String ENDIF_SUFFIX = '_endif'
    private static final String TO_DECISION_SUFFIX = '_to_decision'
    private static final String BRANCH_YES_SUFFIX = '_yes'
    private static final String BRANCH_NO_SUFFIX = '_no'
    private static final String TO_END_SUFFIX = '_to_end'
    private static final String BODY_SUFFIX = '_body_'
    private static final String LOOP_BACK_SUFFIX = '_loop_back'
    private static final String EXIT_SUFFIX = '_exit'
    private static final String DETACH_SUFFIX = '_detach'
    private static final String BRANCH_PLACE_SUFFIX = '_branch_'
    private static final String BRANCH_END_SUFFIX = '_end'
    private static final String MERGE_SUFFIX = '_merge'
    private static final String JOIN_SUFFIX = '_join'
    private static final String MERGE_TRANSITION_SUFFIX = '_merge_'
    private static final String NEGATION_PREFIX = '!('
    private static final String NEGATION_SUFFIX = ')'

    private int constructCounter = 0

    static String negateGuard(String guard) {
        NEGATION_PREFIX + guard + NEGATION_SUFFIX
    }

    int nextConstructId() {
        return ++constructCounter
    }

    String intermediatePlace(int index) {
        PREFIX_INTERMEDIATE_PLACE + index
    }

    String ifDecisionPlace(int conditionalId) {
        PLACE_IF_PREFIX + conditionalId + DECISION_SUFFIX
    }

    String ifThenPlace(int conditionalId) {
        PLACE_IF_PREFIX + conditionalId + THEN_SUFFIX
    }

    String ifElsePlace(int conditionalId) {
        PLACE_IF_PREFIX + conditionalId + ELSE_SUFFIX
    }

    String ifEndifPlace(int conditionalId) {
        PLACE_IF_PREFIX + conditionalId + ENDIF_SUFFIX
    }

    String ifToDecisionTransition(int conditionalId) {
        TRANSITION_IF_PREFIX + conditionalId + TO_DECISION_SUFFIX
    }

    String ifBranchYesTransition(int conditionalId) {
        TRANSITION_IF_PREFIX + conditionalId + BRANCH_YES_SUFFIX
    }

    String ifBranchNoTransition(int conditionalId) {
        TRANSITION_IF_PREFIX + conditionalId + BRANCH_NO_SUFFIX
    }

    String ifExitTransition(int conditionalId) {
        TRANSITION_IF_PREFIX + conditionalId + TO_END_SUFFIX
    }

    String repeatDecisionPlace(int loopId) {
        PLACE_REPEAT_PREFIX + loopId + DECISION_SUFFIX
    }

    String repeatBodyPlacePrefix(int loopId) {
        PLACE_REPEAT_PREFIX + loopId + BODY_SUFFIX
    }

    String repeatLoopBackTransition(int loopId) {
        TRANSITION_REPEAT_PREFIX + loopId + LOOP_BACK_SUFFIX
    }

    String repeatExitTransition(int loopId) {
        TRANSITION_REPEAT_PREFIX + loopId + EXIT_SUFFIX
    }

    String repeatDetachTransition(int loopId) {
        TRANSITION_REPEAT_PREFIX + loopId + DETACH_SUFFIX
    }

    String forkTransition(int forkId) {
        TRANSITION_FORK_PREFIX + forkId
    }

    String forkBranchPlace(int forkId, int branchNumber) {
        PLACE_FORK_PREFIX + forkId + BRANCH_PLACE_SUFFIX + branchNumber
    }

    String forkBranchEndPlace(int forkId, int branchNumber) {
        forkBranchPlace(forkId, branchNumber) + BRANCH_END_SUFFIX
    }

    String forkMergePlace(int forkId) {
        PLACE_FORK_PREFIX + forkId + MERGE_SUFFIX
    }

    String forkJoinPlace(int forkId) {
        PLACE_FORK_PREFIX + forkId + JOIN_SUFFIX
    }

    String forkMergeTransition(int forkId, int branchNumber) {
        TRANSITION_FORK_PREFIX + forkId + MERGE_TRANSITION_SUFFIX + branchNumber
    }

    String forkJoinTransition(int forkId) {
        TRANSITION_FORK_PREFIX + forkId + JOIN_SUFFIX
    }

    String forkToEndTransition(int forkId) {
        TRANSITION_FORK_PREFIX + forkId + TO_END_SUFFIX
    }
}
