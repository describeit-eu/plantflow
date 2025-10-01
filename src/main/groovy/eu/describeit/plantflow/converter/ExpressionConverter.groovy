package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
@Slf4j
enum ExpressionConverter {
  IF_THEN       (~ /^if *\(.*\) *then *\(.*\)$/                     , 'if (evaluate("%s")) { // %s'),
  IF_IS         (~ /^if *\(.*\) *is *\(.*\) *then$/                 , 'if (evaluate("%s", "%s")) { // is'),
  IF_EQUALS     (~ /^if *\(.*\) *equals *\(.*\) *then$/             , 'if (evaluate("%s", "%s")) { // equals'),
  ELSEIF_THEN   (~ /^elseif *\(.*\) *then *\(.*\)$/                 , 'else if (evaluate("%s")) { // %s'),
  ELSEIF_IS     (~ /^elseif *\(.*\) *is *\(.*\) *then$/             , 'else if (evaluate("%s", "%s")) { // is'),
  ELSEIF_EQUALS (~ /^elseif *\(.*\) *equals *\(.*\) *then$/         , 'else if (evaluate("%s", "%s")) { // equals'),
  ELSE          (~ /^else *\(.*\)$/                                 , '} else { // %s'),
  WHILE_IS      (~ /^while *\(.*\) *is *\(.*\)$/                    , 'while (evaluate("%s", "%s")) { // is'),
  WHILE         (~ /^while *\(.*\)$/                                , 'while (evaluate("%s")) {'),
  ENDWHILE      (~ /^endwhile *\(.*\)$/                             , '} // %s'),
  REPEAT_WHILE  (~ /^repeat while *\(.*\) *is *\(.*\) *not *\(.*\)$/, '} while (evaluate("%s", "%s")) // not ("%s")'),

  // Regex pattern for balanced parentheses with up to 3 levels of nesting
  private static final Pattern balancedParenthesesPattern = ~/\(([^()]*(?:\([^()]*(?:\([^()]*\)[^()]*)*\)[^()]*)*)\)/

  final Pattern matcher
  final String expression

  ExpressionConverter(Pattern pattern, String expression) {
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
    List<String> exprData = extractBetweenBalancedParentheses(line)
    log.info('convertExpressionString() - line:"{}", exprString:{}, exprData:{}', line, expression, exprData)
    return String.format(expression, exprData as String[])
  }

  static private List<String> extractBetweenBalancedParentheses(String line) {
    assert line

    List<String> results = []
    Matcher matcher = balancedParenthesesPattern.matcher(line)
    while (matcher.find()) {
      results.add(matcher.group(1).trim())
    }
    
    return results
  }
}
