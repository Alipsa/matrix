package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.file.DataFileStream
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericFixed
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import se.alipsa.matrix.core.Matrix

import java.nio.ByteBuffer
import java.nio.file.Path
import java.time.*

@CompileStatic
class MatrixAvroReader {

  /** Read from a File */
  static Matrix read(File file, String name = null) {
    name = name ?: (file.name ?: "AvroMatrix")
    InputStream is = new FileInputStream(file)
    try {
      return read(is, name)
    } finally {
      is.close()
    }
  }

  /** Read from a Path */
  static Matrix read(Path path) {
    return read(path.toFile())
  }

  /** Read from a URL */
  static Matrix read(URL url, String name = url.getFile() ?: "AvroMatrix") {
    InputStream is = url.openStream()
    try {
      return read(is, name)
    } finally {
      is.close()
    }
  }

  /** Read from a byte array */
  static Matrix read(byte[] content, String name = "AvroMatrix") {
    if (content == null) {
      throw new IllegalArgumentException("Content cannot be null")
    }
    return read(new ByteArrayInputStream(content), name)
  }

  /** Read from an InputStream (Avro OCF). Stream will be closed by caller if needed. */
  static Matrix read(InputStream input, String name = "AvroMatrix") {
    GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<>()
    DataFileStream<GenericRecord> dfs = new DataFileStream<>(input, datumReader)
    try {
      Schema schema = dfs.schema
      List<Schema.Field> fields = schema.fields

      LinkedHashMap<String, List<Object>> columns = new LinkedHashMap<>()
      for (Schema.Field f : fields) {
        columns.put(f.name(), new ArrayList<>())
      }

      for (GenericRecord rec : dfs) {
        for (Schema.Field f : fields) {
          Object raw = rec.get(f.name())
          Object val = convertValue(f.schema(), raw)
          columns.get(f.name()).add(val)
        }
      }

      return Matrix.builder(name).columns(columns).build()
    } finally {
      dfs.close()
    }
  }

  /** Convert an Avro-typed value to a suitable Java value for Matrix. */
  private static Object convertValue(Schema schema, Object v) {
    if (v == null) return null

    // Unwrap UNIONs (commonly ["null", T])
    if (schema.getType() == Schema.Type.UNION) {
      Schema nonNull = schema.getTypes().stream()
          .filter(s -> s.getType() != Schema.Type.NULL)
          .findFirst().orElse(schema)
      return convertValue(nonNull, v)
    }

    // Logical types: switch on the NAME to avoid nested-class access issues
    def lt = schema.getLogicalType()
    if (lt != null) {
      // decimal needs instanceof to read scale; the rest can switch on the name string
      if (lt instanceof LogicalTypes.Decimal) {
        return toBigDecimal((LogicalTypes.Decimal) lt, schema, v)
      }

      String name = lt.getName() // e.g. "date", "time-millis", "uuid", ...
      switch (name) {
        case "date":                    // int days since epoch
          return toLocalDate(v)
        case "time-millis":             // int millis since midnight
          return toLocalTimeMillis(v)
        case "time-micros":             // long micros since midnight
          return toLocalTimeMicros(v)
        case "timestamp-millis":        // long epoch millis UTC
          return toInstantMillis(v)
        case "timestamp-micros":        // long epoch micros UTC
          return toInstantMicros(v)
        case "local-timestamp-millis":  // long millis, no zone
          return toLocalDateTimeMillis(v)
        case "local-timestamp-micros":  // long micros, no zone
          return toLocalDateTimeMicros(v)
        case "uuid":
          return v.toString()           // or UUID.fromString(v.toString())
        default:
          // fall through to primitive/complex handling
          break
      }
    }

    switch (schema.getType()) {
      case Schema.Type.NULL:    return null
      case Schema.Type.BOOLEAN: return (Boolean) v
      case Schema.Type.INT:     return (Integer) v
      case Schema.Type.LONG:    return (Long) v
      case Schema.Type.FLOAT:   return (Float) v
      case Schema.Type.DOUBLE:  return (Double) v

      case Schema.Type.STRING:
        return (v instanceof Utf8) ? v.toString() : (String) v

      case Schema.Type.BYTES:
        return byteBufferToArray((ByteBuffer) v)

      case Schema.Type.FIXED:
        return (v as GenericFixed).bytes().clone()

      case Schema.Type.ENUM:
        return v.toString()

      case Schema.Type.ARRAY:
        Schema elem = schema.getElementType()
        List<?> list = (List<?>) v
        List<Object> out = new ArrayList<>(list.size())
        for (Object e : list) out.add(convertValue(elem, e))
        return out

      case Schema.Type.MAP:
        Schema vs = schema.getValueType()
        Map<Utf8, ?> m = (Map<Utf8, ?>) v
        Map<String, Object> outMap = new LinkedHashMap<>(m.size())
        for (Map.Entry<Utf8, ?> e : m.entrySet()) {
          outMap.put(e.getKey().toString(), convertValue(vs, e.getValue()))
        }
        return outMap

      case Schema.Type.RECORD:
        GenericRecord gr = (GenericRecord) v
        Map<String,Object> recMap = new LinkedHashMap<>(schema.getFields().size())
        for (Schema.Field f : schema.getFields()) {
          recMap.put(f.name(), convertValue(f.schema(), gr.get(f.name())))
        }
        return recMap

      default:
        return v
    }
  }

  private static byte[] byteBufferToArray(ByteBuffer buf) {
    ByteBuffer slice = buf.slice()
    byte[] exact = new byte[slice.remaining()]
    slice.get(exact)
    return exact
  }

  private static LocalDate toLocalDate(Object v) {
    int days = (v instanceof Integer) ? (Integer) v : ((Number) v).intValue()
    return LocalDate.ofEpochDay(days)
  }

  private static LocalTime toLocalTimeMillis(Object v) {
    long ms = (v instanceof Integer) ? ((Integer) v).longValue() : ((Number) v).longValue()
    return LocalTime.ofNanoOfDay(ms * 1_000_000L)
  }

  private static LocalTime toLocalTimeMicros(Object v) {
    long micros = ((Number) v).longValue()
    return LocalTime.ofNanoOfDay(micros * 1_000L)
  }

  private static Instant toInstantMillis(Object v) {
    long ms = ((Number) v).longValue()
    return Instant.ofEpochMilli(ms)
  }

  private static Instant toInstantMicros(Object v) {
    long micros = ((Number) v).longValue()
    long seconds = Math.floorDiv(micros, 1_000_000L)
    long nanos   = Math.floorMod(micros, 1_000_000L) * 1_000L
    return Instant.ofEpochSecond(seconds, nanos)
  }

  private static LocalDateTime toLocalDateTimeMillis(Object v) {
    long ms = ((Number) v).longValue()
    return LocalDateTime.ofEpochSecond(
        Math.floorDiv(ms, 1000L),
        (int)((ms % 1000L) * 1_000_000L),
        ZoneOffset.UTC
    )
  }

  private static LocalDateTime toLocalDateTimeMicros(Object v) {
    long micros = ((Number) v).longValue()
    long seconds = Math.floorDiv(micros, 1_000_000L)
    int nanos    = (int) (Math.floorMod(micros, 1_000_000L) * 1_000L)
    return LocalDateTime.ofEpochSecond(seconds, nanos, ZoneOffset.UTC)
  }

  private static BigDecimal toBigDecimal(LogicalTypes.Decimal dec, Schema schema, Object v) {
    int scale = dec.getScale()
    byte[] bytes
    if (schema.getType() == Schema.Type.BYTES) {
      bytes = byteBufferToArray((ByteBuffer) v)
    } else if (schema.getType() == Schema.Type.FIXED) {
      bytes = ((GenericFixed) v).bytes()
    } else {
      throw new IllegalArgumentException("Decimal logical type on non-bytes/fixed field")
    }
    return new BigDecimal(new BigInteger(bytes), scale)
  }
}
