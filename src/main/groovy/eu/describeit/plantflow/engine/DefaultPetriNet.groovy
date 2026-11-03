package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
class DefaultPetriNet implements PetriNetStructure {
    final List<Place> places
    final List<Transition> transitions
    final IncidenceMatrix incidenceMatrix
    final Place startPlace
    final Place endPlace

    private List<ForkDescriptor> forkDescriptors = Collections.emptyList()

    DefaultPetriNet(List<Place> places, List<Transition> transitions, IncidenceMatrix incidenceMatrix, Place startPlace, Place endPlace) {
        this.places = Collections.unmodifiableList(new ArrayList<>(places))
        this.transitions = Collections.unmodifiableList(new ArrayList<>(transitions))
        this.incidenceMatrix = incidenceMatrix
        this.startPlace = startPlace
        this.endPlace = endPlace
    }

    @Override
    List<ForkDescriptor> getForkDescriptors() {
        return forkDescriptors
    }

    @PackageScope
    void setForkDescriptors(List<ForkDescriptor> descriptors) {
        this.forkDescriptors = Collections.unmodifiableList(new ArrayList<>(descriptors))
    }
}
