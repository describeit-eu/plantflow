---
name: groovy-developer
description: Use when working on Groovy projects with Spock testing. Loads common Groovy/Spock rules and architecture-specific guidelines for library, CLI, REST API, and WebApp projects.
---

# Groovy Developer Skill

You are an expert **Groovy** developer working with **Spock** testing framework. This skill consolidates common Groovy coding patterns, performance optimizations, error handling strategies, and Spock testing best practices, with architecture-specific guidelines for library, CLI, REST API, and Web Application projects.

## When to Use

- Writing or refactoring Groovy code
- Designing architecture for Groovy projects
- Writing or reviewing Spock tests
- Working on library, CLI, REST API, or web app projects in Groovy
- Need guidance on Groovy-specific performance optimizations

## Project Context

Identify your project type and follow the corresponding architecture rules:
- **Library** → See [architectures/library.md](architectures/library.md)
- **CLI** → See [architectures/cli.md](architectures/cli.md)
- **REST API** → See [architectures/restapi.md](architectures/restapi.md)
- **Web App** → See [architectures/webapp.md](architectures/webapp.md)

## Code Style & Structure

### Groovy Defaults

Write **idiomatic Groovy** that balances expressiveness with performance:

- Use `camelCase` for methods and variables, `PascalCase` for class names, and `UPPER_SNAKE_CASE` for constants declared with `static final`.
- Prefer Groovy's **optional typing** (`def`) for local variables and closures, but use **explicit types** for public API method signatures.
- Use **single-quote** for string literals instead of double-quotes (see rule bellow).
- Use **GStrings** (`"Hello, ${name}!"`) for interpolation rather than string concatenation — avoid `"${simple_variable}"` and use `"$simple_variable"` instead.
- Use **`?.` safe navigation** and **`?:` Elvis operator** to handle nullable values concisely without explicit null checks.
- Prefer **Groovy collections API** (`collect`, `findAll`, `groupBy`, `inject`) over Java-style `for` loops for data transformation.
- Use **`@CompileStatic`** on performance-sensitive classes to enable static type checking and improve runtime performance.
- Use **`@Canonical`**, **`@Immutable`**, or **`@ToString`** AST transformations instead of manually writing boilerplate `equals`, `hashCode`, and `toString` methods.
- Use **`@TypeChecked`** on classes where dynamic features are not needed to catch type errors at compile time rather than runtime.
- Use **`@SLF4J`** for logging
- Prefer **named argument maps** in method calls (`createUser(name: 'Alice', role: 'admin')`) over positional argument lists for clarity.
- Use **`with {}` blocks** to group multiple operations on the same object instead of repeating the variable name.
- Use **Groovy traits** for interface-like reuse with default method implementations instead of abstract classes when the hierarchy is flat.
- Use **`@groovy.transform.EqualsAndHashCode`** with `includes` or `excludes` parameters to control which fields participate in equality comparison.

### Paradigm Selection

Choose the right paradigm for each responsibility:
- Use **functional patterns** for data transformations and pure logic.
- Use **OOP** for stateful components, services, and domain modeling.
- Choose the paradigm that best fits each module's responsibility.

## Performance Optimization

**Leading word: tight** — aim for tight, deterministic, low-overhead Groovy code.

- Use **`@CompileStatic`** on hot-path classes to bypass Groovy's dynamic dispatch overhead and generate bytecode comparable to compiled Java.
- Prefer **`List.collectMany {}`** over nested `collect {}` with flatten to avoid creating intermediate lists in large data transformations.
- Use **`LazyMap`** and **`@Lazy`** initialization for expensive computed properties that may not always be accessed.
- Avoid overusing **dynamic features** (`methodMissing`, `propertyMissing`) in performance-critical paths — they add method lookup overhead on every call.
- Use **`eachParallel {}`** or Java **`parallelStream()`** for CPU-bound collection operations that can be safely parallelized.
- Cache results of expensive **`@Memoized`** method calls by annotating them with **`@groovy.transform.Memoized`** for pure functions.
- Prefer **`StringBuilder`** over GString concatenation in tight loops — GString creates intermediate strings on each interpolation.
- **Profile before optimizing** with JVisualVM or async-profiler — dynamic dispatch overhead is rarely the bottleneck in I/O-bound code.
- Use **`@CompileStatic`** with type coercion on scripts that process large data files to reduce the cost of repeated dynamic type checking.
- Use Groovy's **`@TailRecursive`** annotation to convert tail-recursive functions into iterative loops and avoid stack overflow on deep recursion.
- Avoid calling **`metaClass`** methods in production code — accessing the meta-object protocol disables compile-time optimizations and adds per-call overhead.
- Use **`GParsPool.withPool { list.collectParallel { } }`** for parallel collection operations rather than manual thread pool management.

## Error Handling

**Leading word: relentless** — be relentless about proper error handling.

### Groovy-Specific Error Handling

- Use **`try/catch/finally`** with specific exception types rather than catching `Exception` or `Throwable` — catch the most specific type first.
- Use **`?.` safe navigation** to avoid `NullPointerException` when traversing optional object chains: `user?.address?.city`.
- Prefer returning **`Optional.empty()`** or a **null-object pattern** over returning `null` from public methods that may not produce a result.
- Use Groovy's **`multicatch`** syntax (`catch (IOException | SQLException e)`) to handle multiple exception types with shared recovery logic.
- **Log exceptions** with full stack traces using `log.error('Operation failed', e)` rather than `e.printStackTrace()` for consistent log formatting.
- Use **`@Throws(IOException)`** annotations on methods that throw checked exceptions to maintain Java interoperability in mixed codebases.
- Implement service-layer methods with a **result wrapper object** (`Result.success(value)` / `Result.failure(error)`) to make error paths explicit in the return type.
- Use Groovy's **`withCloseable {}`** and **`withStream {}`** patterns for resource management instead of explicit `try/finally` close blocks.
- Define **domain-specific exception hierarchies** (`AppException`, `ValidationException`, `ServiceException`) rather than reusing generic Java exceptions.
- Use **`@EqualsAndHashCode`** on exception classes when they need to be compared in test assertions or used as Map keys.
- Wrap **external API calls** in a `try/catch` that rethrows as a domain exception — this decouples callers from third-party exception hierarchies.
- Use **`Thread.setDefaultUncaughtExceptionHandler`** in long-running Groovy services to log unhandled exceptions from background threads.

### Error Handling Strategy

- **Combine exceptions** for unexpected failures with **Result/Either types** for expected business errors.
- Use **exceptions** for infrastructure failures (network, I/O, OOM) and **result types** for domain validation errors.
- **Never swallow exceptions silently** — always log or propagate with context.
- Use **exceptions for truly exceptional conditions** (infrastructure failures, programming errors) and **Result/Either/Option types** for expected business failures (validation, not-found, permission denied).
- **Wrap third-party library exceptions** at module boundaries into your own domain-specific error types.
- **Always attach context** (operation name, input values, timestamps) when re-throwing or wrapping errors.
- Use a **centralized error handler** for cross-cutting concerns (logging, monitoring, user-facing messages).
- Prefer **typed error enums** or **union types** over generic error strings for pattern matching and exhaustiveness checks.
- **Never use exceptions for control flow** — reserve them for truly unexpected states.
- Define an **error taxonomy**: separate **Recoverable** (retry, fallback) from **Fatal** (crash, alert) from **Expected** (return to caller) errors.
- **Wrap third-party exceptions** at module boundaries — translate external errors into domain-specific error types that callers can pattern-match on.
- Attach **structured context** to all errors: operation name, relevant input IDs, timestamp, and correlation/trace ID for distributed tracing.
- Implement a **centralized error handler middleware** for cross-cutting concerns: structured logging, metrics emission, and user-friendly message formatting.
- Use **typed error enums** or **discriminated unions** over generic strings — this enables exhaustiveness checks at compile time and prevents unhandled error paths.
- **Never swallow exceptions silently**. At minimum log them; prefer propagating to a handler that can decide the correct recovery strategy.
- For **async operations**, ensure errors propagate correctly through promise chains — unhandled rejections should crash the process in production rather than silently failing.
- In **retry logic**, distinguish transient errors (network timeout, 503) from permanent errors (400, 404) — only retry transient failures with exponential backoff and a maximum retry count.
- **Document error contracts** at API boundaries: which errors each function can return and what callers should do about them.

## Testing with Spock

**Leading word: red** — your tests should go red on bugs, and stay red until fixed.

### The Iron Rule

> **Writing 3+ similar tests = YOU MUST use where: block**

No exceptions. Refactoring to `where:` takes 2 minutes. Maintaining 10 separate tests takes hours.

### Spock Essentials

Structure tests properly:
- **`given:`** - Setup, stubs (use `>>` for stubbing)
- **`when:`** - Execute action
- **`then:`** - Assertions, mock verification (use `*` for mocks)
- **`expect:`** - Single-line assertion
- **`where:`** - Data table for parameters

Mock vs Stub:
- **Stub** → Return fake data → Goes in `given:` → Use `>>`
- **Mock** → Verify interaction → Goes in `then:` → Use `*`

### Key Practices

- Use **`@Unroll`** with meaningful names: `@Unroll "adding #a to #b gives #expected"`
- Use **`thrown(ExceptionType)`** in `then:` to assert exceptions
- Use **`@Shared`** for expensive fixtures, initialize in `setupSpec()`, clean up in `cleanupSpec()`
- Use **`GroovyMock()`** when mocking Groovy classes with dynamic dispatch
- Combine **`where:`** tables with **`@Unroll`** and **`@FailsWith`** for edge cases
- Use **`with(object) { assertions }`** for grouped property assertions
- Integrate **Spock with JaCoCo** via Gradle for coverage reports
- Use **`DetachedMockFactory`** for Spock mocks in Spring `@Configuration` classes

### General Testing Rules

- Write unit tests for **every new function** immediately after implementation.
- **Run the full suite before committing** — never push failing tests.
- Test **one behavior per test** case. Keep tests fast, isolated, deterministic.
- Follow **Arrange-Act-Assert** pattern.
- **Mock external dependencies** — unit tests validate logic in isolation.
- Name tests **descriptively**: `should return empty array when no items match filter`.
- Test **edge cases**: empty inputs, nulls, boundary values, error conditions.
- Aim for **80%+ coverage** on business logic, but don't chase 100%.
- Use **test factories/builders** for consistent test data.
- Keep tests **independent** — no cross-test dependencies.
- Write **failing test first** when a bug is found, then fix.
- **Mirror source structure** in test organization.
- Use **parameterized/table-driven tests** for many input combinations.

### Full Spock Reference

See [testing/spock.md](testing/spock.md) for complete Spock testing guidelines including:
- Data-driven testing patterns
- Common mistakes and how to avoid them
- Testing strategy (integration vs unit)
- Validation examples
- Rationalization table for common excuses
- Red flags checklist

## Architecture-Specific Rules

Choose your architecture type and follow the corresponding rules:

| Architecture | File                                                 | When to Use                        |
|--------------|------------------------------------------------------|------------------------------------|
| **Library**  | [architectures/library.md](architectures/library.md) | Building reusable Groovy libraries |
| **CLI**      | [architectures/cli.md](architectures/cli.md)         | Command-line applications          |
| **REST API** | [architectures/restapi.md](architectures/restapi.md) | HTTP API services                  |
| **WebApp**   | [architectures/webapp.md](architectures/webapp.md)   | Web applications with UI           |

Each architecture file contains rules specific to that project type. The common rules above apply to all Groovy projects regardless of architecture.
