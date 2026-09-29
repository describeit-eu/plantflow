# Quality Tools: CodeNarc, JaCoCo, and Pitest

Code quality enforcement based on Gradle plugins: static analysis, test coverage, and mutation testing.

## CodeNarc (Static Analysis)

**Purpose**: Enforce Groovy coding standards and detect code smells via static analysis.

**Leading word**: **strict** — analysis fails the build on any violation.

### Configuration

- **Gradle plugin**: `codenarc`
- **Main ruleset**: `config/codenarc/rules.groovy`
- **Test ruleset**: `config/codenarc/test-rules.groovy` (currently empty)
- **Fail build on violations**: Yes (`isIgnoreFailures = false`)
- **Max priority 1 violations**: 0 (`maxPriority1Violations = 0`)

### Tasks

- `codenarcMain` - Run CodeNarc analysis for main classes
- `codenarcTest` - Run CodeNarc analysis for test classes

### Invocation

```bash
./gradlew codenarcMain
./gradlew codenarcTest
./gradlew check  # Includes CodeNarc verification
```

## JaCoCo (Test Coverage)

**Purpose**: Measure and report test coverage for Groovy code.

**Leading word**: **coverage** — track what percentage of code is exercised by tests.

### Configuration

- **Gradle Plugin**: `jacoco`
- **Report formats**: XML, HTML
- **Integration**: Test task finalizes with `jacocoTestReport`

### Tasks

- `jacocoTestReport` - Generates code coverage report for the test task
- `jacocoTestCoverageVerification` - Verifies code coverage metrics based on specified rules
- `jacocoTestReportToCobertura` - Converts JaCoCo XML report to Cobertura format

### Reports

- **HTML**: `build/reports/jacoco/test/html/`
- **XML**: `build/reports/jacoco/test/jacocoTestReport.xml`
- **Cobertura**: `build/reports/jacoco/test/cobertura-jacocoTestReport.xml`

### Invocation

```bash
./gradlew test  # Automatically runs jacocoTestReport
./gradlew jacocoTestReport
./gradlew jacocoTestReportToCobertura
```

## Pitest (Mutation Testing)

**Purpose**: Evaluate test suite quality by introducing mutations and checking if tests catch them.

**Leading word**: **mutations** — measure how many artificial bugs the tests detect.

### Configuration

- **Plugin**: `info.solidsoft.pitest`
- **Output formats**: XML, HTML
- **Timestamped reports**: Disabled

### Tasks

- `pitest` - Run PIT analysis for Java classes (also covers Groovy via `-FGROOVY`)

### Reports

- **XML**: `build/reports/pitest/`
- **HTML**: `build/reports/pitest/`

### Invocation

```bash
./gradlew pitest
```

## Combined Quality Checks

All quality tools run together:

```bash
./gradlew check  # Runs CodeNarc
./gradlew test  # Runs tests + JaCoCo report
./gradlew pitest  # Runs mutation testing
```

For full quality gate:

```bash
./gradlew check test pitest
```

## When to Run

- **CodeNarc**: Run on every commit; enforced in CI
- **JaCoCo**: Run on every test execution; reports generated automatically
- **Pitest**: Run before major releases or when test coverage changes significantly
