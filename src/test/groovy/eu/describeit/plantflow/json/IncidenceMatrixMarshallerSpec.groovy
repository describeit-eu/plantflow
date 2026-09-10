package eu.describeit.plantflow.json

import eu.describeit.plantflow.engine.IncidenceMatrix
import eu.describeit.plantflow.Marshaller
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class IncidenceMatrixMarshallerSpec extends Specification {
    def 'Marshaller should serialize and deserialize Place'() {
        given:
        def place = new Place(0, 'Start')

        when:
        def json = Marshaller.toJson(place)
        def deserialized = Marshaller.fromJson(json, Place)

        log.info('Place JSON: {}', json)

        then:
        json.contains('"index"')
        json.contains('0')
        json.contains('"label"')
        json.contains('"Start"')
        deserialized.index == 0
        deserialized.label == 'Start'
    }

    def 'Marshaller should serialize and deserialize Transition'() {
        given:
        def transition = new Transition(0, 'Action', 'actionKey')

        when:
        def json = Marshaller.toJson(transition)
        def deserialized = Marshaller.fromJson(json, Transition)

        log.info('Transition JSON: {}', json)

        then:
        json.contains('"index"')
        json.contains('0')
        json.contains('"label"')
        json.contains('"Action"')
        json.contains('"actionKey"')
        deserialized.index == 0
        deserialized.label == 'Action'
        deserialized.actionKey == 'actionKey'
    }

    def 'Marshaller should serialize and deserialize Transition with guardKey'() {
        given:
        def transition = new Transition(0, 'Action', 'actionKey', 'guardKey')

        when:
        def json = Marshaller.toJson(transition)
        def deserialized = Marshaller.fromJson(json, Transition)

        log.info('Transition JSON: {}', json)

        then:
        json.contains('"guardKey"')
        deserialized.guardKey == 'guardKey'
    }

    def 'Marshaller should produce pretty JSON'() {
        given:
        def place = new Place(0, 'Start')

        when:
        def json = Marshaller.toJson(place, true)

        log.info('Place JSON(pretty): {}', json)

        then:
        json.contains('\n')
        json.contains('  ')
    }

    def 'Marshaller should serialize and deserialize IncidenceMatrix'() {
        given:
        def pStart = new Place(0, 'Start')
        def pEnd = new Place(1, 'End')
        def tAction = new Transition(0, 'Action', 'actionKey')

        def places = [pStart, pEnd]
        def transitions = [tAction]
        def inputMatrix = [[1], [0]] as int[][]
        def outputMatrix = [[0], [1]] as int[][]

        def matrix = new IncidenceMatrix(places, transitions, inputMatrix, outputMatrix)

        when:
        def json = Marshaller.toJson(matrix)
        def deserialized = Marshaller.fromJson(json, IncidenceMatrix)
        
        log.info('IncidenceMatrix JSON: {}', json)

        then:
        json.contains('"places"')
        json.contains('"transitions"')
        json.contains('"inputMatrix"')
        json.contains('"outputMatrix"')
        deserialized.places.size() == 2
        deserialized.transitions.size() == 1
        deserialized.getInputWeight(0, 0) == 1
        deserialized.getInputWeight(1, 0) == 0
        deserialized.getOutputWeight(0, 0) == 0
        deserialized.getOutputWeight(1, 0) == 1
    }

    def 'Marshaller should serialize and deserialize complex IncidenceMatrix'() {
        given:
        def p1 = new Place(0, 'P1')
        def p2 = new Place(1, 'P2')
        def p3 = new Place(2, 'P3')
        
        def t1 = new Transition(0, 'T1', 't1')
        def t2 = new Transition(1, 'T2', 't2')

        def places = [p1, p2, p3]
        def transitions = [t1, t2]
        // Input matrix: P1 -> T1 (1), P2 -> T2 (1)
        def inputMatrix = [[1, 0], [0, 1], [0, 0]] as int[][]
        // Output matrix: T1 -> P2 (1), T2 -> P3 (1)
        def outputMatrix = [[0, 0], [1, 0], [0, 1]] as int[][]

        def matrix = new IncidenceMatrix(places, transitions, inputMatrix, outputMatrix)

        when:
        def json = Marshaller.toJson(matrix, true)
        def deserialized = Marshaller.fromJson(json, IncidenceMatrix)
        
        log.info('Complex IncidenceMatrix JSON(pretty): {}', json)

        then:
        // Verify input weights
        deserialized.getInputWeight(0, 0) == 1
        deserialized.getInputWeight(0, 1) == 0
        deserialized.getInputWeight(1, 0) == 0
        deserialized.getInputWeight(1, 1) == 1
        deserialized.getInputWeight(2, 0) == 0
        deserialized.getInputWeight(2, 1) == 0

        // Verify output weights
        deserialized.getOutputWeight(0, 0) == 0
        deserialized.getOutputWeight(0, 1) == 0
        deserialized.getOutputWeight(1, 0) == 1
        deserialized.getOutputWeight(1, 1) == 0
        deserialized.getOutputWeight(2, 0) == 0
        deserialized.getOutputWeight(2, 1) == 1

        // Verify incidence matrix (output - input)
        deserialized.getIncidence(0, 0) == -1
        deserialized.getIncidence(0, 1) == 0
        deserialized.getIncidence(1, 0) == 1
        deserialized.getIncidence(1, 1) == -1
        deserialized.getIncidence(2, 0) == 0
        deserialized.getIncidence(2, 1) == 1
    }

    def 'Marshaller should handle null input and output matrices'() {
        given:
        def p1 = new Place(0, 'P1')
        def t1 = new Transition(0, 'T1', 't1')

        def matrix = new IncidenceMatrix([p1], [t1], null, null)

        when:
        def json = Marshaller.toJson(matrix)
        def deserialized = Marshaller.fromJson(json, IncidenceMatrix)

        then:
        deserialized.places.size() == 1
        deserialized.transitions.size() == 1
        deserialized.getInputWeight(0, 0) == 0
        deserialized.getOutputWeight(0, 0) == 0
        deserialized.getIncidence(0, 0) == 0
    }
}
