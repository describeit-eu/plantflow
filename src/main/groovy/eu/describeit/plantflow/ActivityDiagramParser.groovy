package eu.describeit.plantflow

import eu.describeit.plantflow.engine.DefaultPetriNet
import eu.describeit.plantflow.engine.IncidenceMatrix
import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
@Slf4j
class ActivityDiagramParser {

    private static final Pattern ACTION_PATTERN = Pattern.compile('^\\s*:(.+);\\s*$')
    private static final String START = 'start'
    private static final String END = 'end'
    private static final String STOP = 'stop'

    PetriNet parse(File file) {
        if (file == null) throw new IllegalArgumentException('File cannot be null')
        return parse(file.getText(StandardCharsets.UTF_8.name()))
    }

    PetriNet parse(String pumlContent) {
        if (pumlContent == null || pumlContent.trim().isEmpty()) {
            throw new IllegalArgumentException('PlantUML content cannot be empty')
        }

        List<String> actions = extractActions(pumlContent.readLines())
        return constructPetriNet(actions)
    }

    private List<String> extractActions(List<String> lines) {
        List<String> actions = []
        Boolean hasStart = false
        Boolean hasEnd = false

        for (String rawLine : lines) {
            String line = rawLine.trim()
            Boolean skip = false

            (skip, hasStart, hasEnd) = checkLine(line, hasStart, hasEnd)

            if (!skip) {
                Matcher matcher = ACTION_PATTERN.matcher(line)
                if (matcher.matches()) {
                    actions.add(matcher.group(1).trim())
                }
            }
        }

        validateDiagramStructure(hasStart, hasEnd, actions)
        return actions
    }

    private Tuple3<Boolean, Boolean, Boolean> checkLine(String line, Boolean hasStart, Boolean hasEnd) {
        Tuple3<Boolean, Boolean, Boolean> result = [false, hasStart, hasEnd]

        if (line.isEmpty() || line.startsWith("'") || line.startsWith('@startuml') || line.startsWith('@enduml')) {
            result = [true, hasStart, hasEnd]
        } else if (line == START) {
            result = [true, true, hasEnd]
        } else if (line == END || line == STOP) {
            result = [true, hasStart, true]
        }

        log.info('checkLine() - line:"{}" result:{}', line, result)

        return result
    }

    private void validateDiagramStructure(boolean hasStart, boolean hasEnd, List<String> actions) {
        if (!hasStart) {
            throw new IllegalArgumentException('Diagram must contain \'start\'')
        }
        if (!hasEnd) {
            throw new IllegalArgumentException('Diagram must contain \'end\' or \'stop\'')
        }
        if (actions.isEmpty()) {
            throw new IllegalArgumentException('Diagram must contain at least one action transition')
        }
    }

    private PetriNet constructPetriNet(List<String> actions) {
        int n = actions.size()
        Place startPlace = new Place('P_start', 0, START)
        Place endPlace = new Place('P_end', n, END)

        List<Place> places = [startPlace]
        for (int i = 1; i < n; i++) {
            places.add(new Place("P_${i}", i, "P_${i}"))
        }
        places.add(endPlace)

        List<Transition> transitions = []
        for (int i = 0; i < n; i++) {
            transitions.add(new Transition("T_${i}", i, actions[i], actions[i]))
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
