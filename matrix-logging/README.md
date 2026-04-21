[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-logging/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-logging)

# Matrix Logging

`matrix-logging` is an optional convenience module for Groovy scripts and small
tools using Matrix.

Matrix itself does not require a logging framework. Matrix code logs through the
JDK `System.Logger` facade, and project/application users can wire logging to
SLF4J, Log4j 2, or JUL as they prefer. Some Matrix modules use third-party
libraries that log through SLF4J or Log4j, though, and Groovy script users often
expect those dependencies to work without extra logging setup.

This module provides a simple SLF4J-based default:

- `slf4j-simple` as the lightweight SLF4J provider
- `slf4j-jdk-platform-logging` to route Matrix `System.Logger` calls to SLF4J
- `log4j-to-slf4j` to route Log4j API calls from third-party libraries to SLF4J

## Groovy scripts

Grab this module alongside the Matrix modules you use:

```groovy
@Grab('se.alipsa.matrix:matrix-core:3.7.0')
@Grab('se.alipsa.matrix:matrix-logging:0.1.0')
import se.alipsa.matrix.core.Matrix
```

## Gradle

```groovy
dependencies {
  implementation(platform('se.alipsa.matrix:matrix-bom:2.5.0'))
  implementation('se.alipsa.matrix:matrix-core')
  runtimeOnly('se.alipsa.matrix:matrix-logging')
}
```

For full applications, prefer wiring your logging backend explicitly. See
[Logging setup](../docs/logging.md) for SLF4J/Logback, Log4j 2, and JUL
examples.
