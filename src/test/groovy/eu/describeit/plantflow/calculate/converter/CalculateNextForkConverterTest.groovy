package eu.describeit.plantflow.calculate.converter

import spock.lang.Specification
import spock.lang.Unroll

import static CalculateNextForkConverter.FORK
import static CalculateNextForkConverter.FORK_AGAIN
import static CalculateNextForkConverter.END_MERGE

class CalculateNextForkConverterTest extends Specification {

  @Unroll
  def "match() returns correct enum for '#line'"() {
    expect:
    CalculateNextForkConverter.match(line) == expected

    where:
    line         || expected
    'fork'       || FORK
    'fork again' || FORK_AGAIN
    'end merge'  || END_MERGE
  }

  @Unroll
  def "convertLine() produces expected output for '#expression'"() {
    expect:
    expression.convertExpression('FORK0', 'FORK_BLOCK1') == expected

    where:
    expression || line        || expected
    FORK       || 'fork'      || 'fork("FORK0") { forkBlock("FORK_BLOCK1") {'
    FORK_AGAIN || 'for again' || '} forkBlock("FORK_BLOCK1") {'
    END_MERGE  || 'end merge' || '} } // FORK0'
  }

  def "convert() returns null String for unknown expression"() {
    when:
    def result = CalculateNextForkConverter.convert('unknown something', new CalculateNextConversionContext())

    then:
    result == null
  }
}
