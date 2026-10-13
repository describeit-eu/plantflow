package eu.describeit.plantflow.engine

import spock.lang.Specification

class PlaceSpec extends Specification {

    def 'should construct Place with valid index and label'() {
        when:
        def place = new Place(0, 'start')

        then:
        place.index == 0
        place.label == 'start'
    }

    def 'should throw IllegalArgumentException when label is null, blank, or whitespace'() {
        when:
        new Place(index, invalidLabel)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == message

        where:
        index | invalidLabel | message
        0     | null   | 'label cannot be null'
        1     |''     | 'label cannot be blank'
        2     |'   '  | 'label cannot be blank'
        3     |'\t\n' | 'label cannot be blank'
    }

    def 'should satisfy equals, hashCode, and toString contracts'() {
        given:
        def p1 = new Place(0, 'p1')
        def p2 = new Place(0, 'p1')
        def p3 = new Place(1, 'p1')
        def p4 = new Place(0, 'p2')

        expect:
        p1 == p2
        p1.hashCode() == p2.hashCode()
        p1 != p3
        p1 != p4
        p1.toString().contains('index:0') || p1.toString().contains('index=0')
        p1.toString().contains('label:p1') || p1.toString().contains('label=p1')
    }

    def 'should trim whitespace from label'() {
        when:
        def place = new Place(0, '  hello world  ')

        then:
        place.label == 'hello world'
        place.index == 0
    }

    def 'should trim tab and newline characters from label'() {
        when:
        def place = new Place(1, '\thello\n')

        then:
        place.label == 'hello'
    }

    def 'should accept label with internal whitespace'() {
        when:
        def place = new Place(0, 'hello world foo')

        then:
        place.label == 'hello world foo'
    }
}
