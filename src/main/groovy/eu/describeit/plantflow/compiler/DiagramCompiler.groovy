package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.ast.RepeatNode
import eu.describeit.plantflow.ast.SwitchCase
import eu.describeit.plantflow.ast.SwitchNode
import eu.describeit.plantflow.engine.PetriNetStructure
import groovy.transform.CompileStatic

/**
 * Facade compiler: every diagram is compiled by the single recursive
 * {@link ChainNetCompiler}, which handles Action, Conditional, Repeat, and
 * Fork nodes uniformly at any depth.
 */
@CompileStatic
class DiagramCompiler {

    private static final String ERR_DIAGRAM_NULL = 'Diagram cannot be null'
    private static final String ERR_THEN_EMPTY = 'Then block must contain at least one action'

    private final ChainNetCompiler chainCompiler = new ChainNetCompiler()

    PetriNetStructure compile(ActivityDiagram diagram) {
        if (diagram == null) throw new IllegalArgumentException(ERR_DIAGRAM_NULL)
        if (diagram.isConditional() &&
            !containsNode(diagram.nodes, ConditionalNode) &&
            !containsNode(diagram.nodes, RepeatNode) &&
            !containsNode(diagram.nodes, ForkNode) &&
            !containsNode(diagram.nodes, SwitchNode)) {
            throw new IllegalArgumentException(ERR_THEN_EMPTY)
        }
        return chainCompiler.compile(diagram)
    }

    private static boolean containsNode(List<ActivityNode> nodes, Class<? extends ActivityNode> type) {
        return nodes.any { ActivityNode node -> type.isInstance(node) || containsNode(childrenOf(node), type) }
    }

    private static List<ActivityNode> childrenOf(ActivityNode node) {
        switch (node) {
            case ConditionalNode:
                ConditionalNode conditional = (ConditionalNode) node
                return conditional.thenActions + conditional.elseActions
            case RepeatNode:
                return ((RepeatNode) node).body
            case ForkNode:
                List<ActivityNode> children = []
                for (List<ActivityNode> branch : ((ForkNode) node).branches) {
                    children.addAll(branch)
                }
                return children
            case SwitchNode:
                List<ActivityNode> caseChildren = []
                for (SwitchCase switchCase : ((SwitchNode) node).cases) {
                    caseChildren.addAll(switchCase.actions)
                }
                return caseChildren
            default:
                return []
        }
    }
}
