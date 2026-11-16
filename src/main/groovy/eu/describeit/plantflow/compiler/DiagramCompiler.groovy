package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.ast.RepeatNode
import eu.describeit.plantflow.engine.PetriNetStructure
import groovy.transform.CompileStatic

@CompileStatic
class DiagramCompiler {

    private static final String ERR_DIAGRAM_NULL = 'Diagram cannot be null'
    private static final String ERR_THEN_EMPTY = 'Then block must contain at least one action'

    private final ForkNetCompiler forkCompiler = new ForkNetCompiler()
    private final ChainNetCompiler chainCompiler = new ChainNetCompiler()

    PetriNetStructure compile(ActivityDiagram diagram) {
        if (diagram == null) throw new IllegalArgumentException(ERR_DIAGRAM_NULL)
        ForkNode fork = findForkNode(diagram)
        if (fork != null) return forkCompiler.compile(fork, extractPostForkActions(diagram, fork))
        if (findConditionalNode(diagram) == null && findRepeatNode(diagram) == null && diagram.isConditional()) {
            throw new IllegalArgumentException(ERR_THEN_EMPTY)
        }
        return chainCompiler.compile(diagram)
    }

    private static ConditionalNode findConditionalNode(ActivityDiagram diagram) {
        diagram.nodes.find { ActivityNode n -> n instanceof ConditionalNode } as ConditionalNode
    }

    private static RepeatNode findRepeatNode(ActivityDiagram diagram) {
        diagram.nodes.find { ActivityNode n -> n instanceof RepeatNode } as RepeatNode
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
}
