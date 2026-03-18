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
