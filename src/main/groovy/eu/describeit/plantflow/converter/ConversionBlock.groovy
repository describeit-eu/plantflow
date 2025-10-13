package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includePackage=false, includes="id")
class ConversionBlock {
  enum Type { LOOP, CONDITIONAL, FORK, SEQ }

  Type type
  int idx = 0

  final List<ConversionBlock> children = []
  final List<String> actions = []

  String getId() {
    return type.toString() + idx
  }

  void addChildren(ConversionBlock child) {
    children.add(child)
  }
}
