package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import spock.lang.Specification

class PetriNetEngineSpec extends Specification {

    def 'should have exactly 3 abstract methods'() {
        given:
        def abstractMethods = PetriNetEngine.methods.findAll { it.default == false && !it.synthetic }.collect { it.name }.sort()

        expect:
        abstractMethods == ['fire', 'getEnabledTransitions', 'isEnabled']
    }

    def 'should not have any topology or marking methods'() {
        given:
        def methodNames = PetriNetEngine.methods.collect { it.name }

        expect: 'no topology methods'
        !methodNames.contains('getPlaces')
        !methodNames.contains('getTransitions')
        !methodNames.contains('getStartPlace')
        !methodNames.contains('getEndPlace')
        !methodNames.contains('getIncidenceMatrix')

        and: 'no stateful convenience methods'
        !methodNames.contains('step')
        !methodNames.contains('runUntilEnd')
        !methodNames.contains('seedToken')
    }

    def 'should have method signatures taking PetriNetStructure as first parameter'() {
        given:
        def isEnabled = PetriNetEngine.methods.find { it.name == 'isEnabled' && !it.synthetic }
        def getEnabled = PetriNetEngine.methods.find { it.name == 'getEnabledTransitions' && !it.synthetic }
        def fire = PetriNetEngine.methods.find { it.name == 'fire' && !it.synthetic }

        expect: 'isEnabled takes structure, transition, marking, registry, context'
        isEnabled.parameterTypes.length == 5
        isEnabled.parameterTypes[0] == PetriNetStructure
        isEnabled.parameterTypes[1] == Transition
        isEnabled.parameterTypes[2] == Marking
        isEnabled.parameterTypes[3] == HandlerRegistry
        isEnabled.parameterTypes[4] == ExecutionContext

        and: 'getEnabledTransitions takes structure, marking, registry, context'
        getEnabled.parameterTypes.length == 4
        getEnabled.parameterTypes[0] == PetriNetStructure
        getEnabled.parameterTypes[1] == Marking
        getEnabled.parameterTypes[2] == HandlerRegistry
        getEnabled.parameterTypes[3] == ExecutionContext

        and: 'fire takes structure, transition, marking, registry, context'
        fire.parameterTypes.length == 5
        fire.parameterTypes[0] == PetriNetStructure
        fire.parameterTypes[1] == Transition
        fire.parameterTypes[2] == Marking
        fire.parameterTypes[3] == HandlerRegistry
        fire.parameterTypes[4] == ExecutionContext
    }
}
