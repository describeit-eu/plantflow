package eu.describeit.plantflow.ast

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@EqualsAndHashCode
@ToString
class ActivityDiagram {

    boolean hasStart = false
    boolean hasEnd = false
    boolean hasIf = false
    boolean hasElse = false
    boolean hasEndif = false
    final List<ActivityNode> nodes = []

    ActivityDiagram() {
    }

    ActivityDiagram(boolean hasStart, boolean hasEnd, List<ActivityNode> nodes = []) {
        this.hasStart = hasStart
        this.hasEnd = hasEnd
        if (nodes != null) {
            this.nodes.addAll(nodes)
        }
    }

    ActivityDiagram withStart(boolean hasStart = true) {
        this.hasStart = hasStart
        return this
    }

    ActivityDiagram withEnd(boolean hasEnd = true) {
        this.hasEnd = hasEnd
        return this
    }

    ActivityDiagram withIf(boolean hasIf = true) {
        this.hasIf = hasIf
        return this
    }

    ActivityDiagram withElse(boolean hasElse = true) {
        this.hasElse = hasElse
        return this
    }

    ActivityDiagram withEndif(boolean hasEndif = true) {
        this.hasEndif = hasEndif
        return this
    }

    ActivityDiagram addNode(ActivityNode node) {
        if (node != null) {
            this.nodes.add(node)
        }
        return this
    }

    List<ActivityNode> getNodes() {
        return Collections.unmodifiableList(nodes)
    }

    boolean isConditional() {
        return hasIf || hasElse || hasEndif || nodes.any { ActivityNode node -> node instanceof ConditionalNode }
    }
}
