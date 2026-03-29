# Kimi Code Review Guidelines

This document provides instructions for conducting thorough code reviews that match or exceed the quality of other AI code reviewers (e.g., Claude, Copilot).

## Review Philosophy

Don't just read code—**attack it**. Assume bugs exist and find them. Question every assumption. Test edge cases mentally or actually.

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

## Phase 2: Edge Case Attack Testing

For **every public method**, mentally execute these inputs:

### 2.1 Null and Empty Inputs

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

### 2.2 Boundary Values

- Integers: `0`, `1`, `-1`, `Integer.MAX_VALUE`, `Integer.MIN_VALUE`
- Decimals: `0.0`, very small (`1e-10`), very large (`1e308`)
- String length: empty, single char, very long (10k+ chars)
- Collection size: empty, single element, very large

### 2.3 Malformed/Suspicious Input

- Unbalanced delimiters: `(`, `[`, `{`, `` ` ``
- Invalid escape sequences
- Unicode edge cases: emoji, zero-width spaces, combining chars
- Scientific notation edge cases: `1e`, `1e-`, `e10`, `1.5.6`

### 2.4 Silent Behavior Check

Look for code that **discards input without warning**:

```groovy
// In FormulaSupport.expandTerms():
if (expression instanceof FormulaExpression.NumberLiteral) {
  return [] as LinkedHashSet<FormulaTerm>  // SILENT DROP - flag this!
}
```

Question: Should this warn? Throw? Is this expected per R semantics?

---

## Phase 3: Constructor Safety Audit

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

## Phase 4: API Surface Validation

### 4.1 String Interpolation Traps

Check for dangerous string interpolation:
```groovy
// In Formula.of():
static ParsedFormula of(String response, String predictors) {
  parse("${response} ~ ${predictors}")  // If response is null, becomes "null ~ x"!
}
```

Verify: Does this handle null gracefully or produce garbage?

### 4.2 Fluent/Chaining API Safety

For builder patterns or fluent APIs:
```groovy
builder.setX(null).setY(5).build()  // Does setX(null) throw or accept?
```

### 4.3 Exception Quality

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

## Phase 5: Test Coverage Verification

### 5.1 Run All Tests
```bash
./gradlew :module:test --tests 'package.*' -g ./.gradle-user
```

### 5.2 Identify Untested Public API

For each public method, verify:
- [ ] Happy path tested
- [ ] Null input tested (or rejected at API boundary)
- [ ] Empty input tested
- [ ] Invalid format tested (if applicable)
- [ ] Boundary values tested

### 5.3 Common Test Gaps

Look for missing tests of:
- `toString()` implementations
- `equals()`/`hashCode()` contracts
- Exception message content (not just type)
- Resource cleanup (closeables, streams)
- Thread safety (if applicable)

---

## Phase 6: Architecture & Design Review

### 6.1 Immutability

Check mutable state:
```bash
grep -n "List<\|Map<\|Set<" src/main/groovy/**/*.groovy | grep -v "final"
```

- [ ] Public fields should be rare
- [ ] Returned collections should be unmodifiable or defensive copies
- [ ] Internal state changes are thread-safe (or documented as not thread-safe)

### 6.2 Encapsulation

- [ ] Internal classes marked `@PackageScope` or package-private
- [ ] No implementation details leaked in public API
- [ ] Utility classes have private constructors

### 6.3 Logging

Per AGENTS.md: Use `se.alipsa.matrix.core.util.Logger`, not println/log4j/SLF4J:
```bash
grep -rn "println\|System.out\|System.err\|LoggerFactory\|log4j" src/main/groovy/
```

---

## Phase 7: Security Spot Check

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
- [ ] Did I actually run the tests?
- [ ] Did I verify build passes with spotless/codenarc?

---

## Project-Specific Context

### matrix-stats Patterns

When reviewing matrix-stats:
- Statistical classes often need to reject constant/zero-variance data
- R-style formulas have specific semantics (intercept handling, `^` for interactions)
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
```

---

## Remember

> The goal is not to find *something* to criticize. The goal is to ensure no preventable bugs reach production.

Be thorough. Be skeptical. Be helpful.
