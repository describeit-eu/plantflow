package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import groovy.transform.CompileStatic

@CompileStatic
class LinearDiagramBuilder implements DiagramBuilder {

    ActivityDiagram build(List<ClassifiedLine> lines) {
        boolean hasStart = false
        boolean hasEnd = false
        List<ActivityNode> nodes = []

        for (ClassifiedLine line : lines) {
            switch (line.type) {
                case LineType.START:
                    hasStart = true
                    break
                case LineType.END:
                    hasEnd = true
                    break
                case LineType.ACTION:
                    nodes.add(new ActionNode(line.action))
                    break
                default:
                    break
            }
        }

        return new ActivityDiagram(hasStart, hasEnd, nodes)
    }
}
