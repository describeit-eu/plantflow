package eu.describeit.plantflow

import eu.describeit.plantflow.ast.ActivityDiagram
import eu.describeit.plantflow.compiler.DiagramCompiler
import eu.describeit.plantflow.engine.PetriNetStructure
import eu.describeit.plantflow.parser.ClassifiedLine
import eu.describeit.plantflow.parser.LineType
import eu.describeit.plantflow.parser.PumlLineClassifier
import eu.describeit.plantflow.parser.UnifiedDiagramBuilder
import groovy.transform.CompileStatic

import java.nio.charset.StandardCharsets

@CompileStatic
class ActivityDiagramParser {

    private static final String ERR_MALFORMED_LINE = 'Malformed line'

    private final PumlLineClassifier classifier = new PumlLineClassifier()
    private final DiagramCompiler compiler = new DiagramCompiler()

    PetriNetStructure parse(File file) {
        DiagramValidator.validateFile(file)
        return parse(file.getText(StandardCharsets.UTF_8.name()))
    }

    PetriNetStructure parse(String pumlContent) {
        DiagramValidator.validateContent(pumlContent)
        List<ClassifiedLine> lines = classifier.classifyAll(pumlContent.readLines())
        rejectMalformedLines(lines)
        ActivityDiagram diagram = new UnifiedDiagramBuilder().build(lines)
        DiagramValidator.validate(diagram)
        return compiler.compile(diagram)
    }

    private static void rejectMalformedLines(List<ClassifiedLine> lines) {
        List<String> malformedTexts = lines.findAll { ClassifiedLine line -> line.type == LineType.MALFORMED }
                .collect { ClassifiedLine line -> "'${line.text.trim()}'".toString() }
        if (!malformedTexts.isEmpty()) {
            throw new IllegalArgumentException("$ERR_MALFORMED_LINE: ${malformedTexts.join(', ')}")
        }
    }
}
