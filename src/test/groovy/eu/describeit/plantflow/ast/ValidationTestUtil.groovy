package eu.describeit.plantflow.ast
/**
 * Utility class for creating test AST nodes for validation testing.
 */
class ValidationTestUtil {

    /**
     * Creates an ActionNode with the given action string.
     * Handles trimming and validation.
     */
    static ActionNode action(String action) {
        return new ActionNode(action)
    }

    /**
     * Creates a list of ActionNodes from the given action strings.
     */
    static List<ActionNode> actions(String... actionStrings) {
        return actionStrings.collect { new ActionNode(it) }
    }

    /**
     * Creates a ConditionalNode with the given guard condition and action lists.
     */
    static ConditionalNode conditional(String guardCondition, List<ActionNode> thenActions, List<ActionNode> elseActions) {
        return new ConditionalNode(guardCondition, thenActions, elseActions)
    }

    /**
     * Creates a ConditionalNode with string-based action lists.
     */
    static ConditionalNode conditional(String guardCondition, String[] thenActionStrings, String[] elseActionStrings) {
        return new ConditionalNode(guardCondition, actions(*thenActionStrings), actions(*elseActionStrings))
    }

    /**
     * Creates a simple ActivityDiagram with the given flags and nodes.
     */
    static ActivityDiagram diagram(boolean hasStart, boolean hasEnd, List<ActivityNode> nodes) {
        return new ActivityDiagram(hasStart, hasEnd, nodes)
    }

    /**
     * Creates an ActivityDiagram using fluent builder pattern.
     */
    static ActivityDiagram diagram(Closure<ActivityDiagram> builder) {
        def diagram = new ActivityDiagram()
        builder.delegate = diagram
        builder.call(diagram)
        return diagram
    }
}
