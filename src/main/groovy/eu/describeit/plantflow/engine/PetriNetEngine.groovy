package eu.describeit.plantflow.engine

import eu.describeit.plantflow.ExecutionContext
import eu.describeit.plantflow.HandlerRegistry
import groovy.transform.CompileStatic

@CompileStatic
interface PetriNetEngine {
    boolean isEnabled(PetriNetStructure net, Transition t, Marking m, HandlerRegistry r, ExecutionContext c)
    List<Transition> getEnabledTransitions(PetriNetStructure net, Marking m, HandlerRegistry r, ExecutionContext c)
    boolean fire(PetriNetStructure net, Transition t, Marking m, HandlerRegistry r, ExecutionContext c)
}
