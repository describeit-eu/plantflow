package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
@Slf4j
enum ExpressionConverter {
  IF_THEN        (~ /^if *\(.*\) *then *\(.*\)$/         , 'if (eval("%s")) { // %s'),
  IF_IS_THEN     (~ /^if *\(.*\) *is *\(.*\) *then$/     , 'if (eval("%s") == "%s") { // is'),
  IF_EQUALS_THEN (~ /^if *\(.*\) *equals *\(.*\) *then$/ , 'if (eval("%s") == "%s") { // equals'),

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
    List<String> exprData = extractBetweenBalancedParentheses(line)
    log.info('convertExpressionString() - line:"{}", exprString:{}, exprData:{}', line, exprString, exprData)
    return String.format(exprString, exprData as String[])
  }

  static private String convertExpressionList(String line, List<String> exprPieces) {
    List<String> exprData = extractBetweenBalancedParentheses(line)
    log.info('convertExpressionList() - line:"{}" , exprPieces:{}, exprData:{}', line, exprPieces, exprData)
    assert exprPieces.size() == exprData.size()

    StringBuilder newLine = new StringBuilder()
    exprPieces.eachWithIndex { String expr, int idx ->
      newLine.append("$expr (\"${exprData[idx]}\") ")
    }
    return newLine.toString().trim()
  }

  static private List<String> extractBetweenBalancedParentheses(String line) {
    List<String> results = []
    if (line == null || line.isEmpty()) return results

    // Regex pattern for balanced parentheses with up to 3 levels of nesting
    Pattern pattern = ~/\(([^()]*(?:\([^()]*(?:\([^()]*\)[^()]*)*\)[^()]*)*)\)/

    Matcher matcher = pattern.matcher(line)
    while (matcher.find()) {
      results.add(matcher.group(1).trim())
    }
    
    return results
  }
}
