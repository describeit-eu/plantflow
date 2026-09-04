package eu.describeit.plantflow

import groovy.transform.CompileStatic

@CompileStatic
interface PetriNet {
    List<Place> getPlaces()
    List<Transition> getTransitions()
    Place getStartPlace()
    Place getEndPlace()

    default Place getPlaceById(String id) {
        return getPlaces().find { Place place -> place.id == id }
    }

    default Transition getTransitionById(String id) {
        return getTransitions().find { Transition transition -> transition.id == id }
    }

    boolean isEnabled(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext)

    List<Transition> getEnabledTransitions(Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext)

    boolean fire(Transition transition, Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext)

    default Marking runUntilEnd(Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext, RecordToken token) {
        if (token != null) {
            marking.addToken(getStartPlace(), token)
        } else if (isNetEmpty(marking)) {
            marking.addToken(getStartPlace(), RecordToken.of())
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

    default Marking runUntilEnd(Marking marking, HandlerRegistry handlerRegistry, ExecutionContext executionContext) {
        return runUntilEnd(marking, handlerRegistry, executionContext, null)
    }

    default Marking runUntilEnd(Marking marking) {
        return runUntilEnd(marking, new HandlerRegistry(), new ExecutionContext(), null)
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
