package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

import static eu.describeit.plantflow.converter.ExpressionConverter.ELSEIF_EQUALS
import static eu.describeit.plantflow.converter.ExpressionConverter.ELSEIF_IS
import static eu.describeit.plantflow.converter.ExpressionConverter.ELSEIF_THEN
import static eu.describeit.plantflow.converter.ExpressionConverter.IF_EQUALS
import static eu.describeit.plantflow.converter.ExpressionConverter.IF_THEN
import static eu.describeit.plantflow.converter.ExpressionConverter.IF_IS
import static eu.describeit.plantflow.converter.ExpressionConverter.ELSE

class ExpressionConverterTest extends Specification {

  @Unroll
  def "match returns correct enum for '#line'"() {
    expect:
    ExpressionConverter.match(line) == expected

    where:
    line                                          || expected
    'if (a > b) then (explain)'                   || IF_THEN
    'if (status) is (OK) then'                    || IF_IS
    'if (status) equals (OK) then'                || IF_EQUALS
    'elseif (a > b) then (explain)'               || ELSEIF_THEN
    'elseif (status) is (OK) then'                || ELSEIF_IS
    'elseif (status) equals (OK) then'            || ELSEIF_EQUALS
    'else (because)'                              || ELSE
  }

  @Unroll
  def "convert produces expected output for '#expression'"() {
    expect:
    expression.convertLine(line) == expected

    where:
    expression     || line                                        || expected
    IF_THEN        || 'if (a > b) then (explain)'                 || 'if (eval("a > b")) { // explain'
    IF_IS          || 'if (status) is (OK) then'                  || 'if (eval("status") == "OK") { // is'
    IF_IS          || 'if (func(status)) is (OK) then'            || 'if (eval("func(status)") == "OK") { // is'
    IF_EQUALS      || 'if (status) equals (OK) then'              || 'if (eval("status") == "OK") { // equals'
    IF_EQUALS      || 'if (func(status)) equals (OK) then'        || 'if (eval("func(status)") == "OK") { // equals'
    ELSEIF_THEN    || 'elseif (a > b) then (explain)'             || 'else if (eval("a > b")) { // explain'
    ELSEIF_IS      || 'elseif (status) is (OK) then'              || 'else if (eval("status") == "OK") { // is'
    ELSEIF_IS      || 'elseif (func(status)) is (OK) then'        || 'else if (eval("func(status)") == "OK") { // is'
    ELSEIF_EQUALS  || 'elseif (status) equals (OK) then'          || 'else if (eval("status") == "OK") { // equals'
    ELSEIF_EQUALS  || 'elseif (func(status)) equals (OK) then'    || 'else if (eval("func(status)") == "OK") { // equals'
    ELSE           || 'else (because)'                            || '} else { // because'
  }

  def "convert throws IllegalArgumentException for unknown expression"() {
    when:
    ExpressionConverter.convert('unknown something')

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message.contains('Unknown expression for line:unknown something')
  }
}
