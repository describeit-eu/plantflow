package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

@CompileStatic
interface PetriNetStructure {
    List<Place> getPlaces()
    List<Transition> getTransitions()
    Place getStartPlace()
    Place getEndPlace()
    IncidenceMatrix getIncidenceMatrix()

    default Place getPlaceByIndex(int index) {
        return getPlaces().find { Place place -> place.index == index }
    }

    default Transition getTransitionByIndex(int index) {
        return getTransitions().find { Transition transition -> transition.index == index }
    }
}
