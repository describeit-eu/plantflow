package eu.describeit.plantflow.converter

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includePackage=false, excludes="type,idx")
class ConversionBlock {
  enum Type { LOOP, CONDITIONAL, FORK }

  Type type
  int idx = 0

  String getId() {
    return type.toString() + idx
  }
}
