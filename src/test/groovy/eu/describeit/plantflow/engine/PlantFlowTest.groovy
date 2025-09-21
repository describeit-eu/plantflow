package eu.describeit.plantflow.engine

import groovy.transform.CompileStatic
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

@CompileStatic
class PlantFlowTest {
  static Stream<String> provideTestFileNames() {
    return Stream.of(
        'forkEndMerge',
        'ifThenElseEndif',
        'repeatWhile',
        'sequence',
        'switchCaseEndswitch',
        'whileEndwhile',
        'whileInfinite'
    )
  }

  @ParameterizedTest
  @MethodSource('provideTestFileNames')
  void dryRun(String fileName) {
    new PlantFlow("${fileName}.pflow", null).dryRun()
  }
}
