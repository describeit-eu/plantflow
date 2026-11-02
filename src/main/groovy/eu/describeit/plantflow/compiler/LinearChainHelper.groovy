package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic

@CompileStatic
class LinearChainHelper {

    private LinearChainHelper() {
    }

    static void connectLinearActions(PetriNetBuilder builder, List<Place> places, List<ActionNode> actionNodes) {
        for (int i = 0; i < actionNodes.size(); i++) {
            ActionNode node = actionNodes.get(i)
            Transition transition = builder.addTransition(node.action, node.action, null)
            builder.connect(places.get(i), transition)
            builder.connect(transition, places.get(i + 1))
        }
    }
}
