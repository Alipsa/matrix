# matrix-parquet v0.5.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship matrix-parquet 0.5.0 with all bugs fixed, code quality tools enabled, and API gaps closed per the roadmap in `matrix-parquet/req/v0.5.0-roadmap.md`.

**Architecture:** The module stays as-is: static utility classes `MatrixParquetReader`/`MatrixParquetWriter` with inner `ReaderBuilder`/`WriterBuilder` (already added). All Parquet dependencies are `implementation`-scoped (already changed). This plan fixes bugs, adds missing API surface, enables compile-static + codenarc, and cleans up.

**Tech Stack:** Groovy 5, JDK 21, Apache Parquet, Hadoop, JUnit Jupiter, CodeNarc, Spotless

**Already completed (this session):**
- [x] Changed `api` to `implementation` for parquet deps in `build.gradle`
- [x] Added `WriterBuilder` and `ReaderBuilder` fluent APIs
- [x] Made `buildSchema`, `buildParquetType`, `parseTypeString`, `extractFieldTypes`, `inferDecimalPrecisionAndScale`, `minBytesForPrecision` private

**Remaining work is organized into 10 tasks below.**

---

### Task 1: Fix Bug #1 -- ParquetReader resource leak

The `ParquetReader` on line 761 of `MatrixParquetReader.groovy` is never closed. This leaks file handles.

**Files:**
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetReader.groovy` -- the `read(File)` method (around line 746)

- [ ] **Step 1: Write a test proving resource cleanup works**

No direct test needed -- the existing tests exercise `read(File)` extensively. The fix is structural. Skip to implementation.

- [ ] **Step 2: Wrap the reader in `withCloseable`**

In `MatrixParquetReader.groovy`, replace the `read(File)` method body (from `def reader = ...` through the end of the method) so the reader is closed. Replace lines ~761-801:

```groovy
  static Matrix read(File file) {
    validateFile(file)
    def path = new Path(file.toURI())
    def conf = new Configuration()
    def footer = ParquetFileReader.readFooter(conf, path)
    def keyValueMetaData = footer.getFileMetaData().getKeyValueMetaData()
    def typeString = keyValueMetaData.get(METADATA_COLUMN_TYPES)
    def indexString = keyValueMetaData.get(METADATA_INDEX_COLUMNS)

    List<Class> fieldTypes
    if (typeString != null) {
      fieldTypes = parseTypeString(typeString)
    }

    ParquetReader.builder(new GroupReadSupport(), path).build().withCloseable { reader ->
      String matrixName
      if (file.name.contains('.')) {
        matrixName = file.name.substring(0, file.name.lastIndexOf('.'))
      } else {
        matrixName = file.name
      }

      Group row = reader.read()
      GroupType schema = row != null ? row.getType() : footer.getFileMetaData().getSchema()
      List<String> fieldNames = schema.fields.collect { it.name }
      if (fieldTypes == null) {
        fieldTypes = extractFieldTypes(schema)
      }
      if (row == null) {
        Matrix m = Matrix.builder(matrixName)
            .columnNames(fieldNames)
            .types(fieldTypes)
            .build()
        return restoreIndex(m, indexString)
      }

      def builder = Matrix.builder(matrixName)
          .columnNames(fieldNames)
          .types(fieldTypes)

      while (row != null) {
        def rowData = []
        fieldNames.eachWithIndex { name, i ->
          def type = fieldTypes[i]
          def field = schema.getType(name)
          def value = readValue(row, name, field, type)
          rowData << value
        }
        builder.addRow(rowData as Object[])
        row = reader.read()
      }
      Matrix m = builder.build()
      restoreIndex(m, indexString)
    }
  }
```

- [ ] **Step 3: Run tests**

Run: `./gradlew :matrix-parquet:test`
Expected: All tests pass (BUILD SUCCESSFUL)

- [ ] **Step 4: Commit**

```bash
git add matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetReader.groovy
git commit -m "fix: close ParquetReader to prevent file handle leaks"
```

---

### Task 2: Fix Bug #2 -- Negative BigDecimal values corrupt on write

The byte-padding for `FIXED_LEN_BYTE_ARRAY` uses zero-filled bytes. For negative numbers, `BigInteger.toByteArray()` returns two's complement, so padding with `0x00` instead of `0xFF` corrupts the sign bit.

**Files:**
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy` -- `writePrimitiveValue` method, the `BigDecimal` case (around line 986)
- Modify: `matrix-parquet/src/test/groovy/MatrixParquetTest.groovy`

- [ ] **Step 1: Write the failing test**

Add to `MatrixParquetTest.groovy`:

```groovy
  @Test
  void testNegativeBigDecimalRoundTrip() {
    def data = Matrix.builder('negatives').columns(
        id: [1, 2, 3, 4],
        amount: toBigDecimals([-123.45, -0.01, 0.0, 999.99])
    ).types([Integer, BigDecimal]).build()

    File file = new File("build/negatives.parquet")
    if (file.exists()) {
      file.delete()
    }

    MatrixParquetWriter.builder(data).write(file)
    Matrix result = MatrixParquetReader.read(file)

    assertEquals(data, result, 'Negative BigDecimal values should round-trip correctly')
    assertEquals(new BigDecimal('-123.45'), result.amount[0])
    assertEquals(new BigDecimal('-0.01'), result.amount[1])
  }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :matrix-parquet:test --tests "MatrixParquetTest.testNegativeBigDecimalRoundTrip"`
Expected: FAIL -- negative values read back as corrupted positive values

- [ ] **Step 3: Fix the padding logic**

In `MatrixParquetWriter.groovy`, find the `BigDecimal` case inside `writePrimitiveValue` (around line 996). Replace:

```groovy
          def padded = new byte[size]
          System.arraycopy(bytes, 0, padded, size - bytes.length, bytes.length)
```

with:

```groovy
          def padded = new byte[size]
          if (unscaled.signum() < 0) {
            Arrays.fill(padded, (byte) 0xFF)
          }
          System.arraycopy(bytes, 0, padded, size - bytes.length, bytes.length)
```

Also add `import java.util.Arrays` at the top of the file if not already present. (Check first -- Groovy auto-imports `java.util.*`, so this import is not needed.)

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :matrix-parquet:test --tests "MatrixParquetTest.testNegativeBigDecimalRoundTrip"`
Expected: PASS

- [ ] **Step 5: Run full test suite**

Run: `./gradlew :matrix-parquet:test`
Expected: All tests pass

- [ ] **Step 6: Commit**

```bash
git add matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy \
       matrix-parquet/src/test/groovy/MatrixParquetTest.groovy
git commit -m "fix: pad negative BigDecimal bytes with 0xFF to preserve sign bit"
```

---

### Task 3: Fix Bug #3 -- Timestamp precision: claim MICROS but deliver MILLIS

The schema declares `TIMESTAMP_MICROS` but both writer and reader truncate to milliseconds. The fix: use `Instant.getEpochSecond()` and `getNano()` to compute true microseconds.

**Files:**
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy` -- `writePrimitiveValue`, the `LocalDateTime` and `Timestamp` cases
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetReader.groovy` -- `readPrimitive`, the `TIMESTAMP` case
- Modify: `matrix-parquet/src/test/groovy/MatrixParquetTest.groovy` -- update `testTimePrecisionRoundTrip`

- [ ] **Step 1: Update the existing test to expect microsecond fidelity**

In `MatrixParquetTest.groovy`, find the `testTimePrecisionRoundTrip` test. The current assertions on lines ~255-259 manually truncate nanoseconds to milliseconds. Replace those `LocalDateTime` assertions with true microsecond assertions:

Replace:
```groovy
    assertEquals(dateTime1.withNano((dateTime1.nano / 1_000_000 as int) * 1_000_000),
        matrix.local_datetime[0], "LocalDateTime should round-trip with millisecond precision")
    assertEquals(dateTime2.withNano((dateTime2.nano / 1_000_000 as int) * 1_000_000),
        matrix.local_datetime[1], "LocalDateTime should round-trip with millisecond precision")
    assertEquals(dateTime3, matrix.local_datetime[2], "LocalDateTime midnight should round-trip exactly")
```

With:
```groovy
    assertEquals(dateTime1.withNano((dateTime1.nano / 1_000 as int) * 1_000),
        matrix.local_datetime[0], 'LocalDateTime should round-trip with microsecond precision')
    assertEquals(dateTime2.withNano((dateTime2.nano / 1_000 as int) * 1_000),
        matrix.local_datetime[1], 'LocalDateTime should round-trip with microsecond precision')
    assertEquals(dateTime3, matrix.local_datetime[2], 'LocalDateTime midnight should round-trip exactly')
```

Note: `dateTime1` has nano=123_456_000. Truncating to microseconds gives 123_456_000 (unchanged). `dateTime2` has nano=999_000_000 (also unchanged). So these will only fail if the writer still truncates to millis.

Also add a test with sub-millisecond data to truly verify:

```groovy
  @Test
  void testMicrosecondTimestampPrecision() {
    def dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 45, 123_456_000)
    def data = Matrix.builder('microTest').columns(
        ts: [dateTime]
    ).types([LocalDateTime]).build()

    byte[] bytes = MatrixParquetWriter.builder(data).zoneId('UTC').writeBytes()
    Matrix result = MatrixParquetReader.builder().zoneId('UTC').read(bytes)

    assertEquals(dateTime, result.ts[0], 'Microsecond precision should be preserved')
  }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :matrix-parquet:test --tests "MatrixParquetTest.testMicrosecondTimestampPrecision"`
Expected: FAIL -- the microsecond component (456) is lost

- [ ] **Step 3: Fix the writer -- compute true microseconds**

In `MatrixParquetWriter.groovy`, find the `LocalDateTime` and `Timestamp` cases in `writePrimitiveValue` (around lines 1010-1017). Replace:

```groovy
      case LocalDateTime -> {
        def micros = ((LocalDateTime) value).atZone(getZoneId()).toInstant().toEpochMilli() * 1000
        group.append(fieldName, (long) micros)
      }
      case Timestamp -> {
        def micros = ((Timestamp) value).toInstant().toEpochMilli() * 1000
        group.append(fieldName, (long) micros)
      }
```

With:

```groovy
      case LocalDateTime -> {
        Instant instant = ((LocalDateTime) value).atZone(getZoneId()).toInstant()
        long micros = instant.epochSecond * 1_000_000L + (long) (instant.nano / 1_000)
        group.append(fieldName, micros)
      }
      case Timestamp -> {
        Instant instant = ((Timestamp) value).toInstant()
        long micros = instant.epochSecond * 1_000_000L + (long) (instant.nano / 1_000)
        group.append(fieldName, micros)
      }
```

Add `import java.time.Instant` to the writer imports if not already present.

- [ ] **Step 4: Fix the reader -- reconstruct from true microseconds**

In `MatrixParquetReader.groovy`, find the `TIMESTAMP` case in `readPrimitive` (around line 926). Replace:

```groovy
        if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
          def micros = group.getLong(fieldName, 0)
          def millis = (long) (micros / 1000)
          if (expectedType == java.sql.Timestamp) {
            return new java.sql.Timestamp(millis)
          }
          return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), getZoneId())
        }
```

With:

```groovy
        if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
          long micros = group.getLong(fieldName, 0)
          long epochSecond = Math.floorDiv(micros, 1_000_000L)
          int microOfSecond = (int) Math.floorMod(micros, 1_000_000L)
          Instant instant = Instant.ofEpochSecond(epochSecond, microOfSecond * 1_000L)
          if (expectedType == java.sql.Timestamp) {
            return java.sql.Timestamp.from(instant)
          }
          return LocalDateTime.ofInstant(instant, getZoneId())
        }
```

- [ ] **Step 5: Run tests**

Run: `./gradlew :matrix-parquet:test`
Expected: All tests pass, including the updated `testTimePrecisionRoundTrip` and new `testMicrosecondTimestampPrecision`

- [ ] **Step 6: Commit**

```bash
git add matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy \
       matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetReader.groovy \
       matrix-parquet/src/test/groovy/MatrixParquetTest.groovy
git commit -m "fix: use true microsecond precision for TIMESTAMP_MICROS read/write"
```

---

### Task 4: Fix Bug #4 -- Deprecated `BigDecimal.ROUND_HALF_UP`

`BigDecimal.ROUND_HALF_UP` has been deprecated since Java 9. Replace with `java.math.RoundingMode.HALF_UP`.

**Files:**
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy`

- [ ] **Step 1: Fix the deprecated API call**

In `MatrixParquetWriter.groovy` (around line 986), replace:

```groovy
          def bd = (BigDecimal) value
          def scale = logical.scale
          def precision = logical.precision
          def unscaled = bd.setScale(scale, BigDecimal.ROUND_HALF_UP).unscaledValue()
```

With:

```groovy
          def bd = (BigDecimal) value
          def scale = logical.scale
          def precision = logical.precision
          def unscaled = bd.setScale(scale, RoundingMode.HALF_UP).unscaledValue()
```

Add `import java.math.RoundingMode` to the imports.

- [ ] **Step 2: Run tests**

Run: `./gradlew :matrix-parquet:test`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy
git commit -m "fix: replace deprecated BigDecimal.ROUND_HALF_UP with RoundingMode.HALF_UP"
```

---

### Task 5: Add `write(Matrix, OutputStream)` and `write(Matrix, Path)` overloads

The writer supports `File` and `byte[]` but not `OutputStream` or `Path`. This is inconsistent with the reader.

**Files:**
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy`
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy` -- the `WriterBuilder` inner class
- Modify: `matrix-parquet/src/test/groovy/MatrixParquetTest.groovy`

- [ ] **Step 1: Write failing tests for Path and OutputStream**

Add to `MatrixParquetTest.groovy`:

```groovy
  @Test
  void testWriterBuilderToPath() {
    def data = Matrix.builder('pathWrite').columns(
        id: [1, 2],
        name: ['A', 'B']
    ).types([Integer, String]).build()

    java.nio.file.Path path = java.nio.file.Path.of("build/path_write.parquet")
    java.nio.file.Files.deleteIfExists(path)

    MatrixParquetWriter.builder(data).write(path)
    assertTrue(java.nio.file.Files.exists(path))

    Matrix result = MatrixParquetReader.builder().read(path)
    assertEquals(data, result)
  }

  @Test
  void testWriterBuilderToOutputStream() {
    def data = Matrix.builder('osWrite').columns(
        id: [1, 2, 3],
        value: toBigDecimals([10.5, 20.75, 30.25])
    ).types([Integer, BigDecimal]).build()

    ByteArrayOutputStream bos = new ByteArrayOutputStream()
    MatrixParquetWriter.builder(data).write(bos)

    assertTrue(bos.size() > 0)
    Matrix result = MatrixParquetReader.read(bos.toByteArray())
    assertEquals(data, result)
  }
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :matrix-parquet:test --tests "MatrixParquetTest.testWriterBuilderToPath"`
Expected: Compilation failure -- method `write(Path)` does not exist

- [ ] **Step 3: Add `write(Path)` and `write(OutputStream)` to `WriterBuilder`**

In the `WriterBuilder` inner class in `MatrixParquetWriter.groovy`, add these methods:

```groovy
    /**
     * Writes the matrix to the specified Path.
     *
     * @param path the target path
     * @return the file that was written
     * @throws IllegalArgumentException if path is null
     */
    File write(java.nio.file.Path path) {
      if (path == null) {
        throw new IllegalArgumentException('Path cannot be null')
      }
      write(path.toFile())
    }

    /**
     * Writes the matrix in Parquet format to the specified OutputStream.
     *
     * <p>Writes the complete Parquet content to the stream. The stream is
     * <strong>not</strong> closed by this method -- the caller is responsible
     * for closing it.</p>
     *
     * @param os the output stream to write to
     * @throws IllegalArgumentException if os is null
     */
    void write(OutputStream os) {
      if (os == null) {
        throw new IllegalArgumentException('OutputStream cannot be null')
      }
      byte[] bytes = writeBytes()
      os.write(bytes)
    }
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :matrix-parquet:test`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy \
       matrix-parquet/src/test/groovy/MatrixParquetTest.groovy
git commit -m "add write(Path) and write(OutputStream) to WriterBuilder"
```

---

### Task 6: Add Logger, remove stale `libs.ivy`, fix `hasFixedPrecisionAndScale` semantics

Three small cleanups from the roadmap (items 8, 9, 12).

**Files:**
- Modify: `matrix-parquet/build.gradle` -- remove `libs.ivy`
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy` -- add Logger, replace commented-out `println`
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetReader.groovy` -- add Logger
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/ParquetWriteOptions.groovy` -- rename `hasFixedPrecisionAndScale` to `hasUniformPrecisionAndScale`

- [ ] **Step 1: Remove `libs.ivy` from build.gradle**

In `matrix-parquet/build.gradle`, delete the line:

```groovy
    implementation libs.ivy
```

- [ ] **Step 2: Add Logger to `MatrixParquetWriter`**

Add import:
```groovy
import se.alipsa.matrix.core.util.Logger
```

Add field after the class declaration:
```groovy
  private static final Logger log = Logger.getLogger(MatrixParquetWriter)
```

Replace commented-out `println` statements with `log.debug(...)`. Search for `//println` and replace each with a proper log call. For example:

- `//println "Write, inferPrecisionAndScale = $inferPrecisionAndScale, schema = $schema"` becomes `log.debug("Write, inferPrecisionAndScale = $inferPrecisionAndScale, schema = $schema")`
- `// println "Writing to ${file.absolutePath}"` becomes `log.debug("Writing to ${file.absolutePath}")`
- `//println "Building schema for column '$col' of type $type with decimal meta: $meta"` becomes `log.debug("Building schema for column '$col' of type $type with decimal meta: $meta")`
- `//println "Inferred decimal precision/scale: $result"` becomes `log.debug("Inferred decimal precision/scale: $result")`

- [ ] **Step 3: Add Logger to `MatrixParquetReader`**

Add import:
```groovy
import se.alipsa.matrix.core.util.Logger
```

Add field:
```groovy
  private static final Logger log = Logger.getLogger(MatrixParquetReader)
```

There are no commented-out println statements in the reader, but add a `log.debug` at key points:
- After building the reader: `log.debug("Reading Parquet file: ${file.absolutePath}")`

- [ ] **Step 4: Rename `hasFixedPrecisionAndScale` to `hasUniformPrecisionAndScale`**

In `ParquetWriteOptions.groovy`, rename the method:

```groovy
  boolean hasUniformPrecisionAndScale() {
    precision != null && scale != null
  }
```

Note the logic change from `||` to `&&` -- this was a bug (item 12). The `validate()` method already rejects mismatched precision/scale, so by the time this method is called both are either set or both are null.

In `MatrixParquetWriter.groovy`, update the call site in `createSchema`:

```groovy
    if (options.hasUniformPrecisionAndScale()) {
```

- [ ] **Step 5: Run tests**

Run: `./gradlew :matrix-parquet:test`
Expected: All tests pass

- [ ] **Step 6: Commit**

```bash
git add matrix-parquet/build.gradle \
       matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy \
       matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetReader.groovy \
       matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/ParquetWriteOptions.groovy
git commit -m "cleanup: add Logger, remove stale ivy dep, fix hasUniformPrecisionAndScale semantics"
```

---

### Task 7: Simplify `readFromInputStream` control flow

Item 14 in the roadmap: when no matrixName is provided, the code reads from a temp file (which derives a name from the temp filename), then overwrites it with `'matrix'`. Clean up the flow.

**Files:**
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetReader.groovy` -- `readFromInputStream` method (around line 817)

- [ ] **Step 1: Simplify the method**

Replace the `readFromInputStream` method with:

```groovy
  private static Matrix readFromInputStream(InputStream is, String matrixName, ZoneId zoneId) {
    if (is == null) {
      throw new IllegalArgumentException('InputStream cannot be null')
    }
    java.nio.file.Path tempFile = Files.createTempFile('matrix-parquet-', '.parquet')
    try {
      Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING)
      Matrix result
      if (zoneId != null) {
        result = read(tempFile.toFile(), zoneId)
      } else {
        result = read(tempFile.toFile())
      }
      if (matrixName != null && !matrixName.trim().isEmpty()) {
        result = result.withMatrixName(matrixName)
      }
      result
    } finally {
      try {
        Files.deleteIfExists(tempFile)
      } catch (IOException ignored) {
      }
    }
  }
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :matrix-parquet:test`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetReader.groovy
git commit -m "simplify readFromInputStream control flow"
```

---

### Task 8: Enable compile-static by default and fix CodeNarc violations

Enable `compileStatic.groovy` configuration and the module-local codenarc override with `ignoreFailures = false`. Fix violations.

**Files:**
- Modify: `matrix-parquet/build.gradle`
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetReader.groovy` -- switch to arrow syntax, fix codenarc violations
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/MatrixParquetWriter.groovy` -- switch to arrow syntax, fix codenarc violations
- Modify: `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/InMemoryPositionOutputStream.groovy` -- implement `Closeable`
- Create: `matrix-parquet/config/codenarc/ruleset.groovy`

- [ ] **Step 1: Add compile-static and codenarc override to build.gradle**

In `matrix-parquet/build.gradle`, add after the existing `compileGroovy` block:

```groovy
compileGroovy {
  options.deprecation = true
  groovyOptions.configurationScript = rootProject.file('config/groovy/compileStatic.groovy')
}

codenarc {
  configFile = file('config/codenarc/ruleset.groovy')
  ignoreFailures = false
}
```

Note: the existing `compileGroovy` block only sets `options.deprecation = true`. Add the `groovyOptions.configurationScript` line to that block. Do not create a duplicate block.

- [ ] **Step 2: Create the module-local codenarc ruleset**

Create `matrix-parquet/config/codenarc/ruleset.groovy` based on the `matrix-stats` version. Since `MatrixParquetReader` and `MatrixParquetWriter` are large classes with many methods (especially the reader with ~25 overloads), and use `instanceof` extensively for Parquet type dispatch, customize accordingly:

```groovy
ruleset {
    description 'CodeNarc ruleset for Matrix Parquet'

    ruleset('rulesets/basic.xml') {
    }

    ruleset('rulesets/braces.xml')

    ruleset('rulesets/design.xml') {
        exclude 'AbstractClassWithoutAbstractMethod'
        exclude 'BuilderMethodWithSideEffects'
        exclude 'Instanceof'
        exclude 'NestedForLoop'
        exclude 'PrivateFieldCouldBeFinal'
    }

    ruleset('rulesets/dry.xml') {
        exclude 'DuplicateListLiteral'
        exclude 'DuplicateNumberLiteral'
        exclude 'DuplicateStringLiteral'
    }

    ruleset('rulesets/exceptions.xml') {
        exclude 'CatchException'
        exclude 'CatchThrowable'
    }

    ruleset('rulesets/imports.xml') {
        exclude 'NoWildcardImports'
    }

    ruleset('rulesets/naming.xml') {
        'MethodName' {
            regex = /[a-z][\w]*/
        }
        'ClassName' {
            regex = /[A-Z][\w$]*/
        }
        'FieldName' {
            finalRegex = /[a-z][a-zA-Z0-9]*/
            staticRegex = /([a-z][a-zA-Z0-9]*|[A-Z][A-Z_0-9]*)/
            staticFinalRegex = /(log|[A-Z][A-Z_0-9]*)/
            regex = /[a-z][a-zA-Z0-9]*/
        }
        exclude 'FactoryMethodName'
        exclude 'ConfusingMethodName'
    }

    ruleset('rulesets/size.xml') {
        exclude 'CrapMetric'
        'AbcMetric' {
            maxMethodAbcScore = 70
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        'CyclomaticComplexity' {
            maxMethodComplexity = 35
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        'MethodCount' {
            maxMethods = 250
        }
        'MethodSize' {
            maxLines = 100
        }
        'ClassSize' {
            maxLines = 1200
        }
        'ParameterCount' {
            maxParameters = 7
        }
    }

    ruleset('rulesets/unnecessary.xml') {
        exclude 'UnnecessaryCollectCall'
        exclude 'UnnecessaryElseStatement'
        exclude 'UnnecessaryGString'
        exclude 'UnnecessaryGetter'
        exclude 'UnnecessaryObjectReferences'
        exclude 'UnnecessaryPublicModifier'
        exclude 'UnnecessarySetter'
    }

    ruleset('rulesets/unused.xml')

    ruleset('rulesets/formatting.xml') {
        exclude 'BlockEndsWithBlankLine'
        exclude 'BlockStartsWithBlankLine'
        exclude 'ClassEndsWithBlankLine'
        exclude 'ClassStartsWithBlankLine'
        exclude 'ConsecutiveBlankLines'
        exclude 'Indentation'
        exclude 'LineLength'
        exclude 'SpaceAfterComma'
        exclude 'SpaceAfterMethodCallName'
        exclude 'SpaceAfterOpeningBrace'
        exclude 'SpaceAroundMapEntryColon'
        exclude 'SpaceAroundOperator'
        exclude 'SpaceBeforeClosingBrace'
        exclude 'SpaceInsideParentheses'
    }

    ruleset('rulesets/comments.xml') {
        'ClassJavadoc' {
            doNotApplyToFilesMatching = /.*Test\.groovy/
        }
        exclude 'SpaceAfterCommentDelimiter'
    }

    ruleset('rulesets/groovyism.xml')
}
```

- [ ] **Step 3: Fix `InMemoryPositionOutputStream` -- implement `Closeable`**

CodeNarc reports `CloseWithoutCloseable`. The class has a `close()` method but doesn't implement `Closeable`. Fix:

```groovy
class InMemoryPositionOutputStream extends PositionOutputStream implements Closeable {
```

(The `PositionOutputStream` already extends `OutputStream` which implements `Closeable`, but adding the explicit `implements` makes the intent clear and satisfies CodeNarc.)

Actually, since `PositionOutputStream extends OutputStream` and `OutputStream implements Closeable/Flushable/AutoCloseable`, this is already covered. The issue is likely with `InMemoryOutputFile` -- check that one. If CodeNarc still flags it, the cleanest fix is to add `implements Closeable` or `implements AutoCloseable`.

- [ ] **Step 4: Fix `MatrixParquetReader` -- convert old-style switch to arrow syntax**

The `readPrimitive` method uses old-style `switch` with `case X:` and `return`. Per AGENTS.md exception rule, methods that use `return` inside case arms should keep old-style. The `readPrimitive` method **does** use `return` to exit the method from case arms, so it **must keep old-style switch**. No change needed here.

However, the `getJavaType` method in `MatrixParquetReader` also uses old-style switch with `return`. Same rule applies -- keep it.

The codenarc report showed these issues that need fixing:
- `ClassForName` on line 308: Replace `Class.forName(name)` with `Thread.currentThread().contextClassLoader.loadClass(name)` in the `getCachedClass` method
- `ExplicitLinkedHashMapInstantiation`: Replace `new LinkedHashMap()` and `new LinkedHashMap<>()` with `[:]` in `readMap` and `readStruct`
- `IfStatementBraces`: Add braces around single-line `if` statements in `extractFieldTypes`

- [ ] **Step 5: Fix `MatrixParquetWriter` -- convert switch to arrow syntax where applicable**

The `writePrimitiveValue` method uses old-style switch with `->` (it already uses arrow syntax). Check `buildPrimitiveType` -- it also uses old-style with `return` from case arms, so it must stay old-style per the AGENTS.md exception.

- [ ] **Step 6: Compile and fix any remaining static compilation errors**

Run: `./gradlew :matrix-parquet:compileGroovy`

If there are compile errors from enabling `@CompileStatic` globally, add `@CompileDynamic` to specific methods that need dynamic features. Since all classes already have `@CompileStatic` annotations, enabling it via the configuration script should not cause new issues. Remove the explicit `@CompileStatic` annotations from individual classes since the configuration script handles it globally. BUT: only remove them if the configuration script is working correctly. Test first.

Actually, since all classes already have `@CompileStatic`, enabling the configuration script is redundant but harmless. Leave the annotations in place for clarity.

- [ ] **Step 7: Run codenarc and fix remaining violations**

Run: `./gradlew :matrix-parquet:codenarcMain`
Expected: BUILD SUCCESSFUL with no violations (ignoreFailures = false)

If violations remain, fix them iteratively.

- [ ] **Step 8: Run spotless**

Run: `./gradlew :matrix-parquet:spotlessApply`
Then: `./gradlew :matrix-parquet:spotlessCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Run full tests**

Run: `./gradlew :matrix-parquet:test`
Expected: All tests pass

- [ ] **Step 10: Commit**

```bash
git add matrix-parquet/build.gradle \
       matrix-parquet/config/codenarc/ruleset.groovy \
       matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/
git commit -m "enable compile-static by default and codenarc with ignoreFailures=false"
```

---

### Task 9: Migrate tests to `@TempDir` and clean up boilerplate

Item 13: Most tests manually delete/create directories. Use JUnit's `@TempDir`.

**Files:**
- Modify: `matrix-parquet/src/test/groovy/MatrixParquetTest.groovy`

- [ ] **Step 1: Add `@TempDir` field and refactor tests**

Add to the test class:

```groovy
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MatrixParquetTest {

  @TempDir
  Path tempDir
```

Then replace all `new File("build/...")` patterns with `tempDir.resolve("...").toFile()`. For example, replace:

```groovy
    File file = new File("build/empData.parquet")
    if (file.exists()) {
      file.delete()
    }
```

With:

```groovy
    File file = tempDir.resolve('empData.parquet').toFile()
```

And for directory-based tests:

```groovy
    File dir = new File("build/cars")
    if (dir.exists()) {
      if (dir.isDirectory()) {
        dir.listFiles().each {it.delete()}
      }
      dir.delete()
    }
    dir.mkdirs()
```

Becomes:

```groovy
    File dir = tempDir.resolve('cars').toFile()
    dir.mkdirs()
```

Apply this pattern to **every** test method. There are ~25+ test methods to update. The `@TempDir` directory is automatically cleaned up after each test class.

- [ ] **Step 2: Run tests**

Run: `./gradlew :matrix-parquet:test`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add matrix-parquet/src/test/groovy/MatrixParquetTest.groovy
git commit -m "migrate tests to @TempDir, remove manual cleanup boilerplate"
```

---

### Task 10: Update readme

Item 10: Version drift and missing builder API documentation.

**Files:**
- Modify: `matrix-parquet/readme.md`

- [ ] **Step 1: Update readme**

Update the version references on line 11 from `0.4.0` to `0.5.0`. Update `matrix-core` version to `3.7.1`. Update `groovy` version to `5.0.5`.

Add a "Builder API" section after the "Basic Usage" section showing the new builder pattern:

```markdown
### Builder API (recommended)

The builder API provides a fluent, discoverable interface for reading and writing:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

// Writing
MatrixParquetWriter.builder(matrix)
    .precision(38)
    .scale(18)
    .zoneId('Europe/Stockholm')
    .write(new File('output.parquet'))

// Writing to byte array
byte[] bytes = MatrixParquetWriter.builder(matrix)
    .inferPrecisionAndScale(true)
    .writeBytes()

// Writing to OutputStream
outputStream.withCloseable { os ->
  MatrixParquetWriter.builder(matrix).write(os)
}

// Reading
Matrix data = MatrixParquetReader.builder()
    .matrixName('myData')
    .zoneId('Europe/Stockholm')
    .read(new File('data.parquet'))

// Reading from byte array, InputStream, URL, or Path also supported
Matrix data = MatrixParquetReader.builder()
    .read(parquetBytes)
```
```

Remove the imports of `ParquetReadOptions`/`ParquetWriteOptions` from the SPI usage example (they're internal now -- the SPI uses maps). Update the "Known Limitations" section: change "millisecond precision" to "microsecond precision" for timestamps.

- [ ] **Step 2: Commit**

```bash
git add matrix-parquet/readme.md
git commit -m "update readme for v0.5.0: builder API, version refs, microsecond precision"
```

---

## Summary of items covered

| Roadmap item | Task |
|---|---|
| Codenarc | Task 8 |
| Spotless | Task 8 |
| Compile statically | Task 8 |
| Bug 1: Resource leak | Task 1 |
| Bug 2: Negative BigDecimal | Task 2 |
| Bug 3: Timestamp precision | Task 3 |
| Bug 4: Deprecated ROUND_HALF_UP | Task 4 |
| Item 5: Combinatorial explosion | Already done (builders added) |
| Item 6: No write(OutputStream) | Task 5 |
| Item 7: No write(Path) | Task 5 |
| Item 8: No Logger | Task 6 |
| Item 9: Stale ivy dep | Task 6 |
| Item 10: Readme version | Task 10 |
| Item 11: Public internal APIs | Already done (made private) |
| Item 12: hasFixedPrecisionAndScale | Task 6 |
| Item 13: Test cleanup | Task 9 |
| Item 14: readFromInputStream flow | Task 7 |
