package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ConditionalNode
import groovy.transform.CompileStatic

@CompileStatic
class ConditionalDiagramBuilder implements DiagramBuilder {

    private enum Branch { NONE, THEN, ELSE }

    ActivityDiagram build(List<ClassifiedLine> lines) {
        DiagramState state = new DiagramState()
        for (ClassifiedLine line : lines) {
            processLine(line, state)
        }
        return buildDiagram(state)
    }

    private static void processLine(ClassifiedLine line, DiagramState state) {
        switch (line.type) {
            case LineType.START:
                state.hasStart = true
                break
            case LineType.END:
                state.hasEnd = true
                break
            case LineType.IF:
                processIf(line, state)
                break
            case LineType.ELSE:
                state.hasElse = true
                state.branch = Branch.ELSE
                break
            case LineType.ENDIF:
                state.hasEndif = true
                state.branch = Branch.NONE
                break
            case LineType.ACTION:
                processAction(line, state)
                break
            default:
                break
        }
    }

    private static void processIf(ClassifiedLine line, DiagramState state) {
        if (line.guard != null) {
            state.hasIf = true
            state.guard = line.guard
            state.branch = Branch.THEN
        }
    }

    private static void processAction(ClassifiedLine line, DiagramState state) {
        ActionNode node = new ActionNode(line.action)
        if (state.branch == Branch.THEN) {
            state.thenActions.add(node)
        } else if (state.branch == Branch.ELSE) {
            state.elseActions.add(node)
        }
    }

    private static ActivityDiagram buildDiagram(DiagramState state) {
        ActivityDiagram diagram = new ActivityDiagram(state.hasStart, state.hasEnd, [])
        diagram.hasIf = state.hasIf
        diagram.hasElse = state.hasElse
        diagram.hasEndif = state.hasEndif
        if (state.guard != null) {
            diagram.addNode(new ConditionalNode(state.guard, state.thenActions, state.elseActions))
        } else if (!state.hasElse && !state.hasEndif) {
            diagram.hasEndif = true
        }
        return diagram
    }

    private static class DiagramState {
        boolean hasStart = false
        boolean hasEnd = false
        boolean hasIf = false
        boolean hasElse = false
        boolean hasEndif = false
        String guard = null
        List<ActionNode> thenActions = []
        List<ActionNode> elseActions = []
        Branch branch = Branch.NONE
    }
}
