# matrix-avro

Avro read/write support for [Matrix](https://github.com/Alipsa/matrix).  
This module lets you round-trip data between Avro Object Container Files (`*.avro`) and `se.alipsa.matrix.core.Matrix`, including Avro logical types, and collections (arrays, maps, and record-like structures).

## Features

- Read Avro OCF → `Matrix` (`MatrixAvroReader`)
- Write `Matrix` → Avro OCF (`MatrixAvroWriter`)
- Logical types mapped to idiomatic Java time/number types
- Optional `BigDecimal` → Avro `decimal(bytes)` with **per-column** precision/scale inference
- Arrays, maps, and record-like nested data supported
- All top-level fields are nullable (Avro unions like `["null", T]`)

## Getting started

Add the module to your build (versions are examples; prefer the Matrix BOM if you use it):

```groovy
dependencies {
  api "se.alipsa.matrix:matrix-core:<version>"
  implementation "se.alipsa.matrix:matrix-avro:<version>"
  // Avro runtime is brought in transitively; if you need to pin:
  implementation "org.apache.avro:avro:1.11.4"
}
```

> In a multi-module build: add `include 'matrix-avro'` to `settings.gradle`.

## Usage

### Read Avro → Matrix

```groovy
import se.alipsa.matrix.avro.MatrixAvroReader

import java.nio.file.Paths

def m1 = MatrixAvroReader.read(new File("data/users.avro"))
def m2 = MatrixAvroReader.read(new URL("file:data/users.avro"))
def m3 = MatrixAvroReader.read(Paths.get("data/users.avro"))
println m1.dim()           // [rows, cols]
println m1.columnNames()
```

### Write Matrix → Avro

```groovy
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix
import java.time.*

def cols = [
  id       : [1, 2, 3],
  name     : ["Alice","Bob",null],
  birthday : [LocalDate.of(1990,1,5), null, LocalDate.of(1984,7,23)],
  ldt      : [LocalDateTime.of(2024,7,1,10,20,30,999_000_000), null, LocalDateTime.now()],
  price    : [new BigDecimal("12.34"), null, new BigDecimal("1000.50")],
  tags     : [[ "a","b" ], [], [ "x", null, "z" ]],         // ARRAY
  props    : [[x:1, y:2], [y:5, z:9], null],                 // MAP (varying keys)
  person   : [[name:"Ann", age:30], [name:"Bo", age:41], [:]] // RECORD-like (fixed keys)
] as LinkedHashMap

def m = Matrix.builder("Example").columns(cols).build()

// inferPrecisionAndScale=true → BigDecimal columns use Avro decimal(bytes)
MatrixAvroWriter.write(m, new File("out/example.avro"), true)
```

## Type mapping

| Matrix / Java type           | Avro physical type | Avro logical type           | Notes                                              |
|------------------------------|-------------------:|-----------------------------|----------------------------------------------------|
| `String`                     |           `string` | —                           |                                                    |
| `Boolean`                    |          `boolean` | —                           |                                                    |
| `Integer`                    |              `int` | —                           |                                                    |
| `Long`, `BigInteger`         |             `long` | —                           |                                                    |
| `Float`                      |            `float` | —                           |                                                    |
| `Double`                     |           `double` | —                           |                                                    |
| `BigDecimal` (infer=false)   |           `double` | —                           | fallback for compatibility                         |
| `BigDecimal` (infer=true)    |            `bytes` | `decimal(precision, scale)` | per-column inference                               |
| `byte[]`                     |            `bytes` | —                           |                                                    |
| `LocalDate`, `java.sql.Date` |              `int` | `date`                      | days since epoch                                   |
| `LocalTime`, `java.sql.Time` |              `int` | `time-millis`               | ms since midnight                                  |
| `Instant`, `java.util.Date`  |             `long` | `timestamp-millis`          | epoch millis (UTC)                                 |
| `LocalDateTime`              |             `long` | `local-timestamp-micros`    | zone-less, micros precision                        |
| `UUID`                       |           `string` | `uuid`                      | stored as canonical string                         |
| `List<T>`                    |            `array` | —                           | items are **nullable** unions `["null", itemType]` |
| `Map<String,V>`              |  `map` or `record` | —                           | values are **nullable**; see below                 |

### Arrays, Maps & Record-like nested data

- **Arrays**: element type inferred from first non-null element; elements are written as `["null", T]` to allow `null` entries.
- **Maps**:
  - If **keys vary** across rows → written as Avro `map` of `["null", V]`.
  - If **keys are identical** across rows → written as an Avro `record` with one field per key (each field `["null", T]`).
- On read, nested values become standard Java collections:
  - arrays → `List<?>`
  - maps → `Map<String,?>`
  - records → `Map<String, Object>`

## Decimal handling (`inferPrecisionAndScale`)

- `false` (default): `BigDecimal` columns are stored as `double`. Simple and broadly compatible.
- `true`: `BigDecimal` columns are stored as Avro `decimal(bytes)` with `(precision, scale)` inferred from the column’s non-null values. This enables lossless round-trip.

## Nullability

- All **top-level** fields are nullable (`["null", T]`).
- Array **elements** and map **values** are also nullable (`["null", T]`).
- Record-like fields are nullable per field.

## Precision notes

- `LocalDateTime` is written as `local-timestamp-micros` to preserve sub-second precision (reader supports both `…-micros` and `…-millis`).
- `LocalTime` uses `time-millis` (ms resolution).
- If your source `Matrix` already contains truncated values, that truncation carries into Avro.

## Limitations / heuristics

- Element/value types for arrays/maps are inferred from the **first non-null** sample in the column.
- Record-like detection for maps is heuristic: if the **key set is identical** for all non-null rows, it’s encoded as a record; otherwise a map.
- Mixed numeric columns are promoted: integers → `int`/`long` depending on range; presence of a floating value promotes to `double`. Heterogeneous arrays currently infer from first non-null element.

## Examples & tests

See `src/test/groovy/se/alipsa/matrix/avro/`:
- `MatrixAvroReaderTest` – read methods (File, Path, URL, InputStream)
- `MatrixAvroRoundTripTest` – end-to-end round-trip including logical types & decimals
- `MatrixAvroWriterTest` – schema sanity (logical types)
- `MatrixAvroArrayMapRecordRoundTripTest` – arrays, maps, record-like structures

## Troubleshooting

- **UnresolvedUnionException** for arrays/maps: ensure array **items** and map **values** are nullable unions (`["null", T]`). This module does that automatically.
- **Lost `.999` on `LocalDateTime`**: confirm your source `Matrix` retains nanos (the writer uses `local-timestamp-micros`).
- **BigDecimal rounding**: with `inferPrecisionAndScale=true` we write `decimal(bytes)` using the inferred scale; when writing floating values into a decimal column, values are rounded using `HALF_UP`.

## License

Same license as the parent Matrix project (i.e. MIT). See the repository [LICENSE file](../LICENSE).
