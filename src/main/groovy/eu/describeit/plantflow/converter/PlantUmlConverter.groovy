package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static org.apache.commons.lang3.StringUtils.substringBetween

@CompileStatic
@Slf4j
final class PlantUmlConverter {
  StringBuffer pflowBuffer = new StringBuffer()
  ConversionContext context = new ConversionContext()

  static final List<String> linesToDrop = ['start', 'stop', 'end', 'detach']

  String convertToPlantFlowDsl(final String pumlText) {
    pumlText.eachLine { String line ->
      String tab = line.takeWhile { it == ' ' }
      String lineTrimmed = line.trim()
      String lineConverted = ''

      switch (lineTrimmed) {
        case ~/^@.*/       : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ~/^-.*->$/    : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case linesToDrop   : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ''            : log.trace('convertToPlantFlowDsl() - EMPTY line:{}', line); break
        case ~/^:.*;$/     : lineConverted = convertLineToActionMethod(lineTrimmed, context); break
        default            : lineConverted = convertExpression(lineTrimmed, context); break
      }

      if (lineConverted != null) {
        pflowBuffer.append(tab)
                   .append(lineConverted)
                   .append(System.lineSeparator())
      } else {
        throw new IllegalArgumentException('Unknown case for line:' + line)
      }
    }

      log.info('convertToPlantFlowDsl() - context json:{}', context)

    return pflowBuffer.toString()
  }

  private static String convertLineToActionMethod(String line, ConversionContext context) {
    log.info('convertLineToActionMethod() - line:"{}" context:{}', line, context)

    String actionName = substringBetween(line, ':', ';')
    String contextId = context.geCurrentId() ? context.geCurrentId() : null

    context.addAction(context.geCurrentId(), actionName)

    if (contextId) return "if (isActive(\"${actionName}\", \"${contextId}\")) return"
    else           return "if (isActive(\"${actionName}\")) return"
  }

  private static String convertExpression(String line, ConversionContext context) {
    String convertedLine = ConditionalConverter.convert(line, context)
    if (convertedLine == null) convertedLine = LoopConverter.convert(line, context)
    if (convertedLine == null) convertedLine = ForkConverter.convert(line, context)

    return convertedLine
  }
}
