package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.ast.RepeatNode
import eu.describeit.plantflow.engine.ForkDescriptor
import eu.describeit.plantflow.engine.JoinDescriptor
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic

/**
 * Recursive chain compiler: walks a node list and compiles Action, Conditional,
 * Repeat, and Fork nodes uniformly at any depth. Sequential nodes execute as one
 * chain in written order; a ConditionalNode's branches, a RepeatNode's body, and
 * a ForkNode's Parallel Branches are themselves compiled as chains. All labels are
 * drawn from one shared {@link NamingContext} so they stay unique at any depth.
 *
 * Forks reuse the per-activation ForkDescriptor/JoinDescriptor machinery at any
 * depth: every fork transition registers one activation per firing keyed by its
 * own transition index, a Join (`end fork`) waits for all Parallel Branches of
 * that activation, and a Merge (`end merge`) continues once on the first
 * completed branch. A fork nested inside another fork's branch registers its
 * own descriptors, so nested activations stay isolated from the enclosing
 * fork's activation automatically.
 */
@CompileStatic
class ChainNetCompiler {

    private static final String ERR_NO_ACTIONS = 'Diagram must contain at least one action transition'
    private static final String ERR_THEN_EMPTY = 'Then block must contain at least one action'
    private static final String ERR_ELSE_EMPTY = 'Else block must contain at least one action'
    private static final String ERR_REPEAT_BODY_EMPTY = 'Repeat loop body must contain at least one node'
    private static final String ERR_FORK_BRANCH_EMPTY = 'Fork branch must contain at least one node'
    private static final String ERR_UNSUPPORTED_NODE = 'Unsupported node type in chain: '
    private static final String BRANCH_PLACE_SEPARATOR = '_'

    PetriNetStructure compile(ActivityDiagram diagram) {
        List<ActivityNode> nodes = diagram.nodes
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(ERR_NO_ACTIONS)
        }
        PetriNetBuilder builder = new PetriNetBuilder()
        Place startPlace = builder.addPlace(NamingContext.PLACE_START)
        ChainContext ctx = new ChainContext(builder, new NamingContext(), NamingContext.PREFIX_INTERMEDIATE_PLACE, null)
        // a null toPlace at the top level creates the end place lazily, so it
        // is appended after all chain intermediate places
        compileChain(ctx, nodes, startPlace, null)
        Place endPlace = builder.places.find { Place place -> place.label == NamingContext.PLACE_END }
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
            if (node instanceof ForkNode) {
                // a fork compiles its own continuation place and, when it is the
                // last node of the chain, connects it to the chain target itself
                currentPlace = compileFork(ctx, (ForkNode) node, currentPlace, last, toPlace)
                continue
            }
            Place nextPlace = last ?
                (toPlace != null ? toPlace : ctx.builder.addPlace(NamingContext.PLACE_END)) :
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
        Transition branchNo = builder.addTransition(ctx.naming.ifBranchNoTransition(id), null, NamingContext.negateGuard(node.guardCondition))
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
        Place decisionPlace = builder.addPlace(ctx.naming.repeatDecisionPlace(id))

        String bodyPrefix = ctx.naming.repeatBodyPlacePrefix(id)
        compileChain(ctx.withPlacePrefix(bodyPrefix), node.body, loopStart, decisionPlace)

        Transition loopBack = builder.addTransition(ctx.naming.repeatLoopBackTransition(id), null, node.loopCondition)
        builder.connect(decisionPlace, loopBack)
        builder.connect(loopBack, loopStart)

        if (node.detach) {
            Transition detachTransition = builder.addTransition(ctx.naming.repeatDetachTransition(id))
            builder.connect(decisionPlace, detachTransition)
        } else {
            Transition exit = builder.addTransition(ctx.naming.repeatExitTransition(id), null, NamingContext.negateGuard(node.loopCondition))
            builder.connect(decisionPlace, exit)
            builder.connect(exit, exitTarget)
        }
    }

    /**
     * Compiles a ForkNode at any depth. Returns the place from which the chain
     * continues: the fork's own continuation place when the fork is not the last
     * chain node (the next node consumes from it directly), or the chain target
     * when it is the last node (connected via a fork to-end transition).
     */
    private Place compileFork(ChainContext ctx, ForkNode node, Place fromPlace, boolean last, Place toPlace) {
        validateForkBranches(node)
        int id = ctx.naming.nextConstructId()
        PetriNetBuilder builder = ctx.builder
        Transition forkTransition = builder.addTransition(ctx.naming.forkTransition(id))
        builder.connect(fromPlace, forkTransition)

        // every fork's branches carry that fork's own activation, at any depth
        List<Place> branchEndPlaces = compileForkBranches(ctx, node, id, forkTransition, forkTransition.index)

        Place continuationPlace = node.synchronizingJoin ?
            connectJoin(ctx, id, forkTransition, branchEndPlaces) :
            connectMerge(ctx, id, forkTransition, branchEndPlaces)

        if (!last) {
            return continuationPlace
        }
        return connectForkToTarget(ctx, id, continuationPlace, toPlace)
    }

    private void validateForkBranches(ForkNode node) {
        if (node.branches == null || node.branches.isEmpty()) {
            throw new IllegalArgumentException(ERR_FORK_BRANCH_EMPTY)
        }
        for (List<ActivityNode> branch : node.branches) {
            if (branch == null || branch.isEmpty()) {
                throw new IllegalArgumentException(ERR_FORK_BRANCH_EMPTY)
            }
        }
    }

    private List<Place> compileForkBranches(ChainContext ctx, ForkNode node, int id, Transition forkTransition, Integer branchEnclosingFork) {
        PetriNetBuilder builder = ctx.builder
        ChainContext branchCtx = ctx.withEnclosingFork(branchEnclosingFork)
        List<Place> branchEndPlaces = []
        int branchNumber = 0
        for (List<ActivityNode> branch : node.branches) {
            branchNumber++
            Place branchStart = builder.addPlace(ctx.naming.forkBranchPlace(id, branchNumber))
            builder.connect(forkTransition, branchStart)
            Place branchEnd = builder.addPlace(ctx.naming.forkBranchEndPlace(id, branchNumber))
            compileChain(branchCtx.withPlacePrefix(ctx.naming.forkBranchPlace(id, branchNumber) + BRANCH_PLACE_SEPARATOR),
                branch, branchStart, branchEnd)
            branchEndPlaces.add(branchEnd)
        }
        return branchEndPlaces
    }

    private Place connectForkToTarget(ChainContext ctx, int id, Place continuationPlace, Place toPlace) {
        PetriNetBuilder builder = ctx.builder
        Place target = toPlace != null ? toPlace : builder.addPlace(NamingContext.PLACE_END)
        Transition toEnd = builder.addTransition(ctx.naming.forkToEndTransition(id))
        builder.connect(continuationPlace, toEnd)
        builder.connect(toEnd, target)
        return target
    }

    private Place connectJoin(ChainContext ctx, int id, Transition forkTransition, List<Place> branchEndPlaces) {
        PetriNetBuilder builder = ctx.builder
        Place joinPlace = builder.addPlace(ctx.naming.forkJoinPlace(id))
        Transition joinTransition = builder.addTransition(ctx.naming.forkJoinTransition(id))
        for (Place branchEnd : branchEndPlaces) {
            builder.connect(branchEnd, joinTransition)
        }
        builder.connect(joinTransition, joinPlace)
        builder.addJoinDescriptor(new JoinDescriptor(forkTransition.index, joinTransition.index, branchEndPlaces*.index))
        return joinPlace
    }

    private Place connectMerge(ChainContext ctx, int id, Transition forkTransition, List<Place> branchEndPlaces) {
        PetriNetBuilder builder = ctx.builder
        Place mergePlace = builder.addPlace(ctx.naming.forkMergePlace(id))
        List<Integer> mergeTransitionIndexes = []
        int branchNumber = 0
        for (Place branchEnd : branchEndPlaces) {
            branchNumber++
            Transition mergeTransition = builder.addTransition(ctx.naming.forkMergeTransition(id, branchNumber))
            builder.connect(branchEnd, mergeTransition)
            builder.connect(mergeTransition, mergePlace)
            mergeTransitionIndexes.add(mergeTransition.index)
        }
        builder.addForkDescriptor(new ForkDescriptor(forkTransition.index, mergeTransitionIndexes))
        return mergePlace
    }

    private static class ChainContext {
        final PetriNetBuilder builder
        final NamingContext naming
        final String placePrefix
        final Integer enclosingForkTransitionIndex

        ChainContext(PetriNetBuilder builder, NamingContext naming, String placePrefix, Integer enclosingForkTransitionIndex) {
            this.builder = builder
            this.naming = naming
            this.placePrefix = placePrefix
            this.enclosingForkTransitionIndex = enclosingForkTransitionIndex
        }

        ChainContext withPlacePrefix(String prefix) {
            return new ChainContext(builder, naming, prefix, enclosingForkTransitionIndex)
        }

        ChainContext withEnclosingFork(Integer forkTransitionIndex) {
            return new ChainContext(builder, naming, placePrefix, forkTransitionIndex)
        }
    }
}
