package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@EqualsAndHashCode
@ToString
class ForkNode implements ActivityNode {

    final List<List<ActivityNode>> branches
    final boolean synchronizingJoin

    ForkNode(List<List<ActivityNode>> branches) {
        this(branches, false)
    }

    ForkNode(List<List<ActivityNode>> branches, boolean synchronizingJoin) {
        this.branches = branches != null ?
            Collections.unmodifiableList(new ArrayList<>(branches)) :
            Collections.<List<ActivityNode>>emptyList()
        this.synchronizingJoin = synchronizingJoin
    }

    List<List<ActivityNode>> getBranches() {
        return branches
    }

    boolean isSynchronizingJoin() {
        return synchronizingJoin
    }
}
