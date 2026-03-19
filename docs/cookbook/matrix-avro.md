# Matrix Avro

Focused recipes for the options-first Avro APIs.

## Inspect the available Avro options at runtime

```groovy
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.core.Matrix

println Matrix.listReadOptions('avro')
println Matrix.listWriteOptions('avro')
println AvroReadOptions.describe()
println AvroWriteOptions.describe()
```

## Override the Matrix name on read

```groovy
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.MatrixAvroReader

Matrix users = MatrixAvroReader.read(
    new File('users.avro'),
    new AvroReadOptions().matrixName('Users')
)
```

Read naming precedence is `matrixName(...)`, then the Avro record name, then a source-derived fallback such as the file name or `AvroMatrix`.

## Project an Avro file through a reader schema

```groovy
import org.apache.avro.Schema
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.MatrixAvroReader

Schema projection = new Schema.Parser().parse("""
{
  "type": "record",
  "name": "PersonProjection",
  "fields": [
    {"name":"name", "type":"string"},
    {"name":"age", "type":"long"}
  ]
}
""")

Matrix projected = MatrixAvroReader.read(
    new File('people.avro'),
    new AvroReadOptions().readerSchema(projection)
)
```

## Write with the Matrix name as the default Avro schema name

```groovy
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

Matrix orders = Matrix.builder('Orders')
    .columns(id: [1, 2], total: [10.50, 22.75])
    .types(Integer, BigDecimal)
    .build()

MatrixAvroWriter.write(orders, new File('orders.avro'))
```

Write naming precedence is `schemaName(...)`, then `matrix.matrixName`, then `MatrixSchema`.

## Write decimals safely

```groovy
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter

MatrixAvroWriter.write(orders, new File('orders-decimal.avro'), new AvroWriteOptions()
    .inferPrecisionAndScale(true)
)
```

Without `inferPrecisionAndScale(true)`, `BigDecimal` columns fall back to Avro `double`.

## Force a fixed decimal schema for one column

```groovy
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter

MatrixAvroWriter.write(orders, new File('orders-fixed-decimal.avro'), new AvroWriteOptions()
    .columnSchema('total', AvroSchemaDecl.decimal(12, 2))
)
```

## Write with a custom schema name and namespace

```groovy
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter

MatrixAvroWriter.write(orders, new File('orders-named.avro'), new AvroWriteOptions()
    .schemaName('OrderRecord')
    .namespace('com.example.orders')
)
```

## Force map encoding instead of record-like inference

```groovy
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.core.Matrix

Matrix nested = Matrix.builder('Nested')
    .columns(props: [[x: 1, y: 2], [x: 3, y: 4]])
    .types(Map)
    .build()

nested.write([
    columnSchemas: [
        props: [kind: 'map', valueType: 'INT']
    ]
], new File('nested-map.avro'))
```

By default, a map column becomes a record only when every non-null row uses the same key set.

## Force record encoding for a map column with varying keys

```groovy
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

Matrix people = Matrix.builder('People')
    .columns(person: [[name: 'Alice'], [age: 41]])
    .types(Map)
    .build()

MatrixAvroWriter.write(people, new File('people-record.avro'), new AvroWriteOptions()
    .columnSchema('person', AvroSchemaDecl.record('PersonRecord', [
        name: AvroSchemaDecl.type(String),
        age : AvroSchemaDecl.type(Integer)
    ]))
)
```

## Force a list element type explicitly

```groovy
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

Matrix data = Matrix.builder('TagData')
    .columns(tags: [[1, 2], [3L, null]])
    .types(List)
    .build()

MatrixAvroWriter.write(data, new File('tags.avro'), new AvroWriteOptions()
    .columnSchema('tags', AvroSchemaDecl.array(AvroSchemaDecl.type(Long)))
)
```

The heuristic path samples the first non-null list element when you do not supply a declaration.

## Round-trip through the generic Matrix SPI

```groovy
import se.alipsa.matrix.core.Matrix

Matrix source = Matrix.read([
    matrixName  : 'Users',
    readerSchema: projection
], new File('users.avro'))

source.write([
    inferPrecisionAndScale: true,
    schemaName            : 'UsersCopy',
    columnSchemas         : [
        total: [kind: 'decimal', precision: 12, scale: 2]
    ]
], new File('users-copy.avro'))
```

## Common troubleshooting

- UUID reads back as `String`: expected; Avro `uuid` is imported as `String`
- `BigDecimal` reads back as `Double`: expected when `inferPrecisionAndScale` is left at its default `false`
- Nested type looks wrong: the default heuristic uses the first non-null sample for lists and map values
- Map unexpectedly became a record: all non-null rows shared the same key set, so the writer treated it as record-like

---
[Back to index](cookbook.md)
