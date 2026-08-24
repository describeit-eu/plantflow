package eu.describeit.plantflow

@FunctionalInterface
interface GuardPredicate {
    boolean evaluate(ExecutionContext context, RecordToken token)
}
