package eu.describeit.plantflow

import groovy.transform.CompileStatic

import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class ActivityDiagramParser {

    private static final Pattern ACTION_PATTERN = Pattern.compile('^\\s*:(.+);\\s*$')

    PetriNet parse(File file) {
        if (file == null) throw new IllegalArgumentException("File cannot be null")
        return parse(file.getText(StandardCharsets.UTF_8.name()))
    }

    PetriNet parse(String pumlContent) {
        if (pumlContent == null || pumlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("PlantUML content cannot be empty")
        }

        List<String> actionLabels = []
        boolean hasStart = false
        boolean hasEnd = false

        List<String> lines = pumlContent.readLines()
        for (String rawLine : lines) {
            String line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("'") || line.startsWith("@startuml") || line.startsWith("@enduml")) {
                continue
            }

            if (line == "start") {
                hasStart = true
                continue
            }

            if (line == "end" || line == "stop") {
                hasEnd = true
                continue
            }

            Matcher matcher = ACTION_PATTERN.matcher(line)
            if (matcher.matches()) {
                actionLabels.add(matcher.group(1).trim())
            }
        }

        if (!hasStart) {
            throw new IllegalArgumentException("Diagram must contain 'start'")
        }
        if (!hasEnd) {
            throw new IllegalArgumentException("Diagram must contain 'end' or 'stop'")
        }
        if (actionLabels.isEmpty()) {
            throw new IllegalArgumentException("Diagram must contain at least one action transition")
        }

        int n = actionLabels.size()
        List<Place> places = []
        Place startPlace = new Place("P_start", 0, "start")
        places.add(startPlace)

        for (int i = 1; i < n; i++) {
            places.add(new Place("P_${i}", i, "P_${i}"))
        }
        Place endPlace = new Place("P_end", n, "end")
        places.add(endPlace)

        List<Transition> transitions = []
        for (int i = 0; i < n; i++) {
            transitions.add(new Transition("T_${i}", i, actionLabels[i]))
        }

        int numPlaces = places.size()
        int numTransitions = transitions.size()
        int[][] inputMatrix = new int[numPlaces][numTransitions]
        int[][] outputMatrix = new int[numPlaces][numTransitions]

        for (int i = 0; i < n; i++) {
            inputMatrix[i][i] = 1
            outputMatrix[i + 1][i] = 1
        }

        IncidenceMatrix incidenceMatrix = new IncidenceMatrix(places, transitions, inputMatrix, outputMatrix)

        return new DefaultPetriNet(places, transitions, incidenceMatrix, startPlace, endPlace)
    }
}
