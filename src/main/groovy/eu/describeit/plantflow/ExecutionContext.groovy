package eu.describeit.plantflow

import groovy.transform.CompileStatic

@CompileStatic
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

    Object propertyMissing(String name) {
        return get(name)
    }

    void propertyMissing(String name, Object value) {
        set(name, value)
    }

    boolean hasVariable(String key) {
        return variables.containsKey(key)
    }

    Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(variables))
    }

    @Override
    String toString() {
        return "ExecutionContext(variables=${variables})"
    }
}
