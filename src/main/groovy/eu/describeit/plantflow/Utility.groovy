package eu.describeit.plantflow


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
@Slf4j
class Utility {
  private Utility() {}

  // Regex pattern for balanced parentheses with up to 3 levels of nesting
  private static final Pattern balancedParenthesesPattern = ~/\(([^()]*(?:\([^()]*(?:\([^()]*\)[^()]*)*\)[^()]*)*)\)/

  static String getResourceText(String file) {
    return PlantFlow.class.getClassLoader().getResource(file).text.trim()
  }

  static List<String> extractBetweenBalancedParentheses(String line) {
    List<String> results = []
    Matcher matcher = balancedParenthesesPattern.matcher(line)

    while (matcher.find()) {
      results.add(matcher.group(1).trim())
    }

    return results
  }
}
