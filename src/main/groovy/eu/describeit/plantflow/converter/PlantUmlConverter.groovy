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
  enum ConvertMode {CALCULATE, EXECUTE}

  static final List<String> linesToMethod = [
      'start', 'stop', 'end', 'fork', 'fork again', 'end merge', 'repeat', 'endswitch', 'endwhile', 'detach'
  ]

  static final Map<String, String> linesMap = [
      endif: '}'
  ]

  private static enum ExpressionCase {
    IF_THEN     (~ /^if\b.*\bthen\b.*$/      , 'if (eval("%s") == "%s" ) {'),
    REPEAT_WHilE(~ /^repeat\b.*\bwhile\b.*$/ , ["repeatWhile", 'is', 'not']),
    WHILE_IS    (~ /^while\b.*\bis\b.*$/     , ["whilee", 'is']),
    WHILE       (~ /^while.*$/               , ["whilee"]),
    ENDWHILE    (~ /^endwhile.*/             , ["endwhile"]),
    ELSE        (~ /^else.*/                 , '} else { //%s'),
    SWITCH      (~ /^switch.*/               , ["switchh"]),
    CASE        (~ /^case.*/                 , ["casee"])

    final Pattern matcher
    final Object expression

    ExpressionCase(Pattern pattern, Object expression) {
      this.matcher = pattern
      this.expression = expression
    }

    static ExpressionCase match(String line) {
      values().find { ExpressionCase ec -> (line ==~ ec.matcher) } as ExpressionCase
    }
  }

  static String getResourceText(String file) {
    return PlantUmlConverter.class.getClassLoader().getResource(file).text.trim()
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
        case linesToMethod : pflow.append(convertLineToMethod(tabSize, lineTrimmed)); break
        case linesMap*.key : pflow.append(tab(tabSize)).append(linesMap[line]); break
        case ~/^:.*;$/     : pflow.append(convertLineToActionMethod(tabSize, lineTrimmed)); break

        default :
          def exprCase = ExpressionCase.match(lineTrimmed)
          switch (exprCase.expression) {
            case String:
              pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, exprCase.expression as String))
              break
            case List:
              pflow.append(convertLineWithExpressions(tabSize, lineTrimmed, exprCase.expression as List<String>))
              break
            default:
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

    return tab(tabSize) + "if (action(\"${actionName}\")) { stop(); return }"
  }

  private static String convertLineWithExpressions(int tabSize, String line, String expression) {
    log.info('convertLineWithExpressions() - line:"{}", tabSize:{}, expression:{}', line, tabSize, expression)
    List<String> exprData = substringsBetween(line, '(', ')').collect { it.trim() }
    return tab(tabSize) + String.format(expression, exprData as String[])
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
