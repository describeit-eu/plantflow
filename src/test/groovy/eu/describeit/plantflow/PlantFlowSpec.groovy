package eu.describeit.plantflow

import spock.lang.Specification
import java.time.Instant

class PlantFlowSpec extends Specification {

    def "should execute straight-line activity workflow tracer bullet"() {
        given: "a linear activity diagram"
        def puml = '''
            @startuml
            start
            :process order;
            end
            @enduml
        '''

        and: "a handler registry with registered action"
        def registry = new HandlerRegistry()
        registry.registerAction("process order") { ExecutionContext ctx, RecordToken tok ->
            ctx["status"] = "PROCESSED"
            def currentTotal = (tok.payload["total"] as double)
            return tok.withPayload([orderId: tok.payload["orderId"], total: currentTotal, processed: true])
        }

        and: "an initial seed token and context"
        def initialTimestamp = Instant.parse("2026-08-24T10:00:00Z")
        def seedToken = new RecordToken("order-tok-1", initialTimestamp, [orderId: "ORD-999", total: 150.0])
        def context = new ExecutionContext([initiator: "tester"])

        when: "the workflow engine is instantiated and executed"
        def engine = PlantFlow.from(puml, registry)
        def finalMarking = engine.runUntilEnd(seedToken, context)

        then: "the start place is empty and the end place contains the transformed token"
        finalMarking.isEmpty(engine.petriNet.startPlace)
        !finalMarking.isEmpty(engine.petriNet.endPlace)

        and: "the token in P_end has the same ID and updated payload"
        def endTokens = finalMarking.getTokens(engine.petriNet.endPlace)
        endTokens.size() == 1
        def resultToken = endTokens[0]
        resultToken.id == "order-tok-1"
        resultToken.payload == [orderId: "ORD-999", total: 150.0, processed: true]

        and: "the execution context was updated by the action handler"
        context["status"] == "PROCESSED"
        context["initiator"] == "tester"
        engine.isCompleted()
        engine.getEndToken() == resultToken
    }

    def "should advance multi-step sequential workflow through all intermediate places"() {
        given:
        def puml = '''
            @startuml
            start
            :validate customer;
            :charge credit card;
            end
            @enduml
        '''

        def executionLog = []
        def registry = new HandlerRegistry()
        registry.registerAction("validate customer") { ExecutionContext ctx, RecordToken tok ->
            executionLog.add("validated")
            ctx["customerValid"] = true
            return tok.withPayload(tok.payload + [customerValid: true])
        }
        registry.registerAction("charge credit card") { ExecutionContext ctx, RecordToken tok ->
            executionLog.add("charged")
            ctx["charged"] = true
            return tok.withPayload(tok.payload + [chargedAmount: 200])
        }

        def engine = PlantFlow.from(puml, registry)
        def seedToken = RecordToken.of([customerId: "CUST-1"])

        when:
        def finalMarking = engine.runUntilEnd(seedToken)

        then:
        executionLog == ["validated", "charged"]
        finalMarking.isEmpty(engine.petriNet.startPlace)
        finalMarking.isEmpty("P_1")
        !finalMarking.isEmpty(engine.petriNet.endPlace)

        and:
        def endToken = finalMarking.getTokens(engine.petriNet.endPlace)[0]
        endToken.payload == [customerId: "CUST-1", customerValid: true, chargedAmount: 200]
        engine.executionContext["customerValid"] == true
        engine.executionContext["charged"] == true
        engine.isCompleted()
    }

    def "should execute workflow loaded directly from sequence.puml file"() {
        given:
        def file = new File("src/test/data/puml/sequence.puml")
        def calls = []
        def engine = PlantFlow.from(file)
            .registerAction("Hello world") { ExecutionContext ctx, RecordToken tok ->
                calls.add("hello")
                return [greeting: "Hello, World!"]
            }
            .registerAction("groovy goodness") { ExecutionContext ctx, RecordToken tok ->
                calls.add("groovy")
                return tok.payload + [goodness: true]
            }

        when:
        engine.runUntilEnd()

        then:
        calls == ["hello", "groovy"]
        engine.isCompleted()
        engine.getEndToken().payload == [greeting: "Hello, World!", goodness: true]
    }

    def "should throw UnregisteredHandlerException when action handler is missing during execution"() {
        given:
        def puml = '''
            @startuml
            start
            :unregistered step;
            end
            @enduml
        '''
        def engine = PlantFlow.from(puml, new HandlerRegistry())

        when:
        engine.runUntilEnd(RecordToken.of([key: "val"]))

        then:
        def ex = thrown(UnregisteredHandlerException)
        ex.message.contains("unregistered step")
    }

    def "should execute step by step"() {
        given:
        def puml = '''
            @startuml
            start
            :step one;
            :step two;
            end
            @enduml
        '''
        def registry = new HandlerRegistry()
        registry.registerAction("step one") { ExecutionContext ctx, RecordToken tok ->
            return tok.withPayload([step1: true])
        }
        registry.registerAction("step two") { ExecutionContext ctx, RecordToken tok ->
            return tok.withPayload(tok.payload + [step2: true])
        }

        def engine = PlantFlow.from(puml, registry)
        def token = RecordToken.of([init: true])
        engine.seedToken(token)

        expect:
        engine.marking.getTokenCount(engine.petriNet.startPlace) == 1
        engine.marking.getTokenCount("P_1") == 0
        engine.marking.getTokenCount(engine.petriNet.endPlace) == 0

        when:
        boolean stepped1 = engine.step()

        then:
        stepped1
        engine.marking.getTokenCount(engine.petriNet.startPlace) == 0
        engine.marking.getTokenCount("P_1") == 1
        engine.marking.getTokenCount(engine.petriNet.endPlace) == 0

        when:
        boolean stepped2 = engine.step()

        then:
        stepped2
        engine.marking.getTokenCount(engine.petriNet.startPlace) == 0
        engine.marking.getTokenCount("P_1") == 0
        engine.marking.getTokenCount(engine.petriNet.endPlace) == 1

        when:
        boolean stepped3 = engine.step()

        then:
        !stepped3
        engine.marking.getTokenCount(engine.petriNet.endPlace) == 1
        engine.isCompleted()
    }
}
