package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic

@CompileStatic
class ForkNetAssembler {

    static PetriNet assemble(ForkNode node, List<ActionNode> postForkActions) {
        PetriNetBuilder builder = new PetriNetBuilder()
        int forkIndex = 1

        Place startPlace = builder.addPlace(PetriNetCompiler.PLACE_START)
        Transition forkTransition = addForkTransition(builder, forkIndex)

        List<Place> branchEndPlaces = processForkBranches(builder, node, forkIndex, forkTransition)

        Place mergePlace = connectForkComponents(builder, startPlace, forkTransition, branchEndPlaces, forkIndex)

        Place endPlace = builder.addPlace(PetriNetCompiler.PLACE_END)
        if (postForkActions.isEmpty()) {
            Transition toEndTransition = builder.addTransition(PetriNetCompiler.TRANSITION_FORK_PREFIX + forkIndex + PetriNetCompiler.TRANSITION_FORK_TO_END)
            builder.connect(mergePlace, toEndTransition)
            builder.connect(toEndTransition, endPlace)
        } else {
            connectPostForkActions(builder, mergePlace, endPlace, postForkActions, forkIndex)
        }

        builder.startPlace = startPlace
        builder.endPlace = endPlace

        return builder.build()
    }

    private static Transition addForkTransition(PetriNetBuilder builder, int forkIndex) {
        return builder.addTransition(PetriNetCompiler.TRANSITION_FORK_PREFIX + forkIndex)
    }

    private static List<Place> processForkBranches(PetriNetBuilder builder, ForkNode node,
                                                   int forkIndex, Transition forkTransition) {
        List<Place> branchEndPlaces = []
        int numBranches = node.branches.size()

        for (int branchIdx = 0; branchIdx < numBranches; branchIdx++) {
            List<ActivityNode> branchActions = node.branches.get(branchIdx)
            String branchLabel = PetriNetCompiler.PLACE_FORK_PREFIX + forkIndex + PetriNetCompiler.PLACE_FORK_BRANCH_SUFFIX + (branchIdx + 1)
            String transitionLabel = PetriNetCompiler.TRANSITION_FORK_PREFIX + forkIndex + PetriNetCompiler.TRANSITION_FORK_BRANCH_SUFFIX + (branchIdx + 1)

            Place branchStartPlace = builder.addPlace(branchLabel)
            builder.connect(forkTransition, branchStartPlace)

            Place branchEnd = processBranchActions(builder, branchActions, branchLabel, transitionLabel, branchStartPlace)
            branchEndPlaces.add(branchEnd)
        }

        return branchEndPlaces
    }

    private static Place processBranchActions(PetriNetBuilder builder, List<ActivityNode> branchActions,
                                              String branchLabel, String transitionLabel, Place startPlace) {
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

    private static Place connectForkComponents(PetriNetBuilder builder, Place startPlace,
                                               Transition forkTransition, List<Place> branchEndPlaces,
                                               int forkIndex) {
        builder.connect(startPlace, forkTransition)

        Place mergePlace = builder.addPlace(PetriNetCompiler.PLACE_FORK_PREFIX + forkIndex + PetriNetCompiler.PLACE_FORK_MERGE_SUFFIX)
        Transition mergeSyncTransition = builder.addTransition(PetriNetCompiler.TRANSITION_FORK_PREFIX + forkIndex + PetriNetCompiler.TRANSITION_FORK_MERGE_SYNC)

        branchEndPlaces.each { Place branchEnd -> builder.connect(branchEnd, mergeSyncTransition) }
        builder.connect(mergeSyncTransition, mergePlace)

        return mergePlace
    }

    private static void connectPostForkActions(PetriNetBuilder builder, Place mergePlace,
                                               Place endPlace, List<ActionNode> postForkActions, int forkIndex) {
        List<Place> intermediatePlaces = []
        int count = postForkActions.size()
        for (int i = 1; i < count; i++) {
            intermediatePlaces.add(builder.addPlace(PetriNetCompiler.PLACE_FORK_PREFIX + forkIndex + PetriNetCompiler.PLACE_FORK_POST_MERGE_SUFFIX + i))
        }
        List<Place> allPlaces = [mergePlace] + intermediatePlaces + [endPlace]
        PetriNetCompiler.connectLinearActions(builder, allPlaces, postForkActions)
    }

}
