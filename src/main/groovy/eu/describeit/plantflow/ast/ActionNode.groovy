package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@EqualsAndHashCode
@ToString
class ActionNode implements ActivityNode {

    private static final String ERR_ACTION_EMPTY = 'Action cannot be empty'

    final String action

    ActionNode(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException(ERR_ACTION_EMPTY)
        }
        this.action = action.trim()
    }

    String getAction() {
        return action
    }

    String getActionKey() {
        return action
    }
}
