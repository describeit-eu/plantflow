package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static eu.describeit.plantflow.converter.ConversionUtils.stringFormatLine

@CompileStatic
@Slf4j
enum LoopConverter {
  WHILE_IS     (~ /^while *\(.*\) *is *\(.*\)$/                     , 'while (eval("%s", "%s")) { // is'),
  WHILE        (~ /^while *\(.*\)$/                                 , 'while (eval("%s")) {'),
  ENDWHILE     (~ /^endwhile *\(.*\)$/                              , '} // %s'),
  ENDWHILE2    (~ /^endwhile$/                                      , '}'),
  REPEAT_WHILE (~ /^repeat while *\(.*\) *is *\(.*\) *not *\(.*\)$/ , '} while (eval("%s", "%s")) // not ("%s")'),
  REPEAT       (~ /^repeat$/                                        , 'do {'),

  final Pattern matcher
  final String expression

  LoopConverter(Pattern pattern, String expression) {
    this.matcher = pattern
    this.expression = expression
  }

  static LoopConverter match(String line) {
    return values().find { LoopConverter lc -> (line ==~ lc.matcher) } as LoopConverter
  }

  static String convert(String line) {
    LoopConverter converter = match(line)
    if (!converter) throw new IllegalArgumentException('Unknown loop expression for line:' + line)

    return converter.convertLine(line)
  }

  String convertLine(String line) {
    return stringFormatLine(line, expression)
  }
}
