package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.RepeatNode
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic

@CompileStatic
class RepeatNetCompiler {

    private static final String ERR_REPEAT_NODE_NULL = 'RepeatNode cannot be null'
    private static final String ERR_BODY_EMPTY = 'Repeat loop body must contain at least one action'

    private int loopCounter = 0

    PetriNetStructure compile(
        RepeatNode node,
        List<ActionNode> preActions = Collections.<ActionNode>emptyList(),
        List<ActionNode> postActions = Collections.<ActionNode>emptyList()) {
        if (node == null) {
            throw new IllegalArgumentException(ERR_REPEAT_NODE_NULL)
        }
        if (node.body.isEmpty()) {
            throw new IllegalArgumentException(ERR_BODY_EMPTY)
        }

        loopCounter = 0
        PetriNetBuilder builder = new PetriNetBuilder()
        Place startPlace = builder.addPlace(NetLabeling.PLACE_START)
        Place endPlace = builder.addPlace(NetLabeling.PLACE_END)
        builder.startPlace = startPlace
        builder.endPlace = endPlace

        Place loopEntry = connectPreActions(builder, startPlace, preActions)
        Place exitTarget = connectPostActions(builder, endPlace, postActions)
        compileLoop(builder, node, loopEntry, exitTarget)

        return builder.build()
    }

    private Place connectPreActions(PetriNetBuilder builder, Place startPlace, List<ActionNode> preActions) {
        if (preActions.isEmpty()) {
            return startPlace
        }
        Place loopStart = builder.addPlace(NetLabeling.repeatStartPlace(1))
        List<Place> intermediatePlaces = []
        for (int i = 1; i < preActions.size(); i++) {
            intermediatePlaces.add(builder.addPlace(NetLabeling.intermediatePlace(i)))
        }
        List<Place> places = [startPlace] + intermediatePlaces + [loopStart]
        LinearChainHelper.connectLinearActions(builder, places, preActions)
        return loopStart
    }

    private Place connectPostActions(PetriNetBuilder builder, Place endPlace, List<ActionNode> postActions) {
        if (postActions.isEmpty()) {
            return endPlace
        }
        Place continuationPlace = builder.addPlace(NetLabeling.repeatAfterPlace(1))
        List<Place> intermediatePlaces = []
        for (int i = 1; i < postActions.size(); i++) {
            intermediatePlaces.add(builder.addPlace(NetLabeling.repeatAfterPlace(1, i + 1)))
        }
        List<Place> places = [continuationPlace] + intermediatePlaces + [endPlace]
        LinearChainHelper.connectLinearActions(builder, places, postActions)
        return continuationPlace
    }

    private void compileLoop(PetriNetBuilder builder, RepeatNode node, Place loopStart, Place exitTarget) {
        int loopIndex = ++loopCounter
        Place decisionPlace = builder.addPlace(NetLabeling.repeatDecisionPlace(loopIndex))

        compileBody(builder, node, loopIndex, loopStart, decisionPlace)

        Transition loopBack = builder.addTransition(NetLabeling.repeatLoopBackTransition(loopIndex), null, node.loopCondition)
        builder.connect(decisionPlace, loopBack)
        builder.connect(loopBack, loopStart)

        if (node.detach) {
            Transition detachTransition = builder.addTransition(NetLabeling.repeatDetachTransition(loopIndex))
            builder.connect(decisionPlace, detachTransition)
        } else {
            Transition exit = builder.addTransition(NetLabeling.repeatExitTransition(loopIndex), null, NetLabeling.negateGuard(node.loopCondition))
            builder.connect(decisionPlace, exit)
            builder.connect(exit, exitTarget)
        }
    }

    private void compileBody(PetriNetBuilder builder, RepeatNode node, int loopIndex, Place loopStart, Place decisionPlace) {
        Place currentPlace = loopStart
        List<ActivityNode> body = node.body
        for (int i = 0; i < body.size(); i++) {
            ActivityNode item = body.get(i)
            boolean last = i == body.size() - 1
            if (item instanceof ActionNode) {
                ActionNode action = (ActionNode) item
                Transition transition = builder.addTransition(action.action, action.action, null)
                Place nextPlace = last ? decisionPlace : builder.addPlace(NetLabeling.repeatBodyPlace(loopIndex, i + 1))
                builder.connect(currentPlace, transition)
                builder.connect(transition, nextPlace)
                currentPlace = nextPlace
            } else if (item instanceof RepeatNode) {
                RepeatNode innerLoop = (RepeatNode) item
                Place innerExit = last ? decisionPlace : builder.addPlace(NetLabeling.repeatBodyPlace(loopIndex, i + 1))
                compileLoop(builder, innerLoop, currentPlace, innerExit)
                currentPlace = innerExit
            }
        }
    }
}
