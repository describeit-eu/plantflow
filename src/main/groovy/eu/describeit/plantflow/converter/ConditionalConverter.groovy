package eu.describeit.plantflow.converter

import eu.describeit.plantflow.Utility
import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static eu.describeit.plantflow.block.BlockType.CONDITIONAL

@CompileStatic
@Slf4j
enum ConditionalConverter {
  IF_THEN       (~ /^if *\(.*\) *then *\(.*\)$/,             'conditional("$conditionalId") { if (eval("${exprData[0]}", null, "$conditionalId")) { // ${exprData[1]}'),
  IF_IS         (~ /^if *\(.*\) *is *\(.*\) *then$/,         'conditional("$conditionalId") { if (eval("${exprData[0]}", "${exprData[1]}", "$conditionalId")) { // is'),
  IF_EQUALS     (~ /^if *\(.*\) *equals *\(.*\) *then$/,     'conditional("$conditionalId") { if (eval("${exprData[0]}", "${exprData[1]}", "$conditionalId")) { // equals'),
  ELSEIF_THEN   (~ /^elseif *\(.*\) *then *\(.*\)$/,         'else if (eval("${exprData[0]}", null, "$conditionalId")) { // ${exprData[1]}'),
  ELSEIF_IS     (~ /^elseif *\(.*\) *is *\(.*\) *then$/,     'else if (eval("${exprData[0]}", "${exprData[1]}", "$conditionalId")) { // is'),
  ELSEIF_EQUALS (~ /^elseif *\(.*\) *equals *\(.*\) *then$/, 'else if (eval("${exprData[0]}", "${exprData[1]}", "$conditionalId")) { // equals'),
  ELSE          (~ /^else *\(.*\)$/,                         '} else { // ${exprData[0]} $conditionalId'),
  ENDIF         (~ /^endif$/,                                '} } // $conditionalId'),

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

  String convertLine(String line, String conditionalId) {
    List<String> exprData = Utility.extractBetweenBalancedParentheses(line)
    def binding = [conditionalId: conditionalId, exprData: exprData]

    def engine = new SimpleTemplateEngine()
    return engine.createTemplate(expression).make(binding).toString()
  }
}
