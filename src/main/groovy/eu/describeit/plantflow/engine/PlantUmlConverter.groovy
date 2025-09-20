package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils


@CompileStatic
@Slf4j
final class PlantUmlConverter {
  static def linesToMethod = ['start', 'stop', 'end', 'fork', 'forkAgain', 'endMerge']

  static String getResourceText(String file) {
    return PlantUmlConverter.class.getClassLoader().getResource(file).text.trim()
  }

  static String convertToPlantFlowDsl(final String pumlText) {
    def pflow = new StringBuffer();

    pumlText.eachLine { String line ->
      switch (line) {
        case ~/^@.*/       : log.info('convertToPlantFlowDsl() - DROPPING line:{}', line); break
        case linesToMethod : pflow.append(convertLineToMethod(line)); break
        case ~/.*:.*;$/    : pflow.append(convertLineToActionMethod(line)); break
        case 'fork again'  : pflow.append(convertLineToForkAgainMethod(line)); break
        default            : log.error('convertToPlantFlowDsl() - ???? line:{}', line); break
      }
    }

    return pflow.toString()
  }

  private static String convertLineToMethod(String line) {
    log.info('convertLineToMethod() - line:{}', line)
    return "${line}()\n"
  }

  private static String convertLineToActionMethod(String line) {
    log.info('convertLineToActionMethod() - line:{}', line)
    return 'action("' + StringUtils.substringBetween(line, ':', ';') + '")\n'
  }

  private static convertLineToForkAgainMethod(String line) {
    log.info('convertLineToForkAgainMethod() - line:{}', line)
    return 'forkAgain()'
  }
}
