package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static eu.describeit.plantflow.converter.ConversionUtils.stringFormatLine

@CompileStatic
@Slf4j
enum ForkConverter {
  FORK       (~ /^fork$/       , 'fork("%s") {'),
  FORK_AGAIN (~ /^fork again$/ , '} forkAgain("%s") {'),
  END_MERGE  (~ /^end merge$/  , '}; if (endFork("%s")) return'),

  private static final List blockStarts = [FORK]
  private static final List blockEnds   = [END_MERGE]

  final Pattern matcher
  final String expression

  ForkConverter(Pattern pattern, String expression) {
    this.matcher = pattern
    this.expression = expression
  }

  static ForkConverter match(String line) {
    return values().find { ForkConverter lc -> (line ==~ lc.matcher) } as ForkConverter
  }

  static String convert(String line, ConversionContext context) {
    def converter = match(line)

    if (!converter) return null
    else            return converter.convertLine(line, context)
  }

  String convertLine(String line, ConversionContext context) {
    if (blockStarts.contains(this)) context.start(ConversionBlock.Type.FORK)
    def convertedLine = convertLine(line, context.getId() )
    if (blockEnds.contains(this)) context.end(ConversionBlock.Type.FORK)

    return convertedLine
  }

  String convertLine(String line, String forkId) {
    boolean addFirst = blockStarts.contains(this)
    return stringFormatLine(line, expression, forkId, addFirst)
  }
}
