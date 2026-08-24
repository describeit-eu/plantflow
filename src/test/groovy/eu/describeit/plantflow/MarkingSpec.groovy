package eu.describeit.plantflow

import spock.lang.Specification

class MarkingSpec extends Specification {

    def "should manage tokens in vector indexed places"() {
        given:
        def pStart = new Place("P_start", 0, "start")
        def pEnd = new Place("P_end", 1, "end")
        def marking = new Marking([pStart, pEnd])
        def token = RecordToken.of([data: "initial"])

        when:
        marking.addToken(pStart, token)

        then:
        marking.getTokenCount(pStart) == 1
        marking.getTokenCount(0) == 1
        marking.getTokenCount(pEnd) == 0
        marking.getTokens(pStart) == [token]
        marking.getTokens(0) == [token]
        marking.getTokens("P_start") == [token]
        marking.getMarkingVector() == [1, 0] as int[]

        when:
        def removed = marking.removeToken(pStart, token)
        marking.addToken(pEnd, token)

        then:
        removed
        marking.isEmpty(pStart)
        !marking.isEmpty(pEnd)
        marking.getTokens(pEnd) == [token]
        marking.getMarkingVector() == [0, 1] as int[]
    }
}
