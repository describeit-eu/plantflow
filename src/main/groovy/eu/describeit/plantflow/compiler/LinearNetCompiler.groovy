package eu.describeit.plantflow.compiler

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.engine.PetriNetBuilder
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.engine.Place
import groovy.transform.CompileStatic

@CompileStatic
class LinearNetCompiler {

    private static final String ERR_NO_ACTIONS = 'Diagram must contain at least one action transition'

    PetriNetStructure compile(List<ActionNode> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException(ERR_NO_ACTIONS)
        }
        PetriNetBuilder builder = new PetriNetBuilder()
        Place startPlace = builder.addPlace(NetLabeling.PLACE_START)
        List<Place> intermediatePlaces = generateIntermediatePlaces(builder, actions.size())
        Place endPlace = builder.addPlace(NetLabeling.PLACE_END)

        builder.startPlace = startPlace
        builder.endPlace = endPlace
        LinearChainHelper.connectLinearActions(builder, assemblePlaces(startPlace, intermediatePlaces, endPlace), actions)

        return builder.build()
    }

    private static List<Place> generateIntermediatePlaces(PetriNetBuilder builder, int count) {
        List<Place> places = []
        for (int i = 1; i < count; i++) {
            places.add(builder.addPlace(NetLabeling.intermediatePlace(i)))
        }
        return places
    }

    private static List<Place> assemblePlaces(Place startPlace, List<Place> intermediatePlaces, Place endPlace) {
        List<Place> places = []
        places.add(startPlace)
        places.addAll(intermediatePlaces)
        places.add(endPlace)
        return places
    }
}
