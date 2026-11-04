package eu.describeit.plantflow.compiler

import groovy.transform.CompileStatic

@CompileStatic
class NetLabeling {

    static final String PLACE_START = 'start'
    static final String PLACE_END = 'end'
    static final String PLACE_IF_DECISION = 'P_if_decision'
    static final String PLACE_THEN = 'P_then'
    static final String PLACE_ELSE = 'P_else'
    static final String PLACE_ENDIF = 'P_endif'
    static final String PLACE_FORK_PREFIX = 'P_fork_'
    static final String PLACE_FORK_BRANCH_SUFFIX = '_branch_'
    static final String PLACE_FORK_MERGE_SUFFIX = '_merge'
    static final String PLACE_FORK_JOIN_SUFFIX = '_join'

    static final String TRANSITION_START_TO_DECISION = 'T_start_to_decision'
    static final String TRANSITION_BRANCH_YES = 'T_branch_yes'
    static final String TRANSITION_BRANCH_NO = 'T_branch_no'
    static final String TRANSITION_ENDIF_TO_END = 'T_endif_to_end'
    static final String TRANSITION_FORK_PREFIX = 'T_fork_'
    static final String TRANSITION_FORK_BRANCH_SUFFIX = '_branch_'
    static final String TRANSITION_FORK_MERGE_SUFFIX = '_merge_'
    static final String TRANSITION_FORK_JOIN_SUFFIX = '_join'
    static final String TRANSITION_FORK_TO_END = '_to_end'
    static final String PLACE_REPEAT_PREFIX = 'P_repeat_'
    static final String PLACE_REPEAT_START_SUFFIX = '_start'
    static final String PLACE_REPEAT_DECISION_SUFFIX = '_decision'
    static final String PLACE_REPEAT_BODY_SUFFIX = '_body_'
    static final String PLACE_REPEAT_AFTER_SUFFIX = '_after'
    static final String TRANSITION_REPEAT_PREFIX = 'T_repeat_'
    static final String TRANSITION_REPEAT_LOOP_BACK_SUFFIX = '_loop_back'
    static final String TRANSITION_REPEAT_EXIT_SUFFIX = '_exit'
    static final String TRANSITION_REPEAT_DETACH_SUFFIX = '_detach'

    static final String PREFIX_INTERMEDIATE_PLACE = 'P_'
    static final String PREFIX_THEN_PLACE = 'P_then_'
    static final String PREFIX_ELSE_PLACE = 'P_else_'
    static final String PLACE_FORK_POST_MERGE_SUFFIX = '_post_'
    static final String NEGATION_PREFIX = '!('
    static final String NEGATION_SUFFIX = ')'

    static String intermediatePlace(int index) {
        PREFIX_INTERMEDIATE_PLACE + index
    }

    static String thenPlace(int index) {
        PREFIX_THEN_PLACE + index
    }

    static String elsePlace(int index) {
        PREFIX_ELSE_PLACE + index
    }

    static String forkBranchPlace(int forkIndex, int branchIndex) {
        PLACE_FORK_PREFIX + forkIndex + PLACE_FORK_BRANCH_SUFFIX + branchIndex
    }

    static String forkMergePlace(int forkIndex) {
        PLACE_FORK_PREFIX + forkIndex + PLACE_FORK_MERGE_SUFFIX
    }

    static String forkJoinPlace(int forkIndex) {
        PLACE_FORK_PREFIX + forkIndex + PLACE_FORK_JOIN_SUFFIX
    }

    static String forkBranchTransition(int forkIndex, int branchIndex) {
        TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_BRANCH_SUFFIX + branchIndex
    }

    static String forkTransition(int forkIndex) {
        TRANSITION_FORK_PREFIX + forkIndex
    }

    static String forkMergeTransition(int forkIndex, int branchNumber) {
        TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_MERGE_SUFFIX + branchNumber
    }

    static String forkJoinTransition(int forkIndex) {
        TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_JOIN_SUFFIX
    }

    static String forkToEndTransition(int forkIndex) {
        TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_TO_END
    }

    static String postMergePlace(int forkIndex, int index) {
        PLACE_FORK_PREFIX + forkIndex + PLACE_FORK_POST_MERGE_SUFFIX + index
    }

    static String repeatStartPlace(int loopIndex) {
        PLACE_REPEAT_PREFIX + loopIndex + PLACE_REPEAT_START_SUFFIX
    }

    static String repeatDecisionPlace(int loopIndex) {
        PLACE_REPEAT_PREFIX + loopIndex + PLACE_REPEAT_DECISION_SUFFIX
    }

    static String repeatBodyPlace(int loopIndex, int actionNumber) {
        PLACE_REPEAT_PREFIX + loopIndex + PLACE_REPEAT_BODY_SUFFIX + actionNumber
    }

    static String repeatAfterPlace(int loopIndex) {
        PLACE_REPEAT_PREFIX + loopIndex + PLACE_REPEAT_AFTER_SUFFIX
    }

    static String repeatAfterPlace(int loopIndex, int index) {
        PLACE_REPEAT_PREFIX + loopIndex + PLACE_REPEAT_AFTER_SUFFIX + '_' + index
    }

    static String repeatLoopBackTransition(int loopIndex) {
        TRANSITION_REPEAT_PREFIX + loopIndex + TRANSITION_REPEAT_LOOP_BACK_SUFFIX
    }

    static String repeatExitTransition(int loopIndex) {
        TRANSITION_REPEAT_PREFIX + loopIndex + TRANSITION_REPEAT_EXIT_SUFFIX
    }

    static String repeatDetachTransition(int loopIndex) {
        TRANSITION_REPEAT_PREFIX + loopIndex + TRANSITION_REPEAT_DETACH_SUFFIX
    }

    static String negateGuard(String guard) {
        NEGATION_PREFIX + guard + NEGATION_SUFFIX
    }
}
