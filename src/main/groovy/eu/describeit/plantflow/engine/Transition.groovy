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

    @NullCheck
    Transition(Integer index, String label) {
        this.index = index
        this.label = label.trim()

        if (!this.label) {
            throw new IllegalArgumentException('label cannot be null or blank')
        }
    }

    @JsonCreator
    Transition(
        @JsonProperty('index') Integer index,
        @JsonProperty('label') String label,
        @JsonProperty('actionKey') String actionKey,
        @JsonProperty('guardKey') String guardKey)
    {
        this(index, label)
        this.actionKey = actionKey?.trim()
        this.guardKey = guardKey?.trim()
    }
}
