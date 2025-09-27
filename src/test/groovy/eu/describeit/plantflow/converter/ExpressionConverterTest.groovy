package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.converter.ExpressionConverter.IF_EQUALS_THEN
import static eu.describeit.plantflow.converter.ExpressionConverter.IF_THEN
import static eu.describeit.plantflow.converter.ExpressionConverter.IF_IS_THEN
import static eu.describeit.plantflow.converter.ExpressionConverter.ELSE
import static eu.describeit.plantflow.converter.ExpressionConverter.REPEAT_WHilE

class ExpressionConverterTest extends Specification {

  @Unroll
  def "match returns correct enum for '#line'"() {
    expect:
    ExpressionConverter.match(line) == expected

    where:
    line                                          || expected
    'if (a > b) then (explain)'                   || IF_THEN
    'if (status) is (OK) then'                    || IF_IS_THEN
    'if (status) equals (OK) then'                || IF_EQUALS_THEN
    'else (because)'                              || ELSE
    'repeat while (data?) is (yes) not (no)'      || REPEAT_WHilE
  }

  @Unroll
  def "convert produces expected output for '#expression'"() {
    expect:
    expression.convertLine(line) == expected

    where:
    expression     || line                                        || expected
    IF_THEN        || 'if (a > b) then (explain)'                 || 'if (eval("a > b")) { // explain'
    IF_IS_THEN     || 'if (status) is (OK) then'                  || 'if (eval("status") == "OK") { // is'
    IF_IS_THEN     || 'if (func(status)) is (OK) then'            || 'if (eval("func(status)") == "OK") { // is'
    IF_EQUALS_THEN || 'if (status) equals (OK) then'              || 'if (eval("status") == "OK") { // equals'
    IF_EQUALS_THEN || 'if (func(status)) equals (OK) then'        || 'if (eval("func(status)") == "OK") { // equals'
    ELSE           || 'else (because)'                            || '} else { // because'
    REPEAT_WHilE   || 'repeat while (data?) is (yes) not (no)'    || 'repeatWhile ("data?") is ("yes") not ("no")'
  }

  def "convert throws IllegalArgumentException for unknown expression"() {
    when:
    ExpressionConverter.convert('unknown something')

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message.contains('Unknown expression for line:unknown something')
  }
}
