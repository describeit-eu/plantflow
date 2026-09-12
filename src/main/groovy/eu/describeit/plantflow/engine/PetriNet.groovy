package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import groovy.transform.CompileStatic

@CompileStatic
interface PetriNet {
    List<Place> getPlaces()
    List<Transition> getTransitions()
    Place getStartPlace()
    Place getEndPlace()
    Marking getMarking()

    default Place getPlaceByIndex(int index) {
        return getPlaces().find { Place place -> place.index == index }
    }

    default Transition getTransitionByIndex(int index) {
        return getTransitions().find { Transition transition -> transition.index == index }
    }

    default void addToken(Place place, Token token) {
        getMarking().addToken(place, token)
    }

    default void seedToken(Token token) {
        Token tokenToSeed = token ?: Token.of()
        addToken(getStartPlace(), tokenToSeed)
    }

    default List<Token> getTokens(Place place) {
        return getMarking().getTokens(place)
    }

    default int getTokenCount(Place place) {
        return getMarking().getTokenCount(place)
    }

    default int getTokenCount(int placeIndex) {
        return getMarking().getTokenCount(placeIndex)
    }

    default boolean isEmpty(Place place) {
        return getMarking().isEmpty(place)
    }

    default boolean isEmpty(int placeIndex) {
        return getMarking().isEmpty(placeIndex)
    }

    boolean isEnabled(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext)

    default boolean isEnabled(Transition transition, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        return isEnabled(transition, getMarking(), handlerRegistry, executionContext)
    }

    List<Transition> getEnabledTransitions(Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext)

    default List<Transition> getEnabledTransitions(HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        return getEnabledTransitions(getMarking(), handlerRegistry, executionContext)
    }

    boolean fire(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext)

    default boolean fire(Transition transition, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        return fire(transition, getMarking(), handlerRegistry, executionContext)
    }

    default Marking runUntilEnd(Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext, Token token) {
        if (token != null) {
            marking.addToken(getStartPlace(), token)
        } else if (isNetEmpty(marking)) {
            marking.addToken(getStartPlace(), Token.of())
        }

        while (true) {
            List<Transition> enabled = getEnabledTransitions(marking, handlerRegistry, executionContext)
            if (enabled.isEmpty()) {
                break
            }
            fire(enabled.get(0), marking, handlerRegistry, executionContext)
        }

        return marking
    }

    default Marking runUntilEnd(HandlerRegistry handlerRegistry, ExecutionContext executionContext, Token token) {
        return runUntilEnd(getMarking(), handlerRegistry, executionContext, token)
    }

    default boolean isNetEmpty(Marking marking) {
        for (Place p : getPlaces()) {
            if (!marking.isEmpty(p)) {
                return false
            }
        }
        return true
    }
}
