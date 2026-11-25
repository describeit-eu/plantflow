package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * A switch/case/endswitch construct: the switch expression (documentation only,
 * not compiled into a guard) and the ordered list of case branches.
 */
@CompileStatic
@EqualsAndHashCode
@ToString
class SwitchNode implements ActivityNode {

    final String switchExpression
    final List<SwitchCase> cases

    SwitchNode(String switchExpression, List<SwitchCase> cases) {
        this.switchExpression = switchExpression
        this.cases = cases != null ?
            Collections.unmodifiableList(new ArrayList<>(cases)) :
            Collections.<SwitchCase>emptyList()
    }

    String getSwitchExpression() {
        return switchExpression
    }

    List<SwitchCase> getCases() {
        return cases
    }
}
