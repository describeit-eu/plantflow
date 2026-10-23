package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.PetriNetStructure
import groovy.transform.CompileStatic

@CompileStatic
class DiagramCompiler {

    private static final String ERR_DIAGRAM_NULL = 'Diagram cannot be null'
    private static final String ERR_THEN_EMPTY = 'Then block must contain at least one action'

    private final LinearNetCompiler linearCompiler = new LinearNetCompiler()
    private final ConditionalNetCompiler conditionalCompiler = new ConditionalNetCompiler()
    private final ForkNetCompiler forkCompiler = new ForkNetCompiler()
    PetriNetStructure compile(ActivityDiagram diagram) {
        if (diagram == null) throw new IllegalArgumentException(ERR_DIAGRAM_NULL)
        ConditionalNode cond = findConditionalNode(diagram)
        if (cond != null) return conditionalCompiler.compile(cond)
        ForkNode fork = findForkNode(diagram)
        if (fork != null) return forkCompiler.compile(fork, extractPostForkActions(diagram, fork))
        if (diagram.isConditional()) throw new IllegalArgumentException(ERR_THEN_EMPTY)
        return linearCompiler.compile(extractActionNodes(diagram))
    }

    private static ConditionalNode findConditionalNode(ActivityDiagram diagram) {
        diagram.nodes.find { ActivityNode n -> n instanceof ConditionalNode } as ConditionalNode
    }
    private static ForkNode findForkNode(ActivityDiagram diagram) {
        diagram.nodes.find { ActivityNode n -> n instanceof ForkNode } as ForkNode
    }
    private static List<ActionNode> extractPostForkActions(ActivityDiagram diagram, ForkNode forkNode) {
        boolean forkFound = false
        List<ActionNode> postForkActions = []
        for (ActivityNode node : diagram.nodes) {
            if (node.is(forkNode)) { forkFound = true; continue }
            if (forkFound && node instanceof ActionNode) postForkActions.add((ActionNode) node)
        }
        return postForkActions
    }
    private static List<ActionNode> extractActionNodes(ActivityDiagram diagram) {
        diagram.nodes.findAll { ActivityNode n -> n instanceof ActionNode } as List<ActionNode>
    }
}
