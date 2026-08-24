package eu.describeit.plantflow

@FunctionalInterface
interface ActionHandler {
    Object execute(ExecutionContext context, RecordToken token)
}
