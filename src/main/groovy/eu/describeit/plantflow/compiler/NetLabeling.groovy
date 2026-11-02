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

    static final String TRANSITION_START_TO_DECISION = 'T_start_to_decision'
    static final String TRANSITION_BRANCH_YES = 'T_branch_yes'
    static final String TRANSITION_BRANCH_NO = 'T_branch_no'
    static final String TRANSITION_ENDIF_TO_END = 'T_endif_to_end'
    static final String TRANSITION_FORK_PREFIX = 'T_fork_'
    static final String TRANSITION_FORK_BRANCH_SUFFIX = '_branch_'
    static final String TRANSITION_FORK_MERGE_SYNC = '_merge_sync'
    static final String TRANSITION_FORK_TO_END = '_to_end'

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

    static String forkBranchTransition(int forkIndex, int branchIndex) {
        TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_BRANCH_SUFFIX + branchIndex
    }

    static String forkTransition(int forkIndex) {
        TRANSITION_FORK_PREFIX + forkIndex
    }

    static String forkMergeSyncTransition(int forkIndex) {
        TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_MERGE_SYNC
    }

    static String forkToEndTransition(int forkIndex) {
        TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_TO_END
    }

    static String postMergePlace(int forkIndex, int index) {
        PLACE_FORK_PREFIX + forkIndex + PLACE_FORK_POST_MERGE_SUFFIX + index
    }

    static String negateGuard(String guard) {
        NEGATION_PREFIX + guard + NEGATION_SUFFIX
    }
}
