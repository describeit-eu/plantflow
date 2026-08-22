package eu.describeit.plantflow.calculate.converter

import eu.describeit.plantflow.Utility
import eu.describeit.plantflow.block.BlockType
import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

@CompileStatic
@Slf4j
enum CalculateNextLoopConverter {
  WHILE_IS     (~ /^while *\(.*\) *is *\(.*\)$/,                     'loop("$loopId") { while (eval("${exprData[0]}", "${exprData[1]}", "$loopId")) { loopBlock("$loopBlockId") { // is'),
  WHILE        (~ /^while *\(.*\)$/,                                 'loop("$loopId") { while (eval("${exprData[0]}", null, "$loopId")) { loopBlock("$loopBlockId") {'),
  REPEAT       (~ /^repeat$/,                                        'loop("$loopId") { do { loopBlock("$loopBlockId") {'),
  ENDWHILE     (~ /^endwhile *\(.*\)$/,                              '} } } // ${exprData[0]} $loopId'),
  ENDWHILE2    (~ /^endwhile$/,                                      '} } } // $loopId'),
  REPEAT_WHILE (~ /^repeat while *\(.*\) *is *\(.*\) *not *\(.*\)$/, '} } while (eval("${exprData[0]}", "${exprData[1]}", "$loopId")) } // not ("${exprData[2]}")'),

  final Pattern matcher
  final String expression

  CalculateNextLoopConverter(Pattern pattern, String expression) {
    this.matcher = pattern
    this.expression = expression
  }

  static CalculateNextLoopConverter match(String line) {
    return values().find { CalculateNextLoopConverter lc -> (line ==~ lc.matcher) } as CalculateNextLoopConverter
  }

  static String convert(String line, CalculateNextConversionContext context) {
    def converter = match(line)

    if (!converter) return null
    else            return converter.convertLine(line, context)
  }

  private String convertLine(String line, CalculateNextConversionContext context) {
    String loopId = null
    String loopBlockId = null

    switch (this) {
      case WHILE_IS:
      case WHILE:
      case REPEAT:
        context.start(BlockType.LOOP)
        loopId = context.geCurrentId()
        context.start(BlockType.LOOP_BLOCK)
        loopBlockId = context.geCurrentId()
        break

      case ENDWHILE:
      case ENDWHILE2:
        context.end(BlockType.LOOP_BLOCK)
        loopId = context.geCurrentId()
        context.end(BlockType.LOOP)
        break

      case REPEAT_WHILE:
        context.end(BlockType.LOOP_BLOCK)
        loopId = context.geCurrentId()
        context.end(BlockType.LOOP)
        break
    }

    return convertLine(line, loopId, loopBlockId)
  }

  private String convertLine(String line, String loopId, String loopBlockId) {
    List<String> exprData = Utility.extractBetweenBalancedParentheses(line)
    def binding = [loopId: loopId, loopBlockId: loopBlockId, exprData: exprData]

    def engine = new SimpleTemplateEngine()
    return engine.createTemplate(expression).make(binding).toString()
  }
}
