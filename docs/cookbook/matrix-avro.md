# Matrix Avro

## Override the Matrix name on read

```groovy
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

---
[Back to index](cookbook.md)
