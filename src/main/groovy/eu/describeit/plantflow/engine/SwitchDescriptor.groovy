package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

/**
 * Describes one compiled Switch structure in a Petri net: the switch Decision
 * Place, the switch expression (documentation only, routing is purely by case
 * guards), and the guarded case Branch Transitions leading out of the
 * Decision Place.
 */
@CompileStatic
class SwitchDescriptor {
    final int decisionPlaceIndex
    final String switchExpression
    final List<Integer> caseTransitionIndexes

    SwitchDescriptor(int decisionPlaceIndex, String switchExpression, List<Integer> caseTransitionIndexes) {
        this.decisionPlaceIndex = decisionPlaceIndex
        this.switchExpression = switchExpression
        this.caseTransitionIndexes = Collections.unmodifiableList(new ArrayList<>(caseTransitionIndexes))
    }
}
