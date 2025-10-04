package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

import static LoopConverter.ENDWHILE
import static LoopConverter.REPEAT_WHILE
import static LoopConverter.WHILE
import static LoopConverter.WHILE_IS
import static eu.describeit.plantflow.converter.LoopConverter.ENDWHILE2
import static eu.describeit.plantflow.converter.LoopConverter.REPEAT

class LoopConverterTest extends Specification {

  @Unroll
  def "match returns correct enum for '#line'"() {
    expect:
    LoopConverter.match(line) == expected

    where:
    line                                           || expected
    'while (true)'                                 || WHILE
    'while (check filesize ?) is (not empty)'      || WHILE_IS
    'endwhile (empty)'                             || ENDWHILE
    'endwhile'                                     || ENDWHILE2
    'repeat while (more data?) is (yes) not (no)'  || REPEAT_WHILE
    'repeat'                                       || REPEAT
  }

  @Unroll
  def "convert produces expected output for '#expression'"() {
    expect:
    expression.convertLine(line, 'LOOP0') == expected

    where:
    expression     || line                                           || expected
    WHILE          || 'while (true)'                                 || 'loop("LOOP0") { while (eval("true")) {'
    WHILE_IS       || 'while (check filesize ?) is (not empty)'      || 'loop("LOOP0") { while (eval("check filesize ?", "not empty")) { // is'
    ENDWHILE       || 'endwhile (true)'                              || '} } // true LOOP0'
    ENDWHILE2      || 'endwhile'                                     || '} } // LOOP0'
    REPEAT_WHILE   || 'repeat while (more data?) is (yes) not (no)'  || '} while (eval("more data?", "yes")) } // not ("no") LOOP0'
    REPEAT         || 'repeat'                                       || 'loop("LOOP0") { do {'
  }

  def "convert throws IllegalArgumentException for unknown expression"() {
    when:
    LoopConverter.convert('unknown something', new ConversionContext())

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message.contains('Unknown expression for line:unknown something')
  }
}
