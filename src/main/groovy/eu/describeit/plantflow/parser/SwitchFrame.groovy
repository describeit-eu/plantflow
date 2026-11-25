package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.SwitchCase
import eu.describeit.plantflow.ast.SwitchNode
import groovy.transform.CompileStatic

/**
 * Frame for an open switch/case/endswitch construct on the unified frame
 * stack: keeps the switch expression (documentation only), one condition per
 * case line, and one mutable body per case. A {@link SwitchNode} is materialized
 * when the matching endswitch pops the frame.
 */
@CompileStatic
class SwitchFrame {

    static final String ERR_UNCLOSED_SWITCH = 'Unclosed switch - missing endswitch'
    static final String ERR_CASE_NO_CONTEXT = 'case without active switch'
    static final String ERR_ENDSWITCH_NO_CONTEXT = 'endswitch without active switch'
    static final String ERR_ACTION_OUTSIDE_CASE = 'Action inside switch must directly follow a case line'
    static final String ERR_SWITCH_NO_CASES = 'Empty switch - switch construct must contain at least one case'

    final String switchExpression
    final List<String> caseConditions = []
    final List<List<ActivityNode>> caseBodies = []

    SwitchFrame(String switchExpression) {
        this.switchExpression = switchExpression
    }

    void addCase(String condition) {
        caseConditions.add(condition)
        caseBodies.add([])
    }

    List<ActivityNode> currentCaseBody() {
        if (caseBodies.isEmpty()) {
            throw new IllegalArgumentException(ERR_ACTION_OUTSIDE_CASE)
        }
        return caseBodies.last()
    }

    SwitchNode toNode() {
        if (caseConditions.isEmpty()) {
            throw new IllegalArgumentException(ERR_SWITCH_NO_CASES)
        }
        List<SwitchCase> cases = []
        for (int i = 0; i < caseConditions.size(); i++) {
            cases.add(new SwitchCase(caseConditions[i], caseBodies[i]))
        }
        return new SwitchNode(switchExpression, cases)
    }
}
