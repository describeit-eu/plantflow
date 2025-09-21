package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static org.apache.commons.lang3.StringUtils.substringBetween
import static org.apache.commons.lang3.StringUtils.substringsBetween
import static org.apache.commons.text.CaseUtils.toCamelCase


@CompileStatic
@Slf4j
final class PlantUmlConverter {
  static def linesToMethod = ['start', 'stop', 'end', 'fork', 'fork again', 'end merge', 'endif', 'repeat', 'endswitch']

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
        case ~/^else.*/    : pflow.append(convertLineWithExpression(tabSize, lineTrimmed, "elsee")); break
        case ~/^repeat.*/  : pflow.append(convertLineRepeatWhile(tabSize, lineTrimmed)); break
        case ~/^switch.*/  : pflow.append(convertLineWithExpression(tabSize, lineTrimmed, "switchh")); break
        case ~/^case.*/    : pflow.append(convertLineWithExpression(tabSize, lineTrimmed, "casee")); break
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

  private static String convertLineRepeatWhile(int tabSize, String line) {
    log.info('convertLineRepeatWhile() - line:"{}" , tabSize:{}', line, tabSize)

    def exprData = substringsBetween(line, '(', ')').collect {it.trim()}
    String newLine = "repeatWhile(\"${exprData[0]}\") is(\"${exprData[1]}\") not(\"${exprData[2]}\")"

    return tab(tabSize) + newLine
  }

  private static String convertLineWithExpression(int tabSize, String line, String methodName) {
    log.info('convertLineWithExpression() - line:"{}" , tabSize:{}, methodName:{}', line, tabSize, methodName)

    def exprValue = substringBetween(line, '(', ')').trim()
    String newLine = "${methodName} (\"${exprValue}\")"

    return tab(tabSize) + newLine
  }
}
