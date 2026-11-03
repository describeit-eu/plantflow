package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.engine.PetriNetBuilder
import spock.lang.Specification

import static eu.describeit.plantflow.PlantFlowTestKit.*

class LinearChainHelperSpec extends Specification {

    def 'should connect linear action chain between places'() {
        given:
        def builder = new PetriNetBuilder()
        def pStart = builder.addPlace('start')
        def pMid = builder.addPlace('mid')
        def pEnd = builder.addPlace('end')
        builder.setStartPlace(pStart).setEndPlace(pEnd)

        when:
        LinearChainHelper.connectLinearActions(builder, [pStart, pMid, pEnd], actions('first', 'second'))
        def net = builder.build()

        then:
        def first = findTransitionByAction(net, 'first')
        def second = findTransitionByAction(net, 'second')
        first != null
        second != null
        net.incidenceMatrix.getInputWeight(pStart.index, first.index) == 1
        net.incidenceMatrix.getOutputWeight(pMid.index, first.index) == 1
        net.incidenceMatrix.getInputWeight(pMid.index, second.index) == 1
        net.incidenceMatrix.getOutputWeight(pEnd.index, second.index) == 1
    }

    def 'should instantiate the private constructor via reflection for full method coverage'() {
        when:
        def constructor = LinearChainHelper.getDeclaredConstructor()
        constructor.accessible = true
        def instance = constructor.newInstance()

        then:
        instance != null
    }
}
