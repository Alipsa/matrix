[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-arff/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-arff)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-arff/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-arff)
Matrix-arff provides reading and writing of ARFF (Attribute-Relation File Format) files. ARFF is a standard format used by machine learning tools, particularly Weka, making this module essential for ML workflows and data interchange between different ML tools.

## At A Glance

- read ARFF from `File`, `Path`, `URL`, `InputStream`, `Reader`, and `String`
- write ARFF to `File`, `Path`, `OutputStream`, `Writer`, or `String`
- support dense and sparse `@DATA` rows
- control reads with `ArffReadOptions`
- control schema generation with `ArffWriteOptions`
- use the generic Matrix SPI through `Matrix.read(...)`, `matrix.write(...)`, `Matrix.listReadOptions(...)`, and `Matrix.listWriteOptions(...)`

## What is ARFF?

ARFF (Attribute-Relation File Format) is a text-based format developed for the Weka machine learning toolkit. It's widely used in the machine learning community because it:

- Explicitly defines data types for each column
- Supports both numeric and categorical (nominal) data
- Includes metadata about the dataset (relation name, attribute definitions)
- Is human-readable and easy to inspect

An ARFF file consists of three main sections:

1. **Header comments** - Lines starting with `%` containing metadata
2. **Relation and attribute declarations** - `@RELATION` and `@ATTRIBUTE` directives
3. **Data section** - `@DATA` followed by comma-separated values

## Installation

To use the matrix-arff module, add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.4'
implementation platform("se.alipsa.matrix:matrix-bom:2.5.0")
implementation "se.alipsa.matrix:matrix-core"
implementation "se.alipsa.matrix:matrix-arff"
```

### Maven Configuration

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-bom</artifactId>
        <version>2.5.0</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
<dependencies>
    <dependency>
        <groupId>org.apache.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>5.0.4</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-core</artifactId>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-arff</artifactId>
    </dependency>
</dependencies>
</project>
```

## API Surface

Direct API entry points:

- `MatrixArffReader.read(...)` for `File`, `Path`, `URL`, `InputStream`, `Reader`, and `String`
- `MatrixArffWriter.write(...)` for `File`, `Path`, `OutputStream`, and `Writer`
- `MatrixArffWriter.writeString(...)` for in-memory export
- `ArffReadOptions` for fallback naming and validation behavior
- `ArffWriteOptions` for schema generation and date formatting

Generic Matrix SPI entry points:

- `Matrix.read(new File('data.arff'))`
- `matrix.write(new File('data.arff'))`
- `Matrix.listReadOptions('arff')`
- `Matrix.listWriteOptions('arff')`
- `ArffReadOptions.describe()`
- `ArffWriteOptions.describe()`

## Reading ARFF Files

The `MatrixArffReader` class provides several methods to read ARFF files from different sources.

### Options-First Read API

```groovy
import se.alipsa.matrix.arff.ArffReadOptions
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.core.Matrix

ArffReadOptions readOptions = new ArffReadOptions()
    .fallbackMatrixName('fallback-name')

Matrix iris = MatrixArffReader.read(new File("iris.arff"), readOptions)
Matrix fromPath = MatrixArffReader.read(Path.of("iris.arff"), readOptions)
Matrix fromUrl = MatrixArffReader.read(new URL("https://example.com/iris.arff"), readOptions)
Matrix fromString = MatrixArffReader.readString(arffContent, readOptions)

println "Dataset: ${iris.matrixName}"
println "Rows: ${iris.rowCount()}"
println "Columns: ${iris.columnNames()}"
```

The typed overloads are available for `File`, `Path`, `URL`, `InputStream`, `Reader`, and `String`.

### Convenience Shortcuts

If you do not need typed options, the following convenience overloads are available:

```groovy
Matrix iris = MatrixArffReader.read(new File("iris.arff"))
Matrix fromPath = MatrixArffReader.read(Path.of("iris.arff"))
Matrix fromString = MatrixArffReader.readString(arffContent)
```

Output:
```
Dataset: iris
Rows: 150
Columns: [sepallength, sepalwidth, petallength, petalwidth, class]
```

See [the tutorial](../docs/tutorial/16-matrix-arff.md) for more details.

## Reading Sparse ARFF Rows

`MatrixArffReader` supports sparse data rows in the ARFF `@DATA` section since version 0.2.0.:

```arff
@RELATION sparse_metrics

@ATTRIBUTE score NUMERIC
@ATTRIBUTE note STRING
@ATTRIBUTE status {ok,warning,error}

@DATA
{0 1.5,2 ok}
{1 'late sample',2 warning}
{}
```

When sparse rows are read:

- omitted attributes become `null`
- quoted string and nominal values are supported
- explicit `?` values are treated as missing and become `null`
- duplicate or out-of-range attribute indices are rejected with an `IllegalArgumentException`

## Default Behavior

The defaults are intentionally lenient so basic imports and exports work without extra configuration:

- matrix name defaults to `@RELATION` when present, otherwise to the fallback name from `ArffReadOptions.fallbackMatrixName(...)`, otherwise to the source name such as the file name or `ArffMatrix`
- ARFF missing values (`?`) read as `null`, and `null` values write as `?`
- dense rows with missing trailing values are padded with `null`
- dense rows with extra trailing values ignore the extras unless strict row-length validation is enabled
- unknown attribute types fall back to `STRING` unless strict unknown-type validation is enabled
- `String` and `Object` columns are inferred as nominal only when their distinct-value count is at or below `nominalThreshold`, and for datasets with 10 or more rows also at or below 10% of the row count
- DATE output uses `yyyy-MM-dd'T'HH:mm:ss` unless `dateFormat(...)` or `dateFormatsByColumn(...)` overrides it

To inspect the currently available SPI options at runtime:

```groovy
println Matrix.listReadOptions('arff')
println Matrix.listWriteOptions('arff')
println ArffReadOptions.describe()
println ArffWriteOptions.describe()
```

## Reader Validation Modes

`ArffReadOptions` can be used when you want stricter schema validation during import:

```groovy
import se.alipsa.matrix.arff.ArffReadOptions
import se.alipsa.matrix.arff.MatrixArffReader

ArffReadOptions options = new ArffReadOptions()
    .fallbackMatrixName('fallback-name')
    .strict(true)

Matrix matrix = MatrixArffReader.read(new File('dataset.arff'), options)
```

Useful read options:

- `strict(true)` enables fail-fast validation for unknown attribute types and dense row length mismatches
- `failOnUnknownAttributeType(true)` fails on unsupported `@ATTRIBUTE` types instead of falling back to `STRING`
- `failOnRowLengthMismatch(true)` fails when a dense `@DATA` row has more or fewer values than declared attributes
- `fallbackMatrixName(...)` supplies a fallback matrix name when the ARFF file has no `@RELATION`

Default read behavior stays lenient:

- unknown attribute types fall back to `STRING`
- dense rows with too few values are padded with `null`
- dense rows with too many values ignore the trailing values
- malformed quoted rows still fail immediately because they are not valid ARFF syntax

## Options-First Write API

`ArffWriteOptions` can be used to control how the writer declares ARFF attribute types:

```groovy
import se.alipsa.matrix.arff.ArffTypeDecl
import se.alipsa.matrix.arff.ArffWriteOptions
import se.alipsa.matrix.arff.MatrixArffWriter

ArffWriteOptions options = new ArffWriteOptions()
    .inferNominals(false)
    .nominalColumns(['severity'])
    .nominalMappings([severity: ['high', 'medium', 'low']])   // explicit order is preserved
    .attributeTypesByColumn([
        createdAt: ArffTypeDecl.DATE,
        notes    : ArffTypeDecl.STRING
    ])
    .dateFormat('yyyy-MM-dd')
    .dateFormatsByColumn([createdAt: 'yyyy/MM/dd HH:mm'])

MatrixArffWriter.write(matrix, new File('configured.arff'), options)
```

The typed overloads are available for `File`, `Path`, `OutputStream`, `Writer`, and `writeString(...)`.

Convenience shortcuts such as `MatrixArffWriter.write(matrix, file)` and
`MatrixArffWriter.writeString(matrix)` are still available when you want the defaults.

Useful write options:

- `inferNominals(false)` disables the default String/Object nominal inference heuristic
- `nominalThreshold(n)` changes the maximum distinct-value count used by nominal inference
- `nominalColumns([...])` forces selected columns to be written as nominal
- `stringColumns([...])` forces selected columns to be written as `STRING`
- `attributeTypesByColumn([...])` forces a per-column ARFF type such as `STRING`, `NOMINAL`, `DATE`, `NUMERIC`, or `INTEGER`
- `nominalMappings([...])` supplies explicit nominal values and preserves their declared order
- `dateFormat(...)` and `dateFormatsByColumn([...])` control DATE declarations and output formatting

By default, nominal inference only applies when the distinct-value count is at or below `nominalThreshold` and, for datasets with 10 or more rows, the distinct-value count is also at or below 10% of the row count.

## Using Matrix.read() / matrix.write()

If `matrix-arff` is on the classpath, `.arff` files can also be handled through the generic Matrix SPI API:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.arff.ArffReadOptions
import se.alipsa.matrix.arff.ArffWriteOptions

Matrix iris = Matrix.read(new File('iris.arff'))
Matrix fallback = Matrix.read([fallbackMatrixName: 'fallback'], new File('no-relation.arff'))
Matrix strict = Matrix.read([strict: true], new File('dataset.arff'))

iris.write(new File('iris-copy.arff'))
iris.write([nominalMappings: [class: ['Iris-setosa', 'Iris-versicolor', 'Iris-virginica']]], new File('iris-nominal.arff'))

println Matrix.listReadOptions('arff')
println Matrix.listWriteOptions('arff')
println ArffReadOptions.describe()
println ArffWriteOptions.describe()
```

Schema control is available through the generic SPI API as well:

```groovy
matrix.write([
    inferNominals         : false,
    nominalColumns        : ['severity'],
    nominalMappings       : [severity: ['high', 'medium', 'low']],
    attributeTypesByColumn: [
        createdAt: 'DATE',
        notes    : 'STRING'
    ],
    dateFormatsByColumn   : [createdAt: 'yyyy/MM/dd HH:mm']
], new File('configured.arff'))

Matrix validated = Matrix.read([
    strict                 : true,
    failOnUnknownAttributeType: true,
    failOnRowLengthMismatch   : true
], new File('validated.arff'))
```

## Common Patterns

Sparse input with defaults:

```groovy
Matrix sparse = MatrixArffReader.readString("""
@RELATION sparse_metrics
@ATTRIBUTE score NUMERIC
@ATTRIBUTE note STRING
@DATA
{0 1.5}
{1 'late sample'}
{}
""".trim())
```

Strict import for validation:

```groovy
Matrix validated = MatrixArffReader.read(
    new File('incoming.arff'),
    new ArffReadOptions().strict(true)
)
```

Explicit schema control on write:

```groovy
MatrixArffWriter.write(
    matrix,
    new File('configured.arff'),
    new ArffWriteOptions()
        .inferNominals(false)
        .attributeTypesByColumn([category: ArffTypeDecl.STRING])
        .dateFormatsByColumn([created: 'yyyy-MM-dd'])
)
```

Generic Matrix SPI round-trip:

```groovy
Matrix matrix = Matrix.read([fallbackMatrixName: 'fallback'], new File('input.arff'))
matrix.write([inferNominals: false], new File('output.arff'))
```
