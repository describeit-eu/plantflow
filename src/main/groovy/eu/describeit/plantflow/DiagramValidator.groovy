package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import groovy.transform.CompileStatic

@CompileStatic
class DiagramValidator {

    public static final String ERR_FILE_NULL = 'File cannot be null'
    public static final String ERR_CONTENT_EMPTY = 'PlantUML content cannot be empty'
    public static final String ERR_MUST_CONTAIN_START = "Diagram must contain 'start'"
    public static final String ERR_MUST_CONTAIN_END = "Diagram must contain 'end' or 'stop'"
    public static final String ERR_MUST_CONTAIN_ACTION = 'Diagram must contain at least one action transition'
    public static final String ERR_NO_IF = 'Diagram contains if-then-else but no if statement found'
    public static final String ERR_NO_ELSE = 'if-then-else construct must have an else clause'
    public static final String ERR_NO_ENDIF = 'if-then-else construct must have an endif'
    public static final String ERR_THEN_EMPTY = 'Then block must contain at least one action'
    public static final String ERR_ELSE_EMPTY = 'Else block must contain at least one action'
    public static final String ERR_DIAGRAM_NULL = 'Diagram cannot be null'

    static void validate(ActivityDiagram diagram) {
        if (diagram == null) {
            throw new IllegalArgumentException(ERR_DIAGRAM_NULL)
        }

        boolean isConditional = diagram.isConditional()

        if (isConditional) {
            validateConditionalStructure(diagram)
        } else {
            validateStartAndEnd(diagram.hasStart, diagram.hasEnd)
        }

        validateNodes(diagram, isConditional)
    }

    static void validateIfThenElseStructure(
        boolean hasIf,
        boolean hasElse,
        boolean hasEndif,
        boolean hasStart,
        boolean hasEnd
    ) {
        if (!hasIf) {
            throw new IllegalArgumentException(ERR_NO_IF)
        }
        if (!hasElse) {
            throw new IllegalArgumentException(ERR_NO_ELSE)
        }
        if (!hasEndif) {
            throw new IllegalArgumentException(ERR_NO_ENDIF)
        }
        validateStartAndEnd(hasStart, hasEnd)
    }

    static void validateBranchActions(List<?> thenActions, List<?> elseActions) {
        if (thenActions == null || thenActions.isEmpty()) {
            throw new IllegalArgumentException(ERR_THEN_EMPTY)
        }
        if (elseActions == null || elseActions.isEmpty()) {
            throw new IllegalArgumentException(ERR_ELSE_EMPTY)
        }
    }

    static void validateDiagramStructure(boolean hasStart, boolean hasEnd, List<?> actions) {
        validateStartAndEnd(hasStart, hasEnd)
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException(ERR_MUST_CONTAIN_ACTION)
        }
    }

    static void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_CONTENT_EMPTY)
        }
    }

    static void validateFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException(ERR_FILE_NULL)
        }
    }

    private static void validateConditionalStructure(ActivityDiagram diagram) {
        boolean hasConditionalFlags = diagram.hasIf || diagram.hasElse || diagram.hasEndif
        if (hasConditionalFlags) {
            if (!diagram.hasIf) {
                throw new IllegalArgumentException(ERR_NO_IF)
            }
            if (!diagram.hasElse) {
                throw new IllegalArgumentException(ERR_NO_ELSE)
            }
            if (!diagram.hasEndif) {
                throw new IllegalArgumentException(ERR_NO_ENDIF)
            }
        }
        validateStartAndEnd(diagram.hasStart, diagram.hasEnd)
    }

    private static void validateStartAndEnd(boolean hasStart, boolean hasEnd) {
        if (!hasStart) {
            throw new IllegalArgumentException(ERR_MUST_CONTAIN_START)
        }
        if (!hasEnd) {
            throw new IllegalArgumentException(ERR_MUST_CONTAIN_END)
        }
    }

    private static void validateNodes(ActivityDiagram diagram, boolean isConditional) {
        if (isConditional) {
            boolean hasConditionalNode = false
            for (ActivityNode node : diagram.nodes) {
                if (node instanceof ConditionalNode) {
                    hasConditionalNode = true
                    ConditionalNode condNode = (ConditionalNode) node
                    validateBranchActions(condNode.thenActions, condNode.elseActions)
                }
            }
            if (!hasConditionalNode && diagram.nodes.isEmpty()) {
                throw new IllegalArgumentException(ERR_THEN_EMPTY)
            }
        } else {
            if (diagram.nodes.isEmpty()) {
                throw new IllegalArgumentException(ERR_MUST_CONTAIN_ACTION)
            }
        }
    }
}
