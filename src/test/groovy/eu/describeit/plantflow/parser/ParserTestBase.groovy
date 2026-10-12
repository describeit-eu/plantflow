package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ActivityDiagramParser
import eu.describeit.plantflow.engine.PetriNet

/**
 * Base utility class for parser tests.
 * Provides common fixtures and helper methods for testing PlantUML parsing.
 */
class ParserTestBase {

    /**
     * Creates a new ActivityDiagramParser instance.
     */
    static ActivityDiagramParser createParser() {
        return new ActivityDiagramParser()
    }

    /**
     * Parses PlantUML content from a string.
     */
    static PetriNet parse(String pumlContent) {
        return createParser().parse(pumlContent)
    }

    /**
     * Parses PlantUML content from a file.
     */
    static PetriNet parse(File file) {
        return createParser().parse(file)
    }

    /**
     * Creates a simple linear diagram PlantUML string with the given actions.
     */
    static String createLinearPuml(String... actions) {
        def sb = new StringBuilder()
        sb << '@startuml\n'
        sb << 'start\n'
        actions.each { action ->
            sb << ":${action};\n"
        }
        sb << 'end\n'
        sb << '@enduml\n'
        return sb.toString()
    }

    /**
     * Creates a simple conditional diagram PlantUML string.
     */
    static String createConditionalPuml(String guard, String thenAction, String elseAction) {
        return """@startuml
start
if ( ${guard} ) then (yes)
  :${thenAction};
else (no)
  :${elseAction};
endif
end
@enduml
"""
    }

    /**
     * Creates a conditional diagram PlantUML string with multiple actions in branches.
     */
    static String createConditionalPuml(String guard, String[] thenActions, String[] elseActions) {
        def sb = new StringBuilder()
        sb << '@startuml\n'
        sb << 'start\n'
        sb << "if ( ${guard} ) then (yes)\n"
        thenActions.each { action ->
            sb << "  :${action};\n"
        }
        sb << 'else (no)\n'
        elseActions.each { action ->
            sb << "  :${action};\n"
        }
        sb << 'endif\n'
        sb << 'end\n'
        sb << '@enduml\n'
        return sb.toString()
    }

    /**
     * Creates a fork diagram PlantUML string with multiple branches.
     */
    static String createForkPuml(List<String> branch1Actions, List<String> branch2Actions, List<String> branch3Actions) {
        def sb = new StringBuilder()
        sb << '@startuml\n'
        sb << 'start\n'
        sb << 'fork\n'
        branch1Actions.each { action ->
            sb << "  :${action};\n"
        }
        sb << 'fork again\n'
        branch2Actions.each { action ->
            sb << "  :${action};\n"
        }
        sb << 'fork again\n'
        branch3Actions.each { action ->
            sb << "  :${action};\n"
        }
        sb << 'end merge\n'
        sb << 'end\n'
        sb << '@enduml\n'
        return sb.toString()
    }

    /**
     * Creates a fork diagram PlantUML string with variable number of branches
     * and optional post-merge actions.
     */
    static String createForkPumlWithBranches(List<List<String>> branches, List<String> postMergeActions = []) {
        def sb = new StringBuilder()
        sb << '@startuml\n'
        sb << 'start\n'
        sb << 'fork\n'
        
        branches.eachWithIndex { branchActions, index ->
            if (index > 0) {
                sb << 'fork again\n'
            }
            branchActions.each { action ->
                sb << "  :${action};\n"
            }
        }
        
        sb << 'end merge\n'
        postMergeActions.each { action ->
            sb << ":${action};\n"
        }
        sb << 'end\n'
        sb << '@enduml\n'
        return sb.toString()
    }

    /**
     * Creates a fork diagram PlantUML string with the given number of branches.
     * Each branch contains a single action with the format "action N".
     */
    static String createForkPumlWithBranchCount(int branchCount) {
        def branches = []
        (1..branchCount).each { i -> branches.add(["action ${i}"]) }
        return createForkPumlWithBranches(branches)
    }

    /**
     * Validates that a parsed PetriNet has the expected structure.
     * Verifies place count, transition count, start place, and end place.
     */
    static void validateNetStructure(PetriNet net, int expectedPlaceCount, int expectedTransitionCount) {
        assert net != null : "Parsed PetriNet should not be null"
        assert net.places.size() == expectedPlaceCount : "Expected ${expectedPlaceCount} places but got ${net.places.size()}"
        assert net.transitions.size() == expectedTransitionCount : "Expected ${expectedTransitionCount} transitions but got ${net.transitions.size()}"
        assert net.startPlace != null : "Start place should not be null"
        assert net.endPlace != null : "End place should not be null"
    }

    /**
     * Validates that a transition has the expected action key.
     */
    static void validateTransitionAction(PetriNet net, String expectedAction, int transitionIndex = 0) {
        assert net.transitions.size() > transitionIndex : "Not enough transitions"
        assert net.transitions[transitionIndex].actionKey == expectedAction : \
            "Expected action key '${expectedAction}' but got '${net.transitions[transitionIndex].actionKey}'"
    }

    /**
     * Validates that a transition has the expected guard key.
     */
    static void validateTransitionGuard(PetriNet net, String expectedGuard, int transitionIndex = 0) {
        assert net.transitions.size() > transitionIndex : "Not enough transitions"
        assert net.transitions[transitionIndex].guardKey == expectedGuard : \
            "Expected guard key '${expectedGuard}' but got '${net.transitions[transitionIndex].guardKey}'"
    }

    /**
     * Validates that places have the expected labels.
     */
    static void validatePlaceLabels(PetriNet net, List<String> expectedLabels) {
        assert net.places.size() == expectedLabels.size() : \
            "Expected ${expectedLabels.size()} places but got ${net.places.size()}"
        expectedLabels.eachWithIndex { label, idx ->
            assert net.places[idx].label == label : \
                "Expected place ${idx} label '${label}' but got '${net.places[idx].label}'"
        }
    }

    /**
     * Validates that transitions have the expected labels.
     */
    static void validateTransitionLabels(PetriNet net, List<String> expectedLabels) {
        assert net.transitions.size() == expectedLabels.size() : \
            "Expected ${expectedLabels.size()} transitions but got ${net.transitions.size()}"
        expectedLabels.eachWithIndex { label, idx ->
            assert net.transitions[idx].label == label : \
                "Expected transition ${idx} label '${label}' but got '${net.transitions[idx].label}'"
        }
    }

    /**
     * Helper to verify incidence matrix connections for a transition.
     * Checks that a transition consumes from the expected input place and produces to the expected output place.
     */
    static void verifyTransitionConnection(PetriNet net, int transitionIndex, int inputPlaceIndex, int outputPlaceIndex) {
        def t = net.transitions[transitionIndex]
        assert net.incidenceMatrix.getInputWeight(inputPlaceIndex, t.index) == 1 : \
            "Transition ${t.label} should consume from place ${inputPlaceIndex}"
        assert net.incidenceMatrix.getOutputWeight(outputPlaceIndex, t.index) == 1 : \
            "Transition ${t.label} should produce to place ${outputPlaceIndex}"
    }
}
