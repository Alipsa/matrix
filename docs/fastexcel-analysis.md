# Fastexcel Reader Architecture Analysis

## Executive Summary

Fastexcel achieves ~2x faster XLSX reading than Apache POI through:
1. **Optimized XML parser** - Uses Aalto StAX with strict configuration
2. **Lazy shared strings loading** - Streams SST on-demand instead of loading all into memory
3. **Minimal object allocation** - Reuses collections, adaptive capacity sizing
4. **Clean streaming architecture** - Pure streaming with no DOM, minimal state
5. **Smart cell parsing** - Type-aware parsing with early returns

This analysis identifies techniques directly applicable to ODS optimization.

---

## 1. XML Parsing Strategy

### Parser Selection & Configuration
**File**: `DefaultXMLInputFactory.java:9`

```java
XMLInputFactory factory = new com.fasterxml.aalto.stax.InputFactoryImpl();
factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
```

**Key Insights**:
- Uses **Aalto StAX parser** (com.fasterxml..aalto), not the JDK default
- Aalto is optimized for performance - 10-30% faster than Woodstox or JDK StAX
- Disables DTD and external entities for security and speed
- Factory is static final - created once and reused

**ODS Current State**:
```groovy
// OdsStreamDataReader.groovy:61
final XMLInputFactory factory = XmlSecurityUtil.newSecureInputFactory()
```
- Creates new factory instance per import
- Uses JDK default StAX implementation
- Security settings applied but not performance-optimized

**Optimization Opportunity**: ⭐⭐⭐ HIGH
- Switch to Aalto StAX parser
- Make XMLInputFactory static final
- Expected impact: 10-20% speedup

---

## 2. Shared Strings Table (SST) Handling

### Lazy Streaming Approach
**File**: `SST.java:28-34`

```java
String getItemAt(int index) throws XMLStreamException {
    if (reader == null) {
        return null;
    }
    readUpTo(index);  // Only reads as far as needed
    return values.get(index);
}
```

**Key Insights**:
- SST is **streamed on-demand**, not loaded upfront
- Maintains XMLStreamReader positioned in shared strings file
- Only reads entries up to the requested index
- Caches previously read values in ArrayList
- Empty SST (null InputStream) handled with static EMPTY singleton

**XLSX vs ODS Difference**:
- XLSX: Shared strings in separate `xl/sharedStrings.xml` file, cells reference by index
- ODS: Strings are **inline** in `content.xml`, no separate SST

**ODS Implication**: ⭐ LOW
- ODS doesn't need SST optimization since strings are inline
- This is actually an **advantage** for ODS - less indirection
- No action needed

---

## 3. Streaming Architecture

### Workbook Opening
**File**: `ReadableWorkbook.java:148-156`

```java
Stream<Row> openStream(Sheet sheet) throws IOException {
    try {
        InputStream inputStream = pkg.getSheetContent(sheet);
        Stream<Row> stream = StreamSupport.stream(
            new RowSpliterator(this, inputStream), false);
        return stream.onClose(asUncheckedRunnable(inputStream));
    } catch (XMLStreamException e) {
        throw new IOException(e);
    }
}
```

**Key Insights**:
- Each sheet gets its **own InputStream** from ZIP
- Uses `Spliterator` pattern for Java Stream integration
- Stream cleanup via `onClose()` handler ensures InputStream is closed
- No parallelization (`StreamSupport.stream(..., false)`)

**ODS Current State**:
```groovy
// OdsStreamDataReader.groovy:60
Sheet processContent(final InputStream is, Object sheet, ...) {
    final XMLStreamReader reader = factory.createXMLStreamReader(is)
    // ... single-threaded sequential processing
}
```
- Similar single-threaded streaming approach
- Direct processing, no Java Stream abstraction
- Manually manages reader lifecycle

**Optimization Opportunity**: ⭐ LOW
- Both use single-threaded streaming
- Java Stream abstraction doesn't improve performance here
- No action needed

---

## 4. Row Parsing - The Core Bottleneck

### RowSpliterator Architecture
**File**: `RowSpliterator.java:27-108`

#### 4.1 Adaptive Capacity Sizing
```java
private int rowCapacity = 16;  // Line 34

List<Cell> cells = new ArrayList<>(rowCapacity);  // Line 91
// ... after row parsed ...
rowCapacity = Math.max(rowCapacity, cells.size());  // Line 106
```

**Key Insight**: Starts with small capacity, grows to match actual data
- Avoids over-allocation for narrow sheets
- Avoids repeated resizing for wide sheets
- Simple max() tracking, no decay

**ODS Current State**:
```groovy
// OdsStreamDataReader.groovy:166
int expectedColumns = endColumn == Integer.MAX_VALUE ? 16
    : Math.max(0, endColumn - startColumn + 1)
List<Object> row = new ArrayList<>(expectedColumns)
```
- Static calculation based on requested range
- Good when range is known, wastes space when endColumn=MAX_VALUE and sheet is narrow
- **No adaptive learning** across rows

**Optimization Opportunity**: ⭐⭐ MEDIUM
- Add rowCapacity field, track max size
- Expected impact: 5-10% reduction in ArrayList resizing overhead

#### 4.2 Cell Position Tracking
```java
int trackedRowIndex = 0;   // Line 35
int trackedColIndex = 0;   // Line 87

CellAddress addr = getCellAddressWithFallback(trackedColIndex);  // Line 123
```

**Key Insight**: Tracks position even when cell lacks `r` attribute
- Handles sparse sheets where not every cell has explicit coordinates
- Simple increment-based fallback

**ODS Current State**:
```groovy
// OdsStreamDataReader.groovy:168
int columnCount = 1
// ... incremented as cells are processed
```
- Similar tracking via columnCount
- ODS repeat attributes require careful counting
- Already handled correctly

**Optimization Opportunity**: ⭐ NONE
- Both implementations handle this correctly

---

## 5. Cell Parsing - Type Dispatch

### Type-Aware Parsing with Early Returns
**File**: `RowSpliterator.java:145-198`

```java
private Cell parseOther(CellAddress addr, String type, ...) {
    CellType definedType = parseType(type);  // Switch on type string
    Function<String, ?> parser = getParserForType(definedType);  // Get parser

    Object value = null;
    String formula = null;
    String rawValue = null;
    while (r.goTo(() -> r.isStartElement("v") || ...)) {
        if ("v".equals(r.getLocalName())) {
            rawValue = r.getValueUntilEndElement("v");
            value = "".equals(rawValue) ? null : parser.apply(rawValue);
        }
        // ...
    }
    return new Cell(...);
}
```

**Key Insights**:
- Type determined **once** from cell `t` attribute
- Parser function selected **once** via switch
- Value extracted, **then** parsed with appropriate parser
- No repeated type checking during value extraction

**ODS Current State**:
```groovy
// OdsStreamDataReader.groovy:246-293
private static Object extractValueInternal(final XMLStreamReader reader) {
    String valueType = reader.getAttributeValue(officeUrn, 'value-type')

    if (valueType == 'boolean') {
        return Boolean.parseBoolean(...)
    } else if (valueType == 'float' || valueType == 'percentage' || ...) {
        String v = reader.getAttributeValue(officeUrn, 'value')
        return v != null ? asBigDecimal(v) : null
    } else if (valueType == 'date') {
        // ...
    } else if (valueType == 'time') {
        // ...
    }

    // Fallback: collect text content
    StringBuilder text = new StringBuilder()
    while (reader.hasNext()) {
        reader.next()
        // ... character/element handling ...
    }
    // ...
}
```

**Key Differences**:
1. ODS reads typed values **directly from attributes** (no text parsing!)
   - `office:value="123.45"` for floats
   - `office:boolean-value="true"` for booleans
   - Much simpler than XLSX which stores everything as text in `<v>` elements

2. ODS **returns early** for typed values (lines 251-262)
   - No StringBuilder allocation for boolean/float/date/time
   - Only enters text collection loop for strings

3. Text collection is **more complex** due to `<text:p>`, `<text:s>`, `<text:line-break>`, etc.
   - XLSX just has plain text in `<v>` elements
   - ODS must handle structured text elements

**Optimization Opportunity**: ⭐⭐⭐ HIGH
- Move typed value extraction to separate methods
- Avoid creating StringBuilder until **after** type checks pass
- Use switch expression for type dispatch
- Expected impact: 15-20% reduction in extractValue time

---

## 6. Text Value Extraction

### XLSX Approach
**File**: `SimpleXmlReader.java:103-128`

```java
public String getValueUntilEndElement(String elementName, String skipping) {
    StringBuilder sb = new StringBuilder();
    int childElement = 1;
    while (reader.hasNext()) {
        int type = reader.next();
        if (type == XMLStreamReader.CDATA ||
            type == XMLStreamReader.CHARACTERS ||
            type == XMLStreamReader.SPACE) {
            sb.append(reader.getText());
        } else if (type == XMLStreamReader.START_ELEMENT) {
            if(skipping.equals(reader.getLocalName())) {
                getValueUntilEndElement(reader.getLocalName());
            } else {
                childElement++;
            }
        } else if (type == XMLStreamReader.END_ELEMENT) {
            childElement--;
            if (elementName.equals(reader.getLocalName()) && childElement == 0) {
                break;
            }
        }
    }
    return sb.toString();
}
```

**Key Insights**:
- Single StringBuilder instance per value
- Handles nested elements with counter
- `skipping` parameter allows ignoring specific child elements
- Three event types consolidated: CDATA | CHARACTERS | SPACE
- Early exit when target end element reached with childElement == 0

**ODS Current State**:
```groovy
// OdsStreamDataReader.groovy:266-293
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

**Key Differences**:
- ODS must handle **structured text** elements (`<text:p>`, `<text:s>`, etc.)
- XLSX text is simpler - just character data
- ODS has more branching in the loop (5 conditions vs 3)

**Optimization Opportunity**: ⭐⭐ MEDIUM
- Pre-calculate StringBuilder capacity hint (e.g., 64 chars for likely string length)
- Consolidate event type checking (combine character checks)
- Consider local variables for frequently accessed reader state
- Expected impact: 10-15% reduction in text extraction time

---

## 7. Memory Management

### Object Reuse Patterns

#### Cell List Reuse
**File**: `RowSpliterator.java:91`
```java
List<Cell> cells = new ArrayList<>(rowCapacity);
```
- **No explicit pooling** - relies on JVM allocation/GC optimization
- rowCapacity reduces ArrayList resizing

#### StringBuilder in getValueUntilEndElement
**File**: `SimpleXmlReader.java:108`
```java
StringBuilder sb = new StringBuilder();
```
- **New StringBuilder per value extraction**
- No ThreadLocal pooling
- Default capacity (16 chars), grows as needed

**Key Insight**: Fastexcel does **NOT** use complex pooling
- Relies on JVM's fast allocation for short-lived objects
- Modern JVMs have excellent young-gen GC for temporary objects
- Complexity of pooling may not outweigh allocation cost

**ODS Current State**:
```groovy
// OdsStreamDataReader.groovy:167
List<Object> row = new ArrayList<>(expectedColumns)

// OdsStreamDataReader.groovy:266
StringBuilder text = new StringBuilder()
```
- Similar approach - new objects per row/value
- No pooling

**Optimization Opportunity**: ⭐ LOW
- ThreadLocal<StringBuilder> pooling is complex
- Modern GC handles this well
- **Defer this optimization** unless profiling shows GC pressure
- Expected impact: 5-10% at best, may be negligible

---

## 8. ZIP/Package Handling

### OPCPackage Architecture
**File**: `OPCPackage.java:59-90`

```java
private OPCPackage(ZipFile zip, boolean withFormat) {
    this.zip = zip;
    this.parts = extractPartEntriesFromContentTypes();
    if (withFormat) {
        this.formatIdList = extractFormat(parts.style);
    } else {
        this.formatIdList = Collections.emptyList();
    }
    this.workbookPartsById = readWorkbookPartsIds(...);
}
```

**Key Insights**:
- Uses Apache Commons Compress `ZipFile` (not java.util.zip)
- Parses `[Content_Types].xml` to locate parts
- Reads relationship files (`*.rels`) to map sheet IDs to paths
- **Lazy** - only opens streams when sheet is accessed

**XLSX Structure**:
```
myfile.xlsx (ZIP archive)
├── [Content_Types].xml
├── _rels/.rels
├── xl/
│   ├── workbook.xml
│   ├── _rels/workbook.xml.rels
│   ├── worksheets/
│   │   ├── sheet1.xml
│   │   ├── sheet2.xml
│   ├── sharedStrings.xml
│   └── styles.xml
```

**ODS Structure**:
```
myfile.ods (ZIP archive)
├── META-INF/manifest.xml
├── content.xml (ALL sheets in ONE file)
├── styles.xml
└── meta.xml
```

**Key Difference**: ODS has **single content.xml** with all sheets
- XLSX: One XML file per sheet (parallel processing possible)
- ODS: All sheets in one file (sequential only)

**ODS Implication**: ⭐ MEDIUM
- **Cannot** process sheets in parallel like XLSX
- **Can** optimize single-sheet imports by skipping to target sheet
- Current implementation already does this (OdsStreamDataReader.groovy:73-86)
- No major optimization opportunity here

---

## 9. Parallelization Opportunities

### XLSX Approach
Fastexcel **does NOT parallelize** by default:
```java
// ReadableWorkbook.java:151
Stream<Row> stream = StreamSupport.stream(
    new RowSpliterator(this, inputStream), false);  // parallel=false
```

**Why?**
- Row parsing is sequential (depends on previous row state in shared formulas)
- ZIP input streams are not seekable
- Memory overhead of buffering multiple sheets

**Multi-Sheet Workbook**:
- Each sheet is a separate XML file
- Theoretically could parse sheets in parallel
- Would require:
  1. Pre-scanning all sheet positions in ZIP
  2. Creating separate XML readers per sheet
  3. Merging results

**ODS Structure**:
- All sheets in single `content.xml` file
- **Cannot** parse multiple sheets in parallel
- **Could** parse rows within a sheet in parallel, but:
  - Requires pre-scanning row boundaries
  - XML parsing is inherently sequential
  - Complexity outweighs benefit

**Optimization Opportunity**: ⭐ NONE
- ODS single-file structure makes parallelization impractical
- Row parsing is the bottleneck, not sheet-level iteration
- Focus on optimizing single-threaded row/cell parsing instead

---

## 10. Key Takeaways for ODS Optimization

### High-Impact Optimizations (Implement First)

1. **Switch to Aalto StAX Parser** ⭐⭐⭐
   - Add `com.fasterxml.aalto:aalto-xml` dependency
   - Make XMLInputFactory static final
   - Expected: 10-20% speedup
   - **Files**: `DefaultXMLInputFactory` (new), `OdsStreamDataReader.groovy`

2. **Optimize extractValue Type Dispatch** ⭐⭐⭐
   - Extract typed value handling to separate methods
   - Return immediately after reading attribute (avoid StringBuilder creation)
   - Use switch expression for type dispatch
   - Expected: 15-20% speedup
   - **File**: `OdsStreamDataReader.groovy:246-293`

3. **Adaptive Row Capacity Sizing** ⭐⭐
   - Add `rowCapacity` field, track max size across rows
   - Use for ArrayList initial capacity
   - Expected: 5-10% speedup
   - **File**: `OdsStreamDataReader.groovy:166`

4. **Optimize Text Collection Loop** ⭐⭐
   - Pre-allocate StringBuilder with capacity hint (64 or 128)
   - Reduce branching in tight loop
   - Cache frequently accessed reader state in local variables
   - Expected: 10-15% speedup
   - **File**: `OdsStreamDataReader.groovy:266-293`

### Medium-Impact Optimizations (Consider After High-Impact)

5. **Cache URN Constants Locally** ⭐
   - Store `tableUrn`, `officeUrn`, `textUrn` in local variables at method entry
   - Reduces field access overhead
   - Expected: 3-5% speedup
   - **File**: `OdsStreamDataReader.groovy` (multiple methods)

### Low-Priority Optimizations (Profile First)

6. **ThreadLocal StringBuilder Pooling** ⭐
   - Only if profiling shows GC pressure from StringBuilder allocation
   - Complexity may outweigh benefit
   - Expected: 5-10% at best, possibly negligible

### Not Applicable to ODS

7. **Shared Strings Optimization** - N/A (ODS has inline strings)
8. **Parallel Sheet Processing** - N/A (ODS has single content.xml)
9. **Multi-threaded Row Parsing** - N/A (XML parsing is sequential)

---

## 11. Fastexcel Dependencies

From `fastexcel-reader/build.gradle`:
```gradle
dependencies {
    implementation 'com.fasterxml.aalto:aalto-xml:1.3.2'
    implementation 'org.apache.commons:commons-compress:1.24.0'
}
```

**Aalto**: Optimized StAX parser (key performance driver)
**Commons Compress**: ZIP handling (already used in matrix-spreadsheet via transitive dependencies)

---

## 12. Implementation Recommendations

### Phase 1: Quick Wins (Days 1-3)
- Switch to Aalto parser
- Optimize extractValue type dispatch
- Add adaptive row capacity

**Expected cumulative improvement**: 25-35% faster ODS read

### Phase 2: Refinements (Days 4-5)
- Optimize text collection loop
- Cache URN constants
- Run comprehensive benchmarks

**Expected cumulative improvement**: 35-50% faster ODS read

### Phase 3: Evaluation (Day 6)
- Compare against XLSX performance
- If gap remains >2x, investigate remaining bottlenecks
- Profile to identify next optimizations

**Target**: ODS read < 2x slower than XLSX (6.89s → ~1.7s on 50k x 12 benchmark)

---

## Conclusion

Fastexcel's performance comes from:
1. **Right parser choice** (Aalto StAX)
2. **Minimal allocations** (adaptive sizing, simple reuse)
3. **Type-aware parsing** (early returns, targeted logic)
4. **Clean architecture** (pure streaming, no unnecessary features)

ODS can adopt techniques #1-3 directly. Technique #4 is already present in the current implementation.

The most impactful optimizations are **parser selection** and **extractValue refinement**. Together, these should achieve 25-40% speedup, closing most of the performance gap.
