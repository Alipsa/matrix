# Logging setup

Matrix itself does not require a logging framework. Matrix code logs through
`se.alipsa.matrix.core.util.Logger`, which is backed by the JDK `System.Logger`
facade.

Some Matrix modules use third-party libraries that log through SLF4J, Log4j,
JUL (`java.util.logging`), or JPL (JDK Platform Logging / `System.Logger`).
Those libraries may warn about missing logging providers if your application
does not choose a logging backend. The fix is to choose one backend and route
the other logging APIs into it.

## Groovy scripts and small tools

For Groovy scripts, the simplest option is to grab `matrix-logging` alongside
the Matrix modules you use:

```groovy
@Grab('se.alipsa.matrix:matrix-core:3.7.0')
@Grab('se.alipsa.matrix:matrix-logging:0.1.0')
import se.alipsa.matrix.core.Matrix
```

`matrix-logging` provides a lightweight SLF4J default using `slf4j-simple`,
routes Matrix `System.Logger` calls to SLF4J, and routes Log4j API calls from
third-party libraries to SLF4J. Full applications can use it too, but they often
prefer one of the explicit setups below.

Do not install bridges in both directions. For example:

- Do not combine `log4j-to-slf4j` with `log4j-slf4j2-impl`.
- Do not combine `jul-to-slf4j` with `slf4j-jdk14`.
- Use exactly one SLF4J provider, such as Logback, `slf4j-simple`, or
  `log4j-slf4j2-impl`.

The examples below use concrete dependency versions to make the setup
copy-pasteable. If your application already manages logging versions through a
platform or framework, use those managed versions instead.

## SLF4J or Logback

Use this when your application standardizes on SLF4J, typically with Logback as
the runtime implementation.

Gradle:

```groovy
dependencies {
  runtimeOnly 'ch.qos.logback:logback-classic:1.5.21'

  // Route Matrix System.Logger/JPL calls to SLF4J.
  runtimeOnly 'org.slf4j:slf4j-jdk-platform-logging:2.0.17'

  // Route Log4j API calls from third-party libraries to SLF4J.
  runtimeOnly 'org.apache.logging.log4j:log4j-to-slf4j:2.25.3'

  // Optional: route JUL calls to SLF4J. Requires installing SLF4JBridgeHandler
  // at application startup.
  runtimeOnly 'org.slf4j:jul-to-slf4j:2.0.17'
}
```

If you add `jul-to-slf4j`, install it early in application startup:

```groovy
import org.slf4j.bridge.SLF4JBridgeHandler

SLF4JBridgeHandler.removeHandlersForRootLogger()
SLF4JBridgeHandler.install()
```

Maven:

```xml
<dependencies>
  <dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.21</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-jdk-platform-logging</artifactId>
    <version>2.0.17</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-to-slf4j</artifactId>
    <version>2.25.3</version>
    <scope>runtime</scope>
  </dependency>
  <!-- Optional: route JUL calls to SLF4J. Requires installing SLF4JBridgeHandler. -->
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jul-to-slf4j</artifactId>
    <version>2.0.17</version>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

Do not add `slf4j-jdk14` or `log4j-slf4j2-impl` in this setup.

## Log4j 2

Use this when your application standardizes on Log4j 2.

Gradle:

```groovy
dependencies {
  runtimeOnly platform('org.apache.logging.log4j:log4j-bom:2.25.3')

  // Log4j implementation.
  runtimeOnly 'org.apache.logging.log4j:log4j-core'

  // Route SLF4J calls from third-party libraries to Log4j.
  runtimeOnly 'org.apache.logging.log4j:log4j-slf4j2-impl'

  // Route Matrix System.Logger/JPL calls to Log4j.
  runtimeOnly 'org.apache.logging.log4j:log4j-jpl'

  // Optional: route JUL calls to Log4j.
  runtimeOnly 'org.apache.logging.log4j:log4j-jul'
}
```

If you add `log4j-jul`, activate it before JUL initializes:

```bash
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
```

Maven:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-bom</artifactId>
      <version>2.25.3</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j2-impl</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-jpl</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-jul</artifactId>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

Do not add `log4j-to-slf4j` in this setup. That bridge sends Log4j API calls
to SLF4J, while `log4j-slf4j2-impl` sends SLF4J calls back to Log4j.

## JUL

Use this when your application wants to keep the JDK `java.util.logging`
backend. Matrix `System.Logger` calls use the JDK backend by default, so no
extra bridge is needed for Matrix itself.

Gradle:

```groovy
dependencies {
  // Route SLF4J calls from third-party libraries to JUL.
  runtimeOnly 'org.slf4j:slf4j-jdk14:2.0.17'

  // Route Log4j API calls from third-party libraries to JUL.
  runtimeOnly 'org.apache.logging.log4j:log4j-to-jul:2.25.3'
}
```

Maven:

```xml
<dependencies>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-jdk14</artifactId>
    <version>2.0.17</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-to-jul</artifactId>
    <version>2.25.3</version>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

Do not add `jul-to-slf4j`, `log4j-jul`, or `log4j-jpl` in this setup.

## Quick choice

- For Groovy applications, SLF4J with Logback is usually the most natural
  choice.
- For larger applications already using Log4j 2, use the Log4j 2 setup.
- For small tools or environments that prefer only JDK dependencies, use JUL.

References:

- SLF4J: https://www.slf4j.org/
- SLF4J JDK Platform Logging integration: https://central.sonatype.com/artifact/org.slf4j/slf4j-jdk-platform-logging
- SLF4J bridges: https://slf4j.org/legacy.html
- Log4j installation and bridges: https://logging.apache.org/log4j/2.x/manual/installation.html
