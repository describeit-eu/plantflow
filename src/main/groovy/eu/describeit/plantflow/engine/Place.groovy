package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
class Place {
    final String id
    final int index
    final String label

    Place(String id, int index, String label = null) {
        this.id = id
        this.index = index
        this.label = label ?: id
    }
}
