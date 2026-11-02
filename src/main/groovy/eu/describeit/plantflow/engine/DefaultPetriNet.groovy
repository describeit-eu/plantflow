package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

@CompileStatic
class DefaultPetriNet implements PetriNetStructure {
    final List<Place> places
    final List<Transition> transitions
    final IncidenceMatrix incidenceMatrix
    final Place startPlace
    final Place endPlace

    DefaultPetriNet(List<Place> places, List<Transition> transitions, IncidenceMatrix incidenceMatrix, Place startPlace, Place endPlace) {
        this.places = Collections.unmodifiableList(new ArrayList<>(places))
        this.transitions = Collections.unmodifiableList(new ArrayList<>(transitions))
        this.incidenceMatrix = incidenceMatrix
        this.startPlace = startPlace
        this.endPlace = endPlace
    }
}
