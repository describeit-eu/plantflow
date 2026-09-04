package eu.describeit.plantflow

import spock.lang.Specification

class DefaultPetriNetSpec extends Specification {

    def 'should create DefaultPetriNetSpec with places, transitions and incidence matrix'() {
        given:
        def pStart = new Place('P_start', 0, 'start')
        def pEnd = new Place('P_end', 1, 'end')
        def tAction = new Transition('T_0', 0, 'process order')

        def inputMatrix = [
            [1], // P_start
            [0],  // P_end
        ] as int[][]
        def outputMatrix = [
            [0], // P_start
            [1],  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tAction], inputMatrix, outputMatrix)

        when:
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        then:
        net.places == [pStart, pEnd]
        net.transitions == [tAction]
        net.startPlace == pStart
        net.endPlace == pEnd
        net.incidenceMatrix == incidenceMatrix

        and:
        net.getPlaceById('P_start') == pStart
        net.getPlaceById('P_end') == pEnd
        net.getPlaceById('P_unknown') == null
        net.getTransitionById('T_0') == tAction
        net.getTransitionById('T_unknown') == null
    }

    def 'should evaluate isEnabled and getEnabledTransitions based on token marking, guards, and actions'() {
        given:
        def pStart = new Place('P_start', 0, 'start')
        def pMid = new Place('P_mid', 1, 'mid')
        def pEnd = new Place('P_end', 2, 'end')

        def tGuard = new Transition('T_0', 0, 'guarded', 'action1', 'checkCondition')
        def tAction = new Transition('T_1', 1, 'actionOnly', 'action2', null)

        def inputMatrix = [
            [1, 0], // P_start
            [0, 1], // P_mid
            [0, 0]  // P_end
        ] as int[][]
        def outputMatrix = [
            [0, 0], // P_start
            [1, 0], // P_mid
            [0, 1]  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix([pStart, pMid, pEnd], [tGuard, tAction], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [tGuard, tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerGuard('checkCondition') { ExecutionContext ctx, RecordToken tok ->
            return tok.payload.valid == true
        }
        registry.registerAction('action1') { ExecutionContext ctx, RecordToken tok -> tok }
        registry.registerAction('action2') { ExecutionContext ctx, RecordToken tok -> tok }

        def marking = new Marking(net.places)
        def context = new ExecutionContext()

        expect:
        !net.isEnabled(null, marking, registry, context)
        !net.isEnabled(tGuard, marking, registry, context)
        net.getEnabledTransitions(marking, registry, context).isEmpty()

        when: 'invalid token added to start place'
        def invalidToken = RecordToken.of([valid: false])
        marking.addToken(pStart, invalidToken)

        then: 'guard rejects transition'
        !net.isEnabled(tGuard, marking, registry, context)
        net.getEnabledTransitions(marking, registry, context).isEmpty()

        when: 'valid token replaces invalid token'
        marking.removeToken(pStart, invalidToken)
        def validToken = RecordToken.of([valid: true])
        marking.addToken(pStart, validToken)

        then: 'guarded transition is enabled'
        net.isEnabled(tGuard, marking, registry, context)
        !net.isEnabled(tAction, marking, registry, context)
        net.getEnabledTransitions(marking, registry, context) == [tGuard]
    }

    def 'should fire transition: consume tokens, execute action handler, and produce tokens'() {
        given:
        def pStart = new Place('P_start', 0, 'start')
        def pEnd = new Place('P_end', 1, 'end')
        def tAction = new Transition('T_0', 0, 'transform', 'transformAction', null)

        def inputMatrix = [
            [1], // P_start
            [0]  // P_end
        ] as int[][]
        def outputMatrix = [
            [0], // P_start
            [1]  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix([pStart, pEnd], [tAction], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pEnd], [tAction], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('transformAction') { ExecutionContext ctx, RecordToken tok ->
            return [result: 'success']
        }

        def marking = new Marking(net.places)
        def context = new ExecutionContext()

        when: 'firing when not enabled'
        def firedWithoutToken = net.fire(tAction, marking, registry, context)

        then:
        !firedWithoutToken
        marking.isEmpty(pStart)
        marking.isEmpty(pEnd)

        when: 'firing with token in place'
        def token = RecordToken.of([init: true])
        marking.addToken(pStart, token)
        def firedWithToken = net.fire(tAction, marking, registry, context)

        then:
        firedWithToken
        marking.isEmpty(pStart)
        marking.getTokenCount(pEnd) == 1
        marking.getTokens(pEnd)[0].payload == [result: 'success']
    }

    def 'should execute workflow to end using runUntilEnd'() {
        given:
        def pStart = new Place('P_start', 0, 'start')
        def pMid = new Place('P_mid', 1, 'mid')
        def pEnd = new Place('P_end', 2, 'end')
        def t1 = new Transition('T_0', 0, 'step1', 'action1', null)
        def t2 = new Transition('T_1', 1, 'step2', 'action2', null)

        def inputMatrix = [
            [1, 0], // P_start
            [0, 1], // P_mid
            [0, 0]  // P_end
        ] as int[][]
        def outputMatrix = [
            [0, 0], // P_start
            [1, 0], // P_mid
            [0, 1]  // P_end
        ] as int[][]

        def incidenceMatrix = new IncidenceMatrix([pStart, pMid, pEnd], [t1, t2], inputMatrix, outputMatrix)
        def net = new DefaultPetriNet([pStart, pMid, pEnd], [t1, t2], incidenceMatrix, pStart, pEnd)

        def registry = new HandlerRegistry()
        registry.registerAction('action1') { ExecutionContext ctx, RecordToken tok ->
            return tok.withPayload(tok.payload + [s1: true])
        }
        registry.registerAction('action2') { ExecutionContext ctx, RecordToken tok ->
            return tok.withPayload(tok.payload + [s2: true])
        }

        def marking = new Marking(net.places)
        def context = new ExecutionContext()
        def seedToken = RecordToken.of([init: true])
        marking.addToken(pStart, seedToken)

        when:
        def finalMarking = net.runUntilEnd(marking, registry, context)

        then:
        finalMarking.isEmpty(pStart)
        finalMarking.isEmpty(pMid)
        !finalMarking.isEmpty(pEnd)
        finalMarking.getTokens(pEnd)[0].payload == [init: true, s1: true, s2: true]
    }
}
