# Kimi Code Review Guidelines

This document provides instructions for conducting thorough code reviews that match or exceed the quality of other AI code reviewers (e.g., Claude, Copilot, Codex).

## Review Philosophy

Don't just read code—**attack it**. Assume bugs exist and find them. Question every assumption. Test edge cases mentally or actually. **Verify that the implementation matches the specification, not just that it doesn't crash.**

## Review Workflow

### Branch Management

**CRITICAL:** Stay on the development branch for the entire review session. Do not switch back to `main` or any other branch until:
- The user explicitly says the PR is merged
- The user asks you to switch branches
- The review is completely finished and the user is done working on that PR

**Why:** Switching branches disrupts the development workflow, loses uncommitted context, and makes it harder to verify incremental fixes.

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

## Phase 2: Specification Compliance (CRITICAL)

**Verify the implementation matches the documented requirements.**

### 2.1 Roadmap/Requirements Cross-Reference

When a PR claims to implement a specific section or feature:

1. **Read the requirements first** - Don't review code in isolation
2. **Check every bullet point** in the requirements against the implementation
3. **Question rejections** - If the code throws an error, verify it SHOULD error per the spec

**Example:**
```
Roadmap says: "Supported syntax: dot expansion (.), interaction expansion (^)"
Test shows: "Dot expansion with interaction power (^) cannot be normalized"
Question: Does the roadmap imply these should work together? (.^2)
If yes: Implementation is incomplete
If no: Roadmap should document the limitation
```

**Red flags:**
- Requirements say "supports X and Y" but code rejects "X + Y" combination
- Tests validate rejection of syntax that should work per requirements
- Requirements are updated to match implementation rather than vice versa

### 2.2 Feature Completeness Matrix

For features with multiple components, verify the matrix of combinations:

| Feature A | Feature B | Status | Test |
|-----------|-----------|--------|------|
| Supported | Supported | Should work | `testAWithB()` |
| Supported | Rejected | Should error | Document why |
| Rejected | Rejected | Should error | N/A |

**Example:**
- `.` (dot expansion) = Supported
- `^` (power) = Supported
- `.^2` (combination) = ??? (Must check requirements)

---

## Phase 3: Semantic Correctness Testing (CRITICAL)

**This phase finds logic bugs that don't crash but produce wrong results.**

### 2.1 Round-trip Verification

For parser/normalizer implementations, verify that transformations are intentional:

```groovy
// Example: A normalizer may change syntax while preserving semantics
input = 'y ~ x + z'
output = normalize(input).toString()
// If input != output, is the transformation:
//   1. Correct (semantics preserved)?
//   2. Intentional (documented behavior)?
//   3. Consistent (same input always produces same output)?
```

### 2.2 Invalid Input Rejection

**Question:** Should this input be accepted?

Test cases that should **REJECT** with clear errors:

```groovy
// Example: In a formula DSL, response-side should reject predictor-side operators
// These should error if response-side operations are not supported:
//   'y - z ~ x'       // Subtraction on response
//   'y + z ~ x'       // Multiple responses
//   'y - 1 ~ x'       // Intercept control on response

// Example: Missing operands
//   'y ~ / x'         // Missing left operand
//   'y ~ x :'         // Missing right operand
```

**Red flag:** If malformed inputs produce valid-looking outputs instead of errors.

### 2.3 Reference Implementation Comparison

For domain-specific implementations (parsers, statistical tests, protocol handlers):

- [ ] Test against known reference behavior (reference implementations, standards, specifications)
- [ ] Document intentional deviations from reference behavior
- [ ] Test common patterns from real-world usage

Example:
```groovy
// If implementing R-style formula parsing:
// Test real column naming patterns from R datasets:
//   'sepal.length ~ sepal.width'  // Dotted identifiers
//   '`gross margin` ~ revenue'     // Backtick quoting
//   'y ~ x + z'                    // Basic form
//   update(base, '~ . - z + w')    // Shorthand syntax
```

### 2.4 Context-Sensitive Semantics

For DSLs with context-sensitive semantics (e.g., left-side vs right-side), verify that operators/keywords have the correct meaning in each context:

```groovy
// Example: Formula parsing has different semantics on each side of '~'
// Right side (predictors): accepts operators like +, -, *, /, ^
// Left side (response): should reject most operators

// Valid on right side, invalid on left side:
//   'y ~ x - z'     // OK: remove z from predictors
//   'y - z ~ x'     // ERROR: can't subtract from response

// Valid on right side, invalid on left side:
//   'y ~ 0 + x'     // OK: no intercept
//   '0 ~ x'         // ERROR: literal 0 as response
```

**Check:** Does the implementation correctly track context and apply appropriate validation rules?

### 2.5 Tokenization Ambiguity

Test token boundaries where lexical elements might be confused:

```groovy
// Example: Dotted identifiers vs dot operator in formula parsing
// Is 'y.foo' one identifier or 'y' '.' 'foo'?
// Test both: 'y.foo ~ x' vs 'y . foo ~ x'

// General cases to consider:
// - Special characters in identifiers (dots, underscores, numbers)
// - Multi-character operators vs sequences of single-character operators
// - Escaped/quoted content that should be treated as literal
// - Whitespace sensitivity (when present vs absent)
```

### 2.6 Syntax Variation Testing (CRITICAL)

Test ALL variations of supported syntax, not just the canonical form:

**Principle:** If a feature has multiple syntactic forms, test each one.

```groovy
// Example: R-style formula update syntax has multiple forms
// All of these should work equivalently:
// update(base, '. ~ . - z + w')      // Standard: explicit dot
// update(base, '~ . - z + w')        // Shorthand: omitted LHS
// update(base, '. ~ - z + w')        // Without dot on RHS
```

**Key insight:** Different user communities (R, Python, SAS, SQL) have different syntax conventions. Users will try variations based on their background. Test the variations that users from each background might try.

### 2.7 Real-World Pattern Testing

Test with realistic data patterns, not just synthetic examples:

```groovy
// Example: Formula parsing should handle real column naming patterns
// Synthetic: 'y ~ x + z'
// Real-world: 'sepal.length ~ sepal.width' (dot-separated)
// Real-world: '`Gross Margin` ~ Revenue' (spaces in names)
// Real-world: 'log(Income) ~ Age + Education' (transforms + multiple terms)
```

Use patterns from:
- Famous public datasets in the domain
- The project's own example data
- Documentation examples from reference implementations
- Community usage patterns (Stack Overflow, forums)

### 2.8 Feature Interaction Testing (CRITICAL)

**Test combinations of features**, not just features in isolation:

```groovy
// Example: Dot expansion + Power operator in formulas
// Tested separately:
//   normalize('y ~ .')      // Dot expansion
//   normalize('y ~ (a+b)^2') // Power operator
// Must ALSO test combined:
//   normalize('y ~ .^2')    // Dot + Power interaction

// Example: Grouping + Operators
//   normalize('y ~ (a + b)^2 - a:b')  // Power then subtraction
//   normalize('y ~ (a + b) * c')      // Grouping then expansion
```

**Key insight:** Features that work independently may fail when combined. The matrix of feature combinations must be tested.

### 2.9 Rejection Policy Completeness

When a feature should reject certain inputs, test ALL syntactic forms of that invalid input:

```groovy
// Example: Multi-response formulas should be rejected
// Same conceptual error, different syntax:
//   'y1 + y2 ~ x'           // Binary operator
//   'cbind(y1, y2) ~ x'     // Function call
//   'c(y1, y2) ~ x'         // Alternative function name
//   '(y1 + y2) ~ x'         // Grouped expression

// Example: Invalid response-side operations
//   'y - z ~ x'             // Subtraction
//   'y * z ~ x'             // Multiplication
//   'y:z ~ x'               // Interaction syntax
```

**Check:** Is the rejection logic consistent across all syntactic forms of the same conceptual error?

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

Example:
```groovy
// For a parser method:
parse(null)                // Should throw, not NPE
parse("")                  // Should throw descriptive error
parse("   ")               // Should throw (blank check)
parse("unterminated `foo") // Unterminated quote/escape
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
void testInvalidOperation() {
  def result = parser.parse('invalid - operation')
  assertEquals('simplified', result.toString())  // Silent data loss!
}

// GOOD: Invalid input should error
void testRejectsInvalidOperation() {
  assertThrows(ParseException) {
    parser.parse('invalid - operation')
  }
}
```

**Key rule:** Tests should verify that:
1. Valid inputs produce correct outputs
2. Invalid inputs produce errors (not garbage outputs)
3. Edge cases are handled consistently

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

### 8.1 Parser/Grammar Implementations

Additional checks for parser/grammar implementations:

```groovy
// 1. Operator precedence correctness
// Example: 'a + b * c' should parse as a + (b * c), not (a + b) * c

// 2. Associativity
// Example: 'a - b - c' should be (a - b) - c, not a - (b - c)

// 3. Whitespace handling
// Test both: 'a+b' and 'a + b' should be equivalent

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

**Output format:**
- Only list findings that require action
- Group related issues by type (e.g., "Documentation Defects", "Null Safety Issues")
- Provide exact file paths and line numbers
- Include ready-to-paste code snippets for fixes
- Include exact test commands to verify fixes
- Omit "Good", "Correct", "Well done" assessments entirely
- Omit summary sections

Structure findings as:

```markdown
## Issue Group: [Descriptive Type]

**Impact:** Important Issues (N found) | Minor Issues (N found)

### Issue 1: [Brief name]
- Location: `File.groovy:line`
- Problem: One-line description
- Fix:
  ```groovy
  // ready-to-paste code
  ```

**Test command:**
```bash
./gradlew :module:test --tests 'TestClass.testMethod' -g ./.gradle-user
```

### Issue 2: [Brief name with test coverage gap]
- Location: `File.groovy:line`
- Problem: Missing test for edge case X
- Test to add:
  ```groovy
  @Test
  void testXYZ() {
    // test code
  }
  ```

**Test command:**
```bash
./gradlew :module:test --tests 'TestClass.testXYZ' -g ./.gradle-user
```
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
- **CRITICAL:** R-style formulas have complex context-sensitive semantics
  - Response-side vs predictor-side validation differs
  - Operators interact (e.g., dot expansion + power)
  - Multiple syntactic forms for same concept (multi-response)
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

## Post-Review Analysis: Learning from Misses

When other reviewers (Codex, Claude, human) find issues you missed, analyze why:

### Common Miss Patterns

| Miss Type | Example | Why It Happened | Prevention |
|-----------|---------|-----------------|------------|
| **Happy path bias** | Only tested `y + z ~ x` (multiple responses), missed `y - z ~ x` (invalid operation) | Focused on "does it work" not "does it fail correctly" | Test ALL operators on both sides of `~` |
| **Syntax variation gap** | Tested `. ~ . - z + w` but missed `~ . - z + w` | Assumed one form was sufficient | Test ALL documented syntax variations |
| **Real-world disconnect** | Tested `` `foo bar` `` but missed `sepal.length` | Used synthetic examples, not dataset patterns | Use actual column names from famous datasets |
| **Silent acceptance** | Didn't catch that invalid inputs produced valid-looking outputs | Only checked for crashes, not correctness | Verify invalid inputs produce errors, not garbage |
| **Feature interaction blindspot** | Tested `.` and `^` separately, missed `.^2` | Tested features in isolation | Phase 2.8: Test combinations of features |
| **Specification compliance gap** | Accepted `.^2` rejection without checking if roadmap requires it | Didn't cross-reference with requirements | Phase 2.1: Roadmap/requirements cross-reference |
| **Rejection policy gap** | Tested `y1 + y2 ~ x` but missed `cbind(y1, y2) ~ x` | Only tested one form of invalid input | Phase 2.9: Test ALL syntactic forms of invalid input |

### Analysis Template

When an issue is found post-review, document:
1. **The bug:** What was the incorrect behavior?
2. **The test that would have caught it:** What specific input triggers it?
3. **Why it was missed:** Which checklist item was insufficient?
4. **The fix:** What was added to prevent recurrence?

**Example Analysis:**

| Bug                                             | Test That Would Catch It                         | Miss Type                     | Prevention                                      |
|-------------------------------------------------|--------------------------------------------------|-------------------------------|-------------------------------------------------|
| Invalid operation accepted on wrong side of DSL | Test ALL operators in ALL contexts               | Context sensitivity gap       | Phase 2.4: Context-sensitive semantics          |
| Feature combination loses semantics             | Test features together, not just separately      | Feature interaction blindspot | Phase 2.8: Feature interaction testing          |
| Feature rejects valid combination per spec      | Check if roadmap/requirements say it should work | Spec compliance gap           | Phase 2.1: Roadmap/requirements cross-reference |
| Invalid input accepted in alternative syntax    | Test ALL syntactic forms of invalid input        | Rejection policy gap          | Phase 2.9: Rejection policy completeness        |
| Shorthand syntax rejected                       | Test ALL documented syntax variations            | Syntax variation gap          | Phase 2.6: Syntax variation testing             |
| Real-world patterns fail                        | Use actual dataset patterns, not synthetic       | Real-world disconnect         | Phase 2.7: Real-world pattern testing           |

**Detailed Example from PR #275 (formula parsing):**

*Example 1: Context-sensitive semantics*
- **Bug:** `Formula.normalize('y - z ~ x')` produced `'y ~ 1 + x'` instead of erroring
- **Root cause:** Response-side validation only checked for `+` (multi-response), not other operators
- **Why missed:** Happy path bias - tested what should work, not what should fail
- **Fix:** Added comprehensive response-side validation and `testRejectsInvalidResponseSideExpressions()`

*Example 2: Specification compliance*
- **Bug:** Roadmap claimed `.` and `^` were supported, but `.^2` was rejected
- **Root cause:** Didn't verify feature combinations against requirements
- **Why missed:** Assumed rejection was correct without checking roadmap
- **Fix:** Added Phase 2.1 to require roadmap cross-reference before accepting rejections

---

## Remember

> The goal is not to find *something* to criticize. The goal is to ensure no preventable bugs reach production.

> **Silent data corruption is worse than a crash.** Always verify that invalid inputs are rejected, not converted to something else.

Be thorough. Be skeptical. Be helpful.
