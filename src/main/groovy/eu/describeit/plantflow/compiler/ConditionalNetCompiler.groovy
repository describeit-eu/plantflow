package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.RepeatNode
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic

@CompileStatic
class ConditionalNetCompiler {

    private static final String ERR_CONDITIONAL_NODE_NULL = 'ConditionalNode cannot be null'
    private static final String ERR_THEN_EMPTY = 'Then block must contain at least one action'
    private static final String ERR_ELSE_EMPTY = 'Else block must contain at least one action'
    private static final String ERR_COMPOSED_BRANCH = 'Conditional branch contains a composed node that is not supported here yet'

    private final RepeatNetCompiler repeatLoops = new RepeatNetCompiler()

    PetriNetStructure compile(ConditionalNode node) {
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

    private PetriNetStructure assembleNet(ConditionalNode node) {
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

        builder.with {
            connect(startPlace, startToDecision)
            connect(startToDecision, decisionPlace)
            connect(decisionPlace, branchYes)
            connect(branchYes, thenPlaces.get(0))
            connect(decisionPlace, branchNo)
            connect(branchNo, elsePlaces.get(0))
        }

        compileBranchChain(builder, node.thenActions, thenPlaces, endifPlace)
        compileBranchChain(builder, node.elseActions, elsePlaces, endifPlace)

        Transition endifToEnd = builder.addTransition(NetLabeling.TRANSITION_ENDIF_TO_END)
        builder.connect(endifPlace, endifToEnd)
        builder.connect(endifToEnd, endPlace)

        builder.startPlace = startPlace
        builder.endPlace = endPlace

        return builder.build()
    }

    private List<Place> generateBranchPlaces(PetriNetBuilder builder, String startLabel, String prefix, int count) {
        List<Place> places = []
        places.add(builder.addPlace(startLabel))
        for (int i = 1; i < count; i++) {
            places.add(builder.addPlace(prefix + i))
        }
        return places
    }

    private void compileBranchChain(PetriNetBuilder builder, List<ActivityNode> branch, List<Place> places, Place endifPlace) {
        Place currentPlace = places.get(0)
        for (int i = 0; i < branch.size(); i++) {
            ActivityNode item = branch.get(i)
            boolean last = i == branch.size() - 1
            Place nextPlace = last ? endifPlace : places.get(i + 1)
            switch (item) {
                case ActionNode:
                    ActionNode action = (ActionNode) item
                    Transition transition = builder.addTransition(action.action, action.action, null)
                    builder.connect(currentPlace, transition)
                    builder.connect(transition, nextPlace)
                    break
                case RepeatNode:
                    repeatLoops.compileLoopInto(builder, (RepeatNode) item, currentPlace, nextPlace)
                    break
                default:
                    throw new IllegalArgumentException(ERR_COMPOSED_BRANCH)
            }
            currentPlace = nextPlace
        }
    }
}
