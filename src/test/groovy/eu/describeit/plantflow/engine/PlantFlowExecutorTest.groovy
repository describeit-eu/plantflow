package eu.describeit.plantflow.engine

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

class PlantFlowExecutorTest {
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
  void executePflow(String fileName) {

    new PlantFlowExecutor().execute("${fileName}.pflow")
  }
}
