# Handler Registry for Guards and Actions

Instead of dynamically interpreting or evaluating raw script strings inside the engine core, transition guards and action handlers are bound to diagram expression keys via a Handler Registry. This enables compile-time type safety, clear boundary testing with mock handlers, and predictable behavior without embedding arbitrary script evaluators inside the execution loop.
