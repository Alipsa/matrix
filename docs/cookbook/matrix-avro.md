# Matrix Avro

## Override the Matrix name on read

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.MatrixAvroReader

Matrix users = MatrixAvroReader.read(
    new File('users.avro'),
    new AvroReadOptions().matrixName('Users')
)
```

## Project an Avro file through a reader schema

```groovy
import org.apache.avro.Schema
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.MatrixAvroReader

Schema projection = new Schema.Parser().parse("""
{
  "type": "record",
  "name": "Person",
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

## Use the generic Matrix SPI

```groovy
import se.alipsa.matrix.core.Matrix

Matrix users = Matrix.read([matrixName: 'Users'], new File('users.avro'))
```

## Write with the Matrix name as the default Avro schema name

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.avro.MatrixAvroWriter

Matrix orders = Matrix.builder('Orders')
    .columns(id: [1, 2], total: [10.50, 22.75])
    .types(Integer, BigDecimal)
    .build()

MatrixAvroWriter.write(orders, new File('orders.avro'))
```

## Write with compression and decimal inference

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter

Matrix orders = Matrix.builder('Orders')
    .columns(id: [1, 2], total: [new BigDecimal('10.50'), new BigDecimal('22.75')])
    .types(Integer, BigDecimal)
    .build()

MatrixAvroWriter.write(orders, new File('orders-compressed.avro'), new AvroWriteOptions()
    .inferPrecisionAndScale(true)
    .compression(AvroWriteOptions.Compression.DEFLATE)
    .compressionLevel(6)
    .syncInterval(64000)
)
```

## Force a fixed decimal schema for one column

```groovy
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter

MatrixAvroWriter.write(orders, new File('orders-decimal.avro'), new AvroWriteOptions()
    .columnSchema('total', AvroSchemaDecl.decimal(12, 2))
)
```

## Force map encoding instead of record-like inference

```groovy
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions

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

## Force record encoding for a map column with varying keys

```groovy
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroWriter

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

---
[Back to index](cookbook.md)
