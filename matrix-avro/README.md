[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-avro/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-avro)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-avro/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-avro)
# matrix-avro

[Avro](https://avro.apache.org/) read/write support for [Matrix](https://github.com/Alipsa/matrix).
This module reads and writes Avro Object Container Files (`.avro`) with support for logical types, schema evolution, and nested arrays, maps, and record-like values.

## At A Glance

- read Avro from `File`, `Path`, `URL`, `InputStream`, and byte arrays
- write Avro to `File`, `Path`, `OutputStream`, and byte arrays
- control reads with `AvroReadOptions`
- control writes with `AvroWriteOptions`
- override nested schema inference explicitly with `AvroSchemaDecl`
- use the generic Matrix SPI through `Matrix.read(...)`, `matrix.write(...)`, `Matrix.listReadOptions(...)`, and `Matrix.listWriteOptions(...)`

## Getting Started

Add the module to your build. Versions below are examples; prefer the Matrix BOM when you use multiple modules.

```groovy
dependencies {
  implementation platform("se.alipsa.matrix:matrix-bom:<version>")
  implementation "se.alipsa.matrix:matrix-core"
  implementation "se.alipsa.matrix:matrix-avro"
}
```

## API Surface

Direct API entry points:

- `MatrixAvroReader.read(...)` for `File`, `Path`, `URL`, `InputStream`, and `byte[]`
- `MatrixAvroWriter.write(...)` for `File`, `Path`, and `OutputStream`
- `MatrixAvroWriter.writeBytes(...)` for in-memory export
- `AvroReadOptions` for naming and schema evolution
- `AvroWriteOptions` for schema naming, decimal behavior, compression, and explicit nested schema control
- `AvroSchemaDecl` for per-column decimal, array, map, record, and scalar overrides

Generic Matrix SPI entry points:

- `Matrix.read(new File('data.avro'))`
- `matrix.write(new File('data.avro'))`
- `Matrix.listReadOptions('avro')`
- `Matrix.listWriteOptions('avro')`
- `AvroReadOptions.describe()`
- `AvroWriteOptions.describe()`

Runtime option discovery:

```groovy
println Matrix.listReadOptions('avro')
println Matrix.listWriteOptions('avro')
println AvroReadOptions.describe()
println AvroWriteOptions.describe()
```

## Options-First Read API

Use `AvroReadOptions` as the primary read surface when you want explicit control over naming or schema evolution:

```groovy
import org.apache.avro.Schema
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.core.Matrix

Schema projection = new Schema.Parser().parse("""
{
  "type": "record",
  "name": "UserProjection",
  "fields": [
    {"name":"id", "type":"int"},
    {"name":"name", "type":["null","string"], "default": null}
  ]
}
""")

AvroReadOptions readOptions = new AvroReadOptions()
    .matrixName('Users')
    .readerSchema(projection)

Matrix users = MatrixAvroReader.read(new File('users.avro'), readOptions)
```

Useful read options:

- `matrixName(...)` overrides the resulting Matrix name
- `readerSchema(...)` supplies an Avro reader schema for schema evolution or projection

### Convenience Shortcuts

Convenience overloads remain available for compatibility, but the typed `AvroReadOptions` API is the primary path:

```groovy
Matrix fromFile = MatrixAvroReader.read(new File('users.avro'))
Matrix fromPath = MatrixAvroReader.read(Path.of('users.avro'))
Matrix fromUrl = MatrixAvroReader.read(new URL('file:users.avro'))
Matrix fromBytes = MatrixAvroReader.read(avroBytes)
```

## Options-First Write API

Use `AvroWriteOptions` as the primary write surface when you want explicit schema behavior:

```groovy
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

Matrix orders = Matrix.builder('Orders')
    .columns(
        id: [1, 2],
        amount: [new BigDecimal('12.34'), new BigDecimal('56.78')]
    )
    .types(Integer, BigDecimal)
    .build()

AvroWriteOptions writeOptions = new AvroWriteOptions()
    .inferPrecisionAndScale(true)
    .namespace('com.example.avro')
    .compression(AvroWriteOptions.Compression.DEFLATE)
    .compressionLevel(6)
    .syncInterval(64000)

MatrixAvroWriter.write(orders, new File('orders.avro'), writeOptions)
```

Useful write options:

- `inferPrecisionAndScale(...)` stores `BigDecimal` columns as Avro decimal logical types instead of falling back to `double`
- `namespace(...)` controls the generated Avro namespace
- `schemaName(...)` overrides the generated record name
- `compression(...)`, `compressionLevel(...)`, and `syncInterval(...)` tune the container file
- `columnSchema(...)` and `columnSchemas(...)` override nested schema inference per column

### Convenience Shortcuts

Convenience overloads still exist for default behavior:

```groovy
MatrixAvroWriter.write(matrix, new File('data.avro'))
MatrixAvroWriter.write(matrix, new File('data.avro'), true)
byte[] bytes = MatrixAvroWriter.writeBytes(matrix)
```

They are shortcuts over the typed API, not the primary configuration surface.

## Explicit Schema Control

Use `AvroSchemaDecl` when nested sampling heuristics are not enough:

```groovy
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions

AvroWriteOptions options = new AvroWriteOptions()
    .columnSchema('amount', AvroSchemaDecl.decimal(12, 2))
    .columnSchema('tags', AvroSchemaDecl.array(AvroSchemaDecl.type(Long)))
    .columnSchema('props', AvroSchemaDecl.map(AvroSchemaDecl.type(Integer)))
    .columnSchema('person', AvroSchemaDecl.record('PersonRecord', [
        name: AvroSchemaDecl.type(String),
        age : AvroSchemaDecl.type(Integer)
    ]))
```

Supported declaration kinds:

- `decimal(precision, scale)` for fixed decimal metadata
- `array(...)` for explicit array element types
- `map(...)` for explicit map value types
- `record(...)` for explicit nested record fields
- `type(...)` or `scalar(...)` for direct scalar overrides

## Using Matrix.read() / matrix.write()

If `matrix-avro` is on the classpath, `.avro` files are also available through the generic Matrix SPI API:

```groovy
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.core.Matrix

Matrix data = Matrix.read(new File('users.avro'))
Matrix renamed = Matrix.read([matrixName: 'Users'], new File('users.avro'))

data.write([
    inferPrecisionAndScale: true,
    schemaName            : 'Users'
], new File('users-copy.avro'))

data.write([
    columnSchemas: [
        amount: [kind: 'decimal', precision: 12, scale: 2],
        props : [kind: 'map', valueType: 'INT'],
        person: [
            kind      : 'record',
            recordName: 'PersonRecord',
            fields    : [name: 'STRING', age: 'INT']
        ]
    ]
], new File('users-explicit.avro'))
```

SPI round-tripping is available for both read and write options through `toMap()` / `fromMap(...)`.

## Default Behavior

Read defaults:

- Matrix naming precedence is `AvroReadOptions.matrixName(...)`, then the Avro record name, then a source-derived fallback such as the file name or `AvroMatrix`
- Avro `uuid` values are read as `String`, not `UUID`
- logical types such as `date`, `time-millis`, `timestamp-millis`, `local-timestamp-micros`, and `decimal` are converted to Java values during import
- nested arrays read as `List<?>`, maps as `Map<String, ?>`, and records as `Map<String, Object>`

Write defaults:

- schema naming precedence is `AvroWriteOptions.schemaName(...)`, then `matrix.matrixName`, then `MatrixSchema`
- `inferPrecisionAndScale` defaults to `false`, so `BigDecimal` columns fall back to Avro `double`
- `namespace` defaults to `se.alipsa.matrix.avro`
- `compression` defaults to `NULL`, `compressionLevel` to `-1`, and `syncInterval` to `0`

Nested-type heuristics:

- list element types are inferred from the first non-null element seen in the column
- map value types are inferred from the first non-null value seen in the column
- maps are encoded as records only when all non-null rows share the same key set
- mixed numeric `Object` columns promote to `int`, `long`, or `double` based on observed values
- `columnSchemas` takes precedence over all of the above when present

## Common Patterns

### Schema Evolution with a Reader Schema

```groovy
Schema projection = new Schema.Parser().parse("""
{
  "type": "record",
  "name": "ProjectedOrders",
  "fields": [
    {"name":"id", "type":"int"},
    {"name":"amount", "type":["null","bytes"], "default": null,
     "logicalType":"decimal", "precision":12, "scale":2}
  ]
}
""")

Matrix projected = MatrixAvroReader.read(
    new File('orders.avro'),
    new AvroReadOptions().readerSchema(projection)
)
```

### Decimal-Safe Writes

```groovy
MatrixAvroWriter.write(
    orders,
    new File('orders.avro'),
    new AvroWriteOptions().inferPrecisionAndScale(true)
)
```

### Custom Schema Naming

```groovy
MatrixAvroWriter.write(
    orders,
    new File('orders.avro'),
    new AvroWriteOptions()
        .schemaName('OrderRecord')
        .namespace('com.example.orders')
)
```

### Force Map vs Record Behavior

```groovy
AvroWriteOptions options = new AvroWriteOptions()
    .columnSchema('props', AvroSchemaDecl.map(AvroSchemaDecl.type(Integer)))
    .columnSchema('person', AvroSchemaDecl.record([
        name: AvroSchemaDecl.type(String),
        age : AvroSchemaDecl.type(Integer)
    ]))
```

## Troubleshooting

- Unexpected `String` for a UUID column on read: this is expected; Avro `uuid` is imported as `String`
- `BigDecimal` round-trips as `Double`: enable `inferPrecisionAndScale(true)` or use `columnSchema('col', AvroSchemaDecl.decimal(...))`
- A map column becomes an Avro record: that happens when the non-null rows all share the same key set; force map encoding with `AvroSchemaDecl.map(...)`
- A list or map uses the wrong nested type: the default heuristic samples the first non-null element or value; use `columnSchemas` when the sample is not representative
- Invalid `compressionLevel` or `syncInterval`: option validation is fail-fast and happens both in fluent configuration and `fromMap(...)`

## Type Mapping

| Matrix / Java type           | Avro physical type | Avro logical type           | Notes                                              |
|------------------------------|-------------------:|-----------------------------|----------------------------------------------------|
| `String`                     |           `string` | —                           |                                                    |
| `Boolean`                    |          `boolean` | —                           |                                                    |
| `Integer`                    |              `int` | —                           |                                                    |
| `Long`, `BigInteger`         |             `long` | —                           |                                                    |
| `Float`                      |            `float` | —                           |                                                    |
| `Double`                     |           `double` | —                           |                                                    |
| `BigDecimal` (infer=false)   |           `double` | —                           | fallback                                           |
| `BigDecimal` (infer=true)    |            `bytes` | `decimal(precision, scale)` | inferred or explicit                               |
| `byte[]`                     |            `bytes` | —                           |                                                    |
| `LocalDate`, `java.sql.Date` |              `int` | `date`                      | days since epoch                                   |
| `LocalTime`, `java.sql.Time` |              `int` | `time-millis`               | ms since midnight                                  |
| `Instant`, `java.util.Date`  |             `long` | `timestamp-millis`          | epoch millis                                       |
| `LocalDateTime`              |             `long` | `local-timestamp-micros`    | zone-less                                          |
| `UUID`                       |           `string` | `uuid`                      | reads back as `String`                             |
| `List<T>`                    |            `array` | —                           | elements are nullable                              |
| `Map<String,V>`              |  `map` or `record` | —                           | values/fields are nullable                         |

## Examples & Tests

See `src/test/groovy/test/alipsa/matrix/avro/`:

- `MatrixAvroReaderTest` for the read entry points and naming precedence
- `MatrixAvroWriterTest` for schema generation and option validation
- `MatrixAvroRoundTripTest` for logical types, decimals, and nested collections
- `AvroFormatProviderTest` for SPI option parsing and round-tripping

## License

Same license as the parent Matrix project (MIT). See the repository [LICENSE file](../LICENSE).
