# ODS Optimization Opportunities
## Mapping Fastexcel Techniques to ODS Implementation

This document provides actionable optimization tasks derived from fastexcel analysis.

---

## Priority Ranking

| Priority | Task | Expected Impact | Complexity | ROI |
|----------|------|-----------------|------------|-----|
| ⭐⭐⭐ | Switch to Aalto StAX parser | 10-20% | Low | Very High |
| ⭐⭐⭐ | Optimize extractValue type dispatch | 15-20% | Medium | Very High |
| ⭐⭐ | Adaptive row capacity sizing | 5-10% | Low | High |
| ⭐⭐ | Optimize text collection loop | 10-15% | Medium | High |
| ⭐ | Cache URN constants locally | 3-5% | Low | Medium |
| ⭐ | StringBuilder pooling (if needed) | 5-10% | High | Low |

**Cumulative Expected Impact**: 40-60% faster ODS reading (6.89s → 2.8-4.1s on 50k x 12 benchmark)

---

## 1. Switch to Aalto StAX Parser ⭐⭐⭐

### Current Implementation
**File**: `OdsStreamDataReader.groovy:61`
```groovy
final XMLInputFactory factory = XmlSecurityUtil.newSecureInputFactory()
```

**File**: `XmlSecurityUtil.groovy` (assumed)
```groovy
static XMLInputFactory newSecureInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newInstance()
    // Security settings...
    return factory
}
```

### Problem
- Creates **new factory instance** for every ODS import
- Uses **JDK default StAX implementation** (Woodstox or com.sun.xml)
- JDK parsers are general-purpose, not optimized for speed

### Fastexcel Approach
**File**: `DefaultXMLInputFactory.java:6-14`
```java
static final XMLInputFactory factory = defaultXmlInputFactory();

private static XMLInputFactory defaultXmlInputFactory() {
    XMLInputFactory factory = new com.fasterxml.aalto.stax.InputFactoryImpl();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    return factory;
}
```

**Why Aalto is Faster**:
- Optimized for **high-throughput** parsing
- 10-30% faster than Woodstox or JDK StAX in benchmarks
- Lower memory overhead
- Better CPU cache utilization

### Implementation Plan

#### Step 1: Add Aalto Dependency
**File**: `matrix-spreadsheet/build.gradle`
```gradle
dependencies {
    implementation 'com.fasterxml.aalto:aalto-xml:1.3.2'
    // ... existing dependencies
}
```

#### Step 2: Create Optimized Factory (New File)
**File**: `matrix-spreadsheet/src/main/groovy/se/alipsa/matrix/spreadsheet/fastods/reader/OptimizedXMLInputFactory.groovy`
```groovy
package se.alipsa.matrix.spreadsheet.fastods.reader

import com.fasterxml.aalto.stax.InputFactoryImpl
import groovy.transform.CompileStatic

import javax.xml.stream.XMLInputFactory

@CompileStatic
final class OptimizedXMLInputFactory {
    /**
     * Shared, thread-safe XMLInputFactory configured for maximum performance.
     * Uses Aalto StAX parser (10-30% faster than JDK default) with security hardening.
     */
    static final XMLInputFactory INSTANCE = createFactory()

    private static XMLInputFactory createFactory() {
        XMLInputFactory factory = new InputFactoryImpl()
        // Security: disable DTD and external entities
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE)
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE)
        return factory
    }

    private OptimizedXMLInputFactory() {
        // Utility class - prevent instantiation
    }
}
```

#### Step 3: Update OdsStreamDataReader
**File**: `OdsStreamDataReader.groovy:60-63`

**Before**:
```groovy
Sheet processContent(final InputStream is, Object sheet, ...) {
    final XMLInputFactory factory = XmlSecurityUtil.newSecureInputFactory()
    final XMLStreamReader reader = factory.createXMLStreamReader(is)
    // ...
}
```

**After**:
```groovy
import static se.alipsa.matrix.spreadsheet.fastods.reader.OptimizedXMLInputFactory.INSTANCE

Sheet processContent(final InputStream is, Object sheet, ...) {
    final XMLStreamReader reader = INSTANCE.createXMLStreamReader(is)
    // ...
}
```

### Testing
- Run existing tests: `./gradlew :matrix-spreadsheet:test`
- Run benchmarks: `./gradlew :matrix-spreadsheet:spreadsheetBenchmark --rerun-tasks`
- Compare before/after timing

### Expected Impact
- **10-20% faster ODS read** (6.89s → 5.5-6.2s on 50k x 12)
- Zero risk - Aalto is a drop-in replacement
- Small dependency addition (~200KB jar)

---

## 2. Optimize extractValue Type Dispatch ⭐⭐⭐

### Current Implementation
**File**: `OdsStreamDataReader.groovy:246-293`

```groovy
private static Object extractValueInternal(final XMLStreamReader reader) {
    String valueType = reader.getAttributeValue(officeUrn, 'value-type')

    // Typed values via attributes
    if (valueType == 'boolean') {
        return Boolean.parseBoolean(reader.getAttributeValue(officeUrn, 'boolean-value'))
    } else if (valueType == 'float' || valueType == 'percentage' || valueType == 'currency') {
        String v = reader.getAttributeValue(officeUrn, 'value')
        return v != null ? asBigDecimal(v) : null
    } else if (valueType == 'date') {
        String v = reader.getAttributeValue(officeUrn, 'date-value')
        return (v != null && v.length() == 10) ? LocalDate.parse(v) : (v != null ? LocalDateTime.parse(v) : null)
    } else if (valueType == 'time') {
        String v = reader.getAttributeValue(officeUrn, 'time-value')
        return v != null ? Duration.parse(v) : null
    }

    // Fallback for strings/unknown types: collect <text:p> content
    StringBuilder text = new StringBuilder()
    while (reader.hasNext()) {
        reader.next()
        // ... complex text handling ...
    }
    String s = text.toString()
    return s.isEmpty() ? null : s
}
```

### Problems
1. **If-else chain** instead of switch (less JIT-friendly)
2. **StringBuilder allocated** even when not needed (typed values return early, but allocation happened)
3. **Repeated attribute lookups** (valueType read once, but value attributes read inside if blocks)
4. **Date parsing logic inline** (duplicates null checks and length checks)

### Fastexcel Approach
**File**: `RowSpliterator.java:330-357`

```java
private CellType parseType(String type) {
    switch (type) {
        case "b": return CellType.BOOLEAN;
        case "e": return CellType.ERROR;
        case "n": return CellType.NUMBER;
        // ...
    }
    throw new IllegalStateException("Unknown cell type : " + type);
}

private Function<String, ?> getParserForType(CellType type) {
    switch (type) {
        case BOOLEAN: return RowSpliterator::parseBoolean;
        case NUMBER: return RowSpliterator::parseNumber;
        // ...
    }
}
```

**Key Insight**: Separate **type dispatch** from **value extraction**

### Optimized Implementation

#### Refactored extractValue
**File**: `OdsStreamDataReader.groovy:246-293`

**Replace entire method with**:
```groovy
private static Object extractValueInternal(final XMLStreamReader reader) {
    String valueType = reader.getAttributeValue(officeUrn, 'value-type')

    // Fast path: typed values extracted from attributes (90% of cells)
    if (valueType != null) {
        switch (valueType) {
            case 'boolean':
                return extractBooleanValue(reader)
            case 'float':
            case 'percentage':
            case 'currency':
                return extractNumericValue(reader)
            case 'date':
                return extractDateValue(reader)
            case 'time':
                return extractTimeValue(reader)
            default:
                // fall through to text extraction
                break
        }
    }

    // Slow path: text content from child elements (10% of cells)
    return extractTextContent(reader)
}

private static Boolean extractBooleanValue(final XMLStreamReader reader) {
    String v = reader.getAttributeValue(officeUrn, 'boolean-value')
    return v != null ? Boolean.parseBoolean(v) : null
}

private static BigDecimal extractNumericValue(final XMLStreamReader reader) {
    String v = reader.getAttributeValue(officeUrn, 'value')
    return v != null ? asBigDecimal(v) : null
}

private static Object extractDateValue(final XMLStreamReader reader) {
    String v = reader.getAttributeValue(officeUrn, 'date-value')
    if (v == null) return null
    // Date format: YYYY-MM-DD (10 chars) vs DateTime: YYYY-MM-DDTHH:MM:SS
    return v.length() == 10 ? LocalDate.parse(v) : LocalDateTime.parse(v)
}

private static Duration extractTimeValue(final XMLStreamReader reader) {
    String v = reader.getAttributeValue(officeUrn, 'time-value')
    return v != null ? Duration.parse(v) : null
}

private static String extractTextContent(final XMLStreamReader reader) {
    StringBuilder text = new StringBuilder(64)  // Pre-allocate typical size
    while (reader.hasNext()) {
        reader.next()

        if (reader.isStartElement()) {
            // Handle text elements
            String localName = reader.localName
            if (localName == 'p') {
                if (text.length() > 0) text.append('\n')
            } else if (localName == 's') {
                int numSpaces = asInteger(reader.getAttributeValue(textUrn, 'c')) ?: 1
                text.append(' '.repeat(numSpaces))
            } else if (localName == 'line-break') {
                text.append('\n')
            } else if (localName == 'tab') {
                text.append('\t')
            }
        } else if (reader.isCharacters()) {
            text.append(reader.getText())
        } else if (reader.isEndElement() && reader.localName == 'table-cell') {
            break
        }
    }

    String s = text.toString()
    return s.isEmpty() ? null : s
}
```

### Benefits
1. **Switch statement** - JIT can optimize better than if-else chain
2. **No StringBuilder creation** for typed values (90% of cells)
3. **Separate methods** - easier to optimize individually, better for inlining
4. **Pre-allocated StringBuilder** with 64-char capacity for text (reduces resizing)
5. **Local variable for localName** - avoids repeated method calls

### Testing
- Add unit tests for each extract method
- Verify null handling, empty values, edge cases
- Run full test suite
- Benchmark before/after

### Expected Impact
- **15-20% faster ODS read** (extractValue dominates ~1.3-1.6s of 6.89s)
- After Aalto + this: **30-35% cumulative speedup** (6.89s → 4.5-4.8s)

---

## 3. Adaptive Row Capacity Sizing ⭐⭐

### Current Implementation
**File**: `OdsStreamDataReader.groovy:166-167`
```groovy
int expectedColumns = endColumn == Integer.MAX_VALUE ? 16
    : Math.max(0, endColumn - startColumn + 1)
List<Object> row = new ArrayList<>(expectedColumns)
```

### Problem
- **Static sizing** based on requested range
- When `endColumn = Integer.MAX_VALUE` (no limit), assumes 16 columns
- If sheet has 100 columns, ArrayList resizes repeatedly (expensive)
- If sheet has 3 columns, wastes space (minor issue)

### Fastexcel Approach
**File**: `RowSpliterator.java:34, 91, 106`
```java
private int rowCapacity = 16;  // Instance field

List<Cell> cells = new ArrayList<>(rowCapacity);
// ... parse row ...
rowCapacity = Math.max(rowCapacity, cells.size());  // Learn from actual data
```

**Key Insight**: Tracks max row size seen, uses for subsequent rows

### Optimized Implementation

**File**: `OdsStreamDataReader.groovy`

#### Step 1: Add Instance Field
```groovy
@CompileStatic
final class OdsStreamDataReader extends OdsDataReader {
    private static final Logger logger = LogManager.getLogger(OdsStreamDataReader)
    private static final int TRAILING_EMPTY_ROW_THRESHOLD = 1000

    // NEW: adaptive capacity tracking
    private int rowCapacity = 16

    // ... rest of class ...
}
```

#### Step 2: Update processSheet to Pass Reader Instance
**Current**: `processSheet` is static, can't access instance fields

**Solution**: Make `processSheet` instance method or pass capacity as parameter

**Option A**: Make processSheet instance method
```groovy
final Sheet processSheet(final XMLStreamReader reader, final int startRow, ...) {
    Sheet sheet = new Sheet()
    int rowCount = 1
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table')) {
        if (reader.isStartElement() && reader.localName == 'table-row') {
            // ... existing logic ...
            List<Object> rowValues = processRow(reader, startColumn, endColumn, rowCapacity)
            rowCapacity = Math.max(rowCapacity, rowValues.size())  // Learn
            // ... rest of logic ...
        }
    }
    return sheet
}
```

**Option B**: Pass capacity as mutable reference (less clean but keeps static)
```groovy
final static Sheet processSheet(final XMLStreamReader reader, ..., Holder<Integer> capacityRef) {
    int capacity = capacityRef.value
    // ... parse row ...
    List<Object> rowValues = processRow(reader, startColumn, endColumn, capacity)
    capacityRef.value = Math.max(capacity, rowValues.size())
    // ...
}
```

**Recommendation**: Option A (instance method) is cleaner

#### Step 3: Update processRow Signature
```groovy
private static List<Object> processRowInternal(final XMLStreamReader reader,
                                                final int startColumn,
                                                final int endColumn,
                                                final int initialCapacity) {
    List<Object> row = new ArrayList<>(initialCapacity)  // Use learned capacity
    // ... rest of logic unchanged ...
    return row
}
```

### Testing
- Verify capacity grows correctly
- Test with narrow sheets (3 cols), wide sheets (100 cols), mixed widths
- Benchmark before/after

### Expected Impact
- **5-10% faster ODS read** (reduces ArrayList resizing overhead)
- Most beneficial for wide sheets or when no endColumn specified
- After Aalto + extractValue + this: **35-40% cumulative**

---

## 4. Optimize Text Collection Loop ⭐⭐

### Current Implementation
**File**: `OdsStreamDataReader.groovy:266-293`
```groovy
StringBuilder text = new StringBuilder()
while (reader.hasNext()) {
    reader.next()

    if (reader.isStartElement()) {
        if (reader.localName == 'p') {
            if (text.length() > 0) text.append('\n')
        } else if (reader.localName == 's') {
            int numSpaces = asInteger(reader.getAttributeValue(textUrn, 'c')) ?: 1
            text.append(' '.repeat(numSpaces))
        } else if (reader.localName == 'line-break') {
            text.append('\n')
        } else if (reader.localName == 'tab') {
            text.append('\t')
        }
    } else if (reader.isCharacters()) {
        text.append(reader.getText())
    } else if (reader.isEndElement() && reader.localName == 'table-cell') {
        break
    }
}
```

### Problems
1. **Default StringBuilder capacity** (16 chars) - likely too small, causes resizing
2. **Repeated `reader.localName` calls** - property access overhead
3. **Multiple if-else checks** in hot loop
4. **Repeated `reader.isStartElement()` / `reader.isCharacters()` calls**

### Fastexcel Approach
**File**: `SimpleXmlReader.java:107-127`
```java
StringBuilder sb = new StringBuilder();  // Default capacity
int childElement = 1;
while (reader.hasNext()) {
    int type = reader.next();  // Cache event type
    if (type == XMLStreamReader.CDATA ||
        type == XMLStreamReader.CHARACTERS ||
        type == XMLStreamReader.SPACE) {
        sb.append(reader.getText());
    } else if (type == XMLStreamReader.START_ELEMENT) {
        // ... handle nested elements ...
    } else if (type == XMLStreamReader.END_ELEMENT) {
        // ... check for target end element ...
    }
}
```

**Key Insights**:
- Cache event type in local variable
- Consolidate character event types (CDATA | CHARACTERS | SPACE)
- Minimal branching

### Optimized Implementation

**Already improved in #2 optimization**, but additional refinements:

```groovy
private static String extractTextContent(final XMLStreamReader reader) {
    StringBuilder text = new StringBuilder(64)  // Pre-allocate typical cell text size

    while (reader.hasNext()) {
        int eventType = reader.next()  // Cache event type

        // Fast path: character data (most common in text cells)
        if (eventType == XMLStreamReader.CHARACTERS ||
            eventType == XMLStreamReader.CDATA ||
            eventType == XMLStreamReader.SPACE) {
            text.append(reader.getText())
            continue
        }

        // Element handling
        if (eventType == XMLStreamReader.START_ELEMENT) {
            String localName = reader.localName  // Cache once
            switch (localName) {
                case 'p':
                    if (text.length() > 0) text.append('\n')
                    break
                case 's':
                    int numSpaces = asInteger(reader.getAttributeValue(textUrn, 'c')) ?: 1
                    text.append(' '.repeat(numSpaces))
                    break
                case 'line-break':
                    text.append('\n')
                    break
                case 'tab':
                    text.append('\t')
                    break
            }
        } else if (eventType == XMLStreamReader.END_ELEMENT && reader.localName == 'table-cell') {
            break
        }
    }

    String s = text.toString()
    return s.isEmpty() ? null : s
}
```

### Benefits
1. **Pre-allocated capacity** (64 chars) - covers most cell text without resizing
2. **Cached event type** - single `reader.next()` call
3. **Consolidated character events** - single append path
4. **Switch on localName** - better than if-else for JIT
5. **Early continue** for character data - avoids subsequent checks

### Testing
- Test multi-paragraph cells (`<text:p>` elements)
- Test whitespace handling (`<text:s>`, tabs, line breaks)
- Test empty cells
- Benchmark before/after

### Expected Impact
- **10-15% faster text extraction** (affects ~10-20% of cells)
- After all previous optimizations: **40-50% cumulative speedup**

---

## 5. Cache URN Constants Locally ⭐

### Current Implementation
**File**: `OdsStreamDataReader.groovy:74, 104, 248, 276, etc.`
```groovy
String sheetName = reader.getAttributeValue(tableUrn, 'name')
// ...
int repeatRows = asInteger(reader.getAttributeValue(tableUrn, 'number-rows-repeated') ?: 1)
// ...
String valueType = reader.getAttributeValue(officeUrn, 'value-type')
// ...
int numSpaces = asInteger(reader.getAttributeValue(textUrn, 'c')) ?: 1
```

**File**: `OdsXmlUtil.groovy` (assumed)
```groovy
final static String tableUrn = 'urn:oasis:names:tc:opendocument:xmlns:table:1.0'
final static String officeUrn = 'urn:oasis:names:tc:opendocument:xmlns:office:1.0'
final static String textUrn = 'urn:oasis:names:tc:opendocument:xmlns:text:1.0'
```

### Problem
- **Field access** to static finals in OdsXmlUtil
- While JIT may inline, explicit local caching guarantees no overhead
- Repeated in hot loops (processRow, extractValue)

### Optimization
Cache URN constants at method entry

#### processSheet
```groovy
final static Sheet processSheet(final XMLStreamReader reader, ...) {
    // Cache URN constants
    final String TABLE_URN = tableUrn
    final String OFFICE_URN = officeUrn

    Sheet sheet = new Sheet()
    int rowCount = 1
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table')) {
        if (reader.isStartElement() && reader.localName == 'table-row') {
            int repeatRows = asInteger(reader.getAttributeValue(TABLE_URN, 'number-rows-repeated') ?: 1)
            // ... rest of method uses TABLE_URN instead of tableUrn ...
        }
    }
    return sheet
}
```

#### processRowInternal
```groovy
private static List<Object> processRowInternal(final XMLStreamReader reader, ...) {
    // Cache URN constant
    final String TABLE_URN = tableUrn

    List<Object> row = new ArrayList<>(initialCapacity)
    int columnCount = 1

    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table-row')) {
        if (reader.isStartElement() && reader.localName == 'table-cell') {
            int repeatColumns = asInteger(reader.getAttributeValue(TABLE_URN, 'number-columns-repeated') ?: 1)
            // ... rest uses TABLE_URN ...
        }
    }
    return row
}
```

#### extractValueInternal
```groovy
private static Object extractValueInternal(final XMLStreamReader reader) {
    // Cache URN constants
    final String OFFICE_URN = officeUrn
    final String TEXT_URN = textUrn

    String valueType = reader.getAttributeValue(OFFICE_URN, 'value-type')
    // ... rest uses OFFICE_URN, TEXT_URN ...
}
```

### Testing
- Run existing tests (behavior should be identical)
- Benchmark before/after (micro-benchmark may not show much difference)

### Expected Impact
- **3-5% faster** (reduces field access overhead in hot paths)
- Low-hanging fruit, minimal effort
- After all optimizations: **43-55% cumulative speedup**

---

## 6. StringBuilder Pooling (Conditional) ⭐

### Context
Modern JVMs have excellent young-gen garbage collection for short-lived objects like StringBuilder. Fastexcel **does not** use pooling, relying instead on fast allocation.

### When to Consider Pooling
**Only if profiling shows**:
- High GC time (>10% of total execution time)
- Frequent GC pauses during ODS import
- StringBuilder allocation showing up as hotspot in allocation profiler

### Implementation (If Needed)

**File**: `OdsStreamDataReader.groovy`

```groovy
@CompileStatic
final class OdsStreamDataReader extends OdsDataReader {
    // ThreadLocal pool for StringBuilder reuse
    private static final ThreadLocal<StringBuilder> TEXT_BUILDER = ThreadLocal.withInitial(() -> new StringBuilder(64))

    // ...

    private static String extractTextContent(final XMLStreamReader reader) {
        StringBuilder text = TEXT_BUILDER.get()
        text.setLength(0)  // Clear previous content

        // ... same text collection logic ...

        String result = text.toString()
        // Note: Don't clear StringBuilder here - reused on next call
        return result.isEmpty() ? null : result
    }
}
```

### Risks
- **ThreadLocal overhead** - lookup cost may exceed allocation savings
- **Memory leak risk** - ThreadLocal holds reference until thread dies
- **Complexity** - harder to reason about state

### Recommendation
**Defer this optimization**. Implement only if:
1. All other optimizations implemented
2. Profiling shows GC pressure
3. Benchmark shows net benefit (not always the case)

### Expected Impact
- **0-10%** (highly variable, may be negative)
- Measure before/after carefully

---

## Summary: Implementation Roadmap

### Phase 1: High-Impact Optimizations (Days 1-3)

**Day 1: Aalto Parser**
- Add dependency
- Create OptimizedXMLInputFactory
- Update OdsStreamDataReader
- Test & benchmark
- Expected: 10-20% speedup

**Day 2: extractValue Optimization**
- Refactor to switch + separate methods
- Pre-allocate StringBuilder
- Test edge cases
- Benchmark
- Expected: +15-20% speedup (cumulative 25-40%)

**Day 3: Adaptive Capacity + URN Caching**
- Add rowCapacity field
- Make processSheet instance method (or use holder)
- Cache URN constants in local variables
- Test & benchmark
- Expected: +8-15% speedup (cumulative 33-55%)

**Milestone**: **33-55% faster ODS read** (6.89s → 3.1-4.6s)

### Phase 2: Refinements (Days 4-5)

**Day 4: Text Loop Optimization**
- Cache event types
- Consolidate character handling
- Switch on localName
- Test & benchmark
- Expected: +10-15% speedup (cumulative 43-70%)

**Day 5: Comprehensive Benchmarking**
- Run 50k x 12 benchmark
- Run Crime_Data_from_2023 large file test
- Compare to XLSX performance
- Profile to identify remaining bottlenecks
- Document results

**Milestone**: **43-70% faster ODS read** (6.89s → 2.1-3.9s)

### Phase 3: Evaluation & Contingency (Day 6)

**If target met (< 2x vs XLSX)**:
- Document optimizations in README
- Update roadmap with final metrics
- Proceed to release

**If gap remains**:
- Analyze profiling data
- Consider StringBuilder pooling (if GC pressure)
- Investigate XML parser configuration tuning
- Explore other bottlenecks (ZIP handling, etc.)

---

## Testing Strategy

### Unit Tests
For each optimization:
- Test correctness (values, types, nulls, edges)
- Test with different sheet sizes
- Test with different column ranges

### Integration Tests
- Existing test suite must pass
- Add new tests for edge cases discovered

### Performance Tests
**Benchmark Command**:
```bash
./gradlew :matrix-spreadsheet:spreadsheetBenchmark --rerun-tasks -Dmatrix.spreadsheet.ods.profile=true
```

**Metrics to Track**:
- Total ODS read time (target: < 2x vs XLSX)
- processRow time (currently ~6.3s of 6.89s)
- extractValue time (currently ~1.3-1.6s)
- Cell counts (added/skipped)

**Large File Test**:
```bash
./gradlew :matrix-spreadsheet:test -PrunSlowTests=true --tests "spreadsheet.LargeFileImportTest" --rerun-tasks
```

### Regression Testing
- Run full test suite after each optimization
- Compare results to baseline
- Ensure no performance degradation on XLSX

---

## Success Criteria

### Primary Goals
- [ ] ODS read < 2x slower than XLSX (6.89s → < 1.7s on 50k x 12)
- [ ] Large file ODS read < 2x slower (4m22s → < 52s on Crime_Data)
- [ ] All existing tests pass
- [ ] Line coverage >= 70% maintained

### Secondary Goals
- [ ] Memory footprint < 3x vs XLSX for equivalent datasets
- [ ] No regressions in XLSX performance
- [ ] Code maintainability preserved (no complex abstractions)

### Stretch Goals
- [ ] ODS read comparable to XLSX (< 1.5x slower)
- [ ] ODS write performance improvement (if analysis identifies opportunities)

---

## Appendix: XLSX vs ODS Structural Differences

### Why XLSX Has Advantages
1. **Separate XML files per sheet** - enables potential parallelization
2. **Binary number formats** - faster to parse than text attributes
3. **Simpler cell structure** - `<c><v>123.45</v></c>` vs ODS's multi-element text

### Why ODS Has Advantages
1. **Inline string values** - no shared strings table lookup
2. **Typed value attributes** - `office:value="123.45"` vs parsing text
3. **Repeat attributes** - compact representation of duplicated rows/cells
4. **Single content.xml** - simpler ZIP handling, no relationship parsing

### Net Effect
- Structural differences roughly balance out
- Performance gap is mostly due to **parser choice** and **implementation details**
- With optimizations, ODS should reach parity or near-parity with XLSX

---

## Next Steps

1. **Review and approve** this optimization plan
2. **Implement Phase 1** (Aalto + extractValue + capacity + URN caching)
3. **Benchmark and validate**
4. **Implement Phase 2** (text loop optimization)
5. **Final evaluation** and roadmap update

**Estimated total effort**: 4-6 days

**Expected result**: 40-70% faster ODS reading, closing the gap with XLSX to < 2x.
