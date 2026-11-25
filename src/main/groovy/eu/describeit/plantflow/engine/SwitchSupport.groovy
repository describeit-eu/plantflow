package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic

/**
 * Runtime support for compiled Switch structures: detects a token stalled at
 * a switch Decision Place where no case Branch Transition is enabled and
 * fails loudly with an {@link IllegalStateException} instead of letting the
 * execution silently stall.
 */
@CompileStatic
class SwitchSupport {

    private static final String ERR_NO_MATCHING_CASE = 'No matching case in switch '

    static List<SwitchDescriptor> getSwitchDescriptors(PetriNetStructure net) {
        if (net instanceof DefaultPetriNet defaultNet) {
            return defaultNet.getSwitchDescriptors()
        }
        return Collections.emptyList()
    }

    /**
     * Throws when any switch Decision Place holds a token while none of that
     * switch's case Branch Transitions is among the enabled transitions.
     */
    static void assertNoStalledSwitchToken(PetriNetStructure net, Marking marking, List<Transition> enabledTransitions) {
        for (SwitchDescriptor descriptor : getSwitchDescriptors(net)) {
            if (marking.getTokenCount(descriptor.decisionPlaceIndex) == 0) {
                continue
            }
            if (hasEnabledCaseTransition(descriptor, enabledTransitions)) {
                continue
            }
            Token stalledToken = marking.getTokens(descriptor.decisionPlaceIndex).get(0)
            throw new IllegalStateException(
                "${ERR_NO_MATCHING_CASE}'${descriptor.switchExpression}' for token ${stalledToken.id}")
        }
    }

    private static boolean hasEnabledCaseTransition(SwitchDescriptor descriptor, List<Transition> enabledTransitions) {
        for (Transition transition : enabledTransitions) {
            if (descriptor.caseTransitionIndexes.contains(transition.index)) {
                return true
            }
        }
        return false
    }
}
