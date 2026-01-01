# Repository Guidelines

## Project Structure & Module Organization
This is a Gradle multi-module Groovy/Java project. Each module lives in a `matrix-*` directory (for example `matrix-core`, `matrix-stats`, `matrix-csv`, `matrix-charts`, `matrix-sql`). Source code is primarily in `matrix-*/src/main/groovy` (with some `src/main/java`), and tests live in `matrix-*/src/test/groovy` or `matrix-*/src/test/java`. Shared docs are under `docs/` (tutorial and cookbook), while runnable examples live in `matrix-examples/`. Root build configuration is in `build.gradle`, `settings.gradle`, and `dependencies.gradle`.

## Build, Test, and Development Commands
- `./gradlew build`: build all modules.
- `./gradlew :matrix-core:build`: build a single module.
- `./gradlew test`: run all unit tests.
- `./gradlew :matrix-charts:test --tests "gg.GgPlotTest"`: run a single test class.
- `./gradlew test -PrunSlowTests=true`: include slow integration tests.
- `RUN_EXTERNAL_TESTS=true ./gradlew test`: enable external tests (BigQuery, GSheets).
- `./gradlew publishToMavenLocal`: publish artifacts locally.
- `./gradlew dependencyUpdates`: report newer dependency versions.

## Coding Style & Naming Conventions
Use Groovy 5.0.3 and target Java 21. Follow the existing 2-space indentation and import style in each file. Prefer `@CompileStatic` on performance-critical classes. Classes are PascalCase, methods/fields are camelCase, and packages follow `se.alipsa.matrix.*`. Test classes are named `*Test.groovy` or `*Test.java` and live in module test directories. There is no enforced formatter, so match the surrounding file conventions.

## Testing Guidelines
JUnit Jupiter (JUnit 5) is the primary test framework. Always create tests for new features and update tests when behavior changes; place them in the relevant module’s `src/test` tree. Use `-PrunSlowTests=true` and `RUN_EXTERNAL_TESTS=true` only when you intend to run the slow or external suites. For chart rendering tests, prefer headless mode in CI: `./gradlew :matrix-charts:test -Pheadless=true`. When a task is done, run the full test suite to guard against regressions (`./gradlew test`).

## Commit & Pull Request Guidelines
Commit messages in this repo are short, imperative summaries (e.g., “Fix …”, “Update …”, “Add …”), optionally mentioning the module. For pull requests, include a concise summary, list the modules touched, and record the tests you ran (with commands). Link relevant issues and add screenshots for visual/chart output changes.

## Environment & Constraints
JDK 21 is required. Some modules (parquet/avro, charts, smile) enforce a maximum JDK 21 due to upstream dependencies, so keep toolchains aligned with that constraint.
