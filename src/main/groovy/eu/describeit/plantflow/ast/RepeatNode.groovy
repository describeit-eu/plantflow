package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@EqualsAndHashCode
@ToString
class RepeatNode implements ActivityNode {

    final String loopCondition
    final List<ActivityNode> body
    boolean detach = false

    RepeatNode(String loopCondition, List<ActivityNode> body, boolean detach = false) {
        this.loopCondition = loopCondition
        this.body = body != null ?
            Collections.unmodifiableList(new ArrayList<>(body)) :
            Collections.<ActivityNode>emptyList()
        this.detach = detach
    }

    String getLoopCondition() {
        return loopCondition
    }

    List<ActivityNode> getBody() {
        return body
    }

    boolean isDetach() {
        return detach
    }

    void setDetach(boolean detach) {
        this.detach = detach
    }
}
