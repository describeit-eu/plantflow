# Context Mutator Handlers

Guard predicates and action handlers receive a mutable `ExecutionContext` alongside the input `Token`. This allows guards to check workflow flags (e.g., `actions['process all']`) and actions to mutate workflow state or produce new token data during transition firing, cleanly separating invariant net topology from workflow data mutation.
