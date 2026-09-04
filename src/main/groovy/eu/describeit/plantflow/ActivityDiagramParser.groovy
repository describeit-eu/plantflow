package eu.describeit.plantflow

import groovy.transform.CompileStatic

import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class ActivityDiagramParser {

    private static final Pattern ACTION_PATTERN = Pattern.compile('^\\s*:(.+);\\s*$')
    private static final String START = 'start'
    private static final String END = 'end'

    PetriNet parse(File file) {
        if (file == null) throw new IllegalArgumentException('File cannot be null')
        return parse(file.getText(StandardCharsets.UTF_8.name()))
    }

    PetriNet parse(String pumlContent) {
        if (pumlContent == null || pumlContent.trim().isEmpty()) {
            throw new IllegalArgumentException('PlantUML content cannot be empty')
        }

        List<String> actionLabels = extractActionLabels(pumlContent.readLines())
        return constructPetriNet(actionLabels)
    }

    private List<String> extractActionLabels(List<String> lines) {
        List<String> actionLabels = []
        boolean hasStart = false
        boolean hasEnd = false

        for (String rawLine : lines) {
            String line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("'") || line.startsWith('@startuml') || line.startsWith('@enduml')) {
                continue
            }

            if (line == START) {
                hasStart = true
                continue
            }

            if (line == END || line == 'stop') {
                hasEnd = true
                continue
            }

            Matcher matcher = ACTION_PATTERN.matcher(line)
            if (matcher.matches()) {
                actionLabels.add(matcher.group(1).trim())
            }
        }

        validateDiagramStructure(hasStart, hasEnd, actionLabels)
        return actionLabels
    }

    private void validateDiagramStructure(boolean hasStart, boolean hasEnd, List<String> actionLabels) {
        if (!hasStart) {
            throw new IllegalArgumentException('Diagram must contain \'start\'')
        }
        if (!hasEnd) {
            throw new IllegalArgumentException('Diagram must contain \'end\' or \'stop\'')
        }
        if (actionLabels.isEmpty()) {
            throw new IllegalArgumentException('Diagram must contain at least one action transition')
        }
    }

    private PetriNet constructPetriNet(List<String> actionLabels) {
        int n = actionLabels.size()
        Place startPlace = new Place('P_start', 0, START)
        Place endPlace = new Place('P_end', n, END)

        List<Place> places = [startPlace]
        for (int i = 1; i < n; i++) {
            places.add(new Place("P_${i}", i, "P_${i}"))
        }
        places.add(endPlace)

        List<Transition> transitions = []
        for (int i = 0; i < n; i++) {
            transitions.add(new Transition("T_${i}", i, actionLabels[i]))
        }

        IncidenceMatrix incidenceMatrix = constructIncidenceMatrix(places, transitions, n)
        return new DefaultPetriNet(places, transitions, incidenceMatrix, startPlace, endPlace)
    }

    private IncidenceMatrix constructIncidenceMatrix(List<Place> places, List<Transition> transitions, int n) {
        int numPlaces = places.size()
        int numTransitions = transitions.size()
        int[][] inputMatrix = new int[numPlaces][numTransitions]
        int[][] outputMatrix = new int[numPlaces][numTransitions]

        for (int i = 0; i < n; i++) {
            inputMatrix[i][i] = 1
            outputMatrix[i + 1][i] = 1
        }

        return new IncidenceMatrix(places, transitions, inputMatrix, outputMatrix)
    }
}
