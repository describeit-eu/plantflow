package eu.describeit.plantflow.compiler

import spock.lang.Specification
import spock.lang.Unroll

/**
 * The single naming context defines the one uniform Place/Transition label
 * scheme: every construct id comes from one shared counter, so labels stay
 * unique at any nesting depth.
 */
class NamingContextSpec extends Specification {

    def naming = new NamingContext()

    def 'construct ids come from one shared counter across conditionals, loops and forks'() {
        expect:
        naming.nextConstructId() == 1
        naming.nextConstructId() == 2
        naming.nextConstructId() == 3
    }

    def 'start, end and intermediate place labels use the uniform scheme'() {
        expect:
        NamingContext.PLACE_START == 'start'
        NamingContext.PLACE_END == 'end'
        NamingContext.PREFIX_INTERMEDIATE_PLACE == 'P_'
        naming.intermediatePlace(1) == 'P_1'
        naming.intermediatePlace(2) == 'P_2'
    }

    @Unroll
    def 'conditional labels for id #id use the uniform if scheme'() {
        expect:
        naming.ifDecisionPlace(id) == "P_if_${id}_decision"
        naming.ifThenPlace(id) == "P_if_${id}_then"
        naming.ifElsePlace(id) == "P_if_${id}_else"
        naming.ifEndifPlace(id) == "P_if_${id}_endif"
        naming.ifToDecisionTransition(id) == "T_if_${id}_to_decision"
        naming.ifBranchYesTransition(id) == "T_if_${id}_yes"
        naming.ifBranchNoTransition(id) == "T_if_${id}_no"
        naming.ifExitTransition(id) == "T_if_${id}_to_end"

        where:
        id << [1, 2, 7]
    }

    @Unroll
    def 'repeat loop labels for id #id use the uniform repeat scheme'() {
        expect:
        naming.repeatDecisionPlace(id) == "P_repeat_${id}_decision"
        naming.repeatBodyPlacePrefix(id) == "P_repeat_${id}_body_"
        naming.repeatLoopBackTransition(id) == "T_repeat_${id}_loop_back"
        naming.repeatExitTransition(id) == "T_repeat_${id}_exit"
        naming.repeatDetachTransition(id) == "T_repeat_${id}_detach"

        where:
        id << [1, 2, 7]
    }

    @Unroll
    def 'fork labels for id #id and branch #branch use the uniform fork scheme'() {
        expect:
        naming.forkTransition(id) == "T_fork_${id}"
        naming.forkBranchPlace(id, branch) == "P_fork_${id}_branch_${branch}"
        naming.forkBranchEndPlace(id, branch) == "P_fork_${id}_branch_${branch}_end"
        naming.forkMergePlace(id) == "P_fork_${id}_merge"
        naming.forkJoinPlace(id) == "P_fork_${id}_join"
        naming.forkMergeTransition(id, branch) == "T_fork_${id}_merge_${branch}"
        naming.forkJoinTransition(id) == "T_fork_${id}_join"
        naming.forkToEndTransition(id) == "T_fork_${id}_to_end"

        where:
        id << [1, 2, 7]
        branch << [1, 2, 3]
    }

    def 'all labels generated from one context are unique across constructs'() {
        given:
        def labels = [] as Set<String>

        when: 'a conditional, a loop and a fork each draw their id from the same counter'
        int ifId = naming.nextConstructId()
        int loopId = naming.nextConstructId()
        int forkId = naming.nextConstructId()
        labels.addAll([
            naming.ifDecisionPlace(ifId), naming.ifThenPlace(ifId), naming.ifElsePlace(ifId), naming.ifEndifPlace(ifId),
            naming.ifToDecisionTransition(ifId), naming.ifBranchYesTransition(ifId), naming.ifBranchNoTransition(ifId),
            naming.ifExitTransition(ifId),
            naming.repeatDecisionPlace(loopId), naming.repeatBodyPlacePrefix(loopId),
            naming.repeatLoopBackTransition(loopId), naming.repeatExitTransition(loopId),
            naming.forkTransition(forkId), naming.forkBranchPlace(forkId, 1), naming.forkBranchPlace(forkId, 2),
            naming.forkBranchEndPlace(forkId, 1), naming.forkBranchEndPlace(forkId, 2),
            naming.forkMergePlace(forkId), naming.forkMergeTransition(forkId, 1), naming.forkMergeTransition(forkId, 2),
            naming.forkToEndTransition(forkId),
        ])

        then: 'no label collides even though all ids differ only through the shared counter'
        labels.size() == 21
    }

    def 'negated guard labels wrap the guard in the uniform negation scheme'() {
        expect:
        NamingContext.negateGuard('more data?') == '!(more data?)'
    }
}
