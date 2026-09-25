package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@EqualsAndHashCode
@ToString
class ConditionalNode implements ActivityNode {

    final String guardCondition
    final List<ActionNode> thenActions
    final List<ActionNode> elseActions

    ConditionalNode(String guardCondition, List<ActionNode> thenActions, List<ActionNode> elseActions) {
        this.guardCondition = guardCondition
        this.thenActions = thenActions != null ?
            Collections.unmodifiableList(new ArrayList<>(thenActions)) :
            Collections.<ActionNode>emptyList()
        this.elseActions = elseActions != null ?
            Collections.unmodifiableList(new ArrayList<>(elseActions)) :
            Collections.<ActionNode>emptyList()
    }

    String getGuardCondition() {
        return guardCondition
    }

    List<ActionNode> getThenActions() {
        return thenActions
    }

    List<ActionNode> getElseActions() {
        return elseActions
    }
}
