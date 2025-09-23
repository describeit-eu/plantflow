package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static org.apache.commons.lang3.StringUtils.substringBetween
import static org.apache.commons.lang3.StringUtils.substringsBetween
import static org.apache.commons.text.CaseUtils.toCamelCase

@CompileStatic
@Slf4j
final class PlantUmlConverter {

  static def linesToMethod = ['start', 'stop', 'end', 'fork', 'fork again', 'end merge',
                              'endif', 'repeat', 'endswitch', 'endwhile', 'detach']

  private static enum ExpressionCase {
    IF          (~ /^if\b.*\bthen\b.*$/      , ["iff", 'then']),
    REPEAT_WHilE(~ /^repeat\b.*\bwhile\b.*$/ , ["repeatWhile", 'is', 'not']),
    WHILE_IS    (~ /^while\b.*\bis\b.*$/     , ["whilee", 'is']),
    WHILE       (~ /^while.*$/               , ["whilee"]),
    ENDWHILE    (~ /^endwhile.*/             , ["endwhile"]),
    ELSE        (~ /^else.*/                 , ["elsee"]),
    SWITCH      (~ /^switch.*/               , ["switchh"]),
    CASE        (~ /^case.*/                 , ["casee"])

    final Pattern matcher
    final List<String> exprPieces

    ExpressionCase(Pattern pattern, List<String> exprPieces) {
      this.matcher = pattern
      this.exprPieces = exprPieces
    }

    static ExpressionCase match(String line) {
      values().find { ExpressionCase ec -> (line ==~ ec.matcher) } as ExpressionCase
    }
  }

  static String getResourceText(String file) {
    return PlantUmlConverter.class.getClassLoader().getResource(file).text.trim()
  }

  static String convertToPlantFlowDsl(final String pumlText) {
    def pflow = new StringBuffer();

    pumlText.eachLine { String line ->
      int tabSize = line.takeWhile { it == ' ' }.size()
      String lineTrimmed = line.trim()

      switch (lineTrimmed) {
        case ~/^@.*/       : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ~/^-.*>$/     : log.info('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ''            : pflow.append(tab(tabSize)); break
        case linesToMethod : pflow.append(convertLineToMethod(tabSize, lineTrimmed)); break
        case ~/^:.*;$/     : pflow.append(convertLineToActionMethod(tabSize, lineTrimmed)); break

        default :
          def exprCase = ExpressionCase.match(lineTrimmed)
          if (exprCase) {
            pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, exprCase.exprPieces))
          } else {
            throw new IllegalArgumentException('Uncovered case for line:' + lineTrimmed )
          }
          break
      }
      pflow.append(System.lineSeparator())
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

  private static String convertLineWithExpressions(int tabSize, String line, List<String> exprPieces) {
    log.info('convertLineWithExpressions() - line:"{}" , tabSize:{} , exprPieces:{}', line, tabSize, exprPieces)

    List<String> exprData = substringsBetween(line, '(', ')').collect { it.trim() }
    assert exprPieces.size() == exprData.size()

    StringBuilder newLine = new StringBuilder()
    exprPieces.eachWithIndex { String expr, int idx ->
      newLine.append("$expr (\"${exprData[idx]}\") ")
    }

    return tab(tabSize) + newLine.toString().trim()
  }
}
