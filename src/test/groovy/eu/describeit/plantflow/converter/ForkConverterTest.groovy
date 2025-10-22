package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

import static ForkConverter.FORK
import static ForkConverter.FORK_AGAIN
import static ForkConverter.END_MERGE

class ForkConverterTest extends Specification {

  @Unroll
  def "match() returns correct enum for '#line'"() {
    expect:
    ForkConverter.match(line) == expected

    where:
    line         || expected
    'fork'       || FORK
    'fork again' || FORK_AGAIN
    'end merge'  || END_MERGE
  }

  @Unroll
  def "convertLine() produces expected output for '#expression'"() {
    expect:
    expression.convertLine(line, 'FORK0') == expected

    where:
    expression || line        || expected
    FORK       || 'fork'      || 'fork("FORK0") {'
    FORK_AGAIN || 'for again' || '} forkAgain("FORK0") {'
    END_MERGE  || 'end merge' || '}; if (endFork("FORK0")) return'
  }

  def "convert() returns null String for unknown expression"() {
    when:
    def result = ForkConverter.convert('unknown something', new ConversionContext())

    then:
    result == null
  }
}
