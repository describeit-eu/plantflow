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
    private static final Pattern IF_PATTERN = Pattern.compile(/^\s*if\s*\(\s*(.+?)\s*\)\s*then\s*\(\s*(.+?)\s*\)\s*$/)
    private static final Pattern ELSE_PATTERN = Pattern.compile(/^\s*else\s*\(\s*(.+?)\s*\)\s*$/)
    private static final Pattern ENDIF_PATTERN = Pattern.compile(/^\s*endif\s*$/)
    private static final String START = 'start'
    private static final String END = 'end'
    private static final String STOP = 'stop'
    private static final String IF = 'if'
    private static final String ELSE = 'else'
    private static final String ENDIF = 'endif'

    PetriNet parse(File file) {
        if (file == null) throw new IllegalArgumentException('File cannot be null')
        return parse(file.getText(StandardCharsets.UTF_8.name()))
    }

    PetriNet parse(String pumlContent) {
        if (pumlContent == null || pumlContent.trim().isEmpty()) {
            throw new IllegalArgumentException('PlantUML content cannot be empty')
        }

        List<String> lines = pumlContent.readLines()
        
        // Check if the diagram contains if-then-else-endif
        if (lines.any { it.trim().startsWith('if') }) {
            return parseConditionalDiagram(lines)
        } else {
            List<String> actions = extractActions(lines)
            return constructPetriNet(actions)
        }
    }

    private PetriNet parseConditionalDiagram(List<String> lines) {
        String guardCondition = null
        String thenLabel = null
        String elseLabel = null
        String thenAction = null
        String elseAction = null
        Boolean hasStart = false
        Boolean hasEnd = false
        Boolean inIfBlock = false
        Boolean inThenBlock = false
        Boolean inElseBlock = false
        Boolean hasIf = false
        Boolean hasElse = false
        Boolean hasEndif = false

        for (String rawLine : lines) {
            String line = rawLine.trim()
            Boolean skip = false

            (skip, hasStart, hasEnd) = checkLine(line, hasStart, hasEnd)

            if (skip) {
                continue
            }

            // Parse if statement
            Matcher ifMatcher = IF_PATTERN.matcher(line)
            if (ifMatcher.matches()) {
                guardCondition = ifMatcher.group(1).trim()
                thenLabel = ifMatcher.group(2).trim()
                hasIf = true
                inIfBlock = true
                inThenBlock = true
                continue
            }

            // Parse else statement
            Matcher elseMatcher = ELSE_PATTERN.matcher(line)
            if (elseMatcher.matches()) {
                elseLabel = elseMatcher.group(1).trim()
                hasElse = true
                inIfBlock = false
                inThenBlock = false
                inElseBlock = true
                continue
            }

            // Parse endif statement
            if (ENDIF_PATTERN.matcher(line).matches()) {
                hasEndif = true
                inIfBlock = false
                inThenBlock = false
                inElseBlock = false
                continue
            }

            // Parse action
            Matcher actionMatcher = ACTION_PATTERN.matcher(line)
            if (actionMatcher.matches()) {
                String action = actionMatcher.group(1).trim()
                if (inThenBlock) {
                    thenAction = action
                } else if (inElseBlock) {
                    elseAction = action
                }
            }
        }

        // Validate if-then-else structure
        validateIfThenElseStructure(hasIf, hasElse, hasEndif, hasStart, hasEnd)

        // Build the Petri net with branching
        return constructConditionalPetriNet(guardCondition, thenLabel, elseLabel, thenAction, elseAction, hasStart, hasEnd)
    }

    private void validateIfThenElseStructure(Boolean hasIf, Boolean hasElse, Boolean hasEndif, Boolean hasStart, Boolean hasEnd) {
        if (!hasIf) {
            throw new IllegalArgumentException('Diagram contains if-then-else but no if statement found')
        }
        if (!hasElse) {
            throw new IllegalArgumentException('if-then-else construct must have an else clause')
        }
        if (!hasEndif) {
            throw new IllegalArgumentException('if-then-else construct must have an endif')
        }
        if (!hasStart) {
            throw new IllegalArgumentException('Diagram must contain \'start\'')
        }
        if (!hasEnd) {
            throw new IllegalArgumentException('Diagram must contain \'end\' or \'stop\'')
        }
    }

    private PetriNet constructConditionalPetriNet(String guardCondition, String thenLabel, String elseLabel, String thenAction, String elseAction, Boolean hasStart, Boolean hasEnd) {
        if (thenAction == null) {
            throw new IllegalArgumentException("thenAction cannot be null. Make sure there is an action in the then block.")
        }
        if (elseAction == null) {
            throw new IllegalArgumentException("elseAction cannot be null. Make sure there is an action in the else block.")
        }
        Place startPlace = new Place(0, START)
        Place ifDecisionPlace = new Place(1, 'P_if_decision')
        Place thenPlace = new Place(2, 'P_then')
        Place elsePlace = new Place(3, 'P_else')
        Place endifPlace = new Place(4, 'P_endif')
        Place endPlace = new Place(5, END)

        List<Place> places = [startPlace, ifDecisionPlace, thenPlace, elsePlace, endifPlace, endPlace]

        // Structural transitions (no action handlers required)
        Transition startToDecision = new Transition(0, 'T_start_to_decision', null, null)
        Transition branchYesTransition = new Transition(1, 'T_branch_yes', null, guardCondition)
        Transition branchNoTransition = new Transition(2, 'T_branch_no', null, '!(' + guardCondition + ')')
        
        // Action transitions (require action handlers)
        Transition thenActionTransition = new Transition(3, thenAction, thenAction, null)
        Transition elseActionTransition = new Transition(4, elseAction, elseAction, null)
        
        // Structural transition (no action handler required)
        Transition endifToEnd = new Transition(5, 'T_endif_to_end', null, null)

        List<Transition> transitions = [startToDecision, branchYesTransition, branchNoTransition, thenActionTransition, elseActionTransition, endifToEnd]

        // Build incidence matrix (6 places x 6 transitions)
        int[][] inputMatrix = new int[6][6]
        int[][] outputMatrix = new int[6][6]

        // T_0: start -> if_decision
        inputMatrix[0][0] = 1
        outputMatrix[1][0] = 1

        // T_1: branch yes - consumes from P_if_decision (1), produces to P_then (2)
        inputMatrix[1][1] = 1
        outputMatrix[2][1] = 1

        // T_2: branch no - consumes from P_if_decision (1), produces to P_else (3)
        inputMatrix[1][2] = 1
        outputMatrix[3][2] = 1

        // T_3: action then - consumes from P_then (2), produces to P_endif (4)
        inputMatrix[2][3] = 1
        outputMatrix[4][3] = 1

        // T_4: action else - consumes from P_else (3), produces to P_endif (4)
        inputMatrix[3][4] = 1
        outputMatrix[4][4] = 1

        // T_5: endif to end - consumes from P_endif (4), produces to P_end (5)
        inputMatrix[4][5] = 1
        outputMatrix[5][5] = 1

        IncidenceMatrix incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        return new DefaultPetriNet(places, transitions, incidenceMatrix, startPlace, endPlace)
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
        Place startPlace = new Place(0, START)
        Place endPlace = new Place(n, END)

        List<Place> places = [startPlace]
        for (int i = 1; i < n; i++) {
            places.add(new Place(i, "P_${i}"))
        }
        places.add(endPlace)

        List<Transition> transitions = []
        for (int i = 0; i < n; i++) {
            transitions.add(new Transition(i, actions[i], actions[i], null))
        }

        IncidenceMatrix incidenceMatrix = constructIncidenceMatrix(places.size(), transitions.size(), n)
        return new DefaultPetriNet(places, transitions, incidenceMatrix, startPlace, endPlace)
    }

    private IncidenceMatrix constructIncidenceMatrix(int numPlaces, int numTransitions, int n) {
        int[][] inputMatrix = new int[numPlaces][numTransitions]
        int[][] outputMatrix = new int[numPlaces][numTransitions]

        for (int i = 0; i < n; i++) {
            inputMatrix[i][i] = 1
            outputMatrix[i + 1][i] = 1
        }

        return new IncidenceMatrix(inputMatrix, outputMatrix)
    }
}
