# Strongly Typed Columns - Analysis & Recommendations

## Executive Summary

**Current State**: Columns are loosely typed - they have a `type` field that stores metadata but does not enforce type constraints. 
Any value can be added to any column regardless of its declared type.

**Recommendation**: Introduce **optional** strong typing through a new `TypedColumn<T>` class while keeping the existing `Column` class for backward compatibility. 
This hybrid approach provides benefits without breaking existing code.

---

## Current Implementation Analysis

### What We Have Now

```groovy
@CompileStatic
class Column extends ArrayList {
  String name
  Class type  // Metadata only - not enforced!

  // No validation when adding elements
  Column(Collection c, Class type) {
    super(c)
    this.type = type  // Type is just stored, never checked
  }
}
```

**Key Finding**: The `type` field is purely **descriptive**, not **prescriptive**.

### Type Field Usage Patterns

Found 13 locations where `column.type` is accessed/modified:

1. **Matrix.groovy:308, 352** - `apply()` methods update type based on closure return values
2. **Matrix.groovy:638, 666, 1927** - Type assignment during column operations
3. **Matrix.groovy:1714, 1722** - Type preserved when adding columns
4. **Stat.groovy:63** - Type read for statistics calculations
5. **Column constructors** - Type stored but not enforced

**Pattern**: Type is treated as **documentation** that gets updated when content changes, 
not as a **constraint** that prevents invalid data.

---

## Current Type Enforcement (or Lack Thereof)

### Where NO Validation Occurs

1. **Direct ArrayList operations**:
   ```groovy
   Column intCol = new Column(Integer)
   intCol.add("string")  // ✓ Allowed - no validation
   intCol << LocalDate.now()  // ✓ Allowed - no validation
   ```

2. **Matrix element assignment**:
   ```groovy
   matrix[0, 2] = value  // -> column.set(rowIndex, value)
   // No type checking in putAt(Number, Number, Object)
   ```

3. **Row operations**:
   ```groovy
   matrix.addRow([1, "foo", null, LocalDate.now()])
   // Each value added directly: mColumns[c].add(row[c])
   ```

4. **Arithmetic operations**:
   ```groovy
   Column mixed = new Column([1, "2", 3.0, null])
   mixed * 2  // Runtime errors on "2" only when operation executes
   ```

### Where Type Conversion Happens (But Not Enforcement)

1. **ListConverter.convert()** - Explicit type conversion with error handling
2. **Matrix.apply()** - Type field updated to match actual values after transformation
3. **Column.getAt(Number, Class)** - On-demand conversion when retrieving

**Key Insight**: Type conversion is **opt-in**, not automatic. 
Users explicitly request conversion via `ListConverter` or get typed values via `getAt(index, Class)`.

---

## Real-World Use Cases

### Case 1: Matrix-Charts Data Access

```groovy
// From GeomParallel.groovy:213
List<BigDecimal> values = data.column(col)
    .findAll { it instanceof Number }
    .collect { it as BigDecimal }
```

**Pattern**: Code defensively filters and converts, assuming mixed types possible.

**Impact of Strong Typing**: Could simplify to:
```groovy
TypedColumn<Number> values = data.column(col)
// Guaranteed to be numeric, no filtering needed
```

### Case 2: Data Transformation (apply)

```groovy
// From Matrix.groovy:294-312
Matrix apply(int columnNumber, Closure function) {
  def col = new Column()
  Class updatedClass = null
  column(columnNumber).each { v ->
    def val = function.call(v)
    if (updatedClass == null && val != null) {
      updatedClass = val.class
    }
    col.add(val)
  }
  col.type = updatedClass  // Type determined by actual values
  mColumns[columnNumber] = col
  this
}
```

**Pattern**: Type changes dynamically based on transformation results.

**Impact of Strong Typing**: Would need special handling for type-changing transformations.

### Case 3: Null Handling

```groovy
// From PutAtTest.groovy:125-130
empData[1, 0] = null  // Integer column
assert empData[1, 0] == null
empData["salary"][0] = null  // Number column
assert empData["salary"][0] == null
```

**Pattern**: All columns support nulls regardless of type.

**Impact of Strong Typing**: Must support `TypedColumn<T>` allowing nulls (like `List<Integer>` in Java).

### Case 4: Mixed Numeric Types

```groovy
// Common in data processing
Column numbers = new Column([1, 2.5, 3, 4.0, new BigDecimal("5.5")])
numbers * 2  // Works - Groovy handles coercion
```

**Pattern**: Mixed numeric types (Integer, Double, BigDecimal) coexist and work together via Groovy's type coercion.

**Impact of Strong Typing**: Would need `TypedColumn<Number>` to handle this, but loses precision guarantees (can't enforce "only BigDecimal").

---

## Benefits of Strong Typing

### 1. Data Integrity & Early Error Detection

**Current Problem**:
```groovy
Column dateCol = new Column(LocalDate)
dateCol.add(LocalDate.now())  // ✓
dateCol.add("2024-01-20")     // ✓ Allowed - error only when accessed
dateCol.add(123)              // ✓ Allowed - error only when used
```

**With Strong Typing**:
```groovy
TypedColumn<LocalDate> dateCol = new TypedColumn<>(LocalDate)
dateCol.add(LocalDate.now())      // ✓
dateCol.add("2024-01-20")         // ✗ Compile-time error (with @CompileStatic)
dateCol.add(123)                  // ✗ Compile-time error
```

**Value**: Catch type errors at **insertion time** rather than **usage time**.

### 2. Better IDE Support

**Current**:
```groovy
Column col = matrix.column("age")
col.get(0).  // IDE suggests Object methods only
```

**With Generics**:
```groovy
TypedColumn<Integer> col = matrix.column("age")
col.get(0).  // IDE suggests Integer methods (intValue, compareTo, etc.)
```

**Value**: Autocomplete, refactoring tools, inline documentation.

### 3. Performance Optimization Potential

**Current**: All columns are `ArrayList<Object>`, boxing/unboxing overhead.

**With Specialized Types**:
```groovy
class IntColumn extends TypedColumn<Integer> {
  private int[] primitiveArray  // No boxing for primitives

  Integer get(int index) { primitiveArray[index] }
  void add(Integer value) { /* grow array, store primitive */ }
}
```

**Value**: For numeric-heavy operations (statistics, charting), primitive arrays could be **2-10x faster** and use **50% less memory**.

### 4. API Clarity

**Current**:
```groovy
Matrix addColumn(String name, Class type = Object, List column)
// What does 'type' mean? Hint? Validation? Conversion target?
```

**With Strong Typing**:
```groovy
Matrix addColumn(String name, TypedColumn<?> column)
// Type is enforced by column itself - clearer contract
```

**Value**: Self-documenting APIs, explicit contracts.

### 5. Simplified Downstream Code

**Current** (from matrix-charts):
```groovy
List<?> groupColumn = data.column(categoryColumnName)
List<BigDecimal> values = data.column(col)
    .findAll { it instanceof Number }
    .collect { it as BigDecimal }
```

**With Strong Typing**:
```groovy
TypedColumn<String> groupColumn = data.column(categoryColumnName)
TypedColumn<Number> values = data.column(col)
// No filtering/conversion needed
```

**Value**: Less defensive programming, fewer runtime checks.

---

## Drawbacks of Strong Typing

### 1. Loss of Flexibility

**Current Power**:
```groovy
// Start with strings, convert to dates
matrix.apply("date_str") { it }  // String -> String
matrix.apply("date_str") { LocalDate.parse(it) }  // String -> LocalDate
// Type changes naturally
```

**With Strong Typing**:
```groovy
TypedColumn<String> dateStr = matrix.column("date_str")
TypedColumn<LocalDate> dates = dateStr.map { LocalDate.parse(it) }
// Need new column or explicit type change mechanism
```

**Impact**: Type-changing transformations become more complex.

### 2. Null Handling Complexity

**Question**: How to handle nulls with primitives?

```groovy
TypedColumn<Integer> ages = new TypedColumn<>(Integer)
ages.add(25)    // ✓
ages.add(null)  // What happens?
```

**Options**:
- **Option A**: Allow nulls (use `Integer`, not `int`) - standard Java approach
- **Option B**: Use sentinel value (like -1) - error-prone
- **Option C**: Separate `hasNull` flag array - memory overhead

**Impact**: Null handling adds complexity to implementation.

### 3. Backward Compatibility

**Problem**: Existing code assumes loose typing:

```groovy
// Existing user code
Column col = new Column([1, 2, 3])
col.add("four")  // Works now, would break with enforcement
```

**Solutions**:
- Keep `Column` as-is, add new `TypedColumn`
- Add enforcement flag: `Column(type, strict: true)`
- Major version bump (4.0.0) with breaking changes

**Impact**: Migration burden on users.

### 4. Implementation Complexity

**Challenges**:

1. **Generic Type Erasure**: Java/Groovy erase generics at runtime
   ```groovy
   TypedColumn<Integer> col = new TypedColumn<>(Integer)
   // At runtime, type info lost - need to store Class<T>
   ```

2. **Coercion Rules**: Groovy's flexible type coercion conflicts with strict typing
   ```groovy
   TypedColumn<BigDecimal> nums = new TypedColumn<>(BigDecimal)
   nums.add(1)      // int -> BigDecimal? Auto-convert or error?
   nums.add(1.5)    // double -> BigDecimal?
   nums.add("1.5")  // String -> BigDecimal?
   ```

3. **Arithmetic Operations**: Column operations create new columns - what type?
   ```groovy
   TypedColumn<Integer> a = [1, 2, 3]
   TypedColumn<?> result = a * 2.5  // Integer? Double? BigDecimal?
   ```

**Impact**: Non-trivial design decisions for type coercion and operation semantics.

### 5. Type Inference Limitations

**Challenge**: Matrix builder can't infer generic types from data:

```groovy
// Current
Matrix.builder().data(age: [25, 30, 35]).build()
// How does builder know ages should be TypedColumn<Integer>?

// Would need explicit typing
Matrix.builder()
  .typedColumn("age", Integer, [25, 30, 35])
  .build()
```

**Impact**: More verbose API for typed columns.

---

## Hybrid Approach: Recommended Solution

### Design: Keep Both Column Types

```groovy
// Existing - remains unchanged (loosely typed)
class Column extends ArrayList {
  String name
  Class type  // Metadata only
}

// New - strictly typed (opt-in)
class TypedColumn<T> extends ArrayList<T> {
  String name
  Class<T> type

  TypedColumn(Class<T> type) {
    this.type = type
  }

  @Override
  boolean add(T element) {
    if (element != null && !type.isAssignableFrom(element.class)) {
      throw new TypeMismatchException(
        "Cannot add ${element.class.simpleName} to column of type ${type.simpleName}"
      )
    }
    super.add(element)
  }

  // Override set, addAll, etc. with validation
}
```

### Usage Pattern

```groovy
// Option 1: Loose typing (backward compatible)
Matrix legacy = Matrix.builder()
  .data(age: [25, 30, 35])
  .build()
// Uses Column internally

// Option 2: Strict typing (opt-in)
Matrix strict = Matrix.builder()
  .typedColumn("age", Integer, [25, 30, 35])
  .typedColumn("name", String, ["Alice", "Bob", "Charlie"])
  .build()
// Uses TypedColumn<T> internally

// Option 3: Mixed (gradual migration)
Matrix mixed = Matrix.builder()
  .column("age", Integer, [25, 30, 35])  // Column (loose)
  .typedColumn("name", String, ["Alice", "Bob"])  // TypedColumn (strict)
  .build()
```

### Benefits of Hybrid Approach

1. **Zero Breaking Changes**: Existing code works as-is
2. **Gradual Adoption**: Users can opt-in column-by-column
3. **Clear Migration Path**: TypedColumn signals intent for strictness
4. **Best of Both Worlds**: Flexibility where needed, safety where wanted

---

## Implementation Recommendations

### Phase 1: Foundation (v3.7.0 or v4.0.0)

1. **Create TypedColumn class**:
   ```groovy
   class TypedColumn<T> extends ArrayList<T> {
     final Class<T> type
     final boolean allowNulls

     // Validated add/set operations
     // Type-safe getAt returning T
   }
   ```

2. **Add to MatrixBuilder**:
   ```groovy
   MatrixBuilder typedColumn(String name, Class<T> type, List<T> data) {
     // Create TypedColumn<T>
   }
   ```

3. **Update Matrix to support both**:
   ```groovy
   List column(String name)  // Returns List (could be Column or TypedColumn)
   <T> TypedColumn<T> typedColumn(String name, Class<T> type)  // Typed accessor
   ```

### Phase 2: Specialized Types (v4.1.0)

1. **Primitive-backed columns**:
   ```groovy
   class IntColumn extends TypedColumn<Integer>
   class DoubleColumn extends TypedColumn<Double>
   class BigDecimalColumn extends TypedColumn<BigDecimal>
   ```

2. **Builder auto-detection**:
   ```groovy
   Matrix.builder()
     .data(age: [25, 30, 35])  // Auto-creates IntColumn
     .data(salary: [1.5, 2.5]) // Auto-creates DoubleColumn
     .build()
   ```

### Phase 3: Enhanced API (v4.2.0)

1. **Type-safe Matrix operations**:
   ```groovy
   TypedMatrix<Integer> ages = matrix.select("age", Integer)
   TypedMatrix<String> names = matrix.select("name", String)
   ```

2. **Compile-time safety with @CompileStatic**:
   ```groovy
   @CompileStatic
   void process(TypedColumn<LocalDate> dates) {
     dates.each { LocalDate d ->  // Type known at compile time
       // IDE autocomplete works perfectly
     }
   }
   ```

---

## Trade-offs Summary

| Aspect              | Current (Loose)   | Proposed (Typed)                   | Hybrid Approach        |
|---------------------|-------------------|------------------------------------|------------------------|
| **Type Safety**     | Runtime only      | Compile-time (with @CompileStatic) | Both available         |
| **Flexibility**     | Maximum           | Reduced                            | Choose per column      |
| **Performance**     | Good (boxed)      | Excellent (primitives possible)    | Mixed                  |
| **API Complexity**  | Simple            | More complex                       | Gradual learning curve |
| **Migration Cost**  | N/A               | High                               | Low (opt-in)           |
| **Error Detection** | Late (usage time) | Early (insertion time)             | User's choice          |
| **IDE Support**     | Limited           | Excellent                          | Mixed                  |
| **Null Handling**   | Natural           | Explicit                           | Both patterns          |

---

## Specific Scenarios Analysis

### Scenario 1: Data Import from CSV

**Current**:
```groovy
Matrix data = Csv.read("data.csv")
// All columns are Object, types inferred but not enforced
```

**Challenge**: CSV values are strings - what type should columns be?

**Typed Solution**:
```groovy
Matrix data = Csv.read("data.csv")
  .withTypedColumn("age", Integer)
  .withTypedColumn("date", LocalDate)
  .build()
// Conversion + validation at import time
```

**Benefit**: Catch invalid data immediately, not during analysis.

### Scenario 2: Statistical Operations

**Current**:
```groovy
Column ages = matrix.column("age")
double mean = Stat.mean(ages)  // Assumes numeric, fails at runtime if not
```

**Typed Solution**:
```groovy
TypedColumn<Integer> ages = matrix.typedColumn("age", Integer)
double mean = Stat.mean(ages)  // Compiler knows it's safe
```

**Benefit**: Compile-time guarantee that statistical operations receive numeric data.

### Scenario 3: Chart Rendering

**Current** (from matrix-charts):
```groovy
List<?> values = data.column(columnName)
// Defensive filtering
List<BigDecimal> nums = values.findAll { it instanceof Number }
                             .collect { it as BigDecimal }
```

**Typed Solution**:
```groovy
TypedColumn<Number> values = data.typedColumn(columnName, Number)
// No filtering needed - guaranteed to be numeric
```

**Benefit**: Simpler, faster code; fewer runtime checks.

---

## Conclusion & Recommendation

### Recommended Approach: **Hybrid (TypedColumn + Column)**

1. **Keep Column as-is** - No breaking changes, maintains flexibility
2. **Introduce TypedColumn<T>** - Opt-in strict typing for safety
3. **Gradual migration** - Users choose strictness level per column
4. **Future optimization** - Specialized primitive columns for performance

### Why This Works

- **Backward compatible**: Existing code unaffected
- **Progressive enhancement**: New code can use typed columns
- **Pragmatic**: Balances safety with flexibility
- **Future-proof**: Foundation for performance optimizations

### Implementation Priority

**High Priority** (v3.7.0 or v4.0.0):
- [ ] Implement `TypedColumn<T>` with validation
- [ ] Add `MatrixBuilder.typedColumn()` method
- [ ] Update documentation with typing guide

**Medium Priority** (v4.1.0):
- [ ] Primitive-backed columns (IntColumn, DoubleColumn)
- [ ] Auto-detection in builder for common types
- [ ] Performance benchmarks vs. loose typing

**Low Priority** (v4.2.0):
- [ ] TypedMatrix wrapper for full type safety
- [ ] Enhanced compile-time checking support
- [ ] Migration tooling (Column -> TypedColumn converter)

### Success Metrics

1. **Zero breaking changes** in existing test suite
2. **Opt-in adoption** in new examples/tutorials
3. **Performance improvement** (target: 20% faster for numeric operations)
4. **User feedback** positive on type safety features

---

## Appendix: Code Examples

### Example A: Current Loose Typing Issues

```groovy
// Problem: Type errors caught late
Matrix data = Matrix.builder()
  .data(age: [25, 30, "thirty-five"])  // ✓ Builds successfully
  .build()

double avgAge = Stat.mean(data.column("age"))
// ✗ Runtime error: "Cannot cast String to Number"
```

### Example B: With TypedColumn

```groovy
// Benefit: Type errors caught early
Matrix data = Matrix.builder()
  .typedColumn("age", Integer, [25, 30, "thirty-five"])
  // ✗ Immediate error: "Cannot add String to Integer column"
  .build()

// This never executes - error already caught
double avgAge = Stat.mean(data.typedColumn("age", Integer))
```

### Example C: Hybrid Approach

```groovy
// Best of both worlds
Matrix data = Matrix.builder()
  .typedColumn("age", Integer, [25, 30, 35])      // Strict
  .column("notes", String, ["foo", 123, null])    // Loose
  .build()

// Type-safe operations where needed
TypedColumn<Integer> ages = data.typedColumn("age", Integer)
ages.each { Integer age -> /* compiler knows type */ }

// Flexible operations where convenient
List notes = data.column("notes")  // Mixed types OK
```

---

**Document Version**: 1.0
**Date**: 2026-01-20
**Author**: Claude Code Analysis
**Target Version**: matrix-core 3.7.0+
