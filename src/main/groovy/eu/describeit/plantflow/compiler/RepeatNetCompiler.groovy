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
class RepeatNetCompiler {

    private static final String ERR_REPEAT_NODE_NULL = 'RepeatNode cannot be null'
    private static final String ERR_BODY_EMPTY = 'Repeat loop body must contain at least one action'

    private int loopCounter = 0
    private int condCounter = 0

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
        condCounter = 0
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
        compileChain(new ChainContext(builder, loopIndex, null), node.body, loopStart, decisionPlace)
    }

    private void compileChain(ChainContext ctx, List<ActivityNode> items, Place fromPlace, Place toPlace) {
        Place currentPlace = fromPlace
        for (int i = 0; i < items.size(); i++) {
            ActivityNode item = items.get(i)
            boolean last = i == items.size() - 1
            Place nextPlace = last ? toPlace : ctx.builder.addPlace(nextIntermediatePlace(ctx, i + 1))
            switch (item) {
                case ActionNode:
                    ActionNode action = (ActionNode) item
                    Transition transition = ctx.builder.addTransition(action.action, action.action, null)
                    ctx.builder.connect(currentPlace, transition)
                    ctx.builder.connect(transition, nextPlace)
                    break
                case RepeatNode:
                    compileLoop(ctx.builder, (RepeatNode) item, currentPlace, nextPlace)
                    break
                case ConditionalNode:
                    compileConditional(ctx, (ConditionalNode) item, currentPlace, nextPlace)
                    break
                default:
                    break
            }
            currentPlace = nextPlace
        }
    }

    private void compileConditional(ChainContext ctx, ConditionalNode node, Place fromPlace, Place toPlace) {
        int condIndex = ++condCounter
        PetriNetBuilder builder = ctx.builder
        Place decisionPlace = builder.addPlace(NetLabeling.repeatIfDecisionPlace(ctx.loopIndex, condIndex))
        Transition toDecision = builder.addTransition(NetLabeling.repeatIfToDecisionTransition(ctx.loopIndex, condIndex))
        Transition branchYes = builder.addTransition(NetLabeling.repeatBranchYesTransition(ctx.loopIndex, condIndex), null, node.guardCondition)
        Transition branchNo = builder.addTransition(NetLabeling.repeatBranchNoTransition(ctx.loopIndex, condIndex), null, NetLabeling.negateGuard(node.guardCondition))
        builder.connect(fromPlace, toDecision)
        builder.connect(toDecision, decisionPlace)
        builder.connect(decisionPlace, branchYes)
        builder.connect(decisionPlace, branchNo)

        Place thenStart = builder.addPlace(NetLabeling.repeatThenPlace(ctx.loopIndex, condIndex, 0))
        Place elseStart = builder.addPlace(NetLabeling.repeatElsePlace(ctx.loopIndex, condIndex, 0))
        builder.connect(branchYes, thenStart)
        builder.connect(branchNo, elseStart)

        compileChain(branchContext(ctx, condIndex, true), node.thenActions, thenStart, toPlace)
        compileChain(branchContext(ctx, condIndex, false), node.elseActions, elseStart, toPlace)
    }

    private ChainContext branchContext(ChainContext ctx, int condIndex, boolean thenBranch) {
        new ChainContext(ctx.builder, ctx.loopIndex, NetLabeling.repeatBranchPlacePrefix(ctx.loopIndex, condIndex, thenBranch))
    }

    private String nextIntermediatePlace(ChainContext ctx, int index) {
        ctx.placePrefix != null ? ctx.placePrefix + index : NetLabeling.repeatBodyPlace(ctx.loopIndex, index)
    }

    private static class ChainContext {
        final PetriNetBuilder builder
        final int loopIndex
        final String placePrefix

        ChainContext(PetriNetBuilder builder, int loopIndex, String placePrefix) {
            this.builder = builder
            this.loopIndex = loopIndex
            this.placePrefix = placePrefix
        }
    }
}
