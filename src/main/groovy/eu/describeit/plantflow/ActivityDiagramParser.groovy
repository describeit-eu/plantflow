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

    PetriNetStructure parse(String pumlContent) {
        DiagramValidator.validateContent(pumlContent)
        List<ClassifiedLine> lines = classifier.classifyAll(pumlContent.readLines())
        ActivityDiagram diagram = selectBuilder(lines).build(lines)
        DiagramValidator.validate(diagram)
        return compiler.compile(diagram)
    }

    private static DiagramBuilder selectBuilder(List<ClassifiedLine> lines) {
        if (lines.any { ClassifiedLine line -> line.type == LineType.IF }) return new ConditionalDiagramBuilder()
        if (lines.any { ClassifiedLine line -> line.type in [LineType.FORK, LineType.FORK_AGAIN, LineType.END_MERGE] }) {
            return new ForkDiagramBuilder()
        }
        return new LinearDiagramBuilder()
    }
}
