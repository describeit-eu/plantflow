package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.ast.RepeatNode
import groovy.transform.CompileStatic

/**
 * Single stack-based diagram builder: one frame stack holds conditional,
 * Repeat Loop, Fork, and Switch frames, so any construct nests inside any other
 * at any depth. A nested fork becomes a real nested {@link ForkNode} inside the
 * enclosing branch, and actions after an inner terminator stay in that branch.
 */
@CompileStatic
class UnifiedDiagramBuilder implements DiagramBuilder {

    private static final String ERR_REPEAT_WHILE_NO_CONTEXT = 'repeat while without active repeat'
    private static final String ERR_UNCLOSED_REPEAT = 'Unclosed repeat - missing repeat while'
    private static final String ERR_DETACH_NO_CONTEXT = 'detach must directly follow a repeat while loop'
    private static final String ERR_LOOP_BODY_EMPTY = 'Repeat loop body must contain at least one node'
    private static final String ERR_UNCLOSED_IF = 'Unclosed if - missing endif'
    private static final String ERR_ELSE_NO_CONTEXT = 'else without active if'
    private static final String ERR_ENDIF_NO_CONTEXT = 'endif without active if'
    private static final String ERR_FORK_AGAIN_NO_CONTEXT = 'fork again without active fork context'
    private static final String ERR_END_MERGE_NO_CONTEXT = 'end merge without active fork context'
    private static final String ERR_END_FORK_NO_CONTEXT = 'end fork without active fork context'
    private static final String ERR_UNCLOSED_FORK = 'Unclosed fork - missing end merge or end fork'
    private static final String ERR_FORK_BRANCH_EMPTY = 'Fork branch must contain at least one node'
    private static final String ERR_MISMATCHED_FORK_TERMINATORS = "Mismatched fork terminators within one fork construct: 'end merge' and 'end fork' cannot be mixed"

    private static final List<LineType> REPEAT_LINE_TYPES = [LineType.REPEAT, LineType.REPEAT_WHILE, LineType.DETACH]
    private static final List<LineType> CONDITIONAL_LINE_TYPES = [LineType.IF, LineType.ELSE, LineType.ENDIF]
    private static final List<LineType> FORK_LINE_TYPES = [LineType.FORK, LineType.FORK_AGAIN, LineType.END_MERGE, LineType.END_FORK]
    private static final List<LineType> SWITCH_LINE_TYPES = [LineType.SWITCH, LineType.CASE, LineType.ENDSWITCH]

    ActivityDiagram build(List<ClassifiedLine> lines) {
        UnifiedState state = new UnifiedState()
        for (ClassifiedLine line : lines) {
            processLine(line, state)
        }
        if (!state.stack.isEmpty()) {
            switch (state.stack.peek()) {
                case LoopFrame: throw new IllegalArgumentException(ERR_UNCLOSED_REPEAT)
                case ForkFrame: throw new IllegalArgumentException(ERR_UNCLOSED_FORK)
                case SwitchFrame: throw new IllegalArgumentException(SwitchFrame.ERR_UNCLOSED_SWITCH)
                default: throw new IllegalArgumentException(ERR_UNCLOSED_IF)
            }
        }
        return new ActivityDiagram(state.hasStart, state.hasEnd, state.nodes)
            .withIf(state.hasIf)
            .withElse(state.hasElse)
            .withEndif(state.hasEndif)
            .withFork(state.hasFork)
            .withEndMerge(state.hasEndMerge)
            .withEndFork(state.hasEndFork)
            .withRepeat(state.hasRepeat)
            .withRepeatWhile(state.hasRepeatWhile)
            .withDetach(state.hasDetach)
            .withSwitch(state.hasSwitch)
            .withEndswitch(state.hasEndswitch)
    }

    private static void processLine(ClassifiedLine line, UnifiedState state) {
        switch (line.type) {
            case LineType.START:
                state.hasStart = true
                break
            case LineType.END:
                state.hasEnd = true
                break
            case REPEAT_LINE_TYPES:
                processRepeatLine(line, state)
                break
            case CONDITIONAL_LINE_TYPES:
                processConditionalLine(line, state)
                break
            case FORK_LINE_TYPES:
                processForkLine(line, state)
                break
            case SWITCH_LINE_TYPES:
                processSwitchLine(line, state)
                break
            case LineType.ACTION:
                currentContainer(state).add(new ActionNode(line.action))
                break
            default:
                break
        }
    }

    private static void processRepeatLine(ClassifiedLine line, UnifiedState state) {
        switch (line.type) {
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
            default:
                break
        }
    }

    private static void processConditionalLine(ClassifiedLine line, UnifiedState state) {
        switch (line.type) {
            case LineType.IF:
                processIf(line, state)
                break
            case LineType.ELSE:
                processElse(state)
                break
            case LineType.ENDIF:
                processEndif(state)
                break
            default:
                break
        }
    }

    private static void processForkLine(ClassifiedLine line, UnifiedState state) {
        switch (line.type) {
            case LineType.FORK:
                processFork(state)
                break
            case LineType.FORK_AGAIN:
                processForkAgain(state)
                break
            case LineType.END_MERGE:
                processForkTerminator(state, false)
                break
            case LineType.END_FORK:
                processForkTerminator(state, true)
                break
            default:
                break
        }
    }

    private static void processSwitchLine(ClassifiedLine line, UnifiedState state) {
        if (line.type == LineType.SWITCH) {
            state.hasSwitch = true
            state.stack.push(new SwitchFrame(line.guard))
            return
        }
        Object top = topOf(state)
        if (!(top instanceof SwitchFrame)) {
            throw new IllegalArgumentException(line.type == LineType.CASE ?
                SwitchFrame.ERR_CASE_NO_CONTEXT : SwitchFrame.ERR_ENDSWITCH_NO_CONTEXT)
        }
        SwitchFrame frame = (SwitchFrame) top
        if (line.type == LineType.CASE) {
            frame.addCase(line.guard)
        } else {
            state.hasEndswitch = true
            state.stack.pop()
            currentContainer(state).add(frame.toNode())
        }
    }

    private static void processRepeatWhile(ClassifiedLine line, UnifiedState state) {
        Object top = topOf(state)
        if (!(top instanceof LoopFrame)) {
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
        if (!(top instanceof ConditionalFrame)) {
            throw new IllegalArgumentException(ERR_ELSE_NO_CONTEXT)
        }
        state.hasElse = true
        ((ConditionalFrame) top).inElse = true
    }

    private static void processEndif(UnifiedState state) {
        Object top = topOf(state)
        if (!(top instanceof ConditionalFrame)) {
            throw new IllegalArgumentException(ERR_ENDIF_NO_CONTEXT)
        }
        state.hasEndif = true
        ConditionalFrame frame = (ConditionalFrame) state.stack.pop()
        currentContainer(state).add(new ConditionalNode(frame.guard, frame.thenBranch, frame.elseBranch))
    }

    private static void processFork(UnifiedState state) {
        state.hasFork = true
        state.stack.push(new ForkFrame())
    }

    private static void processForkAgain(UnifiedState state) {
        Object top = topOf(state)
        if (!(top instanceof ForkFrame)) {
            throw new IllegalArgumentException(ERR_FORK_AGAIN_NO_CONTEXT)
        }
        ((ForkFrame) top).branches.add([])
    }

    private static void processForkTerminator(UnifiedState state, boolean endFork) {
        Object top = topOf(state)
        if (!(top instanceof ForkFrame)) {
            throw new IllegalArgumentException(endFork ? ERR_END_FORK_NO_CONTEXT : ERR_END_MERGE_NO_CONTEXT)
        }
        ForkFrame frame = (ForkFrame) top
        // terminator consistency is tracked per fork construct: an inner fork may
        // use end merge inside an outer end fork (and vice versa), but one frame
        // must not mix both terminators
        if (frame.terminator != null && frame.terminator != endFork) {
            throw new IllegalArgumentException(ERR_MISMATCHED_FORK_TERMINATORS)
        }
        frame.terminator = endFork
        if (endFork) {
            state.hasEndFork = true
        } else {
            state.hasEndMerge = true
        }
        state.stack.pop()
        if (frame.branches.any { List<ActivityNode> branch -> branch.isEmpty() }) {
            throw new IllegalArgumentException(ERR_FORK_BRANCH_EMPTY)
        }
        currentContainer(state).add(new ForkNode(frame.branches, endFork))
    }

    private static Object topOf(UnifiedState state) {
        state.stack.isEmpty() ? null : state.stack.peek()
    }

    private static List<ActivityNode> currentContainer(UnifiedState state) {
        if (state.stack.isEmpty()) {
            return state.nodes
        }
        switch (state.stack.peek()) {
            case LoopFrame: return ((LoopFrame) state.stack.peek()).body
            case ForkFrame: return ((ForkFrame) state.stack.peek()).branches.last()
            case SwitchFrame: return ((SwitchFrame) state.stack.peek()).currentCaseBody()
            default:
                ConditionalFrame frame = (ConditionalFrame) state.stack.peek()
                return frame.inElse ? frame.elseBranch : frame.thenBranch
        }
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

    private static class ForkFrame {
        List<List<ActivityNode>> branches = [[]]
        Boolean terminator = null
    }

    private static class UnifiedState {
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
        boolean hasSwitch = false
        boolean hasEndswitch = false
        List<ActivityNode> nodes = []
        Stack<Object> stack = [] as Stack<Object>
        RepeatNode lastLoop = null
    }
}
