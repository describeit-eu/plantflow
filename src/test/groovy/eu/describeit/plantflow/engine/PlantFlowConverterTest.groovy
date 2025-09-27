package eu.describeit.plantflow.engine

import eu.describeit.plantflow.converter.PlantUmlConverter
import groovy.transform.CompileStatic
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import static eu.describeit.plantflow.converter.PlantUmlConverter.getResourceText

@CompileStatic
class PlantFlowConverterTest {

  static Stream<String> provideTestFileNames() {
    return Stream.of(
        'sequence',
        'ifThenElseEndif',
/*
        'forkEndMerge',
        'repeatWhile',
        'switchCaseEndswitch',
        'whileEndwhile',
        'whileInfinite'
*/
    )
  }

  @ParameterizedTest
  @MethodSource('provideTestFileNames')
  void 'convert puml for calculation'(String fileName) {
    def puml = getResourceText("${fileName}.puml")
    def expectedPflow = getResourceText("${fileName}.pflow")

    def resultPflow = PlantUmlConverter.convertToPlantFlowDsl(puml)

    assert resultPflow.contains(expectedPflow)
  }
}
