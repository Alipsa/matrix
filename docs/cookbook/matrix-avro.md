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

---
[Back to index](cookbook.md)
