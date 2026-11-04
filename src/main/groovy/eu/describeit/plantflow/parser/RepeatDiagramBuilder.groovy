package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.RepeatNode
import groovy.transform.CompileStatic

@CompileStatic
class RepeatDiagramBuilder implements DiagramBuilder {

    private static final String ERR_REPEAT_WHILE_NO_CONTEXT = 'repeat while without active repeat'
    private static final String ERR_UNCLOSED_REPEAT = 'Unclosed repeat - missing repeat while'
    private static final String ERR_DETACH_NO_CONTEXT = 'detach must directly follow a repeat while loop'
    private static final String ERR_LOOP_BODY_EMPTY = 'Repeat loop body must contain at least one action'

    ActivityDiagram build(List<ClassifiedLine> lines) {
        RepeatState state = new RepeatState()
        for (ClassifiedLine line : lines) {
            processLine(line, state)
        }
        if (!state.loopStack.isEmpty()) {
            throw new IllegalArgumentException(ERR_UNCLOSED_REPEAT)
        }
        return new ActivityDiagram(state.hasStart, state.hasEnd, state.nodes)
            .withRepeat(state.hasRepeat)
            .withRepeatWhile(state.hasRepeatWhile)
            .withDetach(state.hasDetach)
    }

    private static void processLine(ClassifiedLine line, RepeatState state) {
        switch (line.type) {
            case LineType.START:
                state.hasStart = true
                break
            case LineType.END:
                state.hasEnd = true
                break
            case LineType.REPEAT:
                state.hasRepeat = true
                state.loopStack.push(new LoopFrame())
                break
            case LineType.REPEAT_WHILE:
                processRepeatWhile(line, state)
                break
            case LineType.DETACH:
                processDetach(state)
                break
            case LineType.ACTION:
                currentContainer(state).add(new ActionNode(line.action))
                break
            default:
                break
        }
    }

    private static void processRepeatWhile(ClassifiedLine line, RepeatState state) {
        if (state.loopStack.isEmpty()) {
            throw new IllegalArgumentException(ERR_REPEAT_WHILE_NO_CONTEXT)
        }
        state.hasRepeatWhile = true
        LoopFrame frame = state.loopStack.pop()
        if (frame.body.isEmpty()) {
            throw new IllegalArgumentException(ERR_LOOP_BODY_EMPTY)
        }
        RepeatNode node = new RepeatNode(line.guard, frame.body)
        currentContainer(state).add(node)
        state.lastLoop = node
    }

    private static void processDetach(RepeatState state) {
        List<ActivityNode> container = currentContainer(state)
        if (state.lastLoop == null || container.isEmpty() || !container.last().is(state.lastLoop)) {
            throw new IllegalArgumentException(ERR_DETACH_NO_CONTEXT)
        }
        state.lastLoop.detach = true
        state.hasDetach = true
    }

    private static List<ActivityNode> currentContainer(RepeatState state) {
        return state.loopStack.isEmpty() ? state.nodes : state.loopStack.peek().body
    }

    private static class LoopFrame {
        List<ActivityNode> body = []
    }

    private static class RepeatState {
        boolean hasStart = false
        boolean hasEnd = false
        boolean hasRepeat = false
        boolean hasRepeatWhile = false
        boolean hasDetach = false
        List<ActivityNode> nodes = []
        Stack<LoopFrame> loopStack = [] as Stack<LoopFrame>
        RepeatNode lastLoop = null
    }
}
