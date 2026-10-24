package eu.describeit.plantflow.compiler

import spock.lang.Specification
import spock.lang.Unroll

class NetLabelingSpec extends Specification {

    def 'should have correct place label constants'() {
        expect:
        NetLabeling.PLACE_START == 'start'
        NetLabeling.PLACE_END == 'end'
        NetLabeling.PLACE_IF_DECISION == 'P_if_decision'
        NetLabeling.PLACE_THEN == 'P_then'
        NetLabeling.PLACE_ELSE == 'P_else'
        NetLabeling.PLACE_ENDIF == 'P_endif'
        NetLabeling.PLACE_FORK_PREFIX == 'P_fork_'
        NetLabeling.PLACE_FORK_BRANCH_SUFFIX == '_branch_'
        NetLabeling.PLACE_FORK_MERGE_SUFFIX == '_merge'
    }

    def 'should have correct transition label constants'() {
        expect:
        NetLabeling.TRANSITION_START_TO_DECISION == 'T_start_to_decision'
        NetLabeling.TRANSITION_BRANCH_YES == 'T_branch_yes'
        NetLabeling.TRANSITION_BRANCH_NO == 'T_branch_no'
        NetLabeling.TRANSITION_ENDIF_TO_END == 'T_endif_to_end'
        NetLabeling.TRANSITION_FORK_PREFIX == 'T_fork_'
        NetLabeling.TRANSITION_FORK_BRANCH_SUFFIX == '_branch_'
        NetLabeling.TRANSITION_FORK_MERGE_SYNC == '_merge_sync'
        NetLabeling.TRANSITION_FORK_TO_END == '_to_end'
    }

    def 'should have correct prefix and suffix constants'() {
        expect:
        NetLabeling.PREFIX_INTERMEDIATE_PLACE == 'P_'
        NetLabeling.PREFIX_THEN_PLACE == 'P_then_'
        NetLabeling.PREFIX_ELSE_PLACE == 'P_else_'
        NetLabeling.PLACE_FORK_POST_MERGE_SUFFIX == '_post_'
        NetLabeling.NEGATION_PREFIX == '!('
        NetLabeling.NEGATION_SUFFIX == ')'
    }

    @Unroll
    def 'intermediatePlace(#index) should return P_#index'() {
        expect:
        NetLabeling.intermediatePlace(index) == expected

        where:
        index | expected
        1     | 'P_1'
        2     | 'P_2'
        5     | 'P_5'
    }

    @Unroll
    def 'thenPlace(#index) should return P_then_#index'() {
        expect:
        NetLabeling.thenPlace(index) == expected

        where:
        index | expected
        1     | 'P_then_1'
        2     | 'P_then_2'
    }

    @Unroll
    def 'elsePlace(#index) should return P_else_#index'() {
        expect:
        NetLabeling.elsePlace(index) == expected

        where:
        index | expected
        1     | 'P_else_1'
        3     | 'P_else_3'
    }

    @Unroll
    def 'forkBranchPlace(#forkIndex, #branchIndex) should return expected label'() {
        expect:
        NetLabeling.forkBranchPlace(forkIndex, branchIndex) == expected

        where:
        forkIndex | branchIndex | expected
        1         | 1           | 'P_fork_1_branch_1'
        1         | 2           | 'P_fork_1_branch_2'
        2         | 3           | 'P_fork_2_branch_3'
    }

    @Unroll
    def 'forkMergePlace(#forkIndex) should return expected label'() {
        expect:
        NetLabeling.forkMergePlace(forkIndex) == expected

        where:
        forkIndex | expected
        1         | 'P_fork_1_merge'
        2         | 'P_fork_2_merge'
    }

    @Unroll
    def 'forkBranchTransition(#forkIndex, #branchIndex) should return expected label'() {
        expect:
        NetLabeling.forkBranchTransition(forkIndex, branchIndex) == expected

        where:
        forkIndex | branchIndex | expected
        1         | 1           | 'T_fork_1_branch_1'
        1         | 2           | 'T_fork_1_branch_2'
    }

    @Unroll
    def 'forkTransition(#forkIndex) should return expected label'() {
        expect:
        NetLabeling.forkTransition(forkIndex) == expected

        where:
        forkIndex | expected
        1         | 'T_fork_1'
        2         | 'T_fork_2'
    }

    @Unroll
    def 'forkMergeSyncTransition(#forkIndex) should return expected label'() {
        expect:
        NetLabeling.forkMergeSyncTransition(forkIndex) == expected

        where:
        forkIndex | expected
        1         | 'T_fork_1_merge_sync'
    }

    @Unroll
    def 'forkToEndTransition(#forkIndex) should return expected label'() {
        expect:
        NetLabeling.forkToEndTransition(forkIndex) == expected

        where:
        forkIndex | expected
        1         | 'T_fork_1_to_end'
    }

    @Unroll
    def 'postMergePlace(#forkIndex, #index) should return expected label'() {
        expect:
        NetLabeling.postMergePlace(forkIndex, index) == expected

        where:
        forkIndex | index | expected
        1         | 1     | 'P_fork_1_post_1'
        1         | 2     | 'P_fork_1_post_2'
    }

    @Unroll
    def 'negateGuard(#guard) should return !(#guard)'() {
        expect:
        NetLabeling.negateGuard(guard) == expected

        where:
        guard         | expected
        'isValid'     | '!(isValid)'
        'hasPermission' | '!(hasPermission)'
    }
}
