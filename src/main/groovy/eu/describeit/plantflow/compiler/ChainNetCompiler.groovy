package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.RepeatNode
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic

/**
 * Recursive chain compiler: walks a node list and compiles Action, Conditional,
 * and Repeat nodes uniformly at any depth. Sequential nodes execute as one chain
 * in written order; a ConditionalNode's branches and a RepeatNode's body are
 * themselves compiled as chains. All labels are drawn from one shared
 * {@link NamingContext} so they stay unique at any depth.
 */
@CompileStatic
class ChainNetCompiler {

    private static final String ERR_NO_ACTIONS = 'Diagram must contain at least one action transition'
    private static final String ERR_THEN_EMPTY = 'Then block must contain at least one action'
    private static final String ERR_ELSE_EMPTY = 'Else block must contain at least one action'
    private static final String ERR_REPEAT_BODY_EMPTY = 'Repeat loop body must contain at least one action'
    private static final String ERR_UNSUPPORTED_NODE = 'Unsupported node type in chain: '
    private static final String BRANCH_PLACE_SEPARATOR = '_'

    PetriNetStructure compile(ActivityDiagram diagram) {
        List<ActivityNode> nodes = diagram.nodes
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(ERR_NO_ACTIONS)
        }
        PetriNetBuilder builder = new PetriNetBuilder()
        Place startPlace = builder.addPlace(NetLabeling.PLACE_START)
        ChainContext ctx = new ChainContext(builder, new NamingContext(), NetLabeling.PREFIX_INTERMEDIATE_PLACE)
        // a null toPlace at the top level creates the end place lazily, so it
        // is appended after all chain intermediate places
        compileChain(ctx, nodes, startPlace, null)
        Place endPlace = builder.places.find { Place place -> place.label == NetLabeling.PLACE_END }
        builder.startPlace = startPlace
        builder.endPlace = endPlace
        return builder.build()
    }

    private void compileChain(ChainContext ctx, List<ActivityNode> nodes, Place fromPlace, Place toPlace) {
        Place currentPlace = fromPlace
        int index = 0
        for (int i = 0; i < nodes.size(); i++) {
            ActivityNode node = nodes.get(i)
            boolean last = i == nodes.size() - 1
            Place nextPlace = last ?
                (toPlace != null ? toPlace : ctx.builder.addPlace(NetLabeling.PLACE_END)) :
                ctx.builder.addPlace(ctx.placePrefix + (++index))
            switch (node) {
                case ActionNode:
                    ActionNode action = (ActionNode) node
                    Transition transition = ctx.builder.addTransition(action.action, action.action, null)
                    ctx.builder.connect(currentPlace, transition)
                    ctx.builder.connect(transition, nextPlace)
                    break
                case ConditionalNode:
                    compileConditional(ctx, (ConditionalNode) node, currentPlace, nextPlace)
                    break
                case RepeatNode:
                    compileRepeat(ctx, (RepeatNode) node, currentPlace, nextPlace)
                    break
                default:
                    throw new IllegalArgumentException(ERR_UNSUPPORTED_NODE + node.getClass().getName())
            }
            currentPlace = nextPlace
        }
    }

    private void compileConditional(ChainContext ctx, ConditionalNode node, Place fromPlace, Place exitTarget) {
        if (node.thenActions.isEmpty()) {
            throw new IllegalArgumentException(ERR_THEN_EMPTY)
        }
        if (node.elseActions.isEmpty()) {
            throw new IllegalArgumentException(ERR_ELSE_EMPTY)
        }
        int id = ctx.naming.nextConstructId()
        PetriNetBuilder builder = ctx.builder
        Place decisionPlace = builder.addPlace(ctx.naming.ifDecisionPlace(id))
        Transition toDecision = builder.addTransition(ctx.naming.ifToDecisionTransition(id))
        Transition branchYes = builder.addTransition(ctx.naming.ifBranchYesTransition(id), null, node.guardCondition)
        Transition branchNo = builder.addTransition(ctx.naming.ifBranchNoTransition(id), null, NetLabeling.negateGuard(node.guardCondition))
        builder.connect(fromPlace, toDecision)
        builder.connect(toDecision, decisionPlace)
        builder.connect(decisionPlace, branchYes)
        builder.connect(decisionPlace, branchNo)

        Place thenStart = builder.addPlace(ctx.naming.ifThenPlace(id))
        Place elseStart = builder.addPlace(ctx.naming.ifElsePlace(id))
        builder.connect(branchYes, thenStart)
        builder.connect(branchNo, elseStart)

        Place mergePlace = builder.addPlace(ctx.naming.ifEndifPlace(id))
        compileChain(ctx.withPlacePrefix(ctx.naming.ifThenPlace(id) + BRANCH_PLACE_SEPARATOR), node.thenActions, thenStart, mergePlace)
        compileChain(ctx.withPlacePrefix(ctx.naming.ifElsePlace(id) + BRANCH_PLACE_SEPARATOR), node.elseActions, elseStart, mergePlace)

        Transition exit = builder.addTransition(ctx.naming.ifExitTransition(id))
        builder.connect(mergePlace, exit)
        builder.connect(exit, exitTarget)
    }

    private void compileRepeat(ChainContext ctx, RepeatNode node, Place loopStart, Place exitTarget) {
        if (node.body.isEmpty()) {
            throw new IllegalArgumentException(ERR_REPEAT_BODY_EMPTY)
        }
        int id = ctx.naming.nextConstructId()
        PetriNetBuilder builder = ctx.builder
        Place decisionPlace = builder.addPlace(NetLabeling.repeatDecisionPlace(id))

        String bodyPrefix = NetLabeling.PLACE_REPEAT_PREFIX + id + NetLabeling.PLACE_REPEAT_BODY_SUFFIX
        compileChain(ctx.withPlacePrefix(bodyPrefix), node.body, loopStart, decisionPlace)

        Transition loopBack = builder.addTransition(NetLabeling.repeatLoopBackTransition(id), null, node.loopCondition)
        builder.connect(decisionPlace, loopBack)
        builder.connect(loopBack, loopStart)

        if (node.detach) {
            Transition detachTransition = builder.addTransition(NetLabeling.repeatDetachTransition(id))
            builder.connect(decisionPlace, detachTransition)
        } else {
            Transition exit = builder.addTransition(NetLabeling.repeatExitTransition(id), null, NetLabeling.negateGuard(node.loopCondition))
            builder.connect(decisionPlace, exit)
            builder.connect(exit, exitTarget)
        }
    }

    private static class ChainContext {
        final PetriNetBuilder builder
        final NamingContext naming
        final String placePrefix

        ChainContext(PetriNetBuilder builder, NamingContext naming, String placePrefix) {
            this.builder = builder
            this.naming = naming
            this.placePrefix = placePrefix
        }

        ChainContext withPlacePrefix(String prefix) {
            return new ChainContext(builder, naming, prefix)
        }
    }
}
