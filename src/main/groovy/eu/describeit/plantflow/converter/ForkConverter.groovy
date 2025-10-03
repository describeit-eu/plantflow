package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static eu.describeit.plantflow.converter.ConversionUtils.stringFormatLine

@CompileStatic
@Slf4j
enum ForkConverter {
  FORK       (~ /^fork$/       , 'fork {'),
  FORK_AGAIN (~ /^fork again$/ , '} forkAgain {'),
  END_MERGE  (~ /^end merge$/  , '}; if (endFork()) return'),

  final Pattern matcher
  final String expression

  ForkConverter(Pattern pattern, String expression) {
    this.matcher = pattern
    this.expression = expression
  }

  static ForkConverter match(String line) {
    return values().find { ForkConverter lc -> (line ==~ lc.matcher) } as ForkConverter
  }

  static String convert(String line) {
    ForkConverter converter = match(line)
    if (!converter) throw new IllegalArgumentException('Unknown expression for line:' + line)

    return converter.convertLine(line)
  }

  String convertLine(String line) {
    return stringFormatLine(line, expression)
  }
}
