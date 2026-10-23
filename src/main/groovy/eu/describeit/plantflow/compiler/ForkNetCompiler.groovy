package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic

@CompileStatic
class ForkNetCompiler {

    private static final String ERR_FORK_NODE_NULL = 'ForkNode cannot be null'
    private static final String ERR_BRANCH_EMPTY = 'Fork branch must contain at least one action'

    PetriNet compile(ForkNode node, List<ActionNode> postForkActions = Collections.<ActionNode>emptyList()) {
        if (node == null) {
            throw new IllegalArgumentException(ERR_FORK_NODE_NULL)
        }
        if (node.branches == null || node.branches.isEmpty()) {
            throw new IllegalArgumentException(ERR_BRANCH_EMPTY)
        }
        for (List<ActivityNode> branch : node.branches) {
            if (branch == null || branch.isEmpty()) {
                throw new IllegalArgumentException(ERR_BRANCH_EMPTY)
            }
        }

        PetriNetBuilder builder = new PetriNetBuilder()
        int forkIndex = 1

        Place startPlace = builder.addPlace(NetLabeling.PLACE_START)
        Transition forkTransition = builder.addTransition(NetLabeling.forkTransition(forkIndex))

        List<Place> branchEndPlaces = processForkBranches(builder, node, forkIndex, forkTransition)

        Place mergePlace = connectForkComponents(builder, startPlace, forkTransition, branchEndPlaces, forkIndex)

        Place endPlace = builder.addPlace(NetLabeling.PLACE_END)
        if (postForkActions.isEmpty()) {
            Transition toEndTransition = builder.addTransition(NetLabeling.forkToEndTransition(forkIndex))
            builder.connect(mergePlace, toEndTransition)
            builder.connect(toEndTransition, endPlace)
        } else {
            connectPostForkActions(builder, mergePlace, endPlace, postForkActions, forkIndex)
        }

        builder.startPlace = startPlace
        builder.endPlace = endPlace

        return builder.build()
    }

    private static List<Place> processForkBranches(PetriNetBuilder builder, ForkNode node, int forkIndex, Transition forkTransition) {
        List<Place> branchEndPlaces = []
        int numBranches = node.branches.size()

        for (int branchIdx = 0; branchIdx < numBranches; branchIdx++) {
            List<ActivityNode> branchActions = node.branches.get(branchIdx)
            int branchNumber = branchIdx + 1
            String branchLabel = NetLabeling.forkBranchPlace(forkIndex, branchNumber)
            String transitionLabel = NetLabeling.forkBranchTransition(forkIndex, branchNumber)

            Place branchStartPlace = builder.addPlace(branchLabel)
            builder.connect(forkTransition, branchStartPlace)

            Place branchEnd = processBranchActions(builder, branchActions, branchLabel, transitionLabel, branchStartPlace)
            branchEndPlaces.add(branchEnd)
        }

        return branchEndPlaces
    }

    private static Place processBranchActions(PetriNetBuilder builder, List<ActivityNode> branchActions, String branchLabel, String transitionLabel, Place startPlace) {
        Place currentPlace = startPlace
        int totalActions = branchActions.size()

        for (int actionIdx = 0; actionIdx < totalActions; actionIdx++) {
            ActivityNode actionNode = branchActions.get(actionIdx)
            if (actionNode instanceof ActionNode) {
                ActionNode action = (ActionNode) actionNode
                Transition transition = builder.addTransition(transitionLabel, action.action, null)
                builder.connect(currentPlace, transition)

                String placeSuffix = actionIdx < totalActions - 1 ? '_' + (actionIdx + 1) : '_end'
                currentPlace = builder.addPlace(branchLabel + placeSuffix)
                builder.connect(transition, currentPlace)
            }
        }

        return currentPlace
    }

    private static Place connectForkComponents(PetriNetBuilder builder, Place startPlace, Transition forkTransition, List<Place> branchEndPlaces, int forkIndex) {
        builder.connect(startPlace, forkTransition)

        Place mergePlace = builder.addPlace(NetLabeling.forkMergePlace(forkIndex))
        Transition mergeSyncTransition = builder.addTransition(NetLabeling.forkMergeSyncTransition(forkIndex))

        branchEndPlaces.each { Place branchEnd -> builder.connect(branchEnd, mergeSyncTransition) }
        builder.connect(mergeSyncTransition, mergePlace)

        return mergePlace
    }

    private static void connectPostForkActions(PetriNetBuilder builder, Place mergePlace, Place endPlace, List<ActionNode> postForkActions, int forkIndex) {
        List<Place> intermediatePlaces = []
        int count = postForkActions.size()
        for (int i = 1; i < count; i++) {
            intermediatePlaces.add(builder.addPlace(NetLabeling.postMergePlace(forkIndex, i)))
        }
        List<Place> allPlaces = [mergePlace] + intermediatePlaces + [endPlace]
        connectLinearActions(builder, allPlaces, postForkActions)
    }

    private static void connectLinearActions(PetriNetBuilder builder, List<Place> places, List<ActionNode> actionNodes) {
        for (int i = 0; i < actionNodes.size(); i++) {
            ActionNode node = actionNodes.get(i)
            Transition transition = builder.addTransition(node.action, node.action, null)
            builder.connect(places.get(i), transition)
            builder.connect(transition, places.get(i + 1))
        }
    }
}
