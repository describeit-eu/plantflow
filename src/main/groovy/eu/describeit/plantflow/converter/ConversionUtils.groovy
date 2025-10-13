package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
@Slf4j
class ConversionUtils {
  private ConversionUtils() {}

  // Regex pattern for balanced parentheses with up to 3 levels of nesting
  private static final Pattern balancedParenthesesPattern = ~/\(([^()]*(?:\([^()]*(?:\([^()]*\)[^()]*)*\)[^()]*)*)\)/

  static String stringFormatLine(String line, String expression, String contextId, boolean first) {
    assert line && expression && contextId

    log.debug('stringFormatLine() - line:"{}", expression:{}, contextId:{}', line, expression, contextId)

    List<String> exprData = extractBetweenBalancedParentheses(line)

    if (contextId) {
      if (first) exprData.addFirst(contextId)
      else       exprData.addLast(contextId)
    }
    return String.format(expression, exprData as String[])
  }

  static String getResourceText(String file) {
    return PlantUmlConverter.class.getClassLoader().getResource(file).text.trim()
  }

  private static List<String> extractBetweenBalancedParentheses(String line) {
    List<String> results = []
    Matcher matcher = balancedParenthesesPattern.matcher(line)

    while (matcher.find()) {
      results.add(matcher.group(1).trim())
    }

    return results
  }
}
