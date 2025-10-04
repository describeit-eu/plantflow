package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static org.apache.commons.lang3.StringUtils.substringBetween

@CompileStatic
@Slf4j
final class PlantUmlConverter {
  static final List<String> linesToDrop = ['start', 'stop', 'end', 'detach']

  static String getResourceText(String file) {
    return PlantUmlConverter.class.getClassLoader().getResource(file).text.trim()
  }

  static String convertToPlantFlowDsl(final String pumlText) {
    def pflowBuffer = new StringBuffer()
    ConversionContext context = new ConversionContext()

    pumlText.eachLine { String line ->
      int tabSize = line.takeWhile { it == ' ' }.size()
      String lineTrimmed = line.trim()
      String lineConverted = null

      switch (lineTrimmed) {
        case ~/^@.*/       : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ~/^-.*->$/    : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case linesToDrop   : log.debug('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case ''            : lineConverted = ''; break
        case ~/^:.*;$/     : lineConverted = convertLineToActionMethod(lineTrimmed, context); break
        default            : lineConverted = convertExpression(lineTrimmed, context); break
      }

      if (lineConverted != null) {
        pflowBuffer.append(' '.repeat(tabSize))
                   .append(lineConverted)
                   .append(System.lineSeparator())
      }
    }

    return pflowBuffer.toString()
  }

  private static String convertLineToActionMethod(String line, ConversionContext context) {
    log.info('convertLineToActionMethod() - line:"{}"', line)

    String actionName = '"'+substringBetween(line, ':', ';')+'"'
    String contextId = context.getId() ? '"'+context.getId()+'"' : null

    if (contextId) return "if (isActive(${actionName}, ${contextId})) return"
    else           return "if (isActive(${actionName})) return"
  }

  private static String convertExpression(String line, ConversionContext context) {
    String convertedLine = ConditionalConverter.convert(line)
    if (convertedLine == null) convertedLine = LoopConverter.convert(line, context)
    if (convertedLine == null) convertedLine = ForkConverter.convert(line, context)

    if (convertedLine == null) throw new IllegalArgumentException('Unknown expression for line:' + line)

    return convertedLine
  }
}
