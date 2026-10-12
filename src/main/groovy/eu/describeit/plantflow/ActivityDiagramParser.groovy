package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActionNode
import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.ast.ActivityNode
import eu.describeit.plantflow.ast.ConditionalNode
import eu.describeit.plantflow.ast.ForkNode
import eu.describeit.plantflow.engine.PetriNet
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
@Slf4j
class ActivityDiagramParser {
    private enum Branch { NONE, THEN, ELSE }

    private static final Pattern ACTION_PATTERN = Pattern.compile('^\\s*:(.+);\\s*$')
    private static final Pattern IF_PATTERN = Pattern.compile(/^\s*if\s*\(\s*(.+?)\s*\)\s*then\s*\(\s*(.+?)\s*\)\s*$/)
    private static final Pattern ELSE_PATTERN = Pattern.compile(/^\s*else\s*\(\s*(.+?)\s*\)\s*$/)
    private static final Pattern ENDIF_PATTERN = Pattern.compile(/^\s*endif\s*$/)
    private static final Pattern FORK_PATTERN = Pattern.compile(/^\s*fork\s*$/)
    private static final Pattern FORK_AGAIN_PATTERN = Pattern.compile(/^\s*fork again\s*$/)
    private static final Pattern END_MERGE_PATTERN = Pattern.compile(/^\s*end merge\s*$/)

    private static final String START_KEYWORD = 'start', END_KEYWORD = 'end', STOP_KEYWORD = 'stop', IF_KEYWORD = 'if'
    private static final String FORK_KEYWORD = 'fork', FORK_AGAIN_KEYWORD = 'fork again', END_MERGE_KEYWORD = 'end merge'
    private static final String STARTUML = '@startuml', ENDUML = '@enduml', COMMENT = '\''
    private static final List<String> IGNORED = [COMMENT, STARTUML, ENDUML]

    private final PetriNetCompiler compiler = new PetriNetCompiler()

    PetriNet parse(File file) {
        DiagramValidator.validateFile(file)
        return parse(file.getText(StandardCharsets.UTF_8.name()))
    }

    PetriNet parse(String pumlContent) {
        DiagramValidator.validateContent(pumlContent)
        List<String> lines = pumlContent.readLines()
        ActivityDiagram diagram
        if (lines.any { String line -> line.trim().startsWith(IF_KEYWORD) }) {
            diagram = parseConditional(lines)
        } else if (lines.any { String line -> 
            line.trim().startsWith(FORK_KEYWORD) || 
            line.trim().startsWith(FORK_AGAIN_KEYWORD) || 
            line.trim().startsWith(END_MERGE_KEYWORD) 
        }) {
            diagram = parseFork(lines)
        } else {
            diagram = parseLinear(lines)
        }
        DiagramValidator.validate(diagram)
        return compiler.compile(diagram)
    }

    private ActivityDiagram parseLinear(List<String> lines) {
        ActivityDiagram diagram = new ActivityDiagram()
        for (String rawLine : lines) {
            String line = rawLine.trim()
            Matcher matcher = ACTION_PATTERN.matcher(line)
            if (!checkControl(line, diagram) && matcher.matches()) {
                diagram.addNode(new ActionNode(matcher.group(1).trim()))
            }
        }
        return diagram
    }

    private ActivityDiagram parseConditional(List<String> lines) {
        ActivityDiagram diagram = new ActivityDiagram()
        String guard = null
        List<ActionNode> thenActions = []
        List<ActionNode> elseActions = []
        Branch branch = Branch.NONE
        for (String rawLine : lines) {
            String line = rawLine.trim()
            if (checkControl(line, diagram)) continue
            Matcher ifMatcher = IF_PATTERN.matcher(line)
            if (ifMatcher.matches()) {
                diagram.hasIf = true
                guard = ifMatcher.group(1).trim()
                branch = Branch.THEN
                continue
            }
            branch = updateBranch(line, diagram, branch)
            recordAction(line, branch, thenActions, elseActions)
        }
        finalizeConditional(diagram, guard, thenActions, elseActions)
        return diagram
    }

    private ActivityDiagram parseFork(List<String> lines) {
        ActivityDiagram diagram = new ActivityDiagram()
        List<List<ActionNode>> branches = []
        int currentBranchIndex = -1
        Stack<Integer> forkStack = [] as Stack
        boolean forkComplete = false
        
        for (String rawLine : lines) {
            String line = rawLine.trim()
            
            if (checkControl(line, diagram)) continue
            
            if (handleForkMatching(line, diagram, forkStack, branches)) {
                currentBranchIndex = branches.size() - 1
                continue
            }
            
            if (handleForkAgainMatching(line, forkStack, branches)) {
                currentBranchIndex = branches.size() - 1
                continue
            }
            
            if (handleEndMergeMatching(line, diagram, forkStack, branches)) {
                currentBranchIndex = -1
                forkComplete = forkStack.isEmpty()
                continue
            }
            
            handleActionRecording(line, diagram, branches, currentBranchIndex, forkComplete)
        }
        
        validateForkCompletion(forkStack)

        return diagram
    }

    private boolean handleForkMatching(String line, ActivityDiagram diagram, Stack<Integer> forkStack, 
                                     List<List<ActionNode>> branches) {
        Matcher forkMatcher = FORK_PATTERN.matcher(line)
        if (forkMatcher.matches()) {
            diagram.hasFork = true
            forkStack.push(branches.size())
            branches.add([])
            return true
        }
        return false
    }

    private boolean handleForkAgainMatching(String line, Stack<Integer> forkStack, 
                                           List<List<ActionNode>> branches) {
        Matcher forkAgainMatcher = FORK_AGAIN_PATTERN.matcher(line)
        if (forkAgainMatcher.matches()) {
            if (forkStack.isEmpty()) {
                throw new IllegalArgumentException('fork again without active fork context')
            }
            branches.add([])
            return true
        }
        return false
    }

    private boolean handleEndMergeMatching(String line, ActivityDiagram diagram, Stack<Integer> forkStack,
                                           List<List<ActionNode>> branches) {
        Matcher endMergeMatcher = END_MERGE_PATTERN.matcher(line)
        if (endMergeMatcher.matches()) {
            if (forkStack.isEmpty()) {
                throw new IllegalArgumentException('end merge without active fork context')
            }
            diagram.hasEndMerge = true
            forkStack.pop()
            if (forkStack.isEmpty() && !branches.isEmpty()) {
                List<List<ActivityNode>> branchList = branches as List<List<ActivityNode>>
                diagram.addNode(new ForkNode(branchList))
            }
            return true
        }
        return false
    }

    private void handleActionRecording(String line, ActivityDiagram diagram, List<List<ActionNode>> branches,
                                      int currentBranchIndex, boolean forkComplete) {
        Matcher actionMatcher = ACTION_PATTERN.matcher(line)
        if (actionMatcher.matches()) {
            if (currentBranchIndex >= 0 && currentBranchIndex < branches.size()) {
                branches.get(currentBranchIndex).add(new ActionNode(actionMatcher.group(1).trim()))
            } else if (forkComplete) {
                diagram.addNode(new ActionNode(actionMatcher.group(1).trim()))
            }
        }
    }

    private void validateForkCompletion(Stack<Integer> forkStack) {
        if (!forkStack.isEmpty()) {
            throw new IllegalArgumentException('Unclosed fork - missing end merge')
        }
    }

    private Branch updateBranch(String line, ActivityDiagram diagram, Branch branch) {
        if (ELSE_PATTERN.matcher(line).matches()) {
            diagram.hasElse = true
            return Branch.ELSE
        }
        if (ENDIF_PATTERN.matcher(line).matches()) {
            diagram.hasEndif = true
            return Branch.NONE
        }
        return branch
    }

    private void recordAction(String line, Branch branch, List<ActionNode> thenActions, List<ActionNode> elseActions) {
        Matcher matcher = ACTION_PATTERN.matcher(line)
        if (!matcher.matches()) return
        ActionNode node = new ActionNode(matcher.group(1).trim())
        if (branch == Branch.THEN) thenActions.add(node)
        if (branch == Branch.ELSE) elseActions.add(node)
    }

    private void finalizeConditional(ActivityDiagram diagram, String guard, List<ActionNode> tNodes, List<ActionNode> eNodes) {
        if (guard != null) diagram.addNode(new ConditionalNode(guard, tNodes, eNodes))
        else if (!diagram.hasElse && !diagram.hasEndif) diagram.hasEndif = true
    }

    private boolean checkControl(String line, ActivityDiagram diagram) {
        if (line.isEmpty() || IGNORED.any { String p -> line.startsWith(p) }) return true
        if (line == START_KEYWORD) return (diagram.hasStart = true)
        if (line == END_KEYWORD || line == STOP_KEYWORD) return (diagram.hasEnd = true)
        return false
    }
}
