# Kimi Code Review Guidelines

This document provides instructions for conducting thorough code reviews that match or exceed the quality of other AI code reviewers (e.g., Claude, Copilot, Codex).

## Review Philosophy

Don't just read code—**attack it**. Assume bugs exist and find them. Question every assumption. Test edge cases mentally or actually. **Verify that the implementation matches the specification, not just that it doesn't crash.**

---

## Phase 1: Static Analysis Checklist

Verify every AGENTS.md requirement explicitly:

### 1.1 Documentation Compliance

For **every public class, interface, enum, and inner class**:
```bash
# Search for public types without GroovyDoc
grep -B2 "^class\|^interface\|^enum\|static class" src/main/groovy/**/*.groovy | grep -v "/\*\*"
```

- [ ] Class-level GroovyDoc exists
- [ ] Public methods have GroovyDoc with `@param`, `@return`, `@throws`
- [ ] Complex algorithms have implementation comments

### 1.2 Idiomatic Groovy Patterns

Search for and flag:
```bash
grep -n "new ArrayList<>()\|new HashSet<>()\|new LinkedHashSet<>()\|Arrays.asList()" src/main/groovy/**/*.groovy
grep -n "\.doubleValue()\|\.intValue()" src/main/groovy/**/*.groovy  # Unnecessary unboxing
grep -n "case.*:" src/main/groovy/**/*.groovy  # Old-style switch (JDK 21+ should use ->)
```

Required patterns per AGENTS.md:
- `[]` instead of `new ArrayList<>()`
- `[] as Set` instead of `new HashSet<>()`
- `[*list]` instead of `new ArrayList<>(list)` (spread operator)
- Arrow syntax `case X ->` for switches (unless return-in-case needed)
- `==` instead of `.equals()`
- `"${var}"` instead of string concatenation

### 1.3 Type Safety

Search for Object abuse:
```bash
grep -n "Object " src/main/groovy/**/*.groovy | grep -v "Object>" | grep -v "import"
```

- [ ] No `Object` parameters when specific types are known
- [ ] No `Object` return types when specific types are known
- [ ] Use method overloads instead of `Object` with instanceof chains

### 1.4 @CompileStatic Compliance

```bash
grep -L "@CompileStatic" src/main/groovy/**/*.groovy | grep -v test
```

- [ ] All production classes have `@CompileStatic`
- [ ] Only use `@CompileDynamic` when explicitly justified

---

## Phase 2: Semantic Correctness Testing (CRITICAL)

**This phase finds logic bugs that don't crash but produce wrong results.**

### 2.1 Round-trip Verification

For parser/normalizer implementations, verify:

```groovy
// Input should match output for simple cases
input = 'y ~ x + z'
output = Formula.normalize(input).asFormulaString()
// If input != output, is the transformation correct and intentional?
```

### 2.2 Invalid Input Rejection

**Question:** Should this input be accepted?

Test cases that should **REJECT** with clear errors:
```groovy
// Response-side operations (if not supported)
Formula.normalize('y - z ~ x')        // Should error: response-side subtraction not supported
Formula.normalize('y + z ~ x')        // Should error: multiple responses not supported
Formula.normalize('y - 1 ~ x')        // Should error: response-side intercept control not supported

// Invalid nesting
Formula.normalize('y ~ / x')          // Should error: missing left operand
Formula.normalize('y ~ x :')          // Should error: missing right operand
```

**Red flag:** If malformed inputs produce valid-looking outputs instead of errors.

### 2.3 Reference Implementation Comparison

For domain-specific implementations (R-style formulas, statistical tests, etc.):

- [ ] Test against known reference behavior (R, Python, established libraries)
- [ ] Document intentional deviations from reference behavior
- [ ] Test common patterns from real-world datasets

Example for R-style formulas:
```groovy
// Real column names from datasets
Formula.parse('sepal.length ~ sepal.width')  // Dotted names common in R
Formula.parse('`gross margin` ~ revenue')     // Backtick quoting

// R-style update formulas
Formula.update('y ~ x + z', '. ~ . - z + w')   // Standard
Formula.update('y ~ x + z', '~ . - z + w')     // Omitting LHS (R allows this)
```

### 2.4 Side-specific Semantics

For two-sided formulas (response ~ predictors), verify:

```groovy
// Left side (response) should reject what right side accepts
Formula.normalize('y ~ 0 + x')        // OK: no intercept on predictor side
Formula.normalize('0 ~ x')            // Should this be OK? Probably not!
Formula.normalize('y - 1 ~ x')        // Should error: can't subtract from response

// Operators that differ by side
Formula.normalize('y ~ x - z')        // OK: remove z from model
Formula.normalize('y - z ~ x')        // Should error: can't subtract from response
```

**Check:** Is the parser correctly distinguishing response-side from predictor-side semantics?

### 2.5 Tokenization Ambiguity

Test token boundaries:
```groovy
// Dotted identifiers vs dot operator
Formula.parse('y.foo ~ x')            // Is 'y.foo' one identifier or 'y' '.' 'foo'?
Formula.parse('y . foo ~ x')          // Explicit dot operator

// Scientific notation edge cases
Formula.parse('y ~ 1e2')              // Exponent notation
Formula.parse('y ~ 1e')               // Invalid: incomplete exponent
Formula.parse('y ~ 1.5.6')            // Invalid: multiple decimals

// Backtick contents
Formula.parse('y ~ `x + y`')          // Should 'x + y' be literal name, not expression
```

---

## Phase 3: Edge Case Attack Testing

For **every public method**, mentally execute these inputs:

### 3.1 Null and Empty Inputs

| Input Type | Test Case | Expected Behavior |
|------------|-----------|-------------------|
| String | `null`, `""`, `"   "` | `IllegalArgumentException` or `FormulaParseException` |
| List/Map | `null`, `[]`, `[:]` | Same as above |
| Array | `null`, `[]` | Same as above |
| Number | `null`, `NaN`, `Infinity` | Validate appropriately |

Example for `Formula.parse(String formula)`:
```groovy
Formula.parse(null)        // Should throw, not NPE
Formula.parse("")          // Should throw descriptive error
Formula.parse("   ")       // Should throw (blank check)
Formula.parse("y ~ `foo")  // Unterminated backtick
```

### 3.2 Boundary Values

- Integers: `0`, `1`, `-1`, `Integer.MAX_VALUE`, `Integer.MIN_VALUE`
- Decimals: `0.0`, very small (`1e-10`), very large (`1e308`)
- String length: empty, single char, very long (10k+ chars)
- Collection size: empty, single element, very large

### 3.3 Malformed/Suspicious Input

- Unbalanced delimiters: `(`, `[`, `{`, `` ` ``
- Invalid escape sequences
- Unicode edge cases: emoji, zero-width spaces, combining chars
- Scientific notation edge cases: `1e`, `1e-`, `e10`, `1.5.6`

### 3.4 Silent Behavior Check

Look for code that **discards input without warning**:

```groovy
// In FormulaSupport.expandTerms():
if (expression instanceof FormulaExpression.NumberLiteral) {
  return [] as LinkedHashSet<FormulaTerm>  // SILENT DROP - flag this!
}
```

Question: Should this warn? Throw? Is this expected per R semantics?

---

## Phase 4: Constructor Safety Audit

For **every public constructor** in the PR:

```groovy
class ParsedFormula {
  ParsedFormula(String source, FormulaExpression response, FormulaExpression predictors) {
    // Check: Are these validated?
    this.source = source        // Could be null
    this.response = response    // Could be null
    this.predictors = predictors // Could be null
  }
}
```

Checklist:
- [ ] Null validation on all reference parameters
- [ ] Empty string validation where applicable
- [ ] Range validation on numeric parameters
- [ ] Defensive copying of mutable inputs (`List.copyOf()`, `.clone()`)
- [ ] Immutable fields marked `final`

---

## Phase 5: API Surface Validation

### 5.1 String Interpolation Traps

Check for dangerous string interpolation:
```groovy
// In Formula.of():
static ParsedFormula of(String response, String predictors) {
  parse("${response} ~ ${predictors}")  // If response is null, becomes "null ~ x"!
}
```

Verify: Does this handle null gracefully or produce garbage?

### 5.2 Fluent/Chaining API Safety

For builder patterns or fluent APIs:
```groovy
builder.setX(null).setY(5).build()  // Does setX(null) throw or accept?
```

### 5.3 Exception Quality

Verify exceptions are:
- Descriptive (not raw NPE)
- Include context (position in string, invalid value)
- Use appropriate type (`IllegalArgumentException`, `IllegalStateException`, custom)

Example of good:
```groovy
throw new FormulaParseException(
  "Expected ')' to close grouped expression at position ${pos}\n${source}\n${' ' * pos}^", 
  pos
)
```

---

## Phase 6: Test Coverage Verification

### 6.1 Run All Tests
```bash
./gradlew :module:test --tests 'package.*' -g ./.gradle-user
```

### 6.2 Identify Untested Public API

For each public method, verify:
- [ ] Happy path tested
- [ ] Null input tested (or rejected at API boundary)
- [ ] Empty input tested
- [ ] Invalid format tested (if applicable)
- [ ] Boundary values tested

### 6.3 Common Test Gaps

Look for missing tests of:
- `toString()` implementations
- `equals()`/`hashCode()` contracts
- Exception message content (not just type)
- Resource cleanup (closeables, streams)
- Thread safety (if applicable)

### 6.4 Semantic Test Gaps (CRITICAL)

**Test that invalid inputs are rejected, not silently converted:**

```groovy
// BAD: This test passes but behavior is wrong
void testResponseSideSubtraction() {
  def result = Formula.normalize('y - z ~ x')
  assertEquals('y ~ 1 + x', result.asFormulaString())  // Silent data loss!
}

// GOOD: Invalid input should error
void testRejectsResponseSideSubtraction() {
  assertThrows(FormulaParseException) {
    Formula.normalize('y - z ~ x')
  }
}
```

---

## Phase 7: Architecture & Design Review

### 7.1 Immutability

Check mutable state:
```bash
grep -n "List<\|Map<\|Set<" src/main/groovy/**/*.groovy | grep -v "final"
```

- [ ] Public fields should be rare
- [ ] Returned collections should be unmodifiable or defensive copies
- [ ] Internal state changes are thread-safe (or documented as not thread-safe)

### 7.2 Encapsulation

- [ ] Internal classes marked `@PackageScope` or package-private
- [ ] No implementation details leaked in public API
- [ ] Utility classes have private constructors

### 7.3 Logging

Per AGENTS.md: Use `se.alipsa.matrix.core.util.Logger`, not println/log4j/SLF4J:
```bash
grep -rn "println\|System.out\|System.err\|LoggerFactory\|log4j" src/main/groovy/
```

---

## Phase 8: Domain-Specific Validation

### 8.1 Formula/Grammar Implementations

Additional checks for parser/grammar implementations:

```groovy
// 1. Operator precedence correctness
Formula.parse('y ~ a + b * c')       // Should parse as a + (b * c) or (a + b) * c?
Formula.parse('y ~ a : b ^ 2')       // Should parse as (a : b) ^ 2 or a : (b ^ 2)?

// 2. Associativity
Formula.parse('y ~ a - b - c')       // Should be (a - b) - c, not a - (b - c)
Formula.parse('y ~ a / b / c')       // Nesting associativity

// 3. Whitespace handling
Formula.parse('y~x+z')               // Should work without spaces
Formula.parse('y ~ x   +   z')       // Should work with extra spaces

// 4. Comment/documentation examples actually work
// Copy examples from GroovyDoc and verify they produce expected output
```

### 8.2 Statistical Implementations

For statistical classes:
- [ ] Rejects insufficient data (n < minimum required)
- [ ] Handles constant/zero-variance data appropriately
- [ ] Validates probability inputs in [0, 1] range
- [ ] Checks for numerical instability (division by zero, log(0))

### 8.3 Chart/Visualization

For charting code:
- [ ] Never use `Object` for aesthetic parameters (use typed overloads)
- [ ] Color handling uses shared utilities
- [ ] SVG output tests use direct object access when possible
- [ ] Scale implementations handle `NaN`, `null`, `Infinity`

---

## Phase 9: Security Spot Check

Quick security audit:
- [ ] No `eval()` or dynamic code execution
- [ ] No external command execution
- [ ] Path traversal protection (if file paths accepted)
- [ ] No sensitive data in logs/exceptions
- [ ] Input size limits (if processing user input)

---

## Review Output Format

Structure findings as:

```markdown
## Critical Issues (X found)

1. **[Brief name]**
   - Location: `File.groovy:line`
   - Problem: One-line description
   - Reproduction: Specific input that triggers the issue
   - Fix: Specific recommendation

## Important Issues (X found)

## Minor Issues (X found)

## Test Coverage Gaps

| Priority | Gap | Suggested Test |
|----------|-----|----------------|
| 1 | ... | ... |
```

---

## Self-Correction Checklist

Before submitting review, verify:

- [ ] Did I check EVERY public constructor for null validation?
- [ ] Did I test null inputs on EVERY public method?
- [ ] Did I search for AGENTS.md anti-patterns?
- [ ] Did I question any silent data dropping?
- [ ] Did I verify GroovyDoc on ALL public types?
- [ ] **Did I verify that invalid inputs are REJECTED, not silently converted?**
- [ ] **Did I test against real-world usage patterns (dotted names, update syntax)?**
- [ ] **Did I verify response-side vs predictor-side semantics are correctly handled?**
- [ ] Did I actually run the tests?
- [ ] Did I verify build passes with spotless/codenarc?

---

## Project-Specific Context

### matrix-stats Patterns

When reviewing matrix-stats:
- Statistical classes often need to reject constant/zero-variance data
- R-style formulas have specific semantics (intercept handling, `^` for interactions)
- **CRITICAL:** Response-side operators differ from predictor-side
- **CRITICAL:** Dotted identifiers (`sepal.length`) must work
- Distribution classes need validation for probability range [0,1]
- All new code should remove/replace commons-math3 dependencies

### matrix-charts/matrix-ggplot Patterns

When reviewing charting code:
- Never use `Object` for aesthetic parameters (use typed overloads)
- Color handling should use shared utilities
- SVG output tests should use direct object access, not `toXml()` when possible
- Scale implementations must handle `NaN`, `null`, and `Infinity`

---

## Tools to Use

```bash
# Run all checks before finalizing review
./gradlew :module:spotlessCheck :module:codenarcMain :module:test -g ./.gradle-user

# Find potential issues
grep -rn "TODO\|FIXME\|XXX" src/main/groovy/  # Unresolved items
grep -rn "throw new RuntimeException" src/main/groovy/  # Should be specific type
find src/main/groovy -name "*.groovy" -exec grep -L "@CompileStatic" {} \;  # Missing annotation

# Test semantic edge cases (add to test file temporarily)
echo "Testing semantic edge cases..."
```

---

## Remember

> The goal is not to find *something* to criticize. The goal is to ensure no preventable bugs reach production.

> **Silent data corruption is worse than a crash.** Always verify that invalid inputs are rejected, not converted to something else.

Be thorough. Be skeptical. Be helpful.
