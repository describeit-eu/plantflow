package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils

import java.util.regex.Pattern

@CompileStatic
@Slf4j
enum ExpressionConverter {
  IF_THEN     (~ /^if\b.*\bthen\b.*$/      , 'if (eval("%s")) { // %s'),
  REPEAT_WHilE(~ /^repeat\b.*\bwhile\b.*$/ , ["repeatWhile", 'is', 'not']),
  WHILE_IS    (~ /^while\b.*\bis\b.*$/     , ["whilee", 'is']),
  WHILE       (~ /^while.*$/               , ["whilee"]),
  ENDWHILE    (~ /^endwhile.*/             , ["endwhile"]),
  ELSE        (~ /^else.*/                 , '} else { // %s'),
  SWITCH      (~ /^switch.*/               , ["switchh"]),
  CASE        (~ /^case.*/                 , ["casee"])

  final Pattern matcher
  final Object expression

  ExpressionConverter(Pattern pattern, Object expression) {
    this.matcher = pattern
    this.expression = expression
  }

  static String convert(String line) {
    ExpressionConverter converter = match(line)
    if (!converter) throw new IllegalArgumentException('Unknown expression for line:' + line)

    return converter.convertLine(line)
  }

  static ExpressionConverter match(String line) {
    return values().find { ExpressionConverter ec -> (line ==~ ec.matcher) } as ExpressionConverter
  }

  String convertLine(String line) {
    switch (expression) {
      case String:
        return convertExpressionString(line, expression as String)
      case List:
        return convertExpressionList(line, expression as List<String>)
      default:
        throw new IllegalArgumentException('Uncovered case for line:' + line)
    }
  }

  static private String convertExpressionString(String line, String exprString) {
    log.info('convertExpressionString() - line:"{}", exprString:{}', line, exprString)
    List<String> exprData = StringUtils.substringsBetween(line, '(', ')').collect { it.trim() }
    return String.format(exprString, exprData as String[])
  }

  static private String convertExpressionList(String line, List<String> exprPieces) {
    log.info('convertExpressionList() - line:"{}" , exprPieces:{}', line, exprPieces)

    List<String> exprData = StringUtils.substringsBetween(line, '(', ')').collect { it.trim() }
    assert exprPieces.size() == exprData.size()

    StringBuilder newLine = new StringBuilder()
    exprPieces.eachWithIndex { String expr, int idx ->
      newLine.append("$expr (\"${exprData[idx]}\") ")
    }
    return newLine.toString().trim()
  }
}
