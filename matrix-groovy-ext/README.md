# Matrix Groovy Extensions

This module provides Groovy extensions via "monkey patching" to enable more idiomatic Groovy usage patterns with the Matrix library. It includes:

- BigDecimal extensions to allow for mathematical operations directly on BigDecimal instances. e.g. floor(), ceil(), log10().

It is used by the matrix-charts and matrix-stats modules and needs to be added as a dependency if you use either of them.

The module is automatically registered when adding it as a dependency in your project.

For Maven:
```xml
<dependency>
   <groupId>se.alipsa.matrix</groupId>
   <artifactId>matrix-groovy-ext</artifactId>
   <version>0.1.0</version>
</dependency>
```
For Gradle:

```groovy
implementation('se.alipsa.matrix:matrix-groovy-ext:0.1.0')
```
