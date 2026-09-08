package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.NullCheck
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
@NullCheck(includeGenerated=true)
class Place {
    final int index
    final String label

    Place(int index, String label) {
        this.index = index
        this.label = label.trim()

        if (!this.label) {
            throw new IllegalArgumentException('label cannot be blank')
        }
    }
}
