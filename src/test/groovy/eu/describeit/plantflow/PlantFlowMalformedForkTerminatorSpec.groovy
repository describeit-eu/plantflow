package eu.describeit.plantflow

import eu.describeit.plantflow.engine.Token
import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.PlantFlowTestKit.*

/**
 * Ticket #32: the public workflow API rejects malformed and mismatched Fork
 * terminators instead of silently accepting or reinterpreting them, while
 * valid `end fork` and `end merge` diagrams keep their distinct semantics.
 */
class PlantFlowMalformedForkTerminatorSpec extends Specification {

    @Unroll
    def 'PlantFlow.from rejects malformed fork terminator: #scenario'() {
        when: 'the malformed diagram is parsed through the public API'
        PlantFlow.from(puml, new HandlerRegistry())

        then: 'a clear IllegalArgumentException is thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message.contains(expectedMessage)

        where:
        scenario                                    | puml                                                                                                          | expectedMessage
        'orphan end fork without fork'              | '@startuml\nstart\n:action 1;\nend fork\nend\n@enduml'                                                      | 'end fork without active fork context'
        'orphan end merge without fork'             | '@startuml\nstart\n:action 1;\nend merge\nend\n@enduml'                                                     | 'end merge without active fork context'
        'stray fork again without fork'             | '@startuml\nstart\nfork again\n:action 1;\nend\n@enduml'                                                    | 'fork again without active fork context'
        'unclosed fork terminated by end'           | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nend\n@enduml'                                             | 'Unclosed fork'
        'unclosed fork at diagram end'              | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\n@enduml'                                                  | 'Unclosed fork'
        'fork with a single branch'                 | '@startuml\nstart\nfork\n  :a;\nend fork\nend\n@enduml'                                                     | 'at least 2 branches'
        'fork with empty branches'                  | '@startuml\nstart\nfork\nfork again\nend fork\nend\n@enduml'                                                | 'branch'
        'fork with one empty branch'                | '@startuml\nstart\nfork\n  :a;\nfork again\nend merge\nend\n@enduml'                                         | 'branch'
        'mixed end merge and end fork'              | '@startuml\nstart\nfork\n  :a;\n  fork\n    :b;\n  end merge\nfork again\n  :c;\nend fork\nend\n@enduml'     | 'Mismatched fork terminators'
        'mixed end fork and end merge'              | '@startuml\nstart\nfork\n  :a;\n  fork\n    :b;\n  end fork\nfork again\n  :c;\nend merge\nend\n@enduml'      | 'Mismatched fork terminators'
        'duplicate end fork after closed fork'      | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nend fork\nend fork\nend\n@enduml'                        | 'end fork without active fork context'
        'duplicate end merge after closed fork'     | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nend merge\nend merge\nend\n@enduml'                       | 'end merge without active fork context'
        'end fork misplaced after end merge closed' | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nend merge\nend fork\nend\n@enduml'                        | 'end fork without active fork context'
        'end merge misplaced after end fork closed' | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nend fork\nend merge\nend\n@enduml'                        | 'end merge without active fork context'
        'fork again after fork closed'              | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nend fork\nfork again\n  :c;\nend\n@enduml'                 | 'fork again without active fork context'
        'endfork without space as terminator'       | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nendfork\nend\n@enduml'                                    | 'Malformed'
        'endfork without space in linear diagram'   | '@startuml\nstart\n:action 1;\nendfork\nend\n@enduml'                                                        | 'Malformed'
        'endmerge without space'                    | '@startuml\nstart\n:action 1;\nendmerge\nend\n@enduml'                                                      | 'Malformed'
        'end fork with trailing text'               | '@startuml\nstart\nfork\n  :a;\nfork again\n  :b;\nend fork now\nend\n@enduml'                              | 'Malformed'
        'end merge with trailing semicolon'         | '@startuml\nstart\n:action 1;\nend merge;\nend\n@enduml'                                                     | 'Malformed'
        'forkagain typo'                            | '@startuml\nstart\nfork\n  :a;\nforkagain\n  :b;\nend fork\nend\n@enduml'                                    | 'Malformed'
        'fork with unexpected suffix'               | '@startuml\nstart\nfork now\n  :a;\nend\n@enduml'                                                            | 'Malformed'
        'end fork inside if-then-else diagram'      | '@startuml\nstart\nif (cond) then (yes)\n  :a;\nelse (no)\n  :b;\nendif\nend fork\nend\n@enduml'            | 'mixes if-then-else and fork'
        'stray fork again inside if-then-else'      | '@startuml\nstart\nif (cond) then (yes)\n  :a;\nelse (no)\n  :b;\nendif\nfork again\nend\n@enduml'            | 'mixes if-then-else and fork'
        'fork keyword inside if-then-else diagram'  | '@startuml\nstart\nif (cond) then (yes)\n  :a;\nelse (no)\n  :b;\nendif\nfork\nend\n@enduml'                 | 'mixes if-then-else and fork'
    }

    def 'a valid end fork diagram still parses and executes with synchronizing join semantics'() {
        given: 'a two-branch fork/join with a post-join action'
        def puml = forkJoinPuml([['branch a'], ['branch b']], ['after join'])
        def engine = PlantFlow.from(puml, payloadRegistry(['branch a', 'branch b', 'after join']))

        when: 'the workflow runs to completion'
        engine.runUntilEnd(Token.of([initial: true]))

        then: 'it completed with exactly one merged end token'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [initial: true, branch_a: true, branch_b: true, after_join: true]
    }

    def 'a valid end merge diagram still parses and executes with first-completion semantics'() {
        given: 'a two-branch fork/merge with a post-merge action'
        def puml = forkPuml([['branch a'], ['branch b']], ['after merge'])
        def engine = PlantFlow.from(puml, payloadRegistry(['branch a', 'branch b', 'after merge']))
        engine.seedToken(Token.of([initial: true]))

        when: 'the fork fires and only the first branch completes'
        engine.fire(findTransitionByLabel(engine, 'T_fork_1'))
        engine.fire(findTransitionByAction(engine, 'branch a'))

        then: 'the merge is already enabled (first-completion), unlike a synchronizing join'
        engine.isEnabled(findTransitionByLabel(engine, 'T_fork_1_merge_1'))

        when: 'the merge and post-merge action run, then the remaining branch finishes'
        engine.fire(findTransitionByLabel(engine, 'T_fork_1_merge_1'))
        engine.fire(findTransitionByAction(engine, 'after merge'))
        engine.fire(findTransitionByAction(engine, 'branch b'))

        then: 'the workflow completed exactly once with a single end token'
        engine.isCompleted()
        engine.getEndTokens().size() == 1
        engine.getEndToken().payload == [initial: true, branch_a: true, after_merge: true]
    }
}
