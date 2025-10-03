package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static org.apache.commons.lang3.StringUtils.substringBetween

@CompileStatic
@Slf4j
final class PlantUmlConverter {
  static final List<String> linesToDrop = ['start', 'stop', 'end', 'detach']

  static final Map<String, String> linesMap = [
      'fork'       : 'fork {',
      'fork again' : '} forkAgain {',
      'end merge'  : '}; if (endFork()) return',
  ]

  static String getResourceText(String file) {
    return PlantUmlConverter.class.getClassLoader().getResource(file).text.trim()
  }

  static String convertToPlantFlowDsl(final String pumlText) {
    def pflow = new StringBuffer()

    pumlText.eachLine { String line ->
      int tabSize = line.takeWhile { it == ' ' }.size()
      String lineTrimmed = line.trim()
      String lineConverted = null

      switch (lineTrimmed) {
        case ~/^@.*/       : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ~/^-.*->$/    : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case linesToDrop   : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ''            : lineConverted = ''; break
        case linesMap*.key : lineConverted = linesMap[lineTrimmed]; break
        case ~/^:.*;$/     : lineConverted = convertLineToActionMethod(lineTrimmed); break
        default            : lineConverted = convertExpression(lineTrimmed); break
      }

      if (lineConverted != null) {
        pflow.append(' '.repeat(tabSize))
             .append(lineConverted)
             .append(System.lineSeparator())
      }
    }

    return pflow.toString()
  }

  private static String convertLineToActionMethod(String line) {
    log.info('convertLineToActionMethod() - line:"{}"', line)

    String actionName = substringBetween(line, ':', ';')

    return "if (isActive(\"${actionName}\")) return"
  }

  private static String convertExpression(String line) {
    try {
      return ConditionalConverter.convert(line)
    } catch (IllegalArgumentException ignored) {
      // ignore
    }
    return LoopConverter.convert(line)
  }
}
