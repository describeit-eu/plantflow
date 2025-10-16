package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static eu.describeit.plantflow.block.BlockType.LOOP
import static eu.describeit.plantflow.Utility.stringFormatLine

@CompileStatic
@Slf4j
enum LoopConverter {
  WHILE_IS     (~ /^while *\(.*\) *is *\(.*\)$/                     , 'loop("%s") { while (eval("%s", "%s")) { // is'),
  WHILE        (~ /^while *\(.*\)$/                                 , 'loop("%s") { while (eval("%s")) {'),
  REPEAT       (~ /^repeat$/                                        , 'loop("%s") { do {'),
  ENDWHILE     (~ /^endwhile *\(.*\)$/                              , '} } // %s %s'),
  ENDWHILE2    (~ /^endwhile$/                                      , '} } // %s'),
  REPEAT_WHILE (~ /^repeat while *\(.*\) *is *\(.*\) *not *\(.*\)$/ , '} while (eval("%s", "%s")) } // not ("%s") %s'),

  final Pattern matcher
  final String expression

  private static final List blockStarts = [WHILE, WHILE_IS, REPEAT]
  private static final List blockEnds   = [ENDWHILE, ENDWHILE2, REPEAT_WHILE]

  LoopConverter(Pattern pattern, String expression) {
    this.matcher = pattern
    this.expression = expression
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
    boolean addFirst = blockStarts.contains(this)
    return stringFormatLine(line, expression, loopId, addFirst)
  }
}
