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

    Transition(String id, int index, String label, String actionKey = null, String guardKey = null) {
        this.id = id
        this.index = index
        this.label = label
        this.actionKey = actionKey ?: label
        this.guardKey = guardKey
    }
}
