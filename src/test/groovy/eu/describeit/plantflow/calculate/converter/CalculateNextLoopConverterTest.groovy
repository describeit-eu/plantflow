package eu.describeit.plantflow.calculate.converter

import spock.lang.Specification
import spock.lang.Unroll

import static CalculateNextLoopConverter.ENDWHILE
import static CalculateNextLoopConverter.REPEAT_WHILE
import static CalculateNextLoopConverter.WHILE
import static CalculateNextLoopConverter.WHILE_IS
import static CalculateNextLoopConverter.ENDWHILE2
import static CalculateNextLoopConverter.REPEAT

class CalculateNextLoopConverterTest extends Specification {

  @Unroll
  def "match returns correct enum for '#line'"() {
    expect:
    CalculateNextLoopConverter.match(line) == expected

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
    expression.convertLine(line, 'LOOP0', 'LOOP_BLOCK1') == expected

    where:
    expression     || line                                           || expected
    WHILE          || 'while (true)'                                 || 'loop("LOOP0") { while (eval("true", null, "LOOP0")) { loopBlock("LOOP_BLOCK1") {'
    WHILE_IS       || 'while (check filesize ?) is (not empty)'      || 'loop("LOOP0") { while (eval("check filesize ?", "not empty", "LOOP0")) { loopBlock("LOOP_BLOCK1") { // is'
    ENDWHILE       || 'endwhile (true)'                              || '} } } // true LOOP0'
    ENDWHILE2      || 'endwhile'                                     || '} } } // LOOP0'
    REPEAT_WHILE   || 'repeat while (more data?) is (yes) not (no)'  || '} } while (eval("more data?", "yes", "LOOP0")) } // not ("no")'
    REPEAT         || 'repeat'                                       || 'loop("LOOP0") { do { loopBlock("LOOP_BLOCK1") {'
  }

  def "convert returns null for unknown expression"() {
    when:
    def result = CalculateNextLoopConverter.convert('unknown something', new CalculateNextConversionContext())

    then:
    result == null
  }
}
