package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * One case branch of a {@link SwitchNode}: the case condition (carried verbatim
 * as the guard key of the case's branch transition) and the case body actions.
 */
@CompileStatic
@EqualsAndHashCode
@ToString
class SwitchCase {

    final String condition
    final List<ActivityNode> actions

    SwitchCase(String condition, List<ActivityNode> actions) {
        this.condition = condition
        this.actions = actions != null ?
            Collections.unmodifiableList(new ArrayList<>(actions)) :
            Collections.<ActivityNode>emptyList()
    }

    String getCondition() {
        return condition
    }

    List<ActivityNode> getActions() {
        return actions
    }
}
