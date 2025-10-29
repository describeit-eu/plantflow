package eu.describeit.plantflow.calculate.converter

import eu.describeit.plantflow.block.BlockType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static org.apache.commons.lang3.StringUtils.substringBetween

@CompileStatic
@Slf4j
final class CalculateNextConverter {
  StringBuffer pflowBuffer = new StringBuffer()
  CalculateNextConversionContext context = new CalculateNextConversionContext()

  static final List<String> linesToDrop  = ['detach']
  static final List<String> illegalLines = ['stop']

  String convertToPlantFlowDsl(final String pumlText) {
    String extraIndent = ''

    pumlText.eachLine { String line ->
      String tab = line.takeWhile { it == ' ' } + (line != 'end' ? extraIndent : '')
      String lineTrimmed = line.trim()
      String lineConverted = ''

      switch (lineTrimmed) {
        case ~/^@.*/      : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ~/^-.*->$/   : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case linesToDrop  : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ''           : log.trace('convertToPlantFlowDsl() - EMPTY line:{}', line); break
        case 'start'      : context.start(BlockType.ROOT_BLOCK); lineConverted = "rootBlock(\"${context.geCurrentId()}\") {"; extraIndent = '  '; break
        case 'end'        : context.end(BlockType.ROOT_BLOCK); lineConverted = '}'; break
        case ~/^:.*;$/    : lineConverted = convertLineToActionMethod(lineTrimmed, context); break
        case illegalLines : throw new IllegalArgumentException('Cannot handle puml line:' + line)
        default           : lineConverted = convertExpression(lineTrimmed, context); break
      }

      if (lineConverted != null) {
        pflowBuffer.append(tab)
                   .append(lineConverted)
                   .append(System.lineSeparator())
      } else {
        throw new IllegalArgumentException('Unknown case for line:' + line)
      }

      log.info('convertToPlantFlowDsl() - lineConverted:{}', lineConverted)
    }

    log.info('convertToPlantFlowDsl() - context:{}', context)

    return pflowBuffer.toString()
  }

  private static String convertLineToActionMethod(String line, CalculateNextConversionContext context) {
    String actionName = substringBetween(line, ':', ';')
    String contextId = context.geCurrentId()

    log.debug('convertLineToActionMethod() - line:"{}", contextId:{}, actionName:{}', line, contextId, actionName)

    context.addAction(contextId, actionName)

    if (contextId) return "isActive(\"${actionName}\", \"${contextId}\")"
    else           return "isActive(\"${actionName}\")"
  }

  private static String convertExpression(String line, CalculateNextConversionContext context) {
    String convertedLine = CalculateNextConditionalConverter.convert(line, context)
    if (convertedLine == null) convertedLine = CalculateNextLoopConverter.convert(line, context)
    if (convertedLine == null) convertedLine = CalculateNextForkConverter.convert(line, context)

    return convertedLine
  }
}
