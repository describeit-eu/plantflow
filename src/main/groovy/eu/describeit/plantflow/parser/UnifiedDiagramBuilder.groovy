package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.RepeatNode
import groovy.transform.CompileStatic

@CompileStatic
class UnifiedDiagramBuilder implements DiagramBuilder {

    private static final String ERR_REPEAT_WHILE_NO_CONTEXT = 'repeat while without active repeat'
    private static final String ERR_UNCLOSED_REPEAT = 'Unclosed repeat - missing repeat while'
    private static final String ERR_DETACH_NO_CONTEXT = 'detach must directly follow a repeat while loop'
    private static final String ERR_LOOP_BODY_EMPTY = 'Repeat loop body must contain at least one action'
    private static final String ERR_UNCLOSED_IF = 'Unclosed if - missing endif'
    private static final String ERR_ELSE_NO_CONTEXT = 'else without active if'
    private static final String ERR_ENDIF_NO_CONTEXT = 'endif without active if'

    ActivityDiagram build(List<ClassifiedLine> lines) {
        UnifiedState state = new UnifiedState()
        for (ClassifiedLine line : lines) {
            processLine(line, state)
        }
        if (!state.stack.isEmpty()) {
            if (state.stack.peek() instanceof LoopFrame) {
                throw new IllegalArgumentException(ERR_UNCLOSED_REPEAT)
            }
            throw new IllegalArgumentException(ERR_UNCLOSED_IF)
        }
        return new ActivityDiagram(state.hasStart, state.hasEnd, state.nodes)
            .withIf(state.hasIf)
            .withElse(state.hasElse)
            .withEndif(state.hasEndif)
            .withRepeat(state.hasRepeat)
            .withRepeatWhile(state.hasRepeatWhile)
            .withDetach(state.hasDetach)
    }

    private static void processLine(ClassifiedLine line, UnifiedState state) {
        switch (line.type) {
            case LineType.START:
                state.hasStart = true
                break
            case LineType.END:
                state.hasEnd = true
                break
            case LineType.REPEAT:
                state.hasRepeat = true
                state.stack.push(new LoopFrame())
                break
            case LineType.REPEAT_WHILE:
                processRepeatWhile(line, state)
                break
            case LineType.DETACH:
                processDetach(state)
                break
            case LineType.IF:
                processIf(line, state)
                break
            case LineType.ELSE:
                processElse(state)
                break
            case LineType.ENDIF:
                processEndif(state)
                break
            case LineType.ACTION:
                currentContainer(state).add(new ActionNode(line.action))
                break
            default:
                break
        }
    }

    private static void processRepeatWhile(ClassifiedLine line, UnifiedState state) {
        Object top = topOf(state)
        if (top == null || top instanceof ConditionalFrame) {
            throw new IllegalArgumentException(ERR_REPEAT_WHILE_NO_CONTEXT)
        }
        state.hasRepeatWhile = true
        LoopFrame frame = (LoopFrame) state.stack.pop()
        if (frame.body.isEmpty()) {
            throw new IllegalArgumentException(ERR_LOOP_BODY_EMPTY)
        }
        RepeatNode node = new RepeatNode(line.guard, frame.body)
        currentContainer(state).add(node)
        state.lastLoop = node
    }

    private static void processDetach(UnifiedState state) {
        List<ActivityNode> container = currentContainer(state)
        if (state.lastLoop == null || container.isEmpty() || !container.last().is(state.lastLoop)) {
            throw new IllegalArgumentException(ERR_DETACH_NO_CONTEXT)
        }
        state.lastLoop.detach = true
        state.hasDetach = true
    }

    private static void processIf(ClassifiedLine line, UnifiedState state) {
        if (line.guard != null) {
            state.hasIf = true
        }
        state.stack.push(new ConditionalFrame(line.guard))
    }

    private static void processElse(UnifiedState state) {
        Object top = topOf(state)
        if (top == null || top instanceof LoopFrame) {
            throw new IllegalArgumentException(ERR_ELSE_NO_CONTEXT)
        }
        state.hasElse = true
        ((ConditionalFrame) top).inElse = true
    }

    private static void processEndif(UnifiedState state) {
        Object top = topOf(state)
        if (top == null || top instanceof LoopFrame) {
            throw new IllegalArgumentException(ERR_ENDIF_NO_CONTEXT)
        }
        state.hasEndif = true
        ConditionalFrame frame = (ConditionalFrame) state.stack.pop()
        currentContainer(state).add(new ConditionalNode(frame.guard, frame.thenBranch, frame.elseBranch))
    }

    private static Object topOf(UnifiedState state) {
        state.stack.isEmpty() ? null : state.stack.peek()
    }

    private static List<ActivityNode> currentContainer(UnifiedState state) {
        if (state.stack.isEmpty()) {
            return state.nodes
        }
        Object top = state.stack.peek()
        if (top instanceof LoopFrame) {
            return ((LoopFrame) top).body
        }
        ConditionalFrame frame = (ConditionalFrame) top
        return frame.inElse ? frame.elseBranch : frame.thenBranch
    }

    private static class LoopFrame {
        List<ActivityNode> body = []
    }

    private static class ConditionalFrame {
        final String guard
        List<ActivityNode> thenBranch = []
        List<ActivityNode> elseBranch = []
        boolean inElse = false

        ConditionalFrame(String guard) {
            this.guard = guard
        }
    }

    private static class UnifiedState {
        boolean hasStart = false
        boolean hasEnd = false
        boolean hasIf = false
        boolean hasElse = false
        boolean hasEndif = false
        boolean hasRepeat = false
        boolean hasRepeatWhile = false
        boolean hasDetach = false
        List<ActivityNode> nodes = []
        Stack<Object> stack = [] as Stack<Object>
        RepeatNode lastLoop = null
    }
}
