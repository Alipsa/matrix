# Matrix Json

## Reading JSON

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReader

// From string
Matrix m = JsonReader.read('[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]')

// From file (matrix name derived from filename)
Matrix m = JsonReader.read(new File('employees.json'))

// From URL
Matrix m = JsonReader.readUrl('https://api.example.com/data.json')

// Via SPI with type conversion
Matrix m = Matrix.read(
    [types: [Integer, String, LocalDate], dateTimeFormat: 'yyyy-MM-dd'],
    new File('data.json')
)
```

## Writing JSON

```groovy
import se.alipsa.matrix.json.JsonWriter

// To file with pretty-printing
JsonWriter.write(matrix).indent().to(new File('out.json'))

// To string
String json = JsonWriter.write(matrix).asString()

// With custom date format and column formatter
String json = JsonWriter.write(matrix)
    .dateFormat('dd/MM/yyyy')
    .formatter('price') { "\$${it}" }
    .indent()
    .asString()

// Via SPI
matrix.write([indent: true, dateFormat: 'yyyy/MM/dd'], new File('out.json'))
```

## Nested JSON

Nested objects flatten to dot-notation, arrays to bracket notation:

```groovy
// {"person":{"name":"Alice"},"scores":[90,95]}
// becomes columns: person.name, scores[0], scores[1]
```

---
[Back to index](cookbook.md)  |  [Next (Matrix Spreadsheet)](matrix-spreadsheet.md)
