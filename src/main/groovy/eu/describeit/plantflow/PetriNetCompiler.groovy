package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.PetriNet
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.Place
import eu.describeit.plantflow.engine.Transition
import groovy.transform.CompileStatic

@CompileStatic
class PetriNetCompiler {

    public static final String PLACE_START = 'start'
    public static final String PLACE_END = 'end'
    public static final String PLACE_IF_DECISION = 'P_if_decision'
    public static final String PLACE_THEN = 'P_then'
    public static final String PLACE_ELSE = 'P_else'
    public static final String PLACE_ENDIF = 'P_endif'
    public static final String PLACE_FORK_PREFIX = 'P_fork_'
    public static final String PLACE_FORK_BRANCH_SUFFIX = '_branch_'
    public static final String PLACE_FORK_MERGE_SUFFIX = '_merge'

    public static final String TRANSITION_START_TO_DECISION = 'T_start_to_decision'
    public static final String TRANSITION_BRANCH_YES = 'T_branch_yes'
    public static final String TRANSITION_BRANCH_NO = 'T_branch_no'
    public static final String TRANSITION_ENDIF_TO_END = 'T_endif_to_end'
    public static final String TRANSITION_FORK_PREFIX = 'T_fork_'
    public static final String TRANSITION_FORK_BRANCH_SUFFIX = '_branch_'
    public static final String TRANSITION_FORK_MERGE_SYNC = '_merge_sync'
    public static final String TRANSITION_FORK_TO_END = '_to_end'

    public static final String PREFIX_INTERMEDIATE_PLACE = 'P_'
    public static final String PREFIX_THEN_PLACE = 'P_then_'
    public static final String PREFIX_ELSE_PLACE = 'P_else_'
    public static final String NEGATION_PREFIX = '!('
    public static final String NEGATION_SUFFIX = ')'

    public static final String ERR_DIAGRAM_NULL = 'Diagram cannot be null'
    public static final String ERR_CONDITIONAL_NODE_NULL = 'ConditionalNode cannot be null'
    public static final String ERR_FORK_NODE_NULL = 'ForkNode cannot be null'
    public static final String ERR_NO_ACTIONS = 'Diagram must contain at least one action transition'
    public static final String ERR_THEN_EMPTY = 'Then block must contain at least one action'
    public static final String ERR_ELSE_EMPTY = 'Else block must contain at least one action'
    public static final String ERR_BRANCH_EMPTY = 'Fork branch must contain at least one action'

    static PetriNet compile(ActivityDiagram diagram) {
        if (diagram == null) {
            throw new IllegalArgumentException(ERR_DIAGRAM_NULL)
        }
        ConditionalNode conditionalNode = findConditionalNode(diagram)
        if (conditionalNode != null) {
            return compileConditional(conditionalNode)
        }
        ForkNode forkNode = findForkNode(diagram)
        if (forkNode != null) {
            return compileFork(forkNode)
        }
        if (diagram.isConditional()) {
            throw new IllegalArgumentException(ERR_THEN_EMPTY)
        }
        return compileLinear(diagram)
    }

    static PetriNet compileLinear(ActivityDiagram diagram) {
        if (diagram == null) {
            throw new IllegalArgumentException(ERR_DIAGRAM_NULL)
        }
        List<ActionNode> actionNodes = extractActionNodes(diagram)
        if (actionNodes.isEmpty()) {
            throw new IllegalArgumentException(ERR_NO_ACTIONS)
        }
        return assembleLinearNet(actionNodes)
    }

    static PetriNet compileConditional(ConditionalNode node) {
        if (node == null) {
            throw new IllegalArgumentException(ERR_CONDITIONAL_NODE_NULL)
        }
        if (node.thenActions.isEmpty()) {
            throw new IllegalArgumentException(ERR_THEN_EMPTY)
        }
        if (node.elseActions.isEmpty()) {
            throw new IllegalArgumentException(ERR_ELSE_EMPTY)
        }
        return assembleConditionalNet(node)
    }

    static PetriNet compileFork(ForkNode node) {
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
        return assembleForkNet(node)
    }

    private static PetriNet assembleLinearNet(List<ActionNode> actionNodes) {
        PetriNetBuilder builder = new PetriNetBuilder()
        Place startPlace = builder.addPlace(PLACE_START)
        List<Place> intermediatePlaces = generateIntermediatePlaces(builder, actionNodes.size())
        Place endPlace = builder.addPlace(PLACE_END)

        builder.startPlace = startPlace
        builder.endPlace = endPlace

        List<Place> places = assembleLinearPlaces(startPlace, intermediatePlaces, endPlace)
        connectLinearActions(builder, places, actionNodes)

        return builder.build()
    }

    private static List<Place> generateIntermediatePlaces(PetriNetBuilder builder, int count) {
        List<Place> places = []
        for (int i = 1; i < count; i++) {
            places.add(builder.addPlace(PREFIX_INTERMEDIATE_PLACE + i))
        }
        return places
    }

    private static List<Place> assembleLinearPlaces(
        Place startPlace,
        List<Place> intermediatePlaces,
        Place endPlace
    ) {
        List<Place> places = []
        places.add(startPlace)
        places.addAll(intermediatePlaces)
        places.add(endPlace)
        return places
    }

    private static void connectLinearActions(
        PetriNetBuilder builder,
        List<Place> places,
        List<ActionNode> actionNodes
    ) {
        for (int i = 0; i < actionNodes.size(); i++) {
            ActionNode node = actionNodes.get(i)
            Transition transition = builder.addTransition(node.action, node.action, null)
            builder.connect(places.get(i), transition)
            builder.connect(transition, places.get(i + 1))
        }
    }

    private static PetriNet assembleConditionalNet(ConditionalNode node) {
        PetriNetBuilder builder = new PetriNetBuilder()
        Place startPlace = builder.addPlace(PLACE_START)
        Place decisionPlace = builder.addPlace(PLACE_IF_DECISION)
        List<Place> thenPlaces = generateBranchPlaces(builder, PLACE_THEN, PREFIX_THEN_PLACE, node.thenActions.size())
        List<Place> elsePlaces = generateBranchPlaces(builder, PLACE_ELSE, PREFIX_ELSE_PLACE, node.elseActions.size())
        Place endifPlace = builder.addPlace(PLACE_ENDIF)
        Place endPlace = builder.addPlace(PLACE_END)

        Transition startToDecision = builder.addTransition(TRANSITION_START_TO_DECISION)
        Transition branchYes = builder.addTransition(TRANSITION_BRANCH_YES, null, node.guardCondition)
        Transition branchNo = builder.addTransition(
            TRANSITION_BRANCH_NO, null, NEGATION_PREFIX + node.guardCondition + NEGATION_SUFFIX
        )
        List<Transition> thenTransitions = generateActionTransitions(builder, node.thenActions)
        List<Transition> elseTransitions = generateActionTransitions(builder, node.elseActions)
        Transition endifToEnd = builder.addTransition(TRANSITION_ENDIF_TO_END)

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

    private static List<Place> generateBranchPlaces(
        PetriNetBuilder builder,
        String startLabel,
        String prefix,
        int count
    ) {
        List<Place> places = []
        places.add(builder.addPlace(startLabel))
        for (int i = 1; i < count; i++) {
            places.add(builder.addPlace(prefix + i))
        }
        return places
    }

    private static List<Transition> generateActionTransitions(
        PetriNetBuilder builder,
        List<ActionNode> actionNodes
    ) {
        List<Transition> transitions = []
        for (ActionNode node : actionNodes) {
            transitions.add(builder.addTransition(node.action, node.action, null))
        }
        return transitions
    }

    private static void connectBranchActions(
        PetriNetBuilder builder,
        List<Place> places,
        List<Transition> transitions,
        Place endifPlace
    ) {
        for (int i = 0; i < places.size(); i++) {
            Place fromPlace = places.get(i)
            Transition transition = transitions.get(i)
            Place toPlace = (i < places.size() - 1) ? places.get(i + 1) : endifPlace
            builder.connect(fromPlace, transition)
            builder.connect(transition, toPlace)
        }
    }

    private static PetriNet assembleForkNet(ForkNode node) {
        PetriNetBuilder builder = new PetriNetBuilder()
        int forkIndex = 1 // Using 1 as the default fork index
        int numBranches = node.branches.size()
        
        // Create places
        Place startPlace = builder.addPlace(PLACE_START)
        
        // Create fork transition
        Transition forkTransition = builder.addTransition(TRANSITION_FORK_PREFIX + forkIndex)
        
        // Create branch start places
        List<Place> branchStartPlaces = []
        List<Place> branchEndPlaces = []
        
         // Process each branch
        for (int branchIdx = 0; branchIdx < numBranches; branchIdx++) {
            List<ActivityNode> branchActions = node.branches.get(branchIdx)
            String branchLabel = PLACE_FORK_PREFIX + forkIndex + PLACE_FORK_BRANCH_SUFFIX + (branchIdx + 1)
            
            // Create branch start place
            Place branchStartPlace = builder.addPlace(branchLabel)
            branchStartPlaces.add(branchStartPlace)
            
            // Connect fork transition to branch start place
            builder.connect(forkTransition, branchStartPlace)
            
            // Process actions in this branch
            Place currentPlace = branchStartPlace
            
            for (int actionIdx = 0; actionIdx < branchActions.size(); actionIdx++) {
                ActivityNode actionNode = branchActions.get(actionIdx)
                if (actionNode instanceof ActionNode) {
                    ActionNode action = (ActionNode) actionNode
                    String transitionLabel = TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_BRANCH_SUFFIX + (branchIdx + 1)
                    Transition transition = builder.addTransition(transitionLabel, action.action, null)
                    
                    // Connect current place to transition
                    builder.connect(currentPlace, transition)
                    
                    // Create next place
                    Place nextPlace = builder.addPlace(branchLabel + (actionIdx < branchActions.size() - 1 ? '_' + (actionIdx + 1) : '_end'))
                    builder.connect(transition, nextPlace)
                    currentPlace = nextPlace
                }
            }
            
            branchEndPlaces.add(currentPlace)
        }
        
        // Connect start to fork transition
        builder.connect(startPlace, forkTransition)
        
        // Create merge place and synchronization transition
        Place mergePlace = builder.addPlace(PLACE_FORK_PREFIX + forkIndex + PLACE_FORK_MERGE_SUFFIX)
        Transition mergeSyncTransition = builder.addTransition(
            TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_MERGE_SYNC
        )
        
        // Connect each branch end to merge sync transition
        for (Place branchEnd : branchEndPlaces) {
            builder.connect(branchEnd, mergeSyncTransition)
        }
        
        // Connect merge sync transition to merge place
        builder.connect(mergeSyncTransition, mergePlace)
        
        // Create transition from merge to end
        Transition toEndTransition = builder.addTransition(
            TRANSITION_FORK_PREFIX + forkIndex + TRANSITION_FORK_TO_END
        )
        Place endPlace = builder.addPlace(PLACE_END)
        
        // Connect merge place to end transition
        builder.connect(mergePlace, toEndTransition)
        builder.connect(toEndTransition, endPlace)
        
        builder.startPlace = startPlace
        builder.endPlace = endPlace
        
        return builder.build()
    }

    private static ConditionalNode findConditionalNode(ActivityDiagram diagram) {
        for (ActivityNode node : diagram.nodes) {
            if (node instanceof ConditionalNode) {
                return (ConditionalNode) node
            }
        }
        return null
    }

    private static ForkNode findForkNode(ActivityDiagram diagram) {
        for (ActivityNode node : diagram.nodes) {
            if (node instanceof ForkNode) {
                return (ForkNode) node
            }
        }
        return null
    }

    private static List<ActionNode> extractActionNodes(ActivityDiagram diagram) {
        List<ActionNode> actionNodes = []
        for (ActivityNode node : diagram.nodes) {
            if (node instanceof ActionNode) {
                actionNodes.add((ActionNode) node)
            }
        }
        return actionNodes
    }
}
