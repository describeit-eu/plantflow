package eu.describeit.plantflow.engine

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.NullCheck
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
class Transition {
    final int index
    final String label
    final String actionKey
    final String guardKey

    @JsonCreator
    @NullCheck
    Transition(@JsonProperty('index') Integer index, @JsonProperty('label') String label, @JsonProperty('actionKey') String actionKey) {
        this.index = index
        this.label = label.trim()
        this.actionKey = actionKey.trim()

        if (!this.label) {
            throw new IllegalArgumentException('label cannot be null or blank')
        }

        if (!this.actionKey) {
            throw new IllegalArgumentException('actionKey cannot be null or blank')
        }
    }

    Transition(Integer index, String label, String actionKey, String guardKey) {
        this(index, label, actionKey)
        this.guardKey = guardKey
    }
}
