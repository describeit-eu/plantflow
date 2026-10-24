package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import groovy.transform.CompileStatic

@CompileStatic
class StatefulExecution {
    final PetriNetStructure structure
    final PetriNetEngine engine
    final Marking marking

    StatefulExecution(PetriNetStructure structure, PetriNetEngine engine) {
        this.structure = structure
        this.engine = engine
        this.marking = new Marking(structure.places.size())
    }

    List<Place> getPlaces() {
        return structure.places
    }

    List<Transition> getTransitions() {
        return structure.transitions
    }

    Place getStartPlace() {
        return structure.startPlace
    }

    Place getEndPlace() {
        return structure.endPlace
    }

    IncidenceMatrix getIncidenceMatrix() {
        return structure.incidenceMatrix
    }

    Place getPlaceByIndex(int index) {
        return structure.getPlaceByIndex(index)
    }

    Transition getTransitionByIndex(int index) {
        return structure.getTransitionByIndex(index)
    }

    void seedToken(Token token = null) {
        Token tokenToSeed = token ?: Token.of()
        marking.addToken(structure.startPlace, tokenToSeed)
    }

    boolean isEnabled(Transition transition, HandlerRegistry registry, ExecutionContext context) {
        return engine.isEnabled(structure, transition, marking, registry, context)
    }

    List<Transition> getEnabledTransitions(HandlerRegistry registry, ExecutionContext context) {
        return engine.getEnabledTransitions(structure, marking, registry, context)
    }

    List<Transition> getEnabledTransitions(Marking customMarking, HandlerRegistry registry, ExecutionContext context) {
        return engine.getEnabledTransitions(structure, customMarking, registry, context)
    }

    boolean fire(Transition transition, HandlerRegistry registry, ExecutionContext context) {
        return engine.fire(structure, transition, marking, registry, context)
    }

    boolean fire(Marking customMarking, Transition transition, HandlerRegistry registry, ExecutionContext context) {
        return engine.fire(structure, transition, customMarking, registry, context)
    }

    boolean step(HandlerRegistry registry, ExecutionContext context) {
        List<Transition> enabled = getEnabledTransitions(registry, context)
        if (enabled.isEmpty()) {
            return false
        }
        return fire(enabled.get(0), registry, context)
    }

    Marking runUntilEnd(HandlerRegistry registry, ExecutionContext context, Token token = null) {
        if (token != null) {
            marking.addToken(structure.startPlace, token)
        } else if (isNetEmpty()) {
            marking.addToken(structure.startPlace, Token.of())
        }
        boolean stepped = true
        while (stepped) {
            stepped = step(registry, context)
        }
        return marking
    }

    boolean isNetEmpty() {
        for (Place p : structure.places) {
            if (!marking.isEmpty(p)) {
                return false
            }
        }
        return true
    }

    List<Token> getTokens(Place place) {
        return marking.getTokens(place)
    }

    int getTokenCount(Place place) {
        return marking.getTokenCount(place)
    }

    int getTokenCount(int placeIndex) {
        return marking.getTokenCount(placeIndex)
    }

    boolean isEmpty(Place place) {
        return marking.isEmpty(place)
    }

    boolean isEmpty(int placeIndex) {
        return marking.isEmpty(placeIndex)
    }

    void addToken(Place place, Token token) {
        marking.addToken(place, token)
    }
}
