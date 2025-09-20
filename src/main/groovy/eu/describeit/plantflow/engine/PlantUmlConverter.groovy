package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static org.apache.commons.lang3.StringUtils.substringBetween
import static org.apache.commons.text.CaseUtils.toCamelCase


@CompileStatic
@Slf4j
final class PlantUmlConverter {
  static def linesToMethod = ['start', 'stop', 'end', 'fork', 'fork again', 'end merge', 'endif']

  static String getResourceText(String file) {
    return PlantUmlConverter.class.getClassLoader().getResource(file).text.trim()
  }

  static String convertToPlantFlowDsl(final String pumlText) {
    def pflow = new StringBuffer();

    pumlText.eachLine { String line ->
      int tabSize = line.takeWhile { it == ' ' }.size()

      switch (line.substring(tabSize)) {
        case ~/^@.*/       : log.info('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ''            : pflow.append(tab(tabSize)).append('\n'); break
        case linesToMethod : pflow.append(convertLineToMethod(tabSize, line)); break
        case ~/^ *:.*;$/   : pflow.append(convertLineToActionMethod(tabSize, line)); break
        default            : log.error('convertToPlantFlowDsl() - ???? line:{}', line); break
      }
    }

    return pflow.toString()
  }

  private static String convertLineToMethod(int tabSize, String line) {
    log.info('convertLineToMethod() - line:{} tabSize:{}', line, tabSize)
    return tab(tabSize) + toCamelCase(line, false, ' ' as char) + "()\n"
  }

  private static String convertLineToActionMethod(int tabSize, String line) {
    log.info('convertLineToActionMethod() - line:{} tabSize:{}', line, tabSize)
    return tab(tabSize) + 'action("' + substringBetween(line, ':', ';') + '")\n'
  }

  private static String tab(int size) {
    return ' '.repeat(size)
  }
}
