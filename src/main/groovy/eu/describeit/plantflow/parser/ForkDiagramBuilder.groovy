package eu.describeit.plantflow.parser

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ForkNode
import groovy.transform.CompileStatic

@CompileStatic
class ForkDiagramBuilder implements DiagramBuilder {

    private static final String ERR_FORK_AGAIN_NO_CONTEXT = 'fork again without active fork context'
    private static final String ERR_END_MERGE_NO_CONTEXT = 'end merge without active fork context'
    private static final String ERR_UNCLOSED_FORK = 'Unclosed fork - missing end merge'

    ActivityDiagram build(List<ClassifiedLine> lines) {
        ForkState state = new ForkState()
        for (ClassifiedLine line : lines) {
            processLine(line, state)
        }
        if (!state.forkStack.isEmpty()) {
            throw new IllegalArgumentException(ERR_UNCLOSED_FORK)
        }
        return new ActivityDiagram(state.hasStart, state.hasEnd, state.hasFork, state.hasEndMerge, state.nodes)
    }

    private static void processLine(ClassifiedLine line, ForkState state) {
        switch (line.type) {
            case LineType.START:
                state.hasStart = true
                break
            case LineType.END:
                state.hasEnd = true
                break
            case LineType.FORK:
                processFork(state)
                break
            case LineType.FORK_AGAIN:
                processForkAgain(state)
                break
            case LineType.END_MERGE:
                processEndMerge(state)
                break
            case LineType.ACTION:
                processAction(line, state)
                break
            default:
                break
        }
    }

    private static void processFork(ForkState state) {
        state.hasFork = true
        state.forkStack.push(state.branches.size())
        state.branches.add([])
        state.currentBranchIndex = state.branches.size() - 1
    }

    private static void processForkAgain(ForkState state) {
        if (state.forkStack.isEmpty()) {
            throw new IllegalArgumentException(ERR_FORK_AGAIN_NO_CONTEXT)
        }
        state.branches.add([])
        state.currentBranchIndex = state.branches.size() - 1
    }

    private static void processEndMerge(ForkState state) {
        if (state.forkStack.isEmpty()) {
            throw new IllegalArgumentException(ERR_END_MERGE_NO_CONTEXT)
        }
        state.hasEndMerge = true
        state.forkStack.pop()
        state.currentBranchIndex = -1
        state.forkComplete = state.forkStack.isEmpty()
        if (state.forkComplete && !state.branches.isEmpty()) {
            List<List<ActivityNode>> branchList = state.branches as List<List<ActivityNode>>
            state.nodes.add(new ForkNode(branchList))
        }
    }

    private static void processAction(ClassifiedLine line, ForkState state) {
        ActionNode node = new ActionNode(line.action)
        if (state.currentBranchIndex >= 0 && state.currentBranchIndex < state.branches.size()) {
            state.branches.get(state.currentBranchIndex).add(node)
        } else if (state.forkComplete) {
            state.nodes.add(node)
        }
    }

    private static class ForkState {
        boolean hasStart = false
        boolean hasEnd = false
        boolean hasFork = false
        boolean hasEndMerge = false
        List<ActivityNode> nodes = []
        List<List<ActionNode>> branches = []
        Stack<Integer> forkStack = [] as Stack<Integer>
        int currentBranchIndex = -1
        boolean forkComplete = false
    }
}
