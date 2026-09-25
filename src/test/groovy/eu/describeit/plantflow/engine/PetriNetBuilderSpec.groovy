package eu.describeit.plantflow.engine

import spock.lang.Specification
import spock.lang.Unroll

class PetriNetBuilderSpec extends Specification {

    def 'should add places with incremental indices starting from 0'() {
        given:
        def builder = new PetriNetBuilder()

        when:
        def p0 = builder.addPlace('start')
        def p1 = builder.addPlace('step1')
        def p2 = builder.addPlace('step2')
        def p3 = builder.addPlace('end')

        then:
        p0.index == 0
        p0.label == 'start'
        p1.index == 1
        p1.label == 'step1'
        p2.index == 2
        p2.label == 'step2'
        p3.index == 3
        p3.label == 'end'

        and:
        builder.places == [p0, p1, p2, p3]
    }

    @Unroll
    def 'should throw IllegalArgumentException when adding place with invalid label: "#invalidLabel"'() {
        given:
        def builder = new PetriNetBuilder()

        when:
        builder.addPlace(invalidLabel)

        then:
        thrown(IllegalArgumentException)

        where:
        invalidLabel << [null, '', '   ', '\t\n']
    }

    def 'should add transitions with incremental indices and optional action and guard keys'() {
        given:
        def builder = new PetriNetBuilder()

        when:
        def t0 = builder.addTransition('start_to_decision')
        def t1 = builder.addTransition('process', 'processAction')
        def t2 = builder.addTransition('branch_yes', null, 'hasRemaining')
        def t3 = builder.addTransition('transform', 'transformAction', 'isValid')

        then:
        t0.index == 0
        t0.label == 'start_to_decision'
        t0.actionKey == null
        t0.guardKey == null

        t1.index == 1
        t1.label == 'process'
        t1.actionKey == 'processAction'
        t1.guardKey == null

        t2.index == 2
        t2.label == 'branch_yes'
        t2.actionKey == null
        t2.guardKey == 'hasRemaining'

        t3.index == 3
        t3.label == 'transform'
        t3.actionKey == 'transformAction'
        t3.guardKey == 'isValid'

        and:
        builder.transitions == [t0, t1, t2, t3]
    }

    @Unroll
    def 'should throw IllegalArgumentException when adding transition with invalid label: "#invalidLabel"'() {
        given:
        def builder = new PetriNetBuilder()

        when:
        builder.addTransition(invalidLabel)

        then:
        thrown(IllegalArgumentException)

        where:
        invalidLabel << [null, '', '   ', '\t\n']
    }

    def 'should connect place to transition with default weight 1'() {
        given:
        def builder = new PetriNetBuilder()
        def pStart = builder.addPlace('start')
        def pEnd = builder.addPlace('end')
        def tAction = builder.addTransition('action')
        builder.setStartPlace(pStart).setEndPlace(pEnd)

        when:
        def builderRef = builder.connect(pStart, tAction)
        def net = builder.build()

        then:
        builderRef.is(builder)
        net.incidenceMatrix.getInputWeight(pStart.index, tAction.index) == 1
        net.incidenceMatrix.getOutputWeight(pStart.index, tAction.index) == 0
        net.incidenceMatrix.getIncidence(pStart.index, tAction.index) == -1
    }

    def 'should connect place to transition with custom weight'() {
        given:
        def builder = new PetriNetBuilder()
        def pStart = builder.addPlace('start')
        def pEnd = builder.addPlace('end')
        def tAction = builder.addTransition('action')
        builder.setStartPlace(pStart).setEndPlace(pEnd)

        when:
        builder.connect(pStart, tAction, 3)
        def net = builder.build()

        then:
        net.incidenceMatrix.getInputWeight(pStart.index, tAction.index) == 3
        net.incidenceMatrix.getOutputWeight(pStart.index, tAction.index) == 0
        net.incidenceMatrix.getIncidence(pStart.index, tAction.index) == -3
    }

    def 'should connect transition to place with default weight 1'() {
        given:
        def builder = new PetriNetBuilder()
        def pStart = builder.addPlace('start')
        def pEnd = builder.addPlace('end')
        def tAction = builder.addTransition('action')
        builder.setStartPlace(pStart).setEndPlace(pEnd)

        when:
        def builderRef = builder.connect(tAction, pEnd)
        def net = builder.build()

        then:
        builderRef.is(builder)
        net.incidenceMatrix.getOutputWeight(pEnd.index, tAction.index) == 1
        net.incidenceMatrix.getInputWeight(pEnd.index, tAction.index) == 0
        net.incidenceMatrix.getIncidence(pEnd.index, tAction.index) == 1
    }

    def 'should connect transition to place with custom weight'() {
        given:
        def builder = new PetriNetBuilder()
        def pStart = builder.addPlace('start')
        def pEnd = builder.addPlace('end')
        def tAction = builder.addTransition('action')
        builder.setStartPlace(pStart).setEndPlace(pEnd)

        when:
        builder.connect(tAction, pEnd, 2)
        def net = builder.build()

        then:
        net.incidenceMatrix.getOutputWeight(pEnd.index, tAction.index) == 2
        net.incidenceMatrix.getInputWeight(pEnd.index, tAction.index) == 0
        net.incidenceMatrix.getIncidence(pEnd.index, tAction.index) == 2
    }

    @Unroll
    def 'should throw IllegalArgumentException when connecting place to transition with invalid arguments: #scenario'() {
        given:
        def builder = new PetriNetBuilder()

        when:
        builder.connect(fromPlace, toTransition, weight)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario          | fromPlace          | toTransition           | weight | expectedMessage
        'null fromPlace'  | null               | new Transition(0, 't') | 1      | 'fromPlace cannot be null'
        'null toTransition'| new Place(0, 'p') | null                   | 1      | 'toTransition cannot be null'
        'zero weight'     | new Place(0, 'p')  | new Transition(0, 't') | 0      | 'weight must be greater than 0'
        'negative weight' | new Place(0, 'p')  | new Transition(0, 't') | -1     | 'weight must be greater than 0'
    }

    @Unroll
    def 'should throw IllegalArgumentException when connecting transition to place with invalid arguments: #scenario'() {
        given:
        def builder = new PetriNetBuilder()

        when:
        builder.connect(fromTransition, toPlace, weight)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedMessage

        where:
        scenario             | fromTransition         | toPlace          | weight | expectedMessage
        'null fromTransition'| null                   | new Place(0, 'p')| 1      | 'fromTransition cannot be null'
        'null toPlace'       | new Transition(0, 't') | null             | 1      | 'toPlace cannot be null'
        'zero weight'        | new Transition(0, 't') | new Place(0, 'p')| 0      | 'weight must be greater than 0'
        'negative weight'    | new Transition(0, 't') | new Place(0, 'p')| -1     | 'weight must be greater than 0'
    }

    def 'should throw IllegalArgumentException when setting null startPlace or endPlace'() {
        given:
        def builder = new PetriNetBuilder()

        when:
        builder.setStartPlace(null)

        then:
        def exStart = thrown(IllegalArgumentException)
        exStart.message == 'startPlace cannot be null'

        when:
        builder.setEndPlace(null)

        then:
        def exEnd = thrown(IllegalArgumentException)
        exEnd.message == 'endPlace cannot be null'
    }

    @Unroll
    def 'should throw IllegalStateException when missing #missing on build'() {
        given:
        def builder = new PetriNetBuilder()
        def p0 = builder.addPlace('start')
        def p1 = builder.addPlace('end')
        if (setStart) {
            builder.setStartPlace(p0)
        }
        if (setEnd) {
            builder.setEndPlace(p1)
        }

        when:
        builder.build()

        then:
        def ex = thrown(IllegalStateException)
        ex.message == expectedMessage

        where:
        missing      | setStart | setEnd | expectedMessage
        'startPlace' | false    | true   | 'startPlace must be set'
        'endPlace'   | true     | false  | 'endPlace must be set'
        'both'       | false    | false  | 'startPlace must be set'
    }

    def 'should build a complete DefaultPetriNet with places, transitions, startPlace, endPlace and incidence matrix'() {
        given:
        def builder = new PetriNetBuilder()
        def p0 = builder.addPlace('start')
        def p1 = builder.addPlace('P_1')
        def p2 = builder.addPlace('end')

        def t0 = builder.addTransition('step1', 'step1Action')
        def t1 = builder.addTransition('step2', 'step2Action')

        builder.setStartPlace(p0)
               .setEndPlace(p2)
               .connect(p0, t0)
               .connect(t0, p1)
               .connect(p1, t1)
               .connect(t1, p2)

        when:
        def net = builder.build()

        then:
        net != null
        net.places.size() == 3
        net.transitions.size() == 2
        net.startPlace == p0
        net.endPlace == p2
        builder.startPlace == p0
        builder.endPlace == p2

        and: 'incidence matrix input weights'
        net.incidenceMatrix.getInputWeight(p0.index, t0.index) == 1
        net.incidenceMatrix.getInputWeight(p1.index, t0.index) == 0
        net.incidenceMatrix.getInputWeight(p2.index, t0.index) == 0
        net.incidenceMatrix.getInputWeight(p0.index, t1.index) == 0
        net.incidenceMatrix.getInputWeight(p1.index, t1.index) == 1
        net.incidenceMatrix.getInputWeight(p2.index, t1.index) == 0

        and: 'incidence matrix output weights'
        net.incidenceMatrix.getOutputWeight(p0.index, t0.index) == 0
        net.incidenceMatrix.getOutputWeight(p1.index, t0.index) == 1
        net.incidenceMatrix.getOutputWeight(p2.index, t0.index) == 0
        net.incidenceMatrix.getOutputWeight(p0.index, t1.index) == 0
        net.incidenceMatrix.getOutputWeight(p1.index, t1.index) == 0
        net.incidenceMatrix.getOutputWeight(p2.index, t1.index) == 1

        and: 'incidence matrix calculation (output - input)'
        net.incidenceMatrix.getIncidence(p0.index, t0.index) == -1
        net.incidenceMatrix.getIncidence(p1.index, t0.index) == 1
        net.incidenceMatrix.getIncidence(p1.index, t1.index) == -1
        net.incidenceMatrix.getIncidence(p2.index, t1.index) == 1
    }
}
