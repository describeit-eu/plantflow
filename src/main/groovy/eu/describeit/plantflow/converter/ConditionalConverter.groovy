package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static eu.describeit.plantflow.block.BlockType.CONDITIONAL
import static eu.describeit.plantflow.Utility.stringFormatLine

@CompileStatic
@Slf4j
enum ConditionalConverter {
  IF_THEN       (~ /^if *\(.*\) *then *\(.*\)$/             , 'conditional("%s") { if (eval("%s")) { // %s'),
  IF_IS         (~ /^if *\(.*\) *is *\(.*\) *then$/         , 'conditional("%s") { if (eval("%s", "%s")) { // is'),
  IF_EQUALS     (~ /^if *\(.*\) *equals *\(.*\) *then$/     , 'conditional("%s") { if (eval("%s", "%s")) { // equals'),
  ELSEIF_THEN   (~ /^elseif *\(.*\) *then *\(.*\)$/         , 'else if (eval("%s")) { // %s %s'),
  ELSEIF_IS     (~ /^elseif *\(.*\) *is *\(.*\) *then$/     , 'else if (eval("%s", "%s")) { // is %s'),
  ELSEIF_EQUALS (~ /^elseif *\(.*\) *equals *\(.*\) *then$/ , 'else if (eval("%s", "%s")) { // equals %s'),
  ELSE          (~ /^else *\(.*\)$/                         , '} else { // %s %s'),
  ENDIF         (~ /^endif$/                                , '} } // %s'),

  final Pattern matcher
  final String expression

  private static final List blockStarts = [IF_THEN, IF_IS, IF_EQUALS]
  private static final List blockEnds   = [ENDIF]

  ConditionalConverter(Pattern pattern, String expression) {
    this.matcher = pattern
    this.expression = expression
  }

  static ConditionalConverter match(String line) {
    return values().find { ConditionalConverter ec -> (line ==~ ec.matcher) } as ConditionalConverter
  }

  static String convert(String line, ConversionContext context) {
    def converter = match(line)

    if (!converter) return null
    else            return converter.convertLine(line, context)
  }

  String convertLine(String line, ConversionContext context) {
    if (blockStarts.contains(this)) context.start(CONDITIONAL)
    def convertedLine = convertLine(line, context.geCurrentId() )
    if (blockEnds.contains(this)) context.end(CONDITIONAL)

    return convertedLine
  }

  String convertLine(String line, String conditionId) {
    boolean addFirst = blockStarts.contains(this)
    return stringFormatLine(line, expression, conditionId, addFirst)
  }
}
