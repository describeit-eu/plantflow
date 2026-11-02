package eu.describeit.plantflow.parser

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@EqualsAndHashCode
@ToString
class ClassifiedLine {
    final LineType type
    final String text
    final String action
    final String guard
    final String branchLabel

    ClassifiedLine(LineType type, String text, String action = null, String guard = null, String branchLabel = null) {
        this.type = type
        this.text = text
        this.action = action
        this.guard = guard
        this.branchLabel = branchLabel
    }
}
