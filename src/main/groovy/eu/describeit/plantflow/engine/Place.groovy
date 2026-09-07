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
    final int index
    final String label

    Place(int index, String label) {
        this.index = index
        this.label = label

        if (!label?.trim()) {
            throw new IllegalArgumentException('label cannot be blank')
        }
    }
}
