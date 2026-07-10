# Matrix Logging release history

## v0.1.1, 2026-07-10
- Dependency updates:
  - org.apache.logging.log4j:log4j-to-slf4j 2.25.3 -> 2.26.1
  - org.slf4j:slf4j-bom 2.0.17 -> 2.0.18
- Fix stale slf4j/log4j versions and matrix-core version in docs/logging.md and the README `@Grab` example, left behind by the dependency updates above
- Add a smoke test verifying System.Logger and Log4j API calls are actually routed through to slf4j-simple output

## v0.1.0, 2026-07-04
- Initial version: optional convenience module wiring a default SLF4J-based logging setup (`slf4j-simple`, `slf4j-jdk-platform-logging`, `log4j-to-slf4j`) for Groovy scripts and small tools using Matrix
- Fix broken POM/license/SCM URLs
