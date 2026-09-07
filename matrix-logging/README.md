[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-logging/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-logging)

# Matrix Logging

`matrix-logging` is an optional convenience module for Groovy scripts and small
tools using Matrix.

Matrix itself does not require a logging framework. Matrix code logs through the
JDK `System.Logger` facade, and project/application users can wire logging to
SLF4J, Log4j 2, or JUL as they prefer. Some Matrix modules use third-party
libraries that log through SLF4J or Log4j, though, and Groovy script users often
expect those dependencies to work without extra logging setup.

This module provides a simple SLF4J-based default when its dependencies are on
the application classpath:

- `slf4j-simple` as the lightweight SLF4J provider
- `slf4j-jdk-platform-logging` to route Matrix `System.Logger` calls to SLF4J
- `log4j-to-slf4j` to route Log4j API calls from third-party libraries to SLF4J

## Groovy scripts

Grab this module alongside the Matrix modules you use. `systemClassLoader=true`
is required so SLF4J can discover the grabbed provider and Log4j bridge:

```groovy
@GrabConfig(systemClassLoader=true)
@Grab('se.alipsa.matrix:matrix-core:3.8.0')
@Grab('se.alipsa.matrix:matrix-logging:0.1.2')
import se.alipsa.matrix.core.Matrix
```

Grape and `groovy -cp` load dependencies after the JVM has selected its
`System.LoggerFinder`. Consequently, this script setup routes SLF4J and Log4j
API calls to `slf4j-simple`, but Matrix `System.Logger` calls continue to use
the JDK's default JUL backend. To route JPL/System.Logger calls through SLF4J,
put `matrix-logging` and its runtime dependencies on the JVM launch classpath,
for example with a Gradle or Maven build, `java -cp`, or the `CLASSPATH`
environment variable when launching with `java`. The `groovy` launcher does not
qualify: neither `groovy -cp` nor `CLASSPATH` places entries on the JVM launch
classpath.

## Gradle

```groovy
dependencies {
  implementation(platform('se.alipsa.matrix:matrix-bom:2.5.2'))
  implementation('se.alipsa.matrix:matrix-core')
  runtimeOnly('se.alipsa.matrix:matrix-logging')
}
```

For full applications, prefer wiring your logging backend explicitly. See
[Logging setup](../docs/logging.md) for SLF4J/Logback, Log4j 2, and JUL
examples. Do not depend on `matrix-logging` from an application that already
configures a logging backend: it transitively adds `slf4j-simple` and
`log4j-to-slf4j`, which can introduce a second provider or a routing loop.
