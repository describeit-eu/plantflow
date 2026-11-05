package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@EqualsAndHashCode
@ToString
class ConditionalNode implements ActivityNode {

    final String guardCondition
    final List<ActivityNode> thenActions
    final List<ActivityNode> elseActions

    ConditionalNode(String guardCondition, List<ActivityNode> thenActions, List<ActivityNode> elseActions) {
        this.guardCondition = guardCondition
        this.thenActions = thenActions != null ?
            Collections.unmodifiableList(new ArrayList<>(thenActions)) :
            Collections.<ActivityNode>emptyList()
        this.elseActions = elseActions != null ?
            Collections.unmodifiableList(new ArrayList<>(elseActions)) :
            Collections.<ActivityNode>emptyList()
    }

    String getGuardCondition() {
        return guardCondition
    }

    List<ActivityNode> getThenActions() {
        return thenActions
    }

    List<ActivityNode> getElseActions() {
        return elseActions
    }
}
