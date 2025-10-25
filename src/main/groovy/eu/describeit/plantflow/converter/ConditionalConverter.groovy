package eu.describeit.plantflow.converter

import eu.describeit.plantflow.Utility
import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static eu.describeit.plantflow.block.BlockType.*

@CompileStatic
@Slf4j
enum ConditionalConverter {
  IF_THEN       (~ /^if *\(.*\) *then *\(.*\)$/,             'conditional("$conditionalId") { if (eval("${exprData[0]}", null, "$conditionalId")) { ifBlock("$branchId") { // ${exprData[1]}'),
  IF_IS         (~ /^if *\(.*\) *is *\(.*\) *then$/,         'conditional("$conditionalId") { if (eval("${exprData[0]}", "${exprData[1]}", "$conditionalId")) { ifBlock("$branchId") { // is'),
  IF_EQUALS     (~ /^if *\(.*\) *equals *\(.*\) *then$/,     'conditional("$conditionalId") { if (eval("${exprData[0]}", "${exprData[1]}", "$conditionalId")) { ifBlock("$branchId") { // equals'),
  ELSEIF_THEN   (~ /^elseif *\(.*\) *then *\(.*\)$/,         '} } else if (eval("${exprData[0]}", null, "$conditionalId")) { elseIfBlock("$branchId") { // ${exprData[1]}'),
  ELSEIF_IS     (~ /^elseif *\(.*\) *is *\(.*\) *then$/,     '} } else if (eval("${exprData[0]}", "${exprData[1]}", "$conditionalId")) { elseIfBlock("$branchId") { // is'),
  ELSEIF_EQUALS (~ /^elseif *\(.*\) *equals *\(.*\) *then$/, '} } else if (eval("${exprData[0]}", "${exprData[1]}", "$conditionalId")) { elseIfBlock("$branchId") { // equals'),
  ELSE          (~ /^else *\(.*\)$/,                         '} } else { elseBlock("$branchId") { // ${exprData[0]} $conditionalId'),
  ENDIF         (~ /^endif$/,                                '} } } // $conditionalId'),

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
    String conditionalId = null
    String branchId = null

    switch (this) {
      case IF_THEN:
      case IF_IS:
      case IF_EQUALS:
        context.start(CONDITIONAL)
        conditionalId = context.geCurrentId()
        context.start(IF_BLOCK)
        branchId = context.geCurrentId()
        break

      case ELSEIF_THEN:
      case ELSEIF_IS:
      case ELSEIF_EQUALS:
        context.end(IF_BLOCK)
        conditionalId = context.geCurrentId()
        context.start(ELSEIF_BLOCK)
        branchId = context.geCurrentId()
        break

      case ELSE:
        context.end(IF_BLOCK)
        conditionalId = context.geCurrentId()
        context.start(ELSE_BLOCK)
        branchId = context.geCurrentId()
        break

      case ENDIF:
        if (context.geCurrent()?.type == IF_BLOCK) context.end(IF_BLOCK)
        else if (context.geCurrent()?.type == ELSE_BLOCK) context.end(ELSE_BLOCK)
        conditionalId = context.geCurrentId()
        context.end(CONDITIONAL)
        break
    }

    return convertLine(line, conditionalId, branchId)
  }

  String convertLine(String line, String conditionalId, String branchId) {
    List<String> exprData = Utility.extractBetweenBalancedParentheses(line)
    def binding = [conditionalId: conditionalId, branchId: branchId, exprData: exprData]

    def engine = new SimpleTemplateEngine()
    return engine.createTemplate(expression).make(binding).toString()
  }
}
