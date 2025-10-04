package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static eu.describeit.plantflow.converter.ConversionUtils.stringFormatLine

@CompileStatic
@Slf4j
enum ConditionalConverter {
  IF_THEN       (~ /^if *\(.*\) *then *\(.*\)$/             , 'if (eval("%s")) { // %s'),
  IF_IS         (~ /^if *\(.*\) *is *\(.*\) *then$/         , 'if (eval("%s", "%s")) { // is'),
  IF_EQUALS     (~ /^if *\(.*\) *equals *\(.*\) *then$/     , 'if (eval("%s", "%s")) { // equals'),
  ELSEIF_THEN   (~ /^elseif *\(.*\) *then *\(.*\)$/         , 'else if (eval("%s")) { // %s'),
  ELSEIF_IS     (~ /^elseif *\(.*\) *is *\(.*\) *then$/     , 'else if (eval("%s", "%s")) { // is'),
  ELSEIF_EQUALS (~ /^elseif *\(.*\) *equals *\(.*\) *then$/ , 'else if (eval("%s", "%s")) { // equals'),
  ELSE          (~ /^else *\(.*\)$/                         , '} else { // %s'),
  ENDIF         (~ /^endif$/                                , '}'),

  final Pattern matcher
  final String expression

  ConditionalConverter(Pattern pattern, String expression) {
    this.matcher = pattern
    this.expression = expression
  }

  static ConditionalConverter match(String line) {
    return values().find { ConditionalConverter ec -> (line ==~ ec.matcher) } as ConditionalConverter
  }

  static String convert(String line) {
    ConditionalConverter converter = match(line)
    if (converter == null) return null

    return converter.convertLine(line)
  }

  String convertLine(String line) {
    return stringFormatLine(line, expression)
  }
}
