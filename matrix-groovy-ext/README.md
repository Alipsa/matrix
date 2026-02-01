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

## Usage Examples

The `NumberExtension` class provides mathematical constants and methods that make numeric operations more readable and idiomatic compared to using Java's `Math` class.

### Mathematical Constants

```groovy
import static se.alipsa.matrix.ext.NumberExtension.PI
import static se.alipsa.matrix.ext.NumberExtension.E

// Circle calculations
BigDecimal radius = 5.0
BigDecimal circumference = 2 * PI * radius    // 31.4159...
BigDecimal area = PI * radius ** 2            // 78.5398...

// Natural exponential base
BigDecimal naturalLog = E.log()               // 1.0 (ln(e) = 1)
```

### Trigonometry

```groovy
// Angle conversion and trigonometric functions
BigDecimal angleDegrees = 45.0
BigDecimal angleRadians = angleDegrees.toRadians()  // π/4
BigDecimal sine = angleRadians.sin()                // 0.7071...
BigDecimal cosine = angleRadians.cos()              // 0.7071...
BigDecimal tangent = angleRadians.tan()             // 1.0

// Convert back to degrees
BigDecimal degrees = angleRadians.toDegrees()       // 45.0

// Compare with Java Math (more verbose):
// double angleRadians = Math.toRadians(45.0);
// double sine = Math.sin(angleRadians);
```

### Polar and Cartesian Coordinates

```groovy
// Polar to Cartesian conversion
BigDecimal r = 10.0
BigDecimal theta = (PI / 4)  // 45 degrees
BigDecimal x = r * theta.cos()
BigDecimal y = r * theta.sin()

// Cartesian to Polar - atan2 handles all quadrants correctly
BigDecimal angle = y.atan2(x)  // Returns angle in radians

// Compare with Java Math:
// double angle = Math.atan2(y, x);
```

### Logarithms and Exponentials

```groovy
BigDecimal value = 100.0

// Natural logarithm (ln)
BigDecimal naturalLog = value.log()        // 4.6051... (ln(100))

// Base-10 logarithm
BigDecimal log10 = value.log10()           // 2.0 (log₁₀(100))

// Logarithm with custom base
BigDecimal log2 = value.log(2)             // 6.6438... (log₂(100))
BigDecimal log8 = 64.0.log(8)              // 2.0 (log₈(64))

// Exponential (e^x)
BigDecimal exp1 = 1.0.exp()                // 2.7182... (e¹)
BigDecimal exp2 = 2.0.exp()                // 7.3890... (e²)

// Verify inverse relationship
BigDecimal original = 5.0
original.log().exp()                       // 5.0 (exp and log are inverses)

// Compare with Java Math:
// double naturalLog = Math.log(100.0);
// double log10 = Math.log10(100.0);
// double exp2 = Math.exp(2.0);
```

### Square Root

```groovy
BigDecimal area = 25.0
BigDecimal side = area.sqrt()              // 5.0

// Useful for distance calculations
BigDecimal dx = 3.0
BigDecimal dy = 4.0
BigDecimal distance = (dx ** 2 + dy ** 2).sqrt()  // 5.0 (Pythagorean theorem)

// Compare with Java Math:
// double side = Math.sqrt(25.0);
```

### Rounding

```groovy
BigDecimal value = 3.7

BigDecimal floored = value.floor()         // 3.0
BigDecimal ceiled = value.ceil()           // 4.0

BigDecimal negative = -2.3
negative.floor()                           // -3.0
negative.ceil()                            // -2.0
```

### Min/Max Clamping

```groovy
// Chainable min/max for clamping values
BigDecimal rawValue = 1.5
BigDecimal clamped = rawValue.max(0).min(1)    // 1.0 (clamped to [0, 1])

BigDecimal negative = -0.5
BigDecimal clampedNeg = negative.max(0).min(1) // 0.0

// Works with mixed Number types
int plotWidth = 640
int plotHeight = 480
BigDecimal radius = plotWidth.min(plotHeight) / 2  // 240.0

// Array index clamping
BigDecimal ratio = 0.75
int maxIndex = 10
BigDecimal idx = (ratio * maxIndex).max(0).min(maxIndex)  // 7.5

// Compare with Java Math (more verbose):
// double clamped = Math.max(0, Math.min(1, rawValue));
```

### Unit in Last Place (ULP)

```groovy
// Useful for floating-point comparison tolerance
BigDecimal value = 1.0
BigDecimal epsilon = value.ulp() * 10      // Small tolerance for comparisons

BigDecimal a = 0.1 + 0.2
BigDecimal b = 0.3
boolean equal = (a - b).abs() < epsilon    // Safe floating-point comparison
```

## Why Use NumberExtension?

| Operation      | Static Groovy with NumberExtension | Static Groovy without NumberExtension                    |
|----------------|------------------------------------|----------------------------------------------------------|
| Circle area    | `PI * r ** 2`                      | `Math.PI * r ** 2`                                       |
| Sine of 45°    | `45.0.toRadians().sin()`           | `Math.sin(Math.toRadians(45.0 as double)) as BigDecimal` |
| Clamp to [0,1] | `value.max(0).min(1)`              | `Math.max(0, Math.min(1, value)) as BigDecimal`          |
| Distance       | `(dx**2 + dy**2).sqrt()`           | `Math.sqrt((dx**2 + dy**2) as doble) as BigDecimal`      |
| Log base 2     | `value.log(2)`                     | `(Math.log(value) / Math.log(2)) as BigDecimal`          |
| Polar angle    | `y.atan2(x)`                       | `Math.atan2(y as double, x as double) as BigDecimal`     |
| Square root    | `value.sqrt()`                     | `Math.sqrt(value) as BigDecimal`                         |
| Natural log    | `value.log()`                      | `Math.log(value) as BigDecimal`                          |
| Floor          | `value.floor()`                    | `Math.floor(value) as BigDecimal`                        |

**Benefits:**
- **Readability** - Method chaining reads naturally left-to-right
- **Consistency** - All operations return BigDecimal automatically (no explicit casting needed)
- **Type safety** - Works with any Number type (Integer, Long, Double, BigDecimal)
- **BigDecimal constants** - PI and E are BigDecimal, not double
- **Groovy idioms** - Fits naturally with Groovy's operator overloading and closures
