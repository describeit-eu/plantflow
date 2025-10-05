package eu.describeit.plantflow.converter

import spock.lang.Specification
import spock.lang.Unroll

import static ConditionalConverter.ELSEIF_EQUALS
import static ConditionalConverter.ELSEIF_IS
import static ConditionalConverter.ELSEIF_THEN
import static ConditionalConverter.IF_EQUALS
import static ConditionalConverter.IF_THEN
import static ConditionalConverter.IF_IS
import static ConditionalConverter.ELSE
import static ConditionalConverter.ENDIF

class ConditionalConverterTest extends Specification {

  @Unroll
  def "match returns correct enum for '#line'"() {
    expect:
    ConditionalConverter.match(line) == expected

    where:
    line                               || expected
    'if (a > b) then (explain)'        || IF_THEN
    'if (status) is (OK) then'         || IF_IS
    'if (status) equals (OK) then'     || IF_EQUALS
    'elseif (a > b) then (explain)'    || ELSEIF_THEN
    'elseif (status) is (OK) then'     || ELSEIF_IS
    'elseif (status) equals (OK) then' || ELSEIF_EQUALS
    'else (because)'                   || ELSE
    'endif'                            || ENDIF
  }

  @Unroll
  def "convert produces expected output for '#expression'"() {
    expect:
    expression.convertLine(line, 'CONDITIONAL0') == expected

    where:
    expression     || line                                     || expected
    IF_THEN        || 'if (a > b) then (explain)'              || 'conditional("CONDITIONAL0") { if (eval("a > b")) { // explain'
    IF_IS          || 'if (status) is (OK) then'               || 'conditional("CONDITIONAL0") { if (eval("status", "OK")) { // is'
    IF_IS          || 'if (func(status)) is (OK) then'         || 'conditional("CONDITIONAL0") { if (eval("func(status)", "OK")) { // is'
    IF_EQUALS      || 'if (status) equals (OK) then'           || 'conditional("CONDITIONAL0") { if (eval("status", "OK")) { // equals'
    IF_EQUALS      || 'if (func(status)) equals (OK) then'     || 'conditional("CONDITIONAL0") { if (eval("func(status)", "OK")) { // equals'
    ELSEIF_THEN    || 'elseif (a > b) then (explain)'          || 'else if (eval("a > b")) { // explain CONDITIONAL0'
    ELSEIF_IS      || 'elseif (status) is (OK) then'           || 'else if (eval("status", "OK")) { // is CONDITIONAL0'
    ELSEIF_IS      || 'elseif (func(status)) is (OK) then'     || 'else if (eval("func(status)", "OK")) { // is CONDITIONAL0'
    ELSEIF_EQUALS  || 'elseif (status) equals (OK) then'       || 'else if (eval("status", "OK")) { // equals CONDITIONAL0'
    ELSEIF_EQUALS  || 'elseif (func(status)) equals (OK) then' || 'else if (eval("func(status)", "OK")) { // equals CONDITIONAL0'
    ELSE           || 'else (because)'                         || '} else { // because CONDITIONAL0'
    ENDIF          || 'endif'                                  || '} } // CONDITIONAL0'
  }

  def "convert returns null for unknown expression"() {
    when:
    def result = ConditionalConverter.convert('unknown something', new ConversionContext())

    then:
    result == null
  }
}
