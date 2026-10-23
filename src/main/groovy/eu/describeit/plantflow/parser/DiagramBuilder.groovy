package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActivityDiagram
import groovy.transform.CompileStatic

@CompileStatic
interface DiagramBuilder {
    ActivityDiagram build(List<ClassifiedLine> lines)
}
