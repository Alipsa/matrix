# Matrix Avro Module

This page walks through the Avro module with the typed options APIs first, then shows the compatibility shortcuts and the Matrix SPI equivalents.

## At A Glance

- use `AvroReadOptions` for naming and schema evolution
- use `AvroWriteOptions` for schema naming, decimal behavior, compression, and explicit nested schema control
- use `AvroSchemaDecl` when list or map sampling heuristics are not enough
- use `Matrix.listReadOptions('avro')`, `Matrix.listWriteOptions('avro')`, `AvroReadOptions.describe()`, and `AvroWriteOptions.describe()` to inspect the current option surface at runtime

## Discover the Available Options

```groovy
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.core.Matrix

println Matrix.listReadOptions('avro')
println Matrix.listWriteOptions('avro')
println AvroReadOptions.describe()
println AvroWriteOptions.describe()
```

## Read Avro with Typed Options

Use `AvroReadOptions` when you want explicit naming or a reader schema:

```groovy
import org.apache.avro.Schema
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.core.Matrix

Schema projection = new Schema.Parser().parse("""
{
  "type": "record",
  "name": "PersonProjection",
  "fields": [
    {"name":"id", "type":"int"},
    {"name":"name", "type":["null","string"], "default": null}
  ]
}
""")

AvroReadOptions options = new AvroReadOptions()
    .matrixName('People')
    .readerSchema(projection)

Matrix people = MatrixAvroReader.read(new File('people.avro'), options)
```

Read naming precedence is:

1. `AvroReadOptions.matrixName(...)`
2. the Avro record name from the file schema
3. a source-derived fallback such as the file name or `AvroMatrix`

Default read behavior:

- Avro `uuid` values are converted to `String`
- Avro `decimal` values are converted to `BigDecimal`
- nested arrays become `List<?>`
- nested maps become `Map<String, ?>`
- nested records become `Map<String, Object>`

### Convenience Shortcuts

The convenience overloads remain available, but they sit below the typed API:

```groovy
Matrix fromFile = MatrixAvroReader.read(new File('people.avro'))
Matrix fromPath = MatrixAvroReader.read(Path.of('people.avro'))
Matrix fromUrl = MatrixAvroReader.read(new URL('file:people.avro'))
Matrix fromBytes = MatrixAvroReader.read(avroBytes)
```

## Write Avro with Typed Options

Use `AvroWriteOptions` when you want the current writer behavior to be explicit:

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

AvroWriteOptions options = new AvroWriteOptions()
    .inferPrecisionAndScale(true)
    .schemaName('OrderRecord')
    .namespace('com.example.orders')
    .compression(AvroWriteOptions.Compression.DEFLATE)
    .compressionLevel(6)

MatrixAvroWriter.write(orders, new File('orders.avro'), options)
```

Write naming precedence is:

1. `AvroWriteOptions.schemaName(...)`
2. `matrix.matrixName`
3. `MatrixSchema`

Default write behavior:

- `inferPrecisionAndScale` defaults to `false`, so `BigDecimal` falls back to Avro `double`
- `namespace` defaults to `se.alipsa.matrix.avro`
- `compression` defaults to `NULL`
- `compressionLevel` defaults to `-1`
- `syncInterval` defaults to `0`

### Convenience Shortcuts

The convenience overloads are still available when you want defaults without constructing options:

```groovy
MatrixAvroWriter.write(orders, new File('orders.avro'))
MatrixAvroWriter.write(orders, new File('orders.avro'), true)
byte[] bytes = MatrixAvroWriter.writeBytes(orders)
```

## Schema Evolution with `readerSchema(...)`

`readerSchema(...)` is the typed entry point for projection and schema evolution:

```groovy
Schema projection = new Schema.Parser().parse("""
{
  "type": "record",
  "name": "ProjectedOrders",
  "fields": [
    {"name":"id", "type":"int"},
    {"name":"amount", "type":["null","double"], "default": null}
  ]
}
""")

Matrix projected = MatrixAvroReader.read(
    new File('orders.avro'),
    new AvroReadOptions().readerSchema(projection)
)
```

Use this when you want a stable consumer-side schema instead of reading every field from the writer schema.

## Override Nested Schema Inference Explicitly

When the nested sampling heuristics are not representative, use `AvroSchemaDecl`:

```groovy
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter

Matrix nested = Matrix.builder("Nested")
    .columns(
        amount: [new BigDecimal("12.340"), new BigDecimal("56.780")],
        tags: [[1, 2], [3L, null]],
        props: [[x: 1], [y: 2]],
        person: [[name: "Alice"], [age: 41]]
    )
    .types(BigDecimal, List, Map, Map)
    .build()

MatrixAvroWriter.write(nested, new File("nested.avro"), new AvroWriteOptions()
    .columnSchema('amount', AvroSchemaDecl.decimal(12, 3))
    .columnSchema('tags', AvroSchemaDecl.array(AvroSchemaDecl.type(Long)))
    .columnSchema('props', AvroSchemaDecl.map(AvroSchemaDecl.type(Integer)))
    .columnSchema('person', AvroSchemaDecl.record('PersonRecord', [
        name: AvroSchemaDecl.type(String),
        age : AvroSchemaDecl.type(Integer)
    ]))
)
```

Default nested heuristics when you do not override them:

- list element type comes from the first non-null element seen in the column
- map value type comes from the first non-null value seen in the column
- maps are written as records only when all non-null rows share the same key set

## Use the Matrix SPI

The generic Matrix SPI can round-trip the same behavior through maps:

```groovy
import se.alipsa.matrix.core.Matrix

Matrix loaded = Matrix.read([
    matrixName  : 'People',
    readerSchema: projection
], new File('people.avro'))

loaded.write([
    inferPrecisionAndScale: true,
    schemaName            : 'PeopleCopy',
    columnSchemas         : [
        amount: [kind: 'decimal', precision: 12, scale: 2],
        props : [kind: 'map', valueType: 'INT'],
        person: [
            kind      : 'record',
            recordName: 'PersonRecord',
            fields    : [name: 'STRING', age: 'INT']
        ]
    ]
], new File('people-copy.avro'))
```

The SPI maps are useful when you want format-agnostic entry points, but the typed `AvroReadOptions` and `AvroWriteOptions` APIs remain the preferred documentation path.

## Troubleshooting

- A UUID column reads back as `String`: this is the intended read behavior for Avro `uuid`
- A `BigDecimal` column reads back as `Double`: enable `inferPrecisionAndScale(true)` or declare the column with `AvroSchemaDecl.decimal(...)`
- A map column became a record: that happens when the non-null rows share one key set; force map encoding with `AvroSchemaDecl.map(...)`
- A list or map used the wrong nested type: the default inference uses the first non-null sample; use `columnSchema(...)` when the sample is misleading
- Invalid `compressionLevel` or `syncInterval`: the writer validates these fail-fast when options are built or parsed from SPI maps

## Next Steps

- use the [Avro cookbook](../cookbook/matrix-avro.md) for focused recipes
- use the [module README](../../matrix-avro/README.md) as the complete API reference
