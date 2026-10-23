package eu.describeit.plantflow.parser

import groovy.transform.CompileStatic

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class PumlLineClassifier {

    private static final Pattern ACTION_PATTERN = Pattern.compile('^\\s*:(.+);\\s*$')
    private static final Pattern IF_PATTERN = Pattern.compile(/^\s*if\s*\(\s*(.+?)\s*\)\s*then\s*\(\s*(.+?)\s*\)\s*$/)
    private static final Pattern ELSE_PATTERN = Pattern.compile(/^\s*else\s*\(\s*(.+?)\s*\)\s*$/)
    private static final Pattern ENDIF_PATTERN = Pattern.compile(/^\s*endif\s*$/)

    private static final String START_KEYWORD = 'start'
    private static final String END_KEYWORD = 'end'
    private static final String STOP_KEYWORD = 'stop'
    private static final String IF_KEYWORD = 'if'
    private static final String FORK_AGAIN_KEYWORD = 'fork again'
    private static final String END_MERGE_KEYWORD = 'end merge'
    private static final String FORK_KEYWORD = 'fork'
    private static final String STARTUML = '@startuml'
    private static final String ENDUML = '@enduml'
    private static final String COMMENT = '\''

    ClassifiedLine classify(String rawLine) {
        String line = rawLine.trim()
        if (line.isEmpty()) return ignored(rawLine)
        if (isIgnored(line)) return ignored(rawLine)
        if (line == START_KEYWORD) return new ClassifiedLine(LineType.START, rawLine)
        if (line == END_KEYWORD || line == STOP_KEYWORD) return new ClassifiedLine(LineType.END, rawLine)

        ClassifiedLine action = tryAction(line, rawLine)
        if (action != null) return action

        ClassifiedLine conditional = tryConditional(line, rawLine)
        if (conditional != null) return conditional

        ClassifiedLine forkLine = tryFork(line, rawLine)
        if (forkLine != null) return forkLine

        return ignored(rawLine)
    }

    List<ClassifiedLine> classifyAll(List<String> lines) {
        return lines.collect { String line -> classify(line) }
    }

    private static ClassifiedLine ignored(String rawLine) {
        return new ClassifiedLine(LineType.IGNORED, rawLine)
    }

    private static boolean isIgnored(String line) {
        return line.startsWith(COMMENT) || line.startsWith(STARTUML) || line.startsWith(ENDUML)
    }

    private static ClassifiedLine tryAction(String line, String rawLine) {
        Matcher m = ACTION_PATTERN.matcher(line)
        if (m.matches()) return new ClassifiedLine(LineType.ACTION, rawLine, m.group(1).trim())
        return null
    }

    private static ClassifiedLine tryConditional(String line, String rawLine) {
        if (line.startsWith(IF_KEYWORD)) {
            Matcher m = IF_PATTERN.matcher(line)
            if (m.matches()) return new ClassifiedLine(LineType.IF, rawLine, null, m.group(1).trim())
            return new ClassifiedLine(LineType.IF, rawLine)
        }
        Matcher elseM = ELSE_PATTERN.matcher(line)
        if (elseM.matches()) return new ClassifiedLine(LineType.ELSE, rawLine, null, null, elseM.group(1).trim())
        if (ENDIF_PATTERN.matcher(line).matches()) return new ClassifiedLine(LineType.ENDIF, rawLine)
        return null
    }

    private static ClassifiedLine tryFork(String line, String rawLine) {
        if (line.startsWith(FORK_AGAIN_KEYWORD)) return new ClassifiedLine(LineType.FORK_AGAIN, rawLine)
        if (line.startsWith(END_MERGE_KEYWORD)) return new ClassifiedLine(LineType.END_MERGE, rawLine)
        if (line.startsWith(FORK_KEYWORD)) return new ClassifiedLine(LineType.FORK, rawLine)
        return null
    }
}
