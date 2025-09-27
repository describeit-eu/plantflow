package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static org.apache.commons.lang3.StringUtils.substringBetween
import static org.apache.commons.text.CaseUtils.toCamelCase

@CompileStatic
@Slf4j
final class PlantUmlConverter {
  static final List<String> linesToMethod = [
      'start', 'stop', 'end', 'fork', 'fork again', 'end merge', 'repeat', 'endswitch', 'endwhile', 'detach'
  ]

  static final Map<String, String> linesMap = [
      endif: '}'
  ]

  static String getResourceText(String file) {
    return PlantUmlConverter.class.getClassLoader().getResource(file).text.trim()
  }
  static String tab(int size) {
    return ' '.repeat(size)
  }

  static String convertToPlantFlowDsl(final String pumlText) {
    def pflow = new StringBuffer()

    pumlText.eachLine { String line ->
      int tabSize = line.takeWhile { it == ' ' }.size()
      String lineTrimmed = line.trim()
      String lineConverted = null

      switch (lineTrimmed) {
        case ~/^@.*/       : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ~/^-.*>$/     : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ''            : lineConverted = ''; break
        case linesToMethod : lineConverted = convertLineToMethod(lineTrimmed); break
        case linesMap*.key : lineConverted = linesMap[line]; break
        case ~/^:.*;$/     : lineConverted = convertLineToActionMethod(lineTrimmed); break
        default            : lineConverted = ExpressionConverter.convert(lineTrimmed); break
      }

      if (lineConverted != null) {
        pflow.append(tab(tabSize))
        pflow.append(lineConverted)
        pflow.append(System.lineSeparator())
      }
    }

    return pflow.toString()
  }

  private static String convertLineToMethod(String line) {
    log.info('convertLineToMethod() - line:"{}"', line)

    return toCamelCase(line, false, ' ' as char) + "()"
  }

  private static String convertLineToActionMethod(String line) {
    log.info('convertLineToActionMethod() - line:"{}"', line)

    String actionName = substringBetween(line, ':', ';')

    return "if (action(\"${actionName}\")) { stop(); return }"
  }
}
