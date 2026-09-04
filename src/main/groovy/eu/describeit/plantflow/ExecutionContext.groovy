package eu.describeit.plantflow

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includePackage = false)
class ExecutionContext {
    private final Map<String, Object> variables

    ExecutionContext(Map<String, Object> initialVariables = [:]) {
        this.variables = new LinkedHashMap<>(initialVariables ?: [:])
    }

    Object get(String key) {
        return variables.get(key)
    }

    void set(String key, Object value) {
        variables.put(key, value)
    }

    Object getAt(String key) {
        return get(key)
    }

    void putAt(String key, Object value) {
        set(key, value)
    }
}
