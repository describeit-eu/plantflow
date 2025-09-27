package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static org.apache.commons.lang3.StringUtils.substringBetween
import static org.apache.commons.text.CaseUtils.toCamelCase

@CompileStatic
@Slf4j
final class PlantUmlConverter {
  enum ConvertMode {CALCULATE, EXECUTE}

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

  static String convertToPlantFlowDsl(final ConvertMode mode, final String pumlText) {
    def pflow = new StringBuffer();

    pumlText.eachLine { String line ->
      int tabSize = line.takeWhile { it == ' ' }.size()
      String lineTrimmed = line.trim()

      switch (lineTrimmed) {
        case ~/^@.*/       : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ~/^-.*>$/     : log.info('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ''            : pflow.append(tab(tabSize)); break
        case linesToMethod : pflow.append(tab(tabSize)).append(convertLineToMethod(lineTrimmed)); break
        case linesMap*.key : pflow.append(tab(tabSize)).append(linesMap[line]); break
        case ~/^:.*;$/     : pflow.append(tab(tabSize)).append(convertLineToActionMethod(lineTrimmed)); break

        default :
          pflow.append(tab(tabSize)).append(ExpressionConverter.convert(lineTrimmed))
          break
      }
      pflow.append(System.lineSeparator())
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
