package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.NullCheck
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
@NullCheck
class Place {
    final String id
    final int index
    final String label

    Place(String id, int index, String label) {
        this.id = id
        this.index = index
        this.label = label

        if (!label || !label.trim()) {
            throw new IllegalArgumentException('label cannot be blank')
        }
    }
}
