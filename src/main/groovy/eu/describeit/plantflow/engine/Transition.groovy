package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
class Transition {
    final String id
    final int index
    final String label
    final String actionKey
    final String guardKey

    Transition(String id, int index, String label) {
        this.id = id
        this.index = index
        this.label = label
        this.actionKey = label
        this.guardKey = null

        if (!label || !label.trim()) {
            throw new IllegalArgumentException('label cannot be null or blank')
        }
    }

    Transition(String id, int index, String label, String actionKey, String guardKey) {
        this.id = id
        this.index = index
        this.label = label
        this.actionKey = actionKey ?: label
        this.guardKey = guardKey

        if (!label || !label.trim()) {
            throw new IllegalArgumentException('label cannot be null or blank')
        }
    }
}
