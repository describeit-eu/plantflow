package eu.describeit.plantflow

import groovy.transform.CompileStatic

@CompileStatic
class PetriNet {
    final List<Place> places
    final List<Transition> transitions
    final IncidenceMatrix incidenceMatrix
    final Place startPlace
    final Place endPlace

    PetriNet(List<Place> places, List<Transition> transitions, IncidenceMatrix incidenceMatrix, Place startPlace, Place endPlace) {
        this.places = Collections.unmodifiableList(new ArrayList<>(places))
        this.transitions = Collections.unmodifiableList(new ArrayList<>(transitions))
        this.incidenceMatrix = incidenceMatrix
        this.startPlace = startPlace
        this.endPlace = endPlace
    }

    Place getPlaceById(String id) {
        return places.find { it.id == id }
    }

    Transition getTransitionById(String id) {
        return transitions.find { it.id == id }
    }
}
