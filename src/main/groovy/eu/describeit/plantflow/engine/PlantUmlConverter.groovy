package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static org.apache.commons.lang3.StringUtils.substringBetween
import static org.apache.commons.lang3.StringUtils.substringsBetween
import static org.apache.commons.text.CaseUtils.toCamelCase


@CompileStatic
@Slf4j
final class PlantUmlConverter {
  static def linesToMethod = ['start', 'stop', 'end', 'fork', 'fork again', 'end merge', 'endif', 'repeat']

  static String getResourceText(String file) {
    return PlantUmlConverter.class.getClassLoader().getResource(file).text.trim()
  }

  static String convertToPlantFlowDsl(final String pumlText) {
    def pflow = new StringBuffer();

    pumlText.eachLine { String line ->
      int tabSize = line.takeWhile { it == ' ' }.size()
      String lineTrimmed = line.trim()

      switch (lineTrimmed) {
        case ~/^@.*/       : log.info('convertToPlantFlowDsl() - DROPPING line:{}', lineTrimmed); break
        case ''            : pflow.append(tab(tabSize)); break
        case linesToMethod : pflow.append(convertLineToMethod(tabSize, lineTrimmed)); break
        case ~/^:.*;$/     : pflow.append(convertLineToActionMethod(tabSize, lineTrimmed)); break
        case ~/^if.*/      : pflow.append(convertLineIfThen(tabSize, lineTrimmed)); break
        case ~/^else.*/    : pflow.append(convertLineElse(tabSize, lineTrimmed)); break
        default :
          // throw error
          log.error('convertToPlantFlowDsl() - ???? line:{}', lineTrimmed)
          break
      }
      pflow.append('\n')
    }

    return pflow.toString()
  }

  private static String tab(int size) {
    return ' '.repeat(size)
  }

  private static String convertLineToMethod(int tabSize, String line) {
    log.info('convertLineToMethod() - line:"{}" , tabSize:{}', line, tabSize)
    return tab(tabSize) + toCamelCase(line, false, ' ' as char) + "()"
  }

  private static String convertLineToActionMethod(int tabSize, String line) {
    log.info('convertLineToActionMethod() - line:"{}" , tabSize:{}', line, tabSize)
    String actionName = substringBetween(line, ':', ';')
    return tab(tabSize) + 'action("' + actionName + '")'
  }

  private static String convertLineIfThen(int tabSize, String line) {
    log.info('convertLineIfThen() - line:"{}" , tabSize:{}', line, tabSize)

    def exprData = substringsBetween(line, '(', ')').collect {it.trim()}
    String newLine = "iff (\"${exprData[0]}\") then (\"${exprData[1]}\")"

    return tab(tabSize) + newLine
  }

  private static String convertLineElse(int tabSize, String line) {
    log.info('convertLineElse() - line:"{}" , tabSize:{}', line, tabSize)

    def exprValue = substringBetween(line, '(', ')').trim()
    String newLine = "elsee (\"${exprValue}\")"

    return tab(tabSize) + newLine
  }
}
