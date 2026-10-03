package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@EqualsAndHashCode
@ToString
class ForkNode implements ActivityNode {

    final List<ActivityNode> branches

    ForkNode(List<ActivityNode> branches) {
        this.branches = branches != null ?
            Collections.unmodifiableList(new ArrayList<>(branches)) :
            Collections.<ActivityNode>emptyList()
    }

    List<ActivityNode> getBranches() {
        return branches
    }
}
