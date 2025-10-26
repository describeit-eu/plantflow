package eu.describeit.plantflow.calculate.converter

import eu.describeit.plantflow.block.BlockType
import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

@CompileStatic
@Slf4j
enum CalculateNextForkConverter {
  FORK       (~ /^fork$/       , 'fork("$forkId") { forkBlock("$branchId") {'),
  FORK_AGAIN (~ /^fork again$/ , '} forkBlock("$branchId") {'),
  END_MERGE  (~ /^end merge$/  , '} } // $forkId'),

  final Pattern matcher
  final String expression

  CalculateNextForkConverter(Pattern pattern, String expression) {
    this.matcher = pattern
    this.expression = expression
  }

  static CalculateNextForkConverter match(String line) {
    return values().find { CalculateNextForkConverter lc -> (line ==~ lc.matcher) } as CalculateNextForkConverter
  }

  static String convert(String line, CalculateNextConversionContext context) {
    def converter = match(line)

    if (!converter) return null
    else            return converter.convertLine(context)
  }

  String convertLine(CalculateNextConversionContext context) {
    String forkId = null
    String branchId = null

    switch (this) {
      case FORK:
        context.start(BlockType.FORK)
        forkId = context.geCurrentId()
        context.start(BlockType.FORK_BLOCK)
        branchId = context.geCurrentId()
        break

      case FORK_AGAIN:
        context.end(BlockType.FORK_BLOCK)
        context.start(BlockType.FORK_BLOCK)
        branchId = context.geCurrentId()
        break

      case END_MERGE:
        context.end(BlockType.FORK_BLOCK)
        forkId = context.geCurrentId()
        context.end(BlockType.FORK)
        break
    }

    return convertExpression(forkId, branchId)
  }

  String convertExpression(String forkId, String branchId) {
    def binding = [forkId: forkId, branchId: branchId]
    def engine = new SimpleTemplateEngine()

    return engine.createTemplate(expression).make(binding).toString()
  }
}
