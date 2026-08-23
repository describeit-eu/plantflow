# Strict Fail-Fast Handler Registry

The Handler Registry strictly requires all guards and actions found in parsed activity diagrams to be explicitly registered before execution. Attempting to evaluate or fire a transition with an unmapped guard or action immediately raises an `UnregisteredHandlerException`, preventing silent execution bugs or unintended permissive state transitions.
