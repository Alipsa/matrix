# Matrix Avro Module

The matrix-avro module provides reading and writing of Avro Object Container Files (`.avro`) for `Matrix`.

## Reading Avro Files

Use `MatrixAvroReader` directly when you want typed read options:

```groovy
import org.apache.avro.Schema
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.MatrixAvroReader

Schema readerSchema = new Schema.Parser().parse("""
{
  "type": "record",
  "name": "Person",
  "fields": [
    {"name":"name", "type":"string"},
    {"name":"age", "type":"long"},
    {"name":"country", "type":["null","string"], "default": null}
  ]
}
""")

Matrix people = MatrixAvroReader.read(
    new File("people.avro"),
    new AvroReadOptions()
        .matrixName("People")
        .readerSchema(readerSchema)
)
```

Read naming precedence is:

1. `AvroReadOptions.matrixName(...)` when supplied
2. the Avro record name from the file schema
3. a source-derived fallback such as the file name or `AvroMatrix`

## Using the Matrix SPI

```groovy
import se.alipsa.matrix.core.Matrix

Matrix people = Matrix.read([matrixName: 'People'], new File('people.avro'))
```

Useful read options:

- `matrixName(...)` overrides the resulting Matrix name
- `readerSchema(...)` supplies a reader schema for schema evolution and projection

## Default Read Behavior

- Avro logical types are converted to Java values during import
- arrays are read as `List`
- maps are read as `Map<String, ?>`
- nested records are read as `Map<String, Object>`

## Writing Avro Files

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter

Matrix people = Matrix.builder("People")
    .columns(id: [1, 2], amount: [new BigDecimal("12.34"), new BigDecimal("56.78")])
    .types(Integer, BigDecimal)
    .build()

MatrixAvroWriter.write(people, new File("people.avro"), new AvroWriteOptions()
    .inferPrecisionAndScale(true)
    .compression(AvroWriteOptions.Compression.DEFLATE)
    .compressionLevel(6)
    .syncInterval(64000)
)
```

Write naming precedence is:

1. `AvroWriteOptions.schemaName(...)` when supplied
2. `matrix.matrixName` when present
3. `MatrixSchema`

Useful write options:

- `inferPrecisionAndScale(...)` to store `BigDecimal` columns as Avro decimals
- `namespace(...)` to control the Avro schema namespace
- `schemaName(...)` to override the default record name
- `columnSchema(...)` / `columnSchemas(...)` to override decimal, array, map, and record inference per column
- `compression(...)`, `compressionLevel(...)`, and `syncInterval(...)` for container-file tuning

## Override schema inference for specific columns

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

The equivalent Matrix SPI map is:

```groovy
nested.write([
    columnSchemas: [
        amount: [kind: 'decimal', precision: 12, scale: 3],
        tags  : [kind: 'array', elementType: 'LONG'],
        props : [kind: 'map', valueType: 'INT'],
        person: [
            kind      : 'record',
            recordName: 'PersonRecord',
            fields    : [name: 'STRING', age: 'INT']
        ]
    ]
], new File("nested-spi.avro"))
```
