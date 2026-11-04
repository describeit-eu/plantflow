package eu.describeit.plantflow.engine

import spock.lang.Specification

/**
 * Focused tests for synchronizing Join token consumption: the correlated set
 * of one Fork activation is consumed in diagram branch order, and a missing
 * correlation never consumes another activation's Token.
 */
class ForkJoinSupportSpec extends Specification {

    private PetriNetStructure buildJoinNet() {
        PetriNetBuilder builder = new PetriNetBuilder()
        Place start = builder.addPlace('start')
        Transition fork = builder.addTransition('T_fork_1')
        builder.connect(start, fork)
        Place branch1End = builder.addPlace('P_fork_1_branch_1_end')
        Place branch2End = builder.addPlace('P_fork_1_branch_2_end')
        builder.connect(fork, branch1End)
        builder.connect(fork, branch2End)
        Transition join = builder.addTransition('T_fork_1_join')
        builder.connect(branch1End, join)
        builder.connect(branch2End, join)
        Place joinPlace = builder.addPlace('P_fork_1_join')
        builder.connect(join, joinPlace)
        Place end = builder.addPlace('end')
        Transition toEnd = builder.addTransition('T_fork_1_to_end')
        builder.connect(joinPlace, toEnd)
        builder.connect(toEnd, end)
        builder.setStartPlace(start)
        builder.setEndPlace(end)
        builder.addJoinDescriptor(new JoinDescriptor(fork.index, join.index, [branch1End.index, branch2End.index]))
        return builder.build()
    }

    private Marking markingWithTwoActivations(PetriNetStructure net) {
        Place branch1End = net.places.find { it.label == 'P_fork_1_branch_1_end' }
        Place branch2End = net.places.find { it.label == 'P_fork_1_branch_2_end' }
        Marking marking = new Marking(net.places.size())
        marking.addToken(branch1End, Token.of([run: 1, branch: 'a']), 1)
        marking.addToken(branch1End, Token.of([run: 2, branch: 'a']), 2)
        marking.addToken(branch2End, Token.of([run: 1, branch: 'b']), 1)
        marking.addToken(branch2End, Token.of([run: 2, branch: 'b']), 2)
        return marking
    }

    def 'consumeJoinInputTokens consumes exactly the correlated activation tokens in diagram branch order'() {
        given: 'a join net with two overlapping activations and input places deliberately out of diagram order'
        PetriNetStructure net = buildJoinNet()
        JoinDescriptor descriptor = net.joinDescriptors[0]
        Place branch1End = net.places.find { it.label == 'P_fork_1_branch_1_end' }
        Place branch2End = net.places.find { it.label == 'P_fork_1_branch_2_end' }
        List<Place> reversedInputs = [branch2End, branch1End]
        Marking marking = markingWithTwoActivations(net)

        when: 'the join input tokens for activation 2 are consumed'
        List<Token> consumed = ForkJoinSupport.consumeJoinInputTokens(net, descriptor, marking, reversedInputs, 2)

        then: 'only activation 2 tokens are consumed, ordered by diagram branch order not input order'
        consumed*.payload == [[run: 2, branch: 'a'], [run: 2, branch: 'b']]

        and: 'the other activation tokens remain untouched'
        marking.getTokens(branch1End)*.payload == [[run: 1, branch: 'a']]
        marking.getTokens(branch2End)*.payload == [[run: 1, branch: 'b']]
    }

    def 'consumeJoinInputTokens throws and consumes nothing when the correlated token is missing'() {
        given: 'a marking where only activation 1 has branch tokens'
        PetriNetStructure net = buildJoinNet()
        JoinDescriptor descriptor = net.joinDescriptors[0]
        Place branch1End = net.places.find { it.label == 'P_fork_1_branch_1_end' }
        Place branch2End = net.places.find { it.label == 'P_fork_1_branch_2_end' }
        Marking marking = new Marking(net.places.size())
        marking.addToken(branch1End, Token.of([run: 1, branch: 'a']), 1)
        marking.addToken(branch2End, Token.of([run: 1, branch: 'b']), 1)

        when: 'consumption is attempted for an unknown activation'
        ForkJoinSupport.consumeJoinInputTokens(net, descriptor, marking, [branch1End, branch2End], 99)

        then: 'it throws instead of consuming another activation token'
        thrown(IllegalStateException)

        and: 'the marking is left unchanged'
        marking.getTokenCount(branch1End) == 1
        marking.getTokenCount(branch2End) == 1
        marking.getTokens(branch1End)*.payload == [[run: 1, branch: 'a']]
        marking.getTokens(branch2End)*.payload == [[run: 1, branch: 'b']]
    }

    def 'consumeJoinInputTokens throws and consumes nothing when only part of the correlated set is present'() {
        given: 'activation 1 has a token in branch 1 but another activation owns the branch 2 token'
        PetriNetStructure net = buildJoinNet()
        JoinDescriptor descriptor = net.joinDescriptors[0]
        Place branch1End = net.places.find { it.label == 'P_fork_1_branch_1_end' }
        Place branch2End = net.places.find { it.label == 'P_fork_1_branch_2_end' }
        Marking marking = new Marking(net.places.size())
        marking.addToken(branch1End, Token.of([run: 1, branch: 'a']), 1)
        marking.addToken(branch2End, Token.of([run: 2, branch: 'b']), 2)

        when: 'consumption is attempted for activation 1'
        ForkJoinSupport.consumeJoinInputTokens(net, descriptor, marking, [branch1End, branch2End], 1)

        then: 'it throws without consuming the branch 1 token of activation 1 or the token of activation 2'
        thrown(IllegalStateException)
        marking.getTokenCount(branch1End) == 1
        marking.getTokenCount(branch2End) == 1
    }
}
