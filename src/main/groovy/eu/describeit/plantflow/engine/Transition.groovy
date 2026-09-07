package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.NullCheck
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
class Transition {
    final String id
    final int index
    final String label
    final String actionKey
    final String guardKey = null

    @NullCheck(includeGenerated=true)
    Transition(String id, Integer index, String label, String actionKey) {
        this.id = id
        this.index = index
        this.label = label
        this.actionKey = actionKey

        if (!label?.trim()) {
            throw new IllegalArgumentException('label cannot be null or blank')
        }

        if (!actionKey?.trim()) {
            throw new IllegalArgumentException('actionKey cannot be null or blank')
        }
    }

    Transition(String id, Integer index, String label, String actionKey, String guardKey) {
        this(id, index, label, actionKey)
        this.guardKey = guardKey
    }
}
