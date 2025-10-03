package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

import static ForkConverter.FORK
import static ForkConverter.FORK_AGAIN
import static ForkConverter.END_MERGE

class ForkConverterTest extends Specification {

  @Unroll
  def "match returns correct enum for '#line'"() {
    expect:
    ForkConverter.match(line) == expected

    where:
    line         || expected
    'fork'       || FORK
    'fork again' || FORK_AGAIN
    'end merge'  || END_MERGE
  }

  @Unroll
  def "convert produces expected output for '#expression'"() {
    expect:
    expression.convertLine(line) == expected

    where:
    expression || line        || expected
    FORK       || 'fork'      || 'fork {'
    FORK_AGAIN || 'for again' || '} forkAgain {'
    END_MERGE  || 'end merge' || '}; if (endFork()) return'
  }

  def "convert throws IllegalArgumentException for unknown expression"() {
    when:
    LoopConverter.convert('unknown something')

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message.contains('Unknown expression for line:unknown something')
  }
}
