package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic

@CompileStatic
class ConditionalNetCompiler {

    private static final String ERR_CONDITIONAL_NODE_NULL = 'ConditionalNode cannot be null'
    private static final String ERR_THEN_EMPTY = 'Then block must contain at least one action'
    private static final String ERR_ELSE_EMPTY = 'Else block must contain at least one action'

    PetriNet compile(ConditionalNode node) {
        validate(node)
        return assembleNet(node)
    }

    private static void validate(ConditionalNode node) {
        if (node == null) {
            throw new IllegalArgumentException(ERR_CONDITIONAL_NODE_NULL)
        }
        if (node.thenActions.isEmpty()) {
            throw new IllegalArgumentException(ERR_THEN_EMPTY)
        }
        if (node.elseActions.isEmpty()) {
            throw new IllegalArgumentException(ERR_ELSE_EMPTY)
        }
    }

    private static PetriNet assembleNet(ConditionalNode node) {
        PetriNetBuilder builder = new PetriNetBuilder()
        Place startPlace = builder.addPlace(NetLabeling.PLACE_START)
        Place decisionPlace = builder.addPlace(NetLabeling.PLACE_IF_DECISION)
        List<Place> thenPlaces = generateBranchPlaces(builder, NetLabeling.PLACE_THEN, NetLabeling.PREFIX_THEN_PLACE, node.thenActions.size())
        List<Place> elsePlaces = generateBranchPlaces(builder, NetLabeling.PLACE_ELSE, NetLabeling.PREFIX_ELSE_PLACE, node.elseActions.size())
        Place endifPlace = builder.addPlace(NetLabeling.PLACE_ENDIF)
        Place endPlace = builder.addPlace(NetLabeling.PLACE_END)

        Transition startToDecision = builder.addTransition(NetLabeling.TRANSITION_START_TO_DECISION)
        Transition branchYes = builder.addTransition(NetLabeling.TRANSITION_BRANCH_YES, null, node.guardCondition)
        Transition branchNo = builder.addTransition(NetLabeling.TRANSITION_BRANCH_NO, null, NetLabeling.negateGuard(node.guardCondition))
        List<Transition> thenTransitions = generateActionTransitions(builder, node.thenActions)
        List<Transition> elseTransitions = generateActionTransitions(builder, node.elseActions)
        Transition endifToEnd = builder.addTransition(NetLabeling.TRANSITION_ENDIF_TO_END)

        connectBranchActions(builder, thenPlaces, thenTransitions, endifPlace)
        connectBranchActions(builder, elsePlaces, elseTransitions, endifPlace)

        builder.startPlace = startPlace
        builder.endPlace = endPlace

        builder.with {
            connect(startPlace, startToDecision)
            connect(startToDecision, decisionPlace)
            connect(decisionPlace, branchYes)
            connect(branchYes, thenPlaces.get(0))
            connect(decisionPlace, branchNo)
            connect(branchNo, elsePlaces.get(0))
            connect(endifPlace, endifToEnd)
            connect(endifToEnd, endPlace)
        }

        return builder.build()
    }

    private static List<Place> generateBranchPlaces(PetriNetBuilder builder, String startLabel, String prefix, int count) {
        List<Place> places = []
        places.add(builder.addPlace(startLabel))
        for (int i = 1; i < count; i++) {
            places.add(builder.addPlace(prefix + i))
        }
        return places
    }

    private static List<Transition> generateActionTransitions(PetriNetBuilder builder, List<ActionNode> actionNodes) {
        List<Transition> transitions = []
        for (ActionNode node : actionNodes) {
            transitions.add(builder.addTransition(node.action, node.action, null))
        }
        return transitions
    }

    private static void connectBranchActions(PetriNetBuilder builder, List<Place> places, List<Transition> transitions, Place endifPlace) {
        for (int i = 0; i < places.size(); i++) {
            Place fromPlace = places.get(i)
            Transition transition = transitions.get(i)
            Place toPlace = (i < places.size() - 1) ? places.get(i + 1) : endifPlace
            builder.connect(fromPlace, transition)
            builder.connect(transition, toPlace)
        }
    }
}
