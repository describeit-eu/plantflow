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
    boolean hasFork = false
    boolean hasEndMerge = false
    boolean hasEndFork = false
    boolean hasRepeat = false
    boolean hasRepeatWhile = false
    boolean hasDetach = false
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

    ActivityDiagram(boolean hasStart, boolean hasEnd, boolean hasFork, boolean hasEndMerge, List<ActivityNode> nodes = []) {
        this.hasStart = hasStart
        this.hasEnd = hasEnd
        this.hasFork = hasFork
        this.hasEndMerge = hasEndMerge
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

    ActivityDiagram withFork(boolean hasFork = true) {
        this.hasFork = hasFork
        return this
    }

    ActivityDiagram withEndMerge(boolean hasEndMerge = true) {
        this.hasEndMerge = hasEndMerge
        return this
    }

    ActivityDiagram withEndFork(boolean hasEndFork = true) {
        this.hasEndFork = hasEndFork
        return this
    }

    ActivityDiagram withRepeat(boolean hasRepeat = true) {
        this.hasRepeat = hasRepeat
        return this
    }

    ActivityDiagram withRepeatWhile(boolean hasRepeatWhile = true) {
        this.hasRepeatWhile = hasRepeatWhile
        return this
    }

    ActivityDiagram withDetach(boolean hasDetach = true) {
        this.hasDetach = hasDetach
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

    boolean hasForkStructure() {
        return hasFork || hasEndMerge || hasEndFork || nodes.any { ActivityNode node -> node instanceof ForkNode }
    }

    boolean hasRepeatStructure() {
        return hasRepeat || hasRepeatWhile || hasDetach || nodes.any { ActivityNode node -> node instanceof RepeatNode }
    }
}
