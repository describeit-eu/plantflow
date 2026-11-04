package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.compiler.DiagramCompiler
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.parser.ClassifiedLine
import eu.describeit.plantflow.parser.ConditionalDiagramBuilder
import eu.describeit.plantflow.parser.DiagramBuilder
import eu.describeit.plantflow.parser.ForkDiagramBuilder
import eu.describeit.plantflow.parser.LineType
import eu.describeit.plantflow.parser.LinearDiagramBuilder
import eu.describeit.plantflow.parser.PumlLineClassifier
import groovy.transform.CompileStatic

import java.nio.charset.StandardCharsets

@CompileStatic
class ActivityDiagramParser {

    private final PumlLineClassifier classifier = new PumlLineClassifier()
    private final DiagramCompiler compiler = new DiagramCompiler()

    PetriNetStructure parse(File file) {
        DiagramValidator.validateFile(file)
        return parse(file.getText(StandardCharsets.UTF_8.name()))
    }

    private static final String ERR_MALFORMED_LINE = 'Malformed fork-related line'
    private static final String ERR_MIXED_CONDITIONAL_FORK = 'Diagram mixes if-then-else and fork constructs, which is not supported'

    PetriNetStructure parse(String pumlContent) {
        DiagramValidator.validateContent(pumlContent)
        List<ClassifiedLine> lines = classifier.classifyAll(pumlContent.readLines())
        rejectMalformedLines(lines)
        ActivityDiagram diagram = selectBuilder(lines).build(lines)
        DiagramValidator.validate(diagram)
        return compiler.compile(diagram)
    }

    private static void rejectMalformedLines(List<ClassifiedLine> lines) {
        List<String> malformedTexts = lines.findAll { ClassifiedLine line -> line.type == LineType.MALFORMED }
                .collect { ClassifiedLine line -> "'" + line.text.trim() + "'" }
        if (!malformedTexts.isEmpty()) {
            throw new IllegalArgumentException("$ERR_MALFORMED_LINE: ${malformedTexts.join(', ')}")
        }
    }

    private static DiagramBuilder selectBuilder(List<ClassifiedLine> lines) {
        boolean hasConditional = lines.any { ClassifiedLine line -> line.type == LineType.IF }
        boolean hasForkLines = lines.any { ClassifiedLine line -> line.type in [LineType.FORK, LineType.FORK_AGAIN, LineType.END_MERGE, LineType.END_FORK] }
        if (hasConditional && hasForkLines) {
            throw new IllegalArgumentException(ERR_MIXED_CONDITIONAL_FORK)
        }
        if (hasConditional) return new ConditionalDiagramBuilder()
        if (hasForkLines) return new ForkDiagramBuilder()
        return new LinearDiagramBuilder()
    }
}
