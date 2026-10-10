package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@EqualsAndHashCode
@ToString
class ForkNode implements ActivityNode {

    final List<List<ActivityNode>> branches

    ForkNode(List<List<ActivityNode>> branches) {
        this.branches = branches != null ?
            Collections.unmodifiableList(new ArrayList<>(branches)) :
            Collections.<List<ActivityNode>>emptyList()
    }

    List<List<ActivityNode>> getBranches() {
        return branches
    }
}
