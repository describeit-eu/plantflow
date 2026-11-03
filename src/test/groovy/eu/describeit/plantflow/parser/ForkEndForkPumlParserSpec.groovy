package eu.describeit.plantflow.parser

import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

class ForkEndForkPumlParserSpec extends Specification {

    def 'should classify end fork as END_FORK and not as END or END_MERGE'() {
        given:
        def classifier = new PumlLineClassifier()

        expect:
        classifier.classify('end fork').type == LineType.END_FORK
        classifier.classify('  end   fork  ').type == LineType.END_FORK
        classifier.classify('end merge').type == LineType.END_MERGE
        classifier.classify('end').type == LineType.END
    }

    def 'should parse forkEndFork.puml from file and verify all actions are compiled'() {
        when:
        def net = parse(new File('src/test/data/puml/forkEndFork.puml'))

        then:
        net != null
        net.startPlace.label == 'start'
        net.endPlace.label == 'end'

        and:
        ['action 1', 'action 2', 'action 3', 'action 4', 'action 5'].each { action ->
            assert net.transitions.find { it.actionKey == action } != null
        }
    }

    def 'should compile forkEndFork.puml with a single synchronizing join transition fed by every branch end'() {
        when:
        def net = parse(new File('src/test/data/puml/forkEndFork.puml'))

        then: 'one join transition with one input per branch and a single output'
        def joinTransition = net.transitions.find { it.label == 'T_fork_1_join' }
        assert joinTransition != null
        verifyTransitionIO(net, joinTransition, 3, 1)

        and: 'each branch end place feeds the join transition'
        (1..3).each { i ->
            def branchEndPlace = net.places.find { it.label == "P_fork_1_branch_${i}_end" }
            assert branchEndPlace != null
            assert net.incidenceMatrix.getInputWeight(branchEndPlace.index, joinTransition.index) == 1
        }

        and: 'the join transition produces into the join place feeding the post-join action'
        def joinPlace = net.places.find { it.label == 'P_fork_1_join' }
        assert joinPlace != null
        assert net.incidenceMatrix.getOutputWeight(joinPlace.index, joinTransition.index) == 1
        def action5 = net.transitions.find { it.actionKey == 'action 5' }
        assert net.incidenceMatrix.getInputWeight(joinPlace.index, action5.index) == 1

        and: 'a JoinDescriptor correlates the join transition with the fork transition'
        def forkTransition = net.transitions.find { it.label == 'T_fork_1' }
        def descriptor = net.joinDescriptors.find { it.forkTransitionIndex == forkTransition.index }
        assert descriptor != null
        assert descriptor.joinTransitionIndex == joinTransition.index
    }

    def 'should compile a fork join without post-join actions with a direct join-to-end transition'() {
        when:
        def net = parse(forkJoinPuml([['branch a'], ['branch b']]))

        then:
        def toEndTransition = net.transitions.find { it.label == 'T_fork_1_to_end' }
        assert toEndTransition != null
        def joinPlace = net.places.find { it.label == 'P_fork_1_join' }
        assert joinPlace != null
        assert net.incidenceMatrix.getInputWeight(joinPlace.index, toEndTransition.index) == 1
        assert net.incidenceMatrix.getOutputWeight(net.endPlace.index, toEndTransition.index) == 1
    }

    @Unroll
    def 'should parse fork join diagrams with #branchCount branches from string'() {
        given:
        def branches = []
        (1..branchCount).each { i -> branches.add(["action ${i}"]) }

        when:
        def net = parse(forkJoinPuml(branches))

        then:
        def joinTransition = net.transitions.find { it.label == 'T_fork_1_join' }
        assert joinTransition != null
        verifyTransitionIO(net, joinTransition, branchCount, 1)
        (1..branchCount).each { i ->
            assert net.transitions.find { it.actionKey == "action ${i}" } != null
        }

        where:
        branchCount << [2, 3, 5]
    }

    @Unroll
    def 'should throw IllegalArgumentException for invalid end fork structure: #scenario'() {
        when:
        parse(puml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedMessage)

        where:
        scenario                        | puml                                                                | expectedMessage
        'end fork without active fork'  | '''
            @startuml
            start
            end fork
            end
            @enduml
        '''                                                                | 'end fork without active fork context'
        'unclosed fork'                 | '''
            @startuml
            start
            fork
              :action 1;
            fork again
              :action 2;
            end
            @enduml
        '''                                                                | 'Unclosed fork'
        'end merge after closed end fork' | '''
            @startuml
            start
            fork
              :action 1;
            fork again
              :action 2;
            end fork
            end merge
            end
            @enduml
        '''                                                                | 'end merge without active fork context'
    }
}
