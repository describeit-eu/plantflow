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
        case ~/^if.*/      : pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, "iff", 'then')); break
        case ~/^repeat.*/  : pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, "repeatWhile", 'is', 'not')); break
        case ~/^while.*/   : pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, "whilee", 'is')); break
        case ~/^endwhile.*/: pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, "endwhile")); break
        case ~/^else.*/    : pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, "elsee")); break
        case ~/^switch.*/  : pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, "switchh")); break
        case ~/^case.*/    : pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, "casee")); break
        default :
          // throw error
          log.error('convertToPlantFlowDsl() - ???? line:{}', lineTrimmed)
          throw new IllegalArgumentException()
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

  private static String convertLineWithExpressions(int tabSize, String line, String... exprPieces) {
    log.info('convertLineWithExpressions() - line:"{}" , tabSize:{} , exprPieces:{}', line, tabSize, exprPieces)

    List<String> exprData = substringsBetween(line, '(', ')').collect { it.trim() }

    assert exprPieces
    assert exprPieces.length == exprData.size()

    StringBuilder newLine = new StringBuilder()

    exprPieces.eachWithIndex { String expr, int idx ->
      newLine.append("$expr (\"${exprData[idx]}\") ")
    }

    return tab(tabSize) + newLine.toString().trim()
  }
}
