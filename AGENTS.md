# Repository Guidelines

This is the entry point for agent guidance in this repository. Detailed companion guides live in
`docs/agents/` — consult them when working in their area:

- [Groovy style guide](docs/agents/groovy-style-guide.md) — idiomatic Groovy, typed overloads vs `Object` parameters, flow typing after `instanceof`, BigDecimal/numeric policy, `NumberExtension` catalog, anti-patterns, switch expressions
- [Logging guidelines](docs/agents/logging-guidelines.md) — the `se.alipsa.matrix.core.util.Logger` utility and what to replace
- [Testing guidelines](docs/agents/testing-guidelines.md) — Groovy + JUnit assertions (`groovier-junit`), SVG chart output testing patterns
- [Architecture and modules](docs/agents/architecture.md) — module table, matrix-core/charts/ggplot/smile internals

## Code Review Standards
When reviewing PRs, report ONLY issues and suggestions. Do NOT report observations, praise, or summaries. Before flagging an issue, verify it doesn't already exist in the codebase and confirm the code is actually wrong — avoid false positives. For Java/Groovy code, be aware of `@CompileStatic` semantics, Groovy constructor dispatch with null values, and type boxing differences (e.g., `int` vs `Integer`).

## Project Structure & Module Organization
This is a Gradle multi-module Groovy/Java project. Each module lives in a `matrix-*` directory (for example `matrix-core`, `matrix-stats`, `matrix-csv`, `matrix-charts`, `matrix-sql`). Source code is primarily in `matrix-*/src/main/groovy` (with some `src/main/java`), and tests live in `matrix-*/src/test/groovy` or `matrix-*/src/test/java`. Shared docs are under `docs/` (tutorial and cookbook; agent guides under `docs/agents/`), while runnable examples live in `matrix-examples/`. See [Architecture and modules](docs/agents/architecture.md) for the module table and subsystem internals.

Authoritative project metadata:
- Module list: `settings.gradle`
- Shared build behavior: root `build.gradle`
- Dependency versions: `gradle/libs.versions.toml`

## Build, Test, and Development Commands
- `./gradlew build`: build all modules.
- `./gradlew :matrix-core:build`: build a single module.
- `./gradlew test`: run all unit tests (always run before merge).
- `./gradlew :matrix-core:test`: run tests for a single module.
- `./gradlew :matrix-ggplot:test --tests "gg.GgPlotTest"`: run a single test class.
- `./gradlew :matrix-ggplot:test --tests "gg.GgPlotTest.testPointChartRender"`: run a single test method.
- `./gradlew test -PrunSlowTests=true`: include slow integration tests.
- `RUN_EXTERNAL_TESTS=true ./gradlew test`: enable external tests (BigQuery, GSheets).
- `./gradlew :matrix-charts:test -Pheadless=true` / `./gradlew :matrix-ggplot:test -Pheadless=true`: GUI/chart tests in headless mode (CI).
- `./gradlew :matrix-charts:testFast` / `./gradlew :matrix-ggplot:testFast`: fast unit tests only (no chart rendering) for quick dev-cycle feedback.
- `./gradlew publishToMavenLocal`: publish artifacts locally.
- `./gradlew spotlessApply`: auto-format all source files.
- `./gradlew spotlessCheck`: verify formatting without modifying files (runs as part of `build`).
- `./gradlew dependencyUpdates`: report newer dependency versions.
- `./gradlew :matrix-core:codenarcMain`: run CodeNarc static analysis on main sources for a single module.
- `./gradlew :matrix-core:codenarcTest`: run CodeNarc static analysis on test sources for a single module.

### Verification order
Before an implementation can be considered done, run these checks in order:
1. `./gradlew :<module>:codenarcMain` — fix any static analysis violations first.
2. `./gradlew :<module>:spotlessCheck` — verify formatting (or run `spotlessApply` to auto-fix).
3. `./gradlew :<module>:test` — ensure all tests pass.

## Coding Style & Naming Conventions
- Use Groovy 5.0.6 and target Java 21 (Java compilation target: release 21). MIT License.
- Follow the existing 2-space indentation and import style in each file.
- Static compilation is enabled globally for production code via `config/groovy/compileStatic.groovy`; do not add redundant `@CompileStatic` annotations to production code. Test code is compiled dynamically by default, so use `@CompileStatic` on test classes or methods that need static compilation. Use `@CompileDynamic` only on production classes or methods that require dynamic Groovy features.
- Groovy compiles both .java and .groovy files (no separate Java srcDir).
- Classes are PascalCase, methods/fields are camelCase, and packages follow `se.alipsa.matrix.*`.
- Test classes are named `*Test.groovy` or `*Test.java` and live in module test directories.
- Always add GroovyDoc for public classes and public methods.
- Formatting is enforced by Spotless (`./gradlew spotlessApply` to auto-format).
- CodeNarc 3.7 cannot parse Groovy 5 arrow switch syntax, so the root `build.gradle` auto-excludes arrow-switch files from CodeNarc and warns about old-style switch syntax. Do not assume CodeNarc fully checks files containing arrow switch expressions until the project upgrades to CodeNarc 4+.
- Understand Groovy constructor dispatch ambiguity (especially with null arguments), `@CompileStatic` type requirements (e.g., explicit casts that look redundant but are required), and `int` vs `Integer` behavioral differences (e.g., `sample(int)` vs `sample(Integer)`).

Core style rules — follow the [Groovy style guide](docs/agents/groovy-style-guide.md) for details and examples:
- Write idiomatic Groovy, not Java in Groovy syntax: closures and `each`/`collect`/`findAll` over loops and streams, `[]`/`[:]` literals, `==` for equality, `"${var}"` interpolation.
- **CRITICAL:** Never use `Object` parameters where specific types are known — use typed overloads (an `Object` fallback that dispatches via `instanceof` to typed overloads is acceptable).
- Under `@CompileStatic`, casts after `instanceof` are redundant for local variables (flow typing) but required for fields, property chains, ternaries, closures, and `||` chains.
- Use `Number` parameters and return `BigDecimal`; BigDecimal is Groovy's natural decimal type. Prefer BigDecimal extension methods (`value.log()`, `x.sqrt()`, `a.min(b)`) over `java.lang.Math`.
- Use implicit return (explicit `return` only for early exits) and modern arrow switch expressions (except when a case arm must `return` from the method).

## Logging Guidelines
**CRITICAL:** In production library code under non-example `src/main`, use the `se.alipsa.matrix.core.util.Logger` utility (`private static final Logger log = Logger.getLogger(MyClass)`) instead of System.out, System.err, println, log4j, or SLF4J direct usage. Console output is acceptable in examples, demos, benchmarks, output-inspecting tests, and Gradle task hooks. See [Logging guidelines](docs/agents/logging-guidelines.md) for patterns, levels, and replacement recipes.

## DRY Principle (Don't Repeat Yourself)
**CRITICAL:** Avoid code duplication! Before implementing functionality, check if similar code already exists in the codebase. If you find duplicated code (identical or near-identical methods, constants, or logic in multiple files):

1. **Extract to Utility Class**: Create a shared utility class in an appropriate package to hold the common functionality. For example:
   - Color conversion logic → `ColorSpaceUtil` in the same package
   - Math utilities → `MathUtil` in a common utilities package
   - String processing → `StringUtil` in a common utilities package

2. **Update All References**: When extracting shared code, update ALL files that were using the duplicated code to use the new utility class. Don't leave any instances of the old duplicated code behind.

3. **Update Tests**: When moving code to a utility class, update any tests that were testing the old private methods to test the new utility class methods instead.

4. **Common Examples of Duplication to Avoid**:
   - Constants (like color space reference values, magic numbers, default values)
   - Conversion functions (color space, coordinate, unit conversions)
   - Validation logic (input checking, bounds checking, type validation)
   - Mathematical calculations used in multiple places
   - Parsing/formatting logic

5. **Check Before You Copy**: Before copying a method or code block from one file to another, ask: "Should this be shared?" If the answer is yes or maybe, extract it to a utility class instead of duplicating it.

**Example**: If you're implementing HCL color conversion and find similar code already exists in `ScaleColorManual`, don't copy it—extract it to a `ColorSpaceUtil` class that both can use.

## Testing Guidelines
JUnit Jupiter is the primary test framework. Always create tests for new features and update tests when behavior changes; place them in the relevant module's `src/test` tree. Use `-PrunSlowTests=true` and `RUN_EXTERNAL_TESTS=true` only when you intend to run the slow or external suites. For chart rendering tests, prefer headless mode in CI: `./gradlew :matrix-charts:test -Pheadless=true` and `./gradlew :matrix-ggplot:test -Pheadless=true`. When a task is done, run the full test suite to guard against regressions (`./gradlew test`). **Always** run tests after a task is complete to ensure no regressions (except for documentation-only tasks).

Key patterns — see [Testing guidelines](docs/agents/testing-guidelines.md) for examples:
- Groovy test modules must include the `se.alipsa.groovy:groovier-junit` test dependency; never coerce GStrings to String for JUnit assertions.
- For SVG chart assertions, prefer direct object access (`svg.descendants().findAll { it instanceof Path }`); use `SvgWriter.toXml()` only for serialized-XML assertions; **never** `svg.toString()`.

## Commit & Pull Request Guidelines
Commit messages in this repo are short, imperative summaries (e.g., “Fix …”, “Update …”, “Add …”), optionally mentioning the module. For pull requests, include a concise summary, list the modules touched, and record the tests you ran (with commands). Link relevant issues and add screenshots for visual/chart output changes.

**CRITICAL:** Always confirm with the user before committing and pushing to the main branch. When committing review fixes or follow-up changes, create a separate branch (e.g., `feature-followup`) rather than pushing directly to `main`.

**Do not create git tags** as part of release preparation or any other step. Tags are created by GitHub during the release creation process; manually created tags will conflict with those GitHub-generated tags.

## Environment & Constraints
JDK 21 is required. Some modules (parquet/avro, charts, smile) enforce a maximum JDK 21 due to upstream dependencies, so keep toolchains aligned with that constraint.

| Module(s)                   | Max JDK | Reason                                                                  |
|-----------------------------|---------|-------------------------------------------------------------------------|
| matrix-parquet, matrix-avro | 21      | Hadoop 3.5.x incompatible with JDK 22+                                  |
| matrix-charts               | 21      | JavaFX 23.x requires JDK 21; 24+ requires JDK 22+                       |
| matrix-smile                | 21      | Smile 4.x used requires a minimum of Java 21; Smile 5+ requires Java 25 |

## Planning
When creating plans:
- Always number individual tasks and issues. Use a hierarchical task numbering scheme where the overall feature gets the major number and tasks under it get minor numbers. For example:
```
  2. Unify numeric coercion and NA handling 
  2.1 [ ] Update `ScaleUtils.coerceToNumber` in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleUtils.groovy` to return `BigDecimal` (or `null`), treating `NaN`, `null`, and blank values consistently.
  2.2 [ ] Remove duplicate `coerceToNumber` implementations in `ScaleContinuous`, `ScaleXLog10`, `ScaleXSqrt`, `ScaleXReverse` and route all conversions through `ScaleUtils`.
```
- Use checkboxes `[ ]` for tasks that need to be done and `[x]` for completed tasks. A task is only done when the checkbox is marked as done. A checkbox is not marked as done until tests have been run successfully **and the specific test commands used (for example, `./gradlew :matrix-charts:test` or `./gradlew test`) have been recorded in the plan or PR description**.

## Documentation
When generating documentation, always include:
- concrete usage examples
- all parameter descriptions with defaults
- complete end-to-end instructions on first attempt.

Do not produce minimal stubs that require multiple rounds of refinement.

## Key Dependencies

- **Groovy**: 5.0.6 (groovy, groovy-sql, groovy-ginq)
- **Testing**: JUnit Jupiter 6.0.3
- **Charting**: gsvg 1.1.0, XChart 3.8.8, JavaFX 23.0.2
- **Statistics**: Smile 4.4.2, commons-math3
- **Data formats**: Jackson, Apache POI, commons-csv
