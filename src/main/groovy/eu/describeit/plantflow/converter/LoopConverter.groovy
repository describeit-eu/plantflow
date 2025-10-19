package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static eu.describeit.plantflow.block.BlockType.LOOP
import static eu.describeit.plantflow.Utility.stringFormatLine

@CompileStatic
@Slf4j
enum LoopConverter {
  WHILE_IS     (~ /^while *\(.*\) *is *\(.*\)$/,                     'loop("%s") { while (eval("%s", "%s", "%s")) { // is', [0,3]),
  WHILE        (~ /^while *\(.*\)$/,                                 'loop("%s") { while (eval("%s", null, "%s")) {',       [0,2]),
  REPEAT       (~ /^repeat$/,                                        'loop("%s") { do {',                                   [0]),
  ENDWHILE     (~ /^endwhile *\(.*\)$/,                              '} } // %s %s',                                        [1]),
  ENDWHILE2    (~ /^endwhile$/,                                      '} } // %s',                                           [0]),
  REPEAT_WHILE (~ /^repeat while *\(.*\) *is *\(.*\) *not *\(.*\)$/, '} while (eval("%s", "%s", "%s")) } // not ("%s")',    [2]),

  final Pattern matcher
  final String expression
  final List<Integer> blockIdPositions

  private static final List blockStarts = [WHILE, WHILE_IS, REPEAT]
  private static final List blockEnds   = [ENDWHILE, ENDWHILE2, REPEAT_WHILE]

  LoopConverter(Pattern pattern, String expression, List<Integer> positions) {
    this.matcher = pattern
    this.expression = expression
    this.blockIdPositions = positions
  }

  static LoopConverter match(String line) {
    return values().find { LoopConverter lc -> (line ==~ lc.matcher) } as LoopConverter
  }

  static String convert(String line, ConversionContext context) {
    def converter = match(line)

    if (!converter) return null
    else            return converter.convertLine(line, context)
  }

  String convertLine(String line, ConversionContext context) {
    if (blockStarts.contains(this)) context.start(LOOP)
    def convertedLine = convertLine(line, context.geCurrentId())
    if (blockEnds.contains(this)) context.end(LOOP)

    return convertedLine
  }

  String convertLine(String line, String loopId) {
    return stringFormatLine(line, expression, loopId, blockIdPositions)
  }
}
