package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

@CompileStatic
class PetriNetBuilder {

    private static final String ERR_FROM_PLACE_NULL = 'fromPlace cannot be null'
    private static final String ERR_TO_TRANSITION_NULL = 'toTransition cannot be null'
    private static final String ERR_FROM_TRANSITION_NULL = 'fromTransition cannot be null'
    private static final String ERR_TO_PLACE_NULL = 'toPlace cannot be null'
    private static final String ERR_WEIGHT_POSITIVE = 'weight must be greater than 0'
    private static final String ERR_START_PLACE_NULL = 'startPlace cannot be null'
    private static final String ERR_END_PLACE_NULL = 'endPlace cannot be null'
    private static final String ERR_START_PLACE_MISSING = 'startPlace must be set'
    private static final String ERR_END_PLACE_MISSING = 'endPlace must be set'
    private static final String ERR_FORK_DESCRIPTOR_NULL = 'ForkDescriptor cannot be null'
    private static final String ERR_JOIN_DESCRIPTOR_NULL = 'JoinDescriptor cannot be null'

    private final List<Place> places = []
    private final List<Transition> transitions = []
    private final List<Arc> inputArcs = []
    private final List<Arc> outputArcs = []
    private final List<ForkDescriptor> forkDescriptors = []
    private final List<JoinDescriptor> joinDescriptors = []
    private Place startPlace
    private Place endPlace

    Place addPlace(String label) {
        Place place = new Place(places.size(), label)
        places.add(place)
        return place
    }

    Transition addTransition(String label, String actionKey = null, String guardKey = null) {
        Transition transition = new Transition(transitions.size(), label, actionKey, guardKey)
        transitions.add(transition)
        return transition
    }

    PetriNetBuilder connect(Place fromPlace, Transition toTransition, int weight = 1) {
        if (fromPlace == null) {
            throw new IllegalArgumentException(ERR_FROM_PLACE_NULL)
        }
        if (toTransition == null) {
            throw new IllegalArgumentException(ERR_TO_TRANSITION_NULL)
        }
        if (weight <= 0) {
            throw new IllegalArgumentException(ERR_WEIGHT_POSITIVE)
        }
        inputArcs.add(new Arc(fromPlace.index, toTransition.index, weight))
        return this
    }

    PetriNetBuilder connect(Transition fromTransition, Place toPlace, int weight = 1) {
        if (fromTransition == null) {
            throw new IllegalArgumentException(ERR_FROM_TRANSITION_NULL)
        }
        if (toPlace == null) {
            throw new IllegalArgumentException(ERR_TO_PLACE_NULL)
        }
        if (weight <= 0) {
            throw new IllegalArgumentException(ERR_WEIGHT_POSITIVE)
        }
        outputArcs.add(new Arc(toPlace.index, fromTransition.index, weight))
        return this
    }

    PetriNetBuilder addForkDescriptor(ForkDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException(ERR_FORK_DESCRIPTOR_NULL)
        }
        forkDescriptors.add(descriptor)
        return this
    }

    PetriNetBuilder addJoinDescriptor(JoinDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException(ERR_JOIN_DESCRIPTOR_NULL)
        }
        joinDescriptors.add(descriptor)
        return this
    }

    PetriNetBuilder setStartPlace(Place place) {
        if (place == null) {
            throw new IllegalArgumentException(ERR_START_PLACE_NULL)
        }
        this.startPlace = place
        return this
    }

    PetriNetBuilder setEndPlace(Place place) {
        if (place == null) {
            throw new IllegalArgumentException(ERR_END_PLACE_NULL)
        }
        this.endPlace = place
        return this
    }

    List<Place> getPlaces() {
        return Collections.unmodifiableList(places)
    }

    List<Transition> getTransitions() {
        return Collections.unmodifiableList(transitions)
    }

    Place getStartPlace() {
        return startPlace
    }

    Place getEndPlace() {
        return endPlace
    }

    PetriNetStructure build() {
        if (startPlace == null) {
            throw new IllegalStateException(ERR_START_PLACE_MISSING)
        }
        if (endPlace == null) {
            throw new IllegalStateException(ERR_END_PLACE_MISSING)
        }

        int[][] inputMatrix = new int[places.size()][transitions.size()]
        int[][] outputMatrix = new int[places.size()][transitions.size()]

        for (Arc arc : inputArcs) {
            inputMatrix[arc.placeIndex][arc.transitionIndex] = arc.weight
        }

        for (Arc arc : outputArcs) {
            outputMatrix[arc.placeIndex][arc.transitionIndex] = arc.weight
        }

        IncidenceMatrix incidenceMatrix = new IncidenceMatrix(inputMatrix, outputMatrix)
        DefaultPetriNet net = new DefaultPetriNet(places, transitions, incidenceMatrix, startPlace, endPlace)
        net.setForkDescriptors(forkDescriptors)
        net.setJoinDescriptors(joinDescriptors)
        return net
    }

    private static class Arc {
        final int placeIndex
        final int transitionIndex
        final int weight

        Arc(int placeIndex, int transitionIndex, int weight) {
            this.placeIndex = placeIndex
            this.transitionIndex = transitionIndex
            this.weight = weight
        }
    }
}
